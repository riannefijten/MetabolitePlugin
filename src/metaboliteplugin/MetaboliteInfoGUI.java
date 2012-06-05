//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
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
	
	public static String Inchi(){
		String cid = "methane";
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet getInchi = new HttpGet("http://cactus.nci.nih.gov/chemical/structure/" + cid + "/stdinchikey");
		HttpResponse response = null;
		String inchiInfo = null;
		try {
			response = httpclient.execute(getInchi);
			HttpEntity entity = response.getEntity();
			inchiInfo = "InChI key of " + cid + ": " + (EntityUtils.toString(entity));
		} catch (ClientProtocolException e) {
			inchiInfo = "Exception occured. Request failed.";
		} catch (IOException e) {
			inchiInfo = "Exception occured. Request failed.";
		}
		return inchiInfo;
		}
		
	public Component sub1(){
		//Create sub-panel for structure image
		JPanel GeneralPanelSub2 = new JPanel();
		GeneralPanelSub2.add(new JLabel("Structure image"));
		GeneralPanelSub2.setBorder(BorderFactory.createLineBorder(Color.CYAN));
		return GeneralPanelSub2;
	}
	public Component sub2() {	//Create sub-panel for metabolite information
		
		JLabel label = new JLabel(Inchi());
		//Create panel for metabolite info
		JPanel GeneralPanelSub1 = new JPanel();
		GeneralPanelSub1.add(label);
		GeneralPanelSub1.setBorder(BorderFactory.createLineBorder(Color.CYAN));
		GeneralPanelSub1.setBackground(Color.white);
		
		//Set size of the panel to ensure enough room for both panels
		Dimension panelD = new Dimension(130, 100);  
		GeneralPanelSub1.setPreferredSize(panelD);
		
		return GeneralPanelSub1;
	}
	public Component GeneralPanel(){
		
		//Create panel for general information about the metabolite.
		JPanel GeneralPanel = new JPanel();
		GeneralPanel.setLayout(new BoxLayout(GeneralPanel, BoxLayout.X_AXIS));
		GeneralPanel.add(sub1()); //TODO instead of labels, implement CDK
		GeneralPanel.add(sub2());
		
		//TODO instead of labels, implement CDK
		GeneralPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
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
		scroller.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.LINE_START);
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
