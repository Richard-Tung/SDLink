package me.dyq.android.SDLink;

import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Environment;
import me.dyq.android.SDLink.SettingValueClass.hookType;

public class SettingHandler {
	
	private SharedPreferences sett;

	@SuppressLint("SdCardPath")
	public SettingHandler(SharedPreferences sett)
	{
		this.sett = sett;
		this.isEnable = sett.getBoolean("enable", false);
		this.setGlobalPath = sett.getString("global_path", "AppFile");
		this.defaultHookType = sett.getInt("defhooktype", hookType.MODE_ENHANCED); 
		this.sdpath = sett.getStringSet("sdpath", null);
		if(this.sdpath == null)
		{
			this.sdpath = new HashSet<String>();
			this.sdpath.add("/sdcard");
			this.sdpath.add(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
	}
	
	private boolean isEnable = false;
	private int defaultHookType = hookType.MODE_ENHANCED;
	private String setGlobalPath;
	private Set<String> sdpath;
	
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
	
	/*public String getGlobalAppPath(String packageName)
	{
		return this.setGlobalPath.replace("$package", packageName);
	}*/
	
	public Set<String> getSDPath()
	{
		return this.sdpath;
	}
	
	public void save()
	{
		SharedPreferences.Editor editor = this.sett.edit();
		editor.putBoolean("enable", this.isEnable);
		editor.putString("global_path", this.getGlobalPath());
		editor.putStringSet("sdpath", this.sdpath);
		editor.putInt("defhooktype", this.defaultHookType);
		editor.commit(); 
	}

}
