//////////////////////////////////////////////////////////////////
//Creat a sidepanel in Pathvisio for the metabolite information.//
//////////////////////////////////////////////////////////////////

package metaboliteplugin;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;


public class MetaboliteSidePanel implements Plugin
{
	private PvDesktop desktop;

	public void init(PvDesktop desktop)
	{
		this.desktop = desktop;
			
		// create a new panel to show in the side bar
		JPanel MetaboliteSideBarPanel = new JPanel ();
		MetaboliteSideBarPanel.setLayout (new BorderLayout());
		MetaboliteSideBarPanel.add (new JLabel ("Metabolite information will be shown here."), BorderLayout.PAGE_START);

		// get a reference to the sidebar
		JTabbedPane sidebarTabbedPane = desktop.getSideBarTabbedPane();

		// add or panel with a given Title
		sidebarTabbedPane.add("Metabolite Information", MetaboliteSideBarPanel);
				}

	public void done() {}
}