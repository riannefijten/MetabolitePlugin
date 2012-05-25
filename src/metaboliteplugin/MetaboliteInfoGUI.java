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
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

public class MetaboliteInfoGUI implements Plugin
{
	private PvDesktop desktop;

	public void init(PvDesktop desktop)
	{
		//Create sub-panel for structure formula
		JPanel GeneralPanelSub1 = new JPanel();
			GeneralPanelSub1.add(new JLabel("Structure Formula"));
			GeneralPanelSub1.setBorder(BorderFactory.createLineBorder(Color.CYAN));
		//Create sub-panel for metabolite information
		JPanel GeneralPanelSub2 = new JPanel();
			GeneralPanelSub2.add(new JLabel("Metabolite info"));
			GeneralPanelSub2.setBorder(BorderFactory.createLineBorder(Color.CYAN));
		//Create panel for general information about the metabolite.
		JPanel GeneralPanel = new JPanel();
			GeneralPanel.setLayout(new BoxLayout(GeneralPanel, BoxLayout.X_AXIS));
			GeneralPanel.add(GeneralPanelSub1); //TODO instead of labels, implement CDK
			GeneralPanel.add(GeneralPanelSub2); //TODO instead of labels, implement CDK
			GeneralPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		//Create panel for MS data/information
		JPanel MSPanel = new JPanel();
			MSPanel.add(new JLabel("MS info")); //TODO instead of labels, implement MS data
			MSPanel.setBorder(BorderFactory.createLineBorder(Color.RED));
		//Create panel for NMR data/information
		JPanel NMRPanel = new JPanel();
			NMRPanel.add(new JLabel("NMR info")); //TODO instead of labels, implement NMR data
			NMRPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
			
		// create side bar
		JPanel InfoPanel = new JPanel ();
		JScrollPane scroller = new JScrollPane(InfoPanel);
		InfoPanel.setLayout (new BoxLayout(InfoPanel, BoxLayout.PAGE_AXIS));
		InfoPanel.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.LINE_START);
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