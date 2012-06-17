//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

public class MetaboliteInfoGUI implements Plugin
{
	private PvDesktop desktop;
	
	private final JLabel empty  = new JLabel(" ");
	
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
			HttpGet getInchi = new HttpGet(SERVICE + GetName() + "/stdinchi");
			
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
		return inchiInfo;
		}
	
	//Request InChI string from Cactus
	public static String InchiKey(){
		String inchiKeyInfo = null;
		try {
		//Set up connection and put InChI key into a string
			HttpClient httpclient = new DefaultHttpClient();
			HttpGet getInchiKey = new HttpGet(SERVICE + GetName() + "/stdinchikey");
			
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
		return inchiKeyInfo;
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

		return image;
	}

	public Component GeneralPanel(){

		String name = "Metabolite name: " + GetName() + "\n \n";
		String inchiKey = InchiKey();
		String inchi = Inchi() + "\n \n";
		String info = name + inchi + inchiKey;

		JLabel image = new JLabel(new ImageIcon(image()));
		
		JTextArea area = new JTextArea(info);
		area.setEditable(false); 
		area.setLineWrap(true); 
		area.setWrapStyleWord(true); 
		
		//Create panel for general information about the metabolite.
		JPanel GeneralPanel = new JPanel();
		GeneralPanel.setBackground(Color.WHITE);
		GeneralPanel.setLayout(new BoxLayout(GeneralPanel, BoxLayout.Y_AXIS));

		GeneralPanel.add(image);
		image.setAlignmentX(Component.LEFT_ALIGNMENT);
		GeneralPanel.add(area);
		area.setAlignmentX(Component.LEFT_ALIGNMENT);
		
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
		//scroller.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.LINE_START);
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
