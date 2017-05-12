package me.dyq.android.SDLink;

import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;

public class AppSettingModel {
	
	public AppSettingModel(/*String pack*/)
	{
		//this.Package=pack;
	}

	//public final String Package;
	public int value = AppValue.NOT_USE;
	public int hooktype = hookType.MODE_DEFAULT;
	public String customPath;
	
	public Set<String> ExcludeDir;
}
