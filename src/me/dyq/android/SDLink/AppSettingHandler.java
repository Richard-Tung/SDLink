package me.dyq.android.SDLink;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;

import android.content.SharedPreferences;

public class AppSettingHandler {
	
	//private static final int DefaultSetting=AppValue.NOT_USE;
	
	
	public AppSettingHandler()
	{
		
	}
	
	private HashMap<String,AppSettingModel> appsetting=new HashMap<String,AppSettingModel>();//package name, setting model
	
	public void loadSetting(SharedPreferences sett)
	{
		appsetting.clear();
		Set<String> applist = sett.getStringSet("applist", null);
		if(applist == null) applist = new HashSet<String>();
		if(!applist.isEmpty())
		{
			for(String pn:applist)
			{
				AppSettingModel m = new AppSettingModel();
				m.value = sett.getInt("perapp-"+pn+"-value", AppValue.NOT_USE);
				m.customPath = sett.getString("perapp-"+pn+"-customPath", "");
				m.ExcludeDir = sett.getStringSet("perapp-"+pn+"-excludeDir", null);
				m.hooktype = sett.getInt("perapp-"+pn+"-hooktype", hookType.MODE_DEFAULT);
				if(m.ExcludeDir == null) m.ExcludeDir = new HashSet<String>();
				appsetting.put(pn, m);
			}
		}
		//this.DefaultSetting=sett.getInt("DefaultSetting", AppValue.NOT_USE);
	}
	
	public void saveSetting(SharedPreferences sett)
	{
		Set<String> applist = new HashSet<String>();//package name collection
		SharedPreferences.Editor editor = sett.edit();
		for(Entry<String, AppSettingModel> e: appsetting.entrySet())
		{
			String pn = e.getKey();
			AppSettingModel m = e.getValue();
			applist.add(pn);
			editor.putInt("perapp-"+pn+"-value", m.value);
			editor.putInt("perapp-"+pn+"-hooktype", m.hooktype);
			editor.putString("perapp-"+pn+"-customPath", m.customPath);
			if(m.ExcludeDir != null && m.ExcludeDir.size() > 0) editor.putStringSet("perapp-"+pn+"-excludeDir", m.ExcludeDir);
		}
		editor.putStringSet("applist", applist);
		editor.commit();
	}
	
	public AppSettingModel getAppSetting(String packageName)
	{
		AppSettingModel m = this.appsetting.get(packageName);
		if(m == null) 
		{
			m = new AppSettingModel();
			this.appsetting.put(packageName, m);
		}
		return m;
	}
	
	public HashMap<String,AppSettingModel> getAppSettings()
	{
		return this.appsetting;
	}
	
	public void setAppSetting(String pkgname, AppSettingModel m)
	{
		this.appsetting.put(pkgname, m);
	}
	
	public void removeAppSetting(String pkgname)
	{
		this.appsetting.remove(pkgname);
	}
}
