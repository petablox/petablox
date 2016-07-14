package shord.analyses;

import stamp.app.Component;
import stamp.app.Data;
import stamp.app.IntentFilter;

import java.util.*;

public class SystemComponents
{
	//add android's components
	static void add(List<Component> comps)
	{
		Component gInstallAPK = new Component("INSTALL_APK");
		IntentFilter filter = new IntentFilter();
		Data data = new Data();
		data.mimeType = "application/vnd.android.package-archive";
		filter.addData(data);
		gInstallAPK.addIntentFilter(filter);
		comps.add(gInstallAPK);
	}

}