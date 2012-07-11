package metaboliteplugin;

import javax.swing.JTabbedPane;

import metaboliteplugin.MetaboliteInfo;

import org.pathvisio.desktop.PvDesktop;
import org.pathvisio.desktop.plugin.Plugin;

public class MetaboliteInfoPlugin implements Plugin
{
	MetaboliteInfo pane;
	
	public void init(PvDesktop standaloneEngine) 
	{
		pane = new MetaboliteInfo(standaloneEngine.getSwingEngine());
		JTabbedPane sidebarTabbedPane = standaloneEngine.getSideBarTabbedPane();
		sidebarTabbedPane.add(MetaboliteInfo.TITLE, pane);
		
		standaloneEngine.getSwingEngine().getEngine().addApplicationEventListener(pane);
	}

	public void done() {}

}