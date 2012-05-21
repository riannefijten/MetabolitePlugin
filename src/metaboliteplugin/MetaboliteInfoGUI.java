//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

public class MetaboliteInfoGUI implements Plugin
{
	private PvDesktop desktop;

	public void init(PvDesktop desktop)
	{
		//Create sub-panel for general information about the metabolite.
		JPanel GeneralPanel = new JPanel();
			GeneralPanel.add(new JLabel("Structure Formula")); //TODO instead of labels, implement CDK
			GeneralPanel.add(new JLabel("Metabolite info")); //TODO instead of labels, implement CDK
			GeneralPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Create a sub-panel for MS data/information
		JPanel MSPanel = new JPanel();
			MSPanel.add(new JLabel("MS info")); //TODO instead of labels, implement MS data
			MSPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
		//Create a sub-panel for NMR data/information
		JPanel NMRPanel = new JPanel();
			NMRPanel.add(new JLabel("NMR info")); //TODO instead of labels, implement NMR data
			NMRPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
			
		// create side bar
		JPanel InfoPanel = new JPanel ();
		InfoPanel.setLayout (new BoxLayout(InfoPanel, BoxLayout.PAGE_AXIS));
		InfoPanel.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.PAGE_START);
		InfoPanel.add(GeneralPanel);
		InfoPanel.add(MSPanel);
		InfoPanel.add(NMRPanel);
		// get a reference to the sidebar
		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();
		// add side bar title
		sidebarTabbedPane.add("Metabolite Information", InfoPanel);
				}

	public void done() {}
}