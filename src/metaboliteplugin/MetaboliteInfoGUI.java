//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.formats.MDLV2000Format;
import org.openscience.cdk.templates.MoleculeFactory;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

public class MetaboliteInfoGUI implements Plugin
{
	private PvDesktop desktop;
	
	//String for Cactus service
	private static final String SERVICE = "http://cactus.nci.nih.gov/chemical/structure/";
	
	//Get the name of the metabolite.
	public static String GetName(){
		String name = "Glucose";
		return name; 
	}
	
	//Request InChI string from Cactus
	public static String Inchi(){
		String inchiInfo = null;
		try {
		//Set up connection and put InChI key into a string
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet getInchi = new HttpGet(SERVICE + GetName() + "/stdinchikey");
			
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
		System.out.println(inchiInfo);
		return inchiInfo;
		}
	
	//Request structure image from Cactus
	public Image image(){
		URL imageUrl = null;
		try {
			imageUrl = new URL(SERVICE + GetName() + "/image");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		Image image = Toolkit.getDefaultToolkit().createImage(imageUrl);

		System.out.println(image);
		return image;
	}

	public Component GeneralPanel(){
		
		//Create panel for general information about the metabolite.
		JPanel GeneralPanel = new JPanel();
		GeneralPanel.setBackground(Color.WHITE);
		GeneralPanel.add(new JLabel("Metabolite name: " + GetName()));		
		GeneralPanel.add(new JLabel(Inchi()));
		GeneralPanel.add(new JLabel(new ImageIcon(image())));
		
		return GeneralPanel;
	}
	
	public Component MSPanel(){
		
		//Create panel for MS data/information
		JPanel MSPanel = new JPanel();
		MSPanel.add(new JLabel("MS info")); //TODO instead of labels, implement MS data
		MSPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
		
		return MSPanel;
	}
	
	public Component NMRPanel(){
		
		//Create panel for NMR data/information
		JPanel NMRPanel = new JPanel();
		NMRPanel.add(new JLabel("NMR info")); //TODO instead of labels, implement NMR data
		NMRPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
		
		return NMRPanel;
	}
	
	public void init(PvDesktop desktop)
	{
		// TODO if datanode type equals metabolite{
				
		// create side bar
		JPanel scroller = new JPanel();
		
		scroller.setLayout (new BoxLayout(scroller, BoxLayout.PAGE_AXIS));
		scroller.setBackground(Color.white);
		scroller.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.PAGE_START);
		scroller.add(GeneralPanel());
		scroller.add(MSPanel());
		scroller.add(NMRPanel());
		
		// get a reference to the sidebar
		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
		
		// add side bar title
		sidebarTabbedPane.add("Metabolite Information", scroller);
		
	}

	@Override
	public void done() {
		// TODO Auto-generated method stub
		
	}

}
