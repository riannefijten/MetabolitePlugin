//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.ImageView;
import javax.swing.text.html.HTMLEditorKit.Parser;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bridgedb.AttributeMapper;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.layout.StructureDiagramGenerator;

import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.BremserOneSphereHOSECodePredictor;
import org.openscience.cdk.tools.HOSECodeGenerator;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.model.DataNodeType;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.model.PathwayElementEvent;
import org.pathvisio.core.model.PathwayElementListener;
import org.pathvisio.core.util.Utils;
import org.pathvisio.core.view.GeneProduct;
import org.pathvisio.core.view.SelectionBox.SelectionEvent;
import org.pathvisio.core.view.SelectionBox.SelectionListener;
import org.pathvisio.core.view.VPathway;
import org.pathvisio.core.view.VPathwayElement;
import org.pathvisio.gui.SwingEngine;

public class MetaboliteInfo extends JEditorPane implements SelectionListener, PathwayElementListener, ApplicationEventListener
{

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////PLUGIN WORKING MECHANISM/////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static final String TITLE = "Metabolite";
		
	private Engine engine;
	private ExecutorService executor;

	PathwayElement input;
	
	final static int maxThreads = 1;
	volatile ThreadGroup threads;
	volatile Thread lastThread;
	
	private GdbManager gdbManager;
	private final SwingEngine se;

	public MetaboliteInfo(SwingEngine se)
	{
//		super();
		Engine engine = se.getEngine();
		engine.addApplicationEventListener(this);
		VPathway vp = engine.getActiveVPathway();
		if(vp != null) vp.addSelectionListener(this);
		this.se = se;
		this.gdbManager = se.getGdbManager();
		
		addHyperlinkListener(se);
		setEditable(false);
		setContentType("text/html");
	
		executor = Executors.newSingleThreadExecutor();

		//Workaround for #1313
		//Cause is java bug: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6993691
		setEditorKit(new HTMLEditorKit() {
			protected Parser getParser() {
				try {
					Class c = Class
							.forName("javax.swing.text.html.parser.ParserDelegator");
					Parser defaultParser = (Parser) c.newInstance();
					return defaultParser;
				} catch (Throwable e) {
				}
				return null;
			}
		});
	}
		
	public void setInput(final PathwayElement e) 
	{
		//System.err.println("===== SetInput Called ==== " + e);
		
		if(e == input) return; //Don't set same input twice
		
		//Remove pathwaylistener from old input
		if(input != null) input.removeListener(this);
		
		if(e == null || e.getObjectType() != ObjectType.DATANODE) {
			input = null;
			
		} 
		else {
			input = e;
			input.addListener(this);
			doQuery();
		}
	}

	private void doQuery() 
	{
		setText("Loading");
		currRef = input.getXref();
		
		executor.execute(new Runnable()
		{
			public void run()
			{
				if(input == null) return;
				final String txt = getContent(input);

				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						setText(txt);
						setCaretPosition(0); // scroll to top.
					}
				});
			}
		});
	}
			
	public void selectionEvent(SelectionEvent e) 
	{
		switch(e.type) {
		case SelectionEvent.OBJECT_ADDED:
			//Just take the first DataNode in the selection
			Iterator<VPathwayElement> it = e.selection.iterator();
			while(it.hasNext()) {
				VPathwayElement o = it.next();
				if(o instanceof GeneProduct) {
					setInput(((GeneProduct)o).getPathwayElement());
					break;
				}
			}
			break;
		case SelectionEvent.OBJECT_REMOVED:
			if(e.selection.size() != 0) break;
		case SelectionEvent.SELECTION_CLEARED:
			setInput(null);
			break;
		}
	}

	public void applicationEvent(ApplicationEvent e) {
		if(e.getType() == ApplicationEvent.Type.VPATHWAY_CREATED) {
			((VPathway)e.getSource()).addSelectionListener(this);
		}
	}
		
	Xref currRef;
	
	public void gmmlObjectModified(PathwayElementEvent e) {
		PathwayElement pe = e.getModifiedPathwayElement();
		if(input != null) {
			Xref nref = new Xref (pe.getGeneID(), input.getDataSource());
			if(!nref.equals(currRef)) 
			{
				doQuery();
			}				
		}
	}
	
	private boolean disposed = false;
	public void dispose()
	{
		assert (!disposed);
		engine.removeApplicationEventListener(this);
		VPathway vpwy = engine.getActiveVPathway();
		if (vpwy != null) vpwy.removeSelectionListener(this);
		executor.shutdown();
		disposed = true;
	}
	

	public void done() {}
		

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////PLUGIN CONTENT////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private StringBuilder builder = new StringBuilder();
	
	public String getContent(PathwayElement e){
		
		builder.setLength(0);
		Xref ref = e.getXref();
		IDMapper gdb = gdbManager.getCurrentGdb();		
		Set<Xref> destrefs = null;
		try {
			destrefs = gdb.mapID(ref, BioDataSource.HMDB);
		} catch (IDMapperException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		if (e.getDataNodeType().equals("Metabolite"))
		{			
			String HMDB = null;
			String smiles = null;
			String name = null;
					
			String xref = e.getXref().getId();
						
			if (destrefs.size() < 1){
				String str = "Please select a database";
				return str;
			}
			
			//TODO explain why nothing is shown when no database is selected.
			try
			{
				HMDB = ref.getId(); //TODO assuming that the given id is an HMDB id
				smiles = Utils.oneOf (
						gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "SMILES"));
				String bruto = Utils.oneOf (
						gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "BrutoFormula"));
				name = Utils.oneOf (
						gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "Symbol"));
				builder.append("<h3> General info: </h3>");
				builder.append("<table border=\"0\">");
				builder.append("<tr><td>Name: </td><td>" + name + "</td></tr>");
				builder.append("<tr><td>ID: </td><td>" + xref + "</td></tr>");
				builder.append("<tr><td>Molecular formula: </td><td>" + bruto + "</td></tr>");
				builder.append("<tr><td>SMILES: </td><td>" + smiles + "</td></tr>");
			}
			catch (IDMapperException ex)
			{
				Logger.log.error ("while getting cross refs", ex);
				System.out.println("IDMapperException");
			}
				
		//Inchi			
			String inchi = null;
			try {
			//Set up connection and put InChI key into a string
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/"
				+ smiles + "/stdinchi");
	
				HttpResponse response = null;
				response = httpclient.execute(getInchi);
	
				HttpEntity entity = response.getEntity();
				inchi = EntityUtils.toString(entity);
	
			} catch (ClientProtocolException ClientException) {
				System.out.println(ClientException.getMessage());
				ClientException.printStackTrace();
			} catch (IOException IoException) {
				System.out.println(IoException.getMessage());
				IoException.printStackTrace();
			} catch (Throwable throwable) {
				  System.out.println(throwable.getMessage());
				  throwable.printStackTrace();
			}
			inchi = inchi.replace("InChI=", "");
			builder.append("<tr><td> Inchi: </td><td>" + inchi + "</td></tr>");
			//Inchi Key
			String inchiKey = null;
			try {
			//Set up connection and put InChI key into a string
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet getInchiKey = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" 
						+ smiles + "/stdinchikey");
	
				HttpResponse response = null;
				response = httpclient.execute(getInchiKey);
	
				HttpEntity entity = response.getEntity();
				inchiKey = EntityUtils.toString(entity);
	
			} catch (ClientProtocolException ClientException) {
				System.out.println(ClientException.getMessage());
				ClientException.printStackTrace();
			} catch (IOException IoException) {
				System.out.println(IoException.getMessage());
				IoException.printStackTrace();
			} catch (Throwable throwable) {
				  System.out.println(throwable.getMessage());
				  throwable.printStackTrace();
			}
			inchiKey = inchiKey.replace("InChIKey=", "");
			builder.append("<tr><td> Inchi Key: </td><td>" + inchiKey + "</td></tr></table>");
	
			
			//MS images
			String urlLow = "http://www.hmdb.ca/labm/metabolites/" + HMDB + "/ms/spectra/" + HMDB + "L.png";
			String urlMed = "http://www.hmdb.ca/labm/metabolites/" + HMDB + "/ms/spectraM/" + HMDB + "M.png";
			String urlHigh = "http://www.hmdb.ca/labm/metabolites/" + HMDB + "/ms/spectraH/" + HMDB + "H.png";
			
			builder.append("<br /><h3> Mass spectroscopy images: </h3><p>");
			builder.append("<a href=\"" + urlLow + "\"> Low energy MS image </a><br />");
			builder.append("<a href=\"" + urlMed + "\"> Medium energy MS image </a><br />");
			builder.append("<a href=\"" + urlHigh + "\"> High energy MS image </a><br /></p>");
				
//			//NMR tables
//			System.out.println("builder before nmr: "+builder);
//			builder.append("<h3> NMR peak lists and images: </h3>");
//			
//			//1H NMR predicted spectra
//			
			//1H NMR spectrum image link
			String H1NMRLink = "http://www.hmdb.ca/labm/metabolites/" + HMDB + 
					"/chemical/pred_hnmr_spectrum/" + name + ".gif";
			H1NMRLink = "<a href=\"" + H1NMRLink + "\"> Spectrum image </a><br /><br />";
//		
//			//1H NMR peak list
//			String H1NMR = null;
//			System.out.println("builder before nmr2: "+builder);
//			try {
//				System.out.println("builder in try: "+builder);
//			//Set up connection and put InChI key into a string
//				HttpClient httpclient = new DefaultHttpClient();
//				HttpGet getH1NMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/"
//				+ HMDB + "/chemical/pred_hnmr_peaklist/" + HMDB + "_peaks.txt");
//	
//				HttpResponse response = null;
//				response = httpclient.execute(getH1NMR);
//	
//				HttpEntity entity = response.getEntity();
//				H1NMR = EntityUtils.toString(entity);
//				System.out.println("1: " + H1NMR);
//			} catch (ClientProtocolException ClientException) {
//				System.out.println("clientexception");
//				ClientException.printStackTrace();
//			} catch (IOException IoException) {
//				System.out.println("IOException");
//				IoException.printStackTrace();
//			} catch (Throwable throwable) {
//				  System.out.println("Throwable");
//				  throwable.printStackTrace();
//			}
//			H1NMR = H1NMR.replace("	", "</td><td>");
//			H1NMR = H1NMR.replace("\n", "</tr><tr>");
//			H1NMR = H1NMR.replace("<td></td>", "");
//			System.out.println("2: " + H1NMR);
//			H1NMR = "Peak list: <br /><table border=\"0\"><tr> " + H1NMR + "</tr></table>";
//			//TODO remove last column (the most right)
			builder.append("<sup>1</sup>H NMR peak list and image: <br />" +
					H1NMRLink /*+ H1NMR*/);
//			System.out.println("builder after 1h NMR"+builder);
//			
//			//13C NMR predicted spectra
//			
			//13C NMR spectrum image
			String C13NMRLink = "http://www.hmdb.ca/labm/metabolites/" + HMDB + 
					"/chemical/pred_cnmr_spectrum/" + name + "_C.gif";
			C13NMRLink = "<a href=\"" + C13NMRLink + "\"> Spectrum image </a><br /><br />";
//	
//			//13C NMR peak list
//			String C13NMR = null;
//			try {
//			//Set up connection and put InChI key into a string
//				HttpClient httpclient = new DefaultHttpClient();
//				HttpGet getC13NMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/"
//				+ HMDB + "/chemical/pred_cnmr_peaklist/" + HMDB + "_peaks.txt");
//				System.out.println("http://www.hmdb.ca/labm/metabolites/"
//				+ HMDB + "/chemical/pred_cnmr_peaklist/" + HMDB + "_peaks.txt");
//				HttpResponse response = null;
//				response = httpclient.execute(getC13NMR);
//	
//				HttpEntity entity = response.getEntity();
//				C13NMR = EntityUtils.toString(entity);
//	
//			} catch (ClientProtocolException ClientException) {
//				System.out.println(ClientException.getMessage());
//				ClientException.printStackTrace();
//			} catch (IOException IoException) {
//				System.out.println(IoException.getMessage());
//				IoException.printStackTrace();
//			} catch (Throwable throwable) {
//				  System.out.println(throwable.getMessage());
//				  throwable.printStackTrace();
//			}
//			C13NMR = C13NMR.replace("	", "</td><td>");
//			C13NMR = C13NMR.replace("\n", "</tr><tr>");
//			C13NMR = "Peak list: <br /><table border=\"0\"><tr> " + C13NMR + "</tr></table>";
//			//TODO remove last column (the most right)
			builder.append("<br /> <sup>13</sup>C NMR peak list and image: <br />" +
					C13NMRLink + /*C13NMR +*/ "</p>");
//			System.out.println("builder after nmr: "+builder);
			CreateAtomContainer(smiles);
			return builder.toString();
		}
		
		else {
			String str = "The plugin only works for metabolites";
			return str;
		}
	}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////GENERATE IATOMCONTAINER FOR METABOLITE/////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void CreateAtomContainer(String sm) {
		String text = sm;
		System.out.println("smilestext " + text);
		try
		{
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			IAtomContainer mContainer = sp.parseSmiles(text);
			System.out.println("iatomcontainer " + mContainer);

			HOSEGenerator(mContainer);		
		}
		catch (Exception e)
		{
			Logger.log.error ("Chempaint error", e);
		};
	}
	
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////NMR SHIFT CALCULATIONS/////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////
	protected transient HOSECodeGenerator hcg = new HOSECodeGenerator();
	protected transient BremserOneSphereHOSECodePredictor predictor = new BremserOneSphereHOSECodePredictor();	
	
	public void HOSEGenerator(IAtomContainer ac){
		IAtomContainer mc = ac;
		String hoseCode = null;
		double shift = 0;
		
		builder.append("<h3> Predicted NMR shifts: </h3>" +
				"<table border=\"0\"> " +
				"<tr><td> Atom Number </td><td> Shift</td></tr>");
		
		for (int f = 0; f < mc.getAtomCount(); f++) {
			try {
				hoseCode = hcg.getHOSECode(mc, mc.getAtom(f), 1);
				hoseCode = hcg.makeBremserCompliant(hoseCode);
				shift = predictor.predict(hoseCode);
				System.out.println(f + ": "+shift);
				
				IAtom atom = mc.getAtom(f);
				String Atom2 = atom.getSymbol();
				Double Atom3 = atom.getBondOrderSum();
				System.out.println(Atom2);
				System.out.println(Atom3);
				int g = f++;
				
				builder.append("<tr><td>" + g + "</td><td>" + shift + "</td></tr>");
				
			} catch (Throwable e) {
				System.out.println("Exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
		builder.append("</table>");
		
	}

	
	
	


}
