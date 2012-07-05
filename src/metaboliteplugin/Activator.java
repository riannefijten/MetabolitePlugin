package metaboliteplugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.pathvisio.desktop.plugin.Plugin;

public class Activator implements BundleActivator {

	 private MetaboliteInfo plugin;

	 	@Override
	 	public void start(BundleContext context) throws Exception {
	 		plugin = new MetaboliteInfo();
	 		context.registerService(Plugin.class.getName(), plugin, null);
	 	}

	 	@Override
	 	public void stop(BundleContext context) throws Exception {
	 		plugin.done();
	 	}


}
