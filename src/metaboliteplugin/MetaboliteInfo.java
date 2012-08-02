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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.bridgedb.bio.BioDataSource;
import org.pathvisio.core.ApplicationEvent;
import org.pathvisio.core.Engine;
import org.pathvisio.core.Engine.ApplicationEventListener;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.debug.Logger;
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

public class MetaboliteInfo extends JPanel implements SelectionListener, PathwayElementListener, ApplicationEventListener
{

	public static final String TITLE = "Metabolite";
	String test = "test";
		
		private Engine engine;
		private ExecutorService executor;
	
		PathwayElement input;
		final static int maxThreads = 1;
		volatile ThreadGroup threads;
		volatile Thread lastThread;
		
		private GdbManager gdbManager;
		
		public void setInput(final PathwayElement e) 
		{
			//System.err.println("===== SetInput Called ==== " + e);
			
			if(e == input) return; //Don't set same input twice
			
			//Remove pathwaylistener from old input
			if(input != null) input.removeListener(this);
			
			if(e == null || e.getObjectType() != ObjectType.DATANODE) {
				input = null;
			} else {
				input = e;
				input.addListener(this);
				doQuery();
			}
		}

		private void doQuery() 
		{
			
			currRef = input.getXref();
			
			//System.err.println("\tSetting input " + e + " using " + threads);
			//First check if the number of running threads is not too high
			//(may happen when many SelectionEvent follow very fast)

			if(threads == null || threads.isDestroyed()) {
				threads = new ThreadGroup("backpage-queries" + System.currentTimeMillis());
			}
			if(threads.activeCount() < maxThreads) {
					QueryThread qt = new QueryThread(input);
					qt.start();
					lastThread = qt;		
			} else {

				//When we're on our maximum, remember this element
				//and ignore it when a new one is selected
			}

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
						break; //Selects the last, TODO: use setGmmlDataObjects
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
			
		class QueryThread extends Thread {
			PathwayElement e;
			QueryThread(PathwayElement e) {
				super(threads, e.getGeneID() + e.hashCode());
				this.e = e;
			}
			public void run() {
				
				performTask();
				Info();
				moleculeImage();
				MSimages();
				NMRTables();
				
				if(this.equals(lastThread) && input != e) {

					e = input;
					
					performTask();
					Info();
					moleculeImage();
					MSimages();		
					NMRTables();
					
					lastThread = null;
				}

			}
			void performTask() 
			{
				// return unless we have a valid datanode.
				if (e == null) return;
				if (e.getObjectType() != ObjectType.DATANODE) return;
				Xref ref = e.getXref();
				IDMapper gdb = gdbManager.getCurrentGdb();
				try
				{
					Set<Xref> destrefs = gdb.mapID(ref, BioDataSource.HMDB);
					if (destrefs.size() > 0)
					{
						String HMDB = ref.getId();
						String smiles = Utils.oneOf (
								gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "SMILES"));
						String bruto = Utils.oneOf (
								gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "BrutoFormula"));
						String name = Utils.oneOf (
								gdbManager.getCurrentGdb().getAttributes (Utils.oneOf(destrefs), "Symbol"));
						if(input == e) {setHMDB(HMDB); setSMILES(smiles); setBruto(bruto); setSymbol(name);}
					}
				}
				catch (IDMapperException e)
				{
					Logger.log.error ("while getting cross refs", e);
				}
				
			}
		}
		
		
// Strings that will contain info such as HMDB ID and SMILES		
		String HMDB;
		String SMILES;
		String bruto;
		String symbol;
		String description;

		public String setHMDB(String newText) {
			HMDB = newText;
			System.out.println("HMDB: "+HMDB);
			return HMDB;		
		}
		
		public String setBruto(String newText) {
			bruto = newText;
			return bruto;
		}
		
		public String setSymbol(String newText) {
			symbol = newText;
			return symbol;
		}
		
		public String setSMILES(String newText) {
			SMILES = newText;
			return SMILES;
		}
			
//Set info for metabolite
		public void Info(){
			
			panel.removeAll();

			String inchi = null;
			String inchiKey= null;
			String name = null;
			String smiles = null;
			String brutoFormula = null;
			String HMDBid = null;
			
			HttpResponse rInchi = null;
			HttpResponse rInchiKey = null;
			
			//Create TextArea
			JTextArea generalKey = new JTextArea();
			generalKey.setEditable(false);
			generalKey.setLineWrap(true);
			generalKey.setWrapStyleWord(true);
			
			//HMDB ID
			HMDBid = setHMDB(HMDB);
			
			//metabolite name 
			name = setSymbol(symbol);
						
			//SMILES
			smiles = setSMILES(SMILES);
			
			//bruto formula
			brutoFormula = setBruto(bruto);
			
			//Generate inchi's			
			try {
				//Set up connection
				HttpClient httpclient = new DefaultHttpClient();
				
				HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + setSMILES(SMILES) + "/stdinchi");
				HttpGet getInchiKey = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + setSMILES(SMILES) + "/stdinchikey");
			
				//Generate Inchi ID
				rInchi = httpclient.execute(getInchi);
				HttpEntity entityInchi = rInchi.getEntity();
				inchi = EntityUtils.toString(entityInchi);
		
				//Generate InchiKey ID
				rInchiKey = httpclient.execute(getInchiKey);
				HttpEntity entityInchiKey = rInchiKey.getEntity();
				inchiKey = EntityUtils.toString(entityInchiKey);
			
				//Input info into JTextArea
				generalKey.setText(
						"HMDB ID: " + HMDBid + "\n"
						+ "Name: " + name + "\n" 
						+ inchiKey + "\n"
						+ inchi + "\n"
						+ "SMILES: " + smiles + "\n"
						+ "Bruto Formula: " + brutoFormula + "\n"
						);
				

			} catch (ClientProtocolException ClientException) {
				generalKey.setText("Inchi IDs could not be loaded. Please try again later");
			} catch (IOException IoException) {
				generalKey.setText("Inchi IDs could not be loaded. Please try again later");
			} catch (Throwable throwable) {
				generalKey.setText("Inchi IDs could not be loaded. Please try again later");
			}
			panel.add(generalKey);
		}
		
//Request structure image from Cactus
		public void moleculeImage(){
			URL imageUrl = null;
			try {
				imageUrl = new URL("http://cactus.nci.nih.gov/chemical/structure/" + setSMILES(SMILES) + "/image");
				Image image = ImageIO.read(imageUrl);
			
				panel.add(new JLabel(new ImageIcon(image)));
				
			} catch (MalformedURLException e) {
				panel.add(new JLabel("Image could not be loaded. Please try again later"));
			} catch (IOException e) {
				panel.add(new JLabel("Image could not be loaded. Please try again later"));
			}
			
		}
		
// Mass spectroscopy images
		public void MSimages(){
			
			Image imageLow = null;
			Image imageMed = null;
			Image imageHigh = null;
			
			URL imageUrlLow = null; 
			URL imageUrlMed = null;
			URL imageUrlHigh = null;
		//Set up connection
			try {
				imageUrlLow = new URL("http://www.hmdb.ca/labm/metabolites/" + setHMDB(HMDB) + "/ms/spectra/" + setHMDB(HMDB) + "L.png");
				imageUrlMed = new URL("http://www.hmdb.ca/labm/metabolites/" + setHMDB(HMDB) + "/ms/spectraM/" + setHMDB(HMDB) + "M.png");
				imageUrlHigh = new URL("http://www.hmdb.ca/labm/metabolites/" + setHMDB(HMDB) + "/ms/spectraH/" + setHMDB(HMDB) + "H.png");
			} catch (MalformedURLException e) {
				panel.add(new JLabel("Images could not be loaded. Please try again later"));
			}
		//Read image from url
			try {
			// Low energy image
				imageLow = ImageIO.read(imageUrlLow);
				
				final BufferedImage bufferedImageLow = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
		        final Graphics2D gLow = bufferedImageLow.createGraphics();

		        gLow.setComposite(AlphaComposite.Src);
		        gLow.drawImage(imageLow, 0, 0, 400, 500, null);
		        gLow.dispose();
		        
		        panel.add(new JLabel(new ImageIcon(bufferedImageLow)));
		        
		    //Medium energy image		        
		        imageMed = ImageIO.read(imageUrlMed);
		        
		        final BufferedImage bufferedImageMed = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
		        final Graphics2D gMed = bufferedImageLow.createGraphics();

		        gMed.setComposite(AlphaComposite.Src);
		        gMed.drawImage(imageLow, 0, 0, 400, 500, null);
		        gMed.dispose();
		        
		        panel.add(new JLabel(new ImageIcon(bufferedImageMed)));
		        
		    //High energy image
		        imageHigh = ImageIO.read(imageUrlHigh);
		        
		        final BufferedImage bufferedImageHigh = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
		        final Graphics2D gHigh = bufferedImageLow.createGraphics();

		        gHigh.setComposite(AlphaComposite.Src);
		        gHigh.drawImage(imageLow, 0, 0, 400, 500, null);
		        gHigh.dispose();
		        
		        panel.add(new JLabel(new ImageIcon(bufferedImageHigh)));
					
			} catch (IOException e) {
				panel.add(new JLabel("Images could not be loaded. Please try again later"));
			}
		
		}
//NMR peak lists	
		public void NMRTables(){
			String HNMR = null;
			String CNMR = null;
			HttpResponse responseH = null;
			HttpResponse responseC = null;
				
			JTextArea HC = new JTextArea();
			HC.setEditable(false);
			HC.setLineWrap(true);
			HC.setWrapStyleWord(false);
			
			try {
		//Set up connection
				HttpClient httpclient = new DefaultHttpClient();
				
				HttpGet getHNMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/" + setHMDB(HMDB) + "/chemical/pred_hnmr_peaklist/" + setHMDB(HMDB) + "_peaks.txt");
				HttpGet getCNMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/" + setHMDB(HMDB) + "/chemical/pred_cnmr_peaklist/" + setHMDB(HMDB) + "_peaks.txt");
				
				responseH = httpclient.execute(getHNMR);
				responseC = httpclient.execute(getCNMR);
				
			//Generate HNMR peak list
				HttpEntity entityH = responseH.getEntity();
				HNMR = EntityUtils.toString(entityH);
				
			//Generate CNMR peak list
				HttpEntity entityC = responseH.getEntity();
				CNMR = EntityUtils.toString(entityC);
			//Set peak lists to textarea
				HC.setText(CNMR + "\n" + HNMR);
				
			} catch (ClientProtocolException ClientException) {
				HC.setText("Peak lists could not be loaded. Please try again later");
			} catch (IOException IoException) {
				HC.setText("Peak lists could not be loaded. Please try again later");
			} catch (Throwable throwable) {
				HC.setText("Peak lists could not be loaded. Please try again later");
			}
			
			
			
			
			panel.add(HC);
		}
		
	
//Make panel, scrollpanel and swingengine for execution of the plugin		
		private static final long serialVersionUID = 1L;
		private final SwingEngine se;
		private final JPanel panel;	
		private final JScrollPane scroll;
		
		public MetaboliteInfo(SwingEngine se)
		{
			super();
			Engine engine = se.getEngine();
			engine.addApplicationEventListener(this);
			VPathway vp = engine.getActiveVPathway();
			if(vp != null) vp.addSelectionListener(this);
			
			this.gdbManager = se.getGdbManager();
			this.se = se;
			
			setLayout (new BorderLayout());
			
			panel = new JPanel();
			panel.setLayout(new GridLayout(0,1));
			panel.setBackground(Color.white);
		
			scroll = new JScrollPane(panel);
			add (scroll);			 
			
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


}
