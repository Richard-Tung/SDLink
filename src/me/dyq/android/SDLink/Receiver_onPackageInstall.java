package me.dyq.android.SDLink;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class Receiver_onPackageInstall extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Log.i("receiverdebug", "package installed "+intent.getDataString());
		Intent i = new Intent(context,AppSettingActivity.class);
		
		String intentdata = intent.getDataString();
		String pkgname = intentdata.substring( 8/*"package:".length()*/, intentdata.length());
		if(pkgname != null && !pkgname.isEmpty()) i.putExtra("pkgname", pkgname);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
		
		context.startActivity(i);
	}

}
