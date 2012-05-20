package metaboliteplugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator {

	 private ExSidepanel plugin;

	 	@Override
	 	public void start(BundleContext context) throws Exception {
	 		plugin = new ExSidepanel();
	 		context.registerService(Plugin.class.getName(), plugin, null);
	 	}

	 	@Override
	 	public void stop(BundleContext context) throws Exception {
	 		plugin.done();
	 	}


}
