package me.dyq.android.SDLink;

import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import me.dyq.android.SDLink.SettingValueClass.hookType;

public class SettingHandler {
	
	private SharedPreferences sett;
	
	private static final String tag = "SettingHandler";

	@SuppressLint("SdCardPath")
	public SettingHandler(SharedPreferences sett)
	{
		Log.i(tag, "do Constructor");
		this.sett = sett;
		this.isEnable = sett.getBoolean("enable", false);
		this.fixsdperm = sett.getBoolean("fixsdperm", false);
		this.fixsdperm6 = sett.getBoolean("fixsdperm6", false);
		this.setGlobalPath = sett.getString("global_path", "AppFile");
		this.defaultHookType = sett.getInt("defhooktype", hookType.MODE_ENHANCED); 
		this.sdpath = sett.getStringSet("sdpath", null);
		if(this.sdpath == null)
		{
			Log.i(tag, "no sdpath, create new");
			this.sdpath = new HashSet<String>();
			this.sdpath.add("/sdcard");
			this.sdpath.add(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
		Log.i(tag, "sdpath="+sdpath.toString());
		
	}
	
	private boolean isEnable = false;
	private int defaultHookType = hookType.MODE_ENHANCED;
	private String setGlobalPath;
	private Set<String> sdpath;
	private boolean fixsdperm = false;
	private boolean fixsdperm6 = false;
	
	public void setEnable(boolean value)
	{
		this.isEnable=value;
	}
	
	public boolean isEnable()
	{
		return this.isEnable;
	}
	
	public int getDefaultHookType()
	{
		return this.defaultHookType;
	}
	
	public void setDefaultHookType(int type)
	{
		this.defaultHookType = type;
	}
	
	public String getGlobalPath()
	{
		return this.setGlobalPath;
	}
	
	public boolean isFixSDPerm()
	{
		return this.fixsdperm;
	}
	
	public boolean isFixSDPerm6()
	{
		return this.fixsdperm6;
	}
	
	public void setFixSDPerm(boolean value)
	{
		this.fixsdperm = value;
	}
	
	public void setFixSDPerm6(boolean value)
	{
		this.fixsdperm6 = value;
	}
	
	/*public String getGlobalAppPath(String packageName)
	{
		return this.setGlobalPath.replace("$package", packageName);
	}*/
	
	public Set<String> getSDPath()
	{
		return this.sdpath;
	}
	
	public void setSDPath(Set<String> path)
	{
		this.sdpath = path;
	}
	
	public void save()
	{
		Log.i(tag, "do Save");
		SharedPreferences.Editor editor = this.sett.edit();
		editor.clear();
		editor.putBoolean("enable", this.isEnable);
		editor.putBoolean("fixsdperm", this.fixsdperm);
		editor.putBoolean("fixsdperm6", this.fixsdperm6);
		editor.putString("global_path", this.getGlobalPath());
		Log.i(tag, "sdpath="+this.sdpath.toString());
		editor.putStringSet("sdpath", this.sdpath);
		editor.putInt("defhooktype", this.defaultHookType);
		editor.commit();
	}

}
