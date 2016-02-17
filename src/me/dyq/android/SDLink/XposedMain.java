package me.dyq.android.SDLink;

import java.io.File;
import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;
import android.os.Environment;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedMain implements IXposedHookZygoteInit,IXposedHookLoadPackage {
	
	//public static final boolean DEBUG = false;
	public static final String unChangePrefix = "__dyq_unchange_";

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
	{
		final String packageName=lpparam.packageName;
		//global setting
		XSharedPreferences settpref=new XSharedPreferences("me.dyq.android.SDLink","Setting");
		final SettingHandler sethdl=new SettingHandler(settpref);
		//perappsetting
		XSharedPreferences appsettpref=new XSharedPreferences("me.dyq.android.SDLink","PerAppSetting");
		AppSettingHandler appsethdl=new AppSettingHandler();
		appsethdl.loadSetting(appsettpref);
		if(sethdl.isEnable())
		{
			//app setting
			AppSettingModel m=appsethdl.getAppSetting(packageName);
			
			//app redirect value
			int value = m.value;
			int hooktype = m.hooktype;
			if(hooktype == hookType.MODE_DEFAULT) hooktype = sethdl.getDefaultHookType();
			Set<String> exdirs = m.ExcludeDir;
			
			if(value == AppValue.GLOBAL_SETTING || value == AppValue.SELECT_PATH)
			{
				DebugLog("hook to app: "+packageName);
				//redirect to path
				String topath = null;
				
				//all sd path
				Set<String> allsdpath = sethdl.getSDPath();
				
				if(value == AppValue.GLOBAL_SETTING)
				{
					//use global setting
					topath = sethdl.getGlobalPath()+"/"+packageName;
					
					//make missing dirs
					for(String presd: allsdpath)
					{
						File hookPath = new File(fixPath(presd+"/"+topath));
						if(!hookPath.exists()) hookPath.mkdirs();
					}
				}
				else if(value == AppValue.SELECT_PATH)
				{
					//use app setting
					topath = m.customPath;
					
					//make missing dir
					File hookPath = new File(fixPath(topath));
					if(!hookPath.exists()) hookPath.mkdirs();
				}
				//do hook
				
				if(hooktype == hookType.MODE_ENHANCED)
				{
					this.doEnhancedHook(packageName,lpparam.classLoader,allsdpath,topath,exdirs);
				}
				else if(hooktype == hookType.MODE_COMPATIBILITY)
				{
					this.doCompatibilityHook(packageName,lpparam.classLoader,allsdpath,topath,exdirs);
				}
				
				
			};
				
				//XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, filehook);
		}
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable 
	{
		// TODO Auto-generated method stub
		
	}
	
	private void doEnhancedHook(final String pkgname, ClassLoader cl, final Set<String> allsdpath, final String topath, final Set<String> exdirs)
	{
		if(topath == null) return;//if hookpath is null return
		
		//hook File
		XC_MethodHook fileHook = new XC_MethodHook(){
			@Override
			protected void beforeHookedMethod(MethodHookParam param)
			{
				DebugLog("in EnhancedHook");
				String ufoldpath = (String) param.args[0];
				
				String newpath = ReplacePath(ufoldpath,allsdpath,exdirs,topath);
				if(newpath != null) param.args[0] = newpath;
			}
		};
		
		//hook File
		XposedHelpers.findAndHookConstructor("java.io.File", cl, String.class, fileHook);
		XposedHelpers.findAndHookConstructor("java.io.File", cl, String.class, String.class, fileHook);
		
		
		
		
		/*if(!rPath.startsWith("/"))//hookpath not a absolute path
		{
			
			XposedHelpers.findAndHookConstructor("android.os.storage.StorageVolume", cl, File.class, int.class, boolean.class, boolean.class,
		            boolean.class, int.class, boolean.class, long.class, UserHandle.class,
		            new XC_MethodHook(){
				@Override
				protected void afterHookedMethod(MethodHookParam param)
				{
					if(DEBUG == true) XposedBridge.log("in android.os.storage.StorageVolume.Constructor, dischange");
					File f = (File) XposedHelpers.getObjectField(param.thisObject, "mPath");
					XposedHelpers.setObjectField(param.thisObject, "mPath", cutHookedPath(f,rPath));
				}
			});
		}*/
		
		//================================================
		/*
		XC_MethodHook storagevolumehook = new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param)
			{
				DebugLog("StorageVolume: mPath.getPath()=");
				File of = (File) XposedHelpers.getObjectField(param.thisObject, "mPath");
				String ofs = of.getPath();
				DebugLog("StorageVolume: mPath.getPath()="+ofs);
				for(String sd : allsdpath)
				{
					if(ofs.contains(sd))
					{
						File nf = new File(unChangePrefix+sd);
						DebugLog("StorageVolume: set mPath="+sd);
					}
				}
			}
		};
		XposedHelpers.findAndHookConstructor("android.os.storage.StorageVolume", cl, Parcel.class, storagevolumehook);
		XposedHelpers.findAndHookConstructor("android.os.storage.StorageVolume", cl, File.class, int.class, boolean.class, boolean.class, boolean.class, int.class, boolean.class, long.class, UserHandle.class, storagevolumehook);
		*/
		//============================================
		this.fixStorageState(cl);
		
		this.doCompatibilityHook(pkgname, cl, allsdpath, topath, exdirs);
	}
	
	private void doCompatibilityHook(final String pkgname, ClassLoader cl, final Set<String> allsdpath, final String topath, final Set<String> exdirs)
	{
		if(topath == null) return;
		XC_MethodHook hookFileReturn = new XC_MethodHook(){
			@Override
			protected void afterHookedMethod(MethodHookParam param)
			{
				DebugLog("in CompatibilityHook");
				File f = (File)param.getResult();
				String newpath = ReplacePath(f.getAbsolutePath(),allsdpath,exdirs,topath);
				if(newpath != null) param.setResult(new File(newpath));
			}
		};
		XposedHelpers.findAndHookMethod(Environment.class, "getExternalStorageDirectory", hookFileReturn);
		XposedHelpers.findAndHookMethod(XposedHelpers.findClass("android.app.ContextImpl", cl),
				"getExternalFilesDir", String.class, hookFileReturn);
		XposedHelpers.findAndHookMethod(XposedHelpers.findClass("android.app.ContextImpl", cl), 
				"getObbDir", hookFileReturn);
		XposedHelpers.findAndHookMethod(Environment.class, 
				"getExternalStoragePublicDirectory", String.class, hookFileReturn);
	}
	
	//19
	private void fixStorageState(ClassLoader cl)
	{
		//hook Android Environment.getStorageState
		XC_MethodReplacement fixstorage = new XC_MethodReplacement(){
			@Override
			protected Object replaceHookedMethod(MethodHookParam param)
			{
				DebugLog("in android.os.Environment.getStorageState");
				File oldf = (File) param.args[0];
				//if(oldf.getAbsolutePath().contains(rPath))
				//{
					//File f = cutHookedPath((File) param.args[0], rPath);
					//param.args[0] = cutHookedPath(f,rPath);
					//old
					if(oldf.exists())
					{
						if(oldf.canRead())
						{
							if(oldf.canWrite())
							{
								return Environment.MEDIA_MOUNTED;
							}
							else
							{
								return Environment.MEDIA_MOUNTED_READ_ONLY;
							}
						}
					}
					return Environment.MEDIA_REMOVED;
					
					/*if(oldf.exists())
					{
						if(oldf.canRead())
						{
							if(oldf.canWrite())
							{
								param.setResult(Environment.MEDIA_MOUNTED);
								return;
							}
							else
							{
								param.setResult(Environment.MEDIA_MOUNTED_READ_ONLY);
								return;
							}
						}
					}
					param.setResult(Environment.MEDIA_UNMOUNTABLE);
					return;*/
					
				//}
			}
		};
		XposedHelpers.findAndHookMethod("android.os.Environment", cl, "getStorageState", File.class, fixstorage);
		XposedHelpers.findAndHookMethod("android.os.Environment", cl, "getExternalStorageState", File.class, fixstorage);
		
		
	}
	
	//21
	/*private void fixStorageState(ClassLoader cl)
	{
		
	}*/
	
	@SuppressWarnings("unused")
	private static File cutHookedPath(File f, String hookedPath)
	{
		String path = f.getAbsolutePath();
		if(path.contains(hookedPath))//path is hooked
		{
			String newpath = unChangePrefix + fixPath(path.replace(hookedPath, ""));
			File nf = new File( newpath );//return old sd path and add unchange prefix
			DebugLog("dischange path "+newpath);
			return nf;
		}
		return f;
	}
	
	private static String fixPath(String path)
	{
		String newPath = path;
		newPath = newPath.replaceAll("//", "/");
		//while (newPath.endsWith("/")) newPath = newPath.substring(0,newPath.length()-1);
		//XposedBridge.log("fixPath: old path="+path+" new path="+newPath);
		return newPath;
	}
	
	
	private static String getNewPath(String oldPath, String sdPath, String hookPath)
	{
		DebugLog("old path="+oldPath);
		String hookedPath;
		if(hookPath.startsWith("/"))//absolute path
		{
			//if path include redirect path then return
			if(oldPath.startsWith(hookPath)) return null;
			//if path is android default sd path then return
			//if(oldPath.startsWith(fixPath(sdPath+"/Android/data/"+pkgname))) return null;
			
			//cut subdir that app want to write to sdcard
			String subdir = oldPath.substring(sdPath.length(), oldPath.length());
			//if(subdir.startsWith("/")) subdir = subdir.substring(1,subdir.length());
			hookedPath = fixPath(hookPath + "/" + subdir);
		}
		else//else
		{
			//if path include redirect path then return
			if(oldPath.startsWith(fixPath(sdPath+"/"+hookPath))) return null;
			//if path is android default sd path then return
			//if(oldPath.startsWith(fixPath(sdPath+"/Android/data/"+pkgname))) return null;
			
			//cut subdir that app want to write to sdcard
			String subdir = oldPath.substring(fixPath(sdPath).length(),oldPath.length());
			//if(subdir.startsWith("/")) subdir = subdir.substring(1,subdir.length());
			hookedPath =  fixPath(sdPath + "/" + hookPath + "/" + subdir);
		}
		DebugLog("new path="+hookedPath);
		return hookedPath;
	}
	
	private static String ReplacePath(String oldpath, Set<String> allsdpath, Set<String> exdirs, String topath)
	//null = stock path
	{
		if(oldpath.startsWith(unChangePrefix))//find unchange prefix
		{
			String newpath = oldpath.substring(unChangePrefix.length(), oldpath.length());
			//param.args[0] = newpath;//dischange and return
			DebugLog("find unchange prefix, use stock path "+newpath);
			return newpath;
		}

		String fixoldpath = fixPath(oldpath);
		String thissd = null;
		for(String sdpath:allsdpath)
		{
			if(fixoldpath.startsWith(sdpath))//find sd path
			{
				
				thissd = sdpath;
				
				break;
			}
		}
		
		if(thissd == null) return null;//not in sd return
		
		//exdir
		if(exdirs != null && exdirs.size() != 0)
		{
			for(String ufexdir: exdirs)
			{
				if(ufexdir.equals("")) continue;//this exdir is empty, continue
				
				String exdir = fixPath(ufexdir);
				if(exdir.startsWith("/"))//absolute path
				{
					if(oldpath.startsWith(exdir))
					{
						DebugLog("in exclude dir "+exdir+" , return");
						return null;
					}
				}
				else//else
				{
					//if in exclude path return
					String prefix1 = thissd+"/"+exdir;
					if(fixoldpath.startsWith(prefix1+"/") || fixoldpath.equalsIgnoreCase(prefix1)) return null;
					//if in redirect path then force change to old path
					String prefix2 = thissd+"/"+topath+"/"+exdir;
					if(fixoldpath.startsWith(prefix2+"/") || fixoldpath.equalsIgnoreCase(prefix2)) 
					{
						String suffix = "";
						if(fixoldpath.length() > prefix2.length())
						{
							suffix = fixoldpath.substring(prefix2.length(), fixoldpath.length());
						}
						String newpath = thissd+"/"+exdir+suffix;
						//param.args[0] = newpath;
						DebugLog("in exclude dir "+exdir+" , set dir to "+newpath);
						return newpath;
					}
				}
			}
		}
		//exdir end
		
		String newpath = getNewPath(fixoldpath,thissd,topath);
		return newpath;
	}
	
	private static void DebugLog(String log)
	{
		if(isDebugger()) XposedBridge.log(log);
	}
	
	private static boolean isDebugger()//debug on/off
	{
		return false;
	}
		

}
