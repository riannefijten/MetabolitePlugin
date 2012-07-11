//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.Parser;

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
import org.pathvisio.core.debug.WorkerThreadOnly;
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
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;
import org.pathvisio.gui.BackpageTextProvider;
import org.pathvisio.gui.SwingEngine;

public class MetaboliteInfo extends JPanel implements SelectionListener, PathwayElementListener, ApplicationEventListener
{

	public static final String TITLE = "Metabolite";
		
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
			System.out.println("input: " + input);
			System.out.println("e: " + e);
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
				if(this.equals(lastThread) && input != e) {

					e = input;
					performTask();
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
						if(input == e) setText(HMDB);
					}
				}
				catch (IDMapperException e)
				{
					Logger.log.error ("while getting cross refs", e);
				}
				
			}
		}
		
		String text;
		
		private void setText(String newText) {
			text = newText;

		}
		
		private static final long serialVersionUID = 1L;
		private final SwingEngine se;
		private final JPanel panel;
		
		public MetaboliteInfo(SwingEngine se)
		{
			Engine engine = se.getEngine();
			engine.addApplicationEventListener(this);
			VPathway vp = engine.getActiveVPathway();
			if(vp != null) vp.addSelectionListener(this);
			
			this.gdbManager = se.getGdbManager();
			this.se = se;
			
			setLayout (new BorderLayout());

			panel = new JPanel();		
			add (panel, BorderLayout.CENTER);			 
			
		}
	
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////////
	

//	//Use multithreads
//	private PvDesktop desktop;
//
//	
//	//Get the name of the metabolite.
//	public static String GetName(){
//
//		String name = "Glucose";
//		return name; 
//	}
//	
//	//Request InChI string from Cactus
//	public static String Inchi(){
//		String inchiInfo = null;
//		try {
//		//Set up connection and put InChI key into a string
//			HttpClient httpclient = new DefaultHttpClient();
//			HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + GetName() + "/stdinchi");
//			
//			HttpResponse response = null;
//			response = httpclient.execute(getInchi);
//			
//			HttpEntity entity = response.getEntity();
//			inchiInfo = EntityUtils.toString(entity);
//		
//		} catch (ClientProtocolException ClientException) {
//			System.out.println(ClientException.getMessage());
//			ClientException.printStackTrace();
//		} catch (IOException IoException) {
//			System.out.println(IoException.getMessage());
//			IoException.printStackTrace();
//		} catch (Throwable throwable) {
//			  System.out.println(throwable.getMessage());
//			  throwable.printStackTrace();
//		}
//		return inchiInfo;
//		}
//	
//	//Request InChI string from Cactus
//	public static String InchiKey(){
//		String inchiKeyInfo = null;
//		try {
//		//Set up connection and put InChI key into a string
//			HttpClient httpclient = new DefaultHttpClient();
//			HttpGet getInchiKey = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + GetName() + "/stdinchikey");
//			
//			HttpResponse response = null;
//			response = httpclient.execute(getInchiKey);
//			
//			HttpEntity entity = response.getEntity();
//			inchiKeyInfo = EntityUtils.toString(entity);
//		
//		} catch (ClientProtocolException ClientException) {
//			System.out.println(ClientException.getMessage());
//			ClientException.printStackTrace();
//		} catch (IOException IoException) {
//			System.out.println(IoException.getMessage());
//			IoException.printStackTrace();
//		} catch (Throwable throwable) {
//			  System.out.println(throwable.getMessage());
//			  throwable.printStackTrace();
//		}
//		return inchiKeyInfo;
//	}
//	
////	public static String Pubchem(){
////		String cid = "5793";
////		String pubChemInchi = null;
////		try {
////		
////		HttpClient httpclient = new DefaultHttpClient();
////		HttpGet pubChemRequest = new HttpGet("http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid="
////				+ cid + "&disopt=SaveXML");
////		pubChemRequest.getAllHeaders();
////		System.out.println(pubChemRequest);
////
////		HttpResponse response = null;
////		response = httpclient.execute(pubChemRequest);
////		HttpEntity entity = response.getEntity();
////		pubChemInchi = EntityUtils.toString(entity);
////		System.out.println(pubChemInchi);
////		
////	} catch (ClientProtocolException ClientException) {
////		System.out.println(ClientException.getMessage());
////		ClientException.printStackTrace();
////	} catch (IOException IoException) {
////		System.out.println(IoException.getMessage());
////		IoException.printStackTrace();
////	} catch (Throwable throwable) {
////		  System.out.println(throwable.getMessage());
////		  throwable.printStackTrace();
////	}
////		return pubChemInchi;
////		
////	}
////	
//	public Image MSImageLow(){
//		URL imageUrl = null;
//		try {
//			imageUrl = new URL("http://www.hmdb.ca/labm/metabolites/" + HMDB + "/ms/spectra/" + HMDB + "L.png");
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		
//		Image image = null;
//		try {
//			image = ImageIO.read(imageUrl);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		final BufferedImage bufferedImage = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
//        final Graphics2D graphics2D = bufferedImage.createGraphics();
//
//        graphics2D.setComposite(AlphaComposite.Src);
//        graphics2D.drawImage(image, 0, 0, 400, 500, null);
//        graphics2D.dispose();
//		return bufferedImage;
//	}
//	
//	public Image MSImageMed(){
//		URL imageUrl = null;
//		try {
//			imageUrl = new URL("http://www.hmdb.ca/labm/metabolites/" 
//			+ HMDB + "/ms/spectraM/" + HMDB + "M.png");
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		
//		Image image = null;
//		try {
//			image = ImageIO.read(imageUrl);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//		final BufferedImage bufferedImage = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
//        final Graphics2D graphics2D = bufferedImage.createGraphics();
//
//        graphics2D.setComposite(AlphaComposite.Src);
//        graphics2D.drawImage(image, 0, 0, 400, 500, null);
//        graphics2D.dispose();
//		return bufferedImage;
//	}
//	
//	public static Image MSImageHigh(){
//		URL imageUrl = null;
//		try {
//			imageUrl = new URL("http://www.hmdb.ca/labm/metabolites/" 
//			+ HMDB + "/ms/spectraH/" + HMDB + "H.png");
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		
//		Image image = null;
//		try {
//			image = ImageIO.read(imageUrl);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		final BufferedImage bufferedImage = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
//        final Graphics2D graphics2D = bufferedImage.createGraphics();
//      
//        graphics2D.setComposite(AlphaComposite.Src);
//        graphics2D.drawImage(image, 0, 0, 400, 500, null);
//        graphics2D.dispose();
//        
//		return bufferedImage;
//	}
//	
//	public String h1NMR(){
//		String H1NMR = null;
//
//		try {
//		//Set up connection and put InChI key into a string
//			HttpClient httpclient = new DefaultHttpClient();
//			HttpGet getH1NMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/"
//			+ HMDB + "/chemical/pred_hnmr_peaklist/" + HMDB + "_peaks.txt");
//			
//			HttpResponse response = null;
//			response = httpclient.execute(getH1NMR);
//			
//			HttpEntity entity = response.getEntity();
//			H1NMR = EntityUtils.toString(entity);
//		
//		} catch (ClientProtocolException ClientException) {
//			System.out.println(ClientException.getMessage());
//			ClientException.printStackTrace();
//		} catch (IOException IoException) {
//			System.out.println(IoException.getMessage());
//			IoException.printStackTrace();
//		} catch (Throwable throwable) {
//			  System.out.println(throwable.getMessage());
//			  throwable.printStackTrace();
//		}
//		return H1NMR;
//		
//	}
//	
//	public String c13NMR(){
//		String C13NMR = null;
//		try {
//		//Set up connection and put InChI key into a string
//			HttpClient httpclient = new DefaultHttpClient();
//			HttpGet getC13NMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/"
//			+ HMDB + "/chemical/pred_cnmr_peaklist/" + HMDB + "_peaks.txt");
//			
//			HttpResponse response = null;
//			response = httpclient.execute(getC13NMR);
//			
//			HttpEntity entity = response.getEntity();
//			C13NMR = EntityUtils.toString(entity);
//		
//		} catch (ClientProtocolException ClientException) {
//			System.out.println(ClientException.getMessage());
//			ClientException.printStackTrace();
//		} catch (IOException IoException) {
//			System.out.println(IoException.getMessage());
//			IoException.printStackTrace();
//		} catch (Throwable throwable) {
//			  System.out.println(throwable.getMessage());
//			  throwable.printStackTrace();
//		}
//		return C13NMR;
//	}
//	//Request structure image from Cactus
//	public Image moleculeImage(){
//		URL imageUrl = null;
//		try {
//			imageUrl = new URL("http://cactus.nci.nih.gov/chemical/structure/" + GetName() + "/image");
//		} catch (MalformedURLException e) {
//			e.printStackTrace();
//		}
//		
//		Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);
//
//		return image;
//	}
//
//	public Component GeneralPanel(PathwayElement e){
//
//		String name = "Metabolite name: " + GetName() + "\n \n";
//		String inchiKey = InchiKey();
//		String inchi = Inchi() + "\n \n";
//		//String pubchem = Pubchem();
//		String info = name + inchi + inchiKey;
//
//		JLabel image = new JLabel(new ImageIcon(moleculeImage()));
//		
//		JTextArea area = new JTextArea(info);
//		area.setEditable(false); 
//		area.setLineWrap(true); 
//		area.setWrapStyleWord(true); 
//		
//		//Create panel for general information about the metabolite.
//		JPanel GeneralPanel = new JPanel();
//		GeneralPanel.setBackground(Color.WHITE);
//		GeneralPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//		GeneralPanel.setLayout(new BoxLayout(GeneralPanel, BoxLayout.Y_AXIS));
//		
//		GeneralPanel.add(area);
//		GeneralPanel.add(image);
//		System.out.println("hello");
//		
//		return GeneralPanel;
//	}
//	
//	public Component MSPanel(){
//
//		//Create panel for MS data/information
//		JPanel MSPanel = new JPanel();
//		MSPanel.add(new JLabel("MS info")); //TODO instead of labels, implement MS data
//		MSPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
//		MSPanel.setLayout(new BoxLayout(MSPanel, BoxLayout.Y_AXIS));
//		JPanel MSPanel2 = new JPanel();
//		MSPanel.add(MSPanel2);
//		MSPanel.add(new JLabel(new ImageIcon(MSImageLow())));
//		MSPanel.add(new JLabel(new ImageIcon(MSImageMed())));
//		MSPanel.add(new JLabel(new ImageIcon(MSImageHigh())));
//		
//		return MSPanel;
//	}
//	
//	public Component NMRPanel(){
//		
//		//Create panel for NMR data/information
//		JPanel NMRPanel = new JPanel();
//		NMRPanel.add(new JLabel("NMR info")); //TODO instead of labels, implement NMR data
//		NMRPanel.setLayout(new BoxLayout(NMRPanel, BoxLayout.Y_AXIS));
//		NMRPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
//		
//		JTextArea C13NMR = new JTextArea(c13NMR());
//			C13NMR.setEditable(false);
//			C13NMR.setLineWrap(true); 
//			C13NMR.setWrapStyleWord(true);
//		
//		JTextArea H1NMR = new JTextArea(h1NMR());
//			H1NMR.setEditable(false);
//			H1NMR.setLineWrap(true); 
//			H1NMR.setWrapStyleWord(true);
//			
//		NMRPanel.add(C13NMR);
//		NMRPanel.add(H1NMR);
//		return NMRPanel;
//	}
//	
//	public void init(PvDesktop desktop)
//	{
//		// TODO if datanode type equals metabolite{
//				
//		// create side bar
//		JPanel panel = new JPanel();
//		
//		panel.setLayout (new BoxLayout(panel, BoxLayout.PAGE_AXIS));
//		//panel.setBackground(Color.white);
//		panel.add(MSPanel());
//		panel.add(NMRPanel());
//		
//		// get a reference to the sidebar
//		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
//		JScrollPane jsp = new JScrollPane(panel);
//		// add side bar title
//		sidebarTabbedPane.add("Metabolite Information", jsp);
//		
//	}
//	
////	
////	private boolean disposed = false;
////	public void dispose()
////	{
////		assert (!disposed);
////		engine.removeApplicationEventListener(this);
////		VPathway vpwy = engine.getActiveVPathway();
////		if (vpwy != null) vpwy.removeSelectionListener(this);
////		executor.shutdown();
////		disposed = true;
////	}

	public void done() {}


}
