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
				MSimages();
				NMRTables();
				
				if(this.equals(lastThread) && input != e) {

					e = input;
					
					performTask();
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
						if(input == e) {setText(HMDB); setText2(smiles);}
					}
				}
				catch (IDMapperException e)
				{
					Logger.log.error ("while getting cross refs", e);
				}
				
			}
		}
		
		
// setText(text) will contain the HMDB ID.		
		String text;
		String text2;

		public String setText(String newText) {
			text = newText;
			System.out.println("HMDB: "+ text);
			return text;		
		}
		
		public String setText2(String newText) {
			text2 = newText;
			System.out.println("SMILES: "+ text2);
			return text2;
		}
				
		//Request InChI string from Cactus
		public void Inchi(){
			String inchiInfo = null;
			try {
			//Set up connection and put InChI key into a string
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + setText2(text2) + "/stdinchi");
				
				HttpResponse response = null;
				response = httpclient.execute(getInchi);
				
				HttpEntity entity = response.getEntity();
				inchiInfo = EntityUtils.toString(entity);
			
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
			
			panel.removeAll();
			panel.add(new JLabel(inchiInfo));
			}
		
		//Request InChI string from Cactus
		public void InchiKey(){
			String inchiKeyInfo = null;
			try {
			//Set up connection and put InChI key into a string
				HttpClient httpclient = new DefaultHttpClient();
				HttpGet getInchiKey = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + setText2(text2) + "/stdinchikey");
				
				HttpResponse response = null;
				response = httpclient.execute(getInchiKey);
				
				HttpEntity entity = response.getEntity();
				inchiKeyInfo = EntityUtils.toString(entity);
			
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
			panel.add(new JLabel(inchiKeyInfo));
		}
		
		//Request structure image from Cactus
		public void moleculeImage(){
			URL imageUrl = null;
			try {
				imageUrl = new URL("http://cactus.nci.nih.gov/chemical/structure/" + setText2(text2) + "/image");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
			Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);
	
			panel.add(new JLabel(new ImageIcon(image)));
		}
		
// Mass spectroscopy image low energy
		public void MSimages(){
			
			Image imageLow = null;
			Image imageMed = null;
			Image imageHigh = null;
			
			URL imageUrlLow = null; 
			URL imageUrlMed = null;
			URL imageUrlHigh = null;
		//Set up connection
			try {
				imageUrlLow = new URL("http://www.hmdb.ca/labm/metabolites/" + setText(text) + "/ms/spectra/" + setText(text) + "L.png");
				imageUrlMed = new URL("http://www.hmdb.ca/labm/metabolites/" + setText(text) + "/ms/spectraM/" + setText(text) + "M.png");
				imageUrlHigh = new URL("http://www.hmdb.ca/labm/metabolites/" + setText(text) + "/ms/spectraH/" + setText(text) + "H.png");
			} catch (MalformedURLException e) {
				System.out.println("Image could not be loaded. Please try again later");
				panel.add(new JLabel("Image could not be loaded. Please try again later"));
			}
		//Read image from url
			try {
				imageLow = ImageIO.read(imageUrlLow);
				imageMed = ImageIO.read(imageUrlMed);
				imageHigh = ImageIO.read(imageUrlHigh);
			} catch (IOException e) {
				System.out.println("Image could not be loaded. Please try again later");
				panel.add(new JLabel("Image could not be loaded. Please try again later"));
			}
		// Low energy image
			final BufferedImage bufferedImageLow = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
	        final Graphics2D gLow = bufferedImageLow.createGraphics();

	        gLow.setComposite(AlphaComposite.Src);
	        gLow.drawImage(imageLow, 0, 0, 400, 500, null);
	        gLow.dispose();
	        
	    //Medium energy image
	        final BufferedImage bufferedImageMed = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
	        final Graphics2D gMed = bufferedImageLow.createGraphics();

	        gMed.setComposite(AlphaComposite.Src);
	        gMed.drawImage(imageLow, 0, 0, 400, 500, null);
	        gMed.dispose();
	        
	    //High energy image
	        final BufferedImage bufferedImageHigh = new BufferedImage(400, 500, BufferedImage.TYPE_INT_RGB);
	        final Graphics2D gHigh = bufferedImageLow.createGraphics();

	        gHigh.setComposite(AlphaComposite.Src);
	        gHigh.drawImage(imageLow, 0, 0, 400, 500, null);
	        gHigh.dispose();
	        
	    //Add images to panel
	        panel.add(new JLabel(new ImageIcon(bufferedImageLow)));
	        panel.add(new JLabel(new ImageIcon(bufferedImageMed)));
	        panel.add(new JLabel(new ImageIcon(bufferedImageHigh)));
		}

		
		
		public void NMRTables(){
			String HNMR = null;
			String CNMR = null;
			HttpResponse responseH = null;
			HttpResponse responseC = null;
	
			try {
		//Set up connection
				HttpClient httpclient = new DefaultHttpClient();
				
				HttpGet getHNMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/" + setText(text) + "/chemical/pred_hnmr_peaklist/" + setText(text) + "_peaks.txt");
				HttpGet getCNMR = new HttpGet("http://www.hmdb.ca/labm/metabolites/" + setText(text) + "/chemical/pred_cnmr_peaklist/" + setText(text) + "_peaks.txt");
				
				responseH = httpclient.execute(getHNMR);
				responseC = httpclient.execute(getCNMR);
				
			//Generate HNMR peak list
				HttpEntity entityH = responseH.getEntity();
				HNMR = EntityUtils.toString(entityH);
				
			//Generate CNMR peak list
				HttpEntity entityC = responseH.getEntity();
				HNMR = EntityUtils.toString(entityC);

			
			} catch (ClientProtocolException ClientException) {
				System.out.println("Peak list could not be loaded. Please try again later");
				panel.add(new JLabel("Peak list could not be loaded. Please try again later"));
			} catch (IOException IoException) {
				System.out.println("Peak list could not be loaded. Please try again later");
				panel.add(new JLabel("Peak list could not be loaded. Please try again later"));
			} catch (Throwable throwable) {
				System.out.println("Peak list could not be loaded. Please try again later");
				panel.add(new JLabel("Peak list could not be loaded. Please try again later"));
			}

		//put peak lists into JTextAreas
			JTextArea H = new JTextArea(HNMR);
				H.setEditable(false);
			JTextArea C = new JTextArea(CNMR);
				C.setEditable(false);

		// Add TextAreas to panel
			panel.add(H);
			panel.add(C);
			
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
				
			scroll = new JScrollPane(panel);
			add (scroll, BorderLayout.CENTER);			 
			
		}
	

	public void done() {}


}
