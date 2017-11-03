package me.dyq.android.SDLink;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.util.ArrayMap;
import android.util.Log;
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
	public static boolean debug = false;
	
	protected XSettingHandler xsett;

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable 
	{
		final String packageName=lpparam.packageName;
		
		if(packageName.equals("me.dyq.android.SDLink"))//自己
		{
			DebugLog("in self");
			/*XC_MethodReplacement checkversion = new XC_MethodReplacement()
			{

				@Override
				protected Object replaceHookedMethod(MethodHookParam param)
						throws Throwable {
					DebugLog("set enabledversion = "+MainActivity.currentVersion);
					return MainActivity.currentVersion;
				}
				
			};
			
			XposedHelpers.findAndHookMethod(MainActivity.class, "getEnabledVersion", checkversion);*/
			XposedHelpers.findAndHookMethod("me.dyq.android.SDLink.MainActivity", lpparam.classLoader, "getEnabledVersion", 
					XC_MethodReplacement.returnConstant(MainActivity.currentVersion));
		}
		
		//global setting
		XSharedPreferences settpref=new XSharedPreferences("me.dyq.android.SDLink","Setting");
		final SettingHandler sethdl=new SettingHandler(settpref);
		
		if(sethdl.isFixSDPerm() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.M) 
		{
			DebugLog("enable fixsdperm");
			this.fixSDPermission(lpparam.packageName, lpparam.processName, lpparam.classLoader);
		}
		
		if(sethdl.isFixSDPerm6() && lpparam.packageName.equals("android") && lpparam.processName.equals("android") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		//if(sethdl.isFixSDPerm6() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			DebugLog("enable fixsdperm6");
			this.fixSDPermission6(lpparam.packageName, lpparam.processName, lpparam.classLoader);
		}
		
		
		//perappsetting
		XSharedPreferences appsettpref=new XSharedPreferences("me.dyq.android.SDLink","PerAppSetting");
		AppSettingHandler appsethdl=new AppSettingHandler();
		appsethdl.loadSetting(appsettpref);
		if(sethdl.isEnable())
		{
			//app setting
			AppSettingModel mSettingModel=appsethdl.getAppSetting(packageName);
			
			//app redirect value
			int value = mSettingModel.value;
			int hooktype = mSettingModel.hooktype;
			if(hooktype == hookType.MODE_DEFAULT) hooktype = sethdl.getDefaultHookType();
			//Set<String> exdirs = mSettingModel.ExcludeDir;
			
			if(value == AppValue.GLOBAL_SETTING || value == AppValue.SELECT_PATH) //已启用
			{
				DebugLog("hook to app: "+packageName);
				
				this.xsett=new XSettingHandler();
				xsett.exdirs.addAll(mSettingModel.ExcludeDir);//放入SD路径
				//redirect to path
				this.xsett.hookPath = null;//重定向路径
				
				//all sd path
				//Set<String> allsdpath = sethdl.getSDPath();
				this.xsett.allsdpath.addAll(sethdl.getSDPath());//所有SD路径
				
				if(value == AppValue.GLOBAL_SETTING)
				{
					//use global setting
					this.xsett.hookPath = sethdl.getGlobalPath()+"/"+packageName;
					
					//make missing dirs
					for(String presd: xsett.allsdpath)
					{
						File hookPath = new File(fixPath(presd+"/"+this.xsett.hookPath));
						if(!hookPath.exists())
						{
							try
							{
							hookPath.mkdirs();
							File nomedia = new File(hookPath,".nomedia");
							if(!nomedia.exists()) nomedia.createNewFile();
							} catch(Exception e) {XposedLog("[SDLink] unable to create folder for "+packageName);}
						}
					}
				}
				else if(value == AppValue.SELECT_PATH)
				{
					//程序独立设置
					this.xsett.hookPath = mSettingModel.customPath;
					
					//make missing dir
					File hookPath = new File(fixPath(this.xsett.hookPath));
					if(!hookPath.exists()) 
					{
						try
						{
						hookPath.mkdirs();
						File nomedia = new File(hookPath,".nomedia");
						if(nomedia.exists())nomedia.createNewFile();
						} catch(Exception e) {XposedLog("[SDLink] unable to create folder for "+packageName);}
					}
				}
				
				//添加Android文件夹排除
				xsett.exdirs.add("Android/data/"+packageName);
				xsett.exdirs.add("Android/obb/"+packageName);
				//do hook
				
				
				this.doHook(packageName,hooktype,lpparam.classLoader,xsett);
				
			};
				
				//XposedHelpers.findAndHookConstructor("java.io.File", lpparam.classLoader, String.class, filehook);
		}
	}
	
	private void doHook(final String pkgname,int hooktype, ClassLoader cl, XSettingHandler xsett)
	{
		if(hooktype == hookType.MODE_ENHANCED)
		{
			this.doEnhancedHook(pkgname,cl,xsett.allsdpath, xsett.hookPath, xsett.exdirs);
		}
		else if(hooktype == hookType.MODE_COMPATIBILITY)
		{
			this.doCompatibilityHook(pkgname,cl, xsett.allsdpath, xsett.hookPath, xsett.exdirs);
		}
	}
	
	private void fixSDPermission(String packageName, String processName, ClassLoader cl)
	{
		if(packageName.equals("android") && processName.equals("android"))
		{
			XC_MethodHook fixsdperm = new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param)
						throws Throwable {
					DebugLog("do fixsdperm");
					String permission = (String) param.args[1];
					if (permission.equals("android.permission.WRITE_EXTERNAL_STORAGE")
							|| permission.equals("android.permission.ACCESS_ALL_EXTERNAL_STORAGE") )
					{
						Class<?> process = XposedHelpers.findClass("android.os.Process", null);
						int gidsdrw = (Integer) XposedHelpers.callStaticMethod(process, "getGidForName", "sdcard_rw");
						int gidmediarw = (Integer) XposedHelpers.callStaticMethod(process, "getGidForName", "media_rw");
						Object permissions = null;
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) 
						{
							permissions = XposedHelpers.getObjectField(param.thisObject, "mPermissions");
						} 
						else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) 
						{
							Object settings = XposedHelpers.getObjectField(
									param.thisObject, "mSettings");
							permissions = XposedHelpers.getObjectField(settings,
									"mPermissions");
						}
						Object bp = XposedHelpers.callMethod(permissions, "get", permission);
						int[] bpGids = (int[]) XposedHelpers.getObjectField(bp, "gids");
						int[] newbpGids = appendInt(appendInt(bpGids, gidsdrw),gidmediarw);
						if(isDebugger())
						{
							StringBuilder sb = new StringBuilder();
							sb.append("old gid = ");
							for(int a:bpGids)
							{
								sb.append(a).append(",");
							}
							sb.append("\nnew gid = ");
							for(int a:newbpGids)
							{
								sb.append(a).append(",");
							}
							DebugLog(sb.toString());
						}
						XposedHelpers.setObjectField(bp, "gids", newbpGids);
					}
				}
			};
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				XposedHelpers.findAndHookMethod(
						XposedHelpers.findClass("com.android.server.SystemConfig", cl), "readPermission",
						XmlPullParser.class, String.class,
						fixsdperm);
			} else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT) {
				XposedHelpers.findAndHookMethod(
						XposedHelpers.findClass("com.android.server.pm.PackageManagerService", cl), "readPermission",
						XmlPullParser.class, String.class,
						fixsdperm);
			}
		
		
		
		
		}
		
		
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	public void fixSDPermission6(String packageName, String processName, final ClassLoader cl)
	{
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			//work but need improve
			/*XC_MethodHook fixperm = new XC_MethodHook()
			{
				@Override
				public void beforeHookedMethod(MethodHookParam para)
				{
					DebugLog("do fixsdperm6 at "+para.thisObject.toString());
					
					@SuppressWarnings("unchecked")
					android.util.ArrayMap<String, Object> mPermissions = (ArrayMap<String, Object>) XposedHelpers.getObjectField(para.thisObject, "mPermissions");
					Object bpWriteMedia = mPermissions.get(SystemValue.strWriteMedia);
					//Object bpWriteStorage = mPermissions.get("android.permission.WRITE_EXTERNAL_STORAGE");
					Object permState =  para.args[1];
						if ((Integer)XposedHelpers.callMethod(permState, "grantInstallPermission", bpWriteMedia) ==
	                            SystemValue.PERMISSION_OPERATION_FAILURE) {
	                            DebugLog("failed, did it already have permission?");
	                        //Slog.w(PackageManagerService.TAG, "Permission already added: " + name);
	                    } else {
	                    DebugLog("success, update");
	                        XposedHelpers.callMethod(permState, "updatePermissionFlags", bpWriteMedia, SystemValue.USER_ALL,
	                                SystemValue.MASK_PERMISSION_FLAGS, 0);
	                    }
					
				}
			};
			XposedHelpers.findAndHookMethod("com.android.server.pm.Settings", cl, "readInstallPermissionsLPr",
					XmlPullParser.class, XposedHelpers.findClass("com.android.server.pm.PermissionsState",cl),
					fixperm);*/
			XC_MethodHook fixperm = new XC_MethodHook()
			{
				@SuppressWarnings("unchecked")
				@Override
				public void beforeHookedMethod(MethodHookParam para)
				{
					DebugLog("do fixsdperm6 at "+para.thisObject.toString());
					
					Object pkg = para.args[0];
					
					Object mSettings = XposedHelpers.getObjectField(para.thisObject, "mSettings");
					android.util.ArrayMap<String, Object> mPermissions = (ArrayMap<String, Object>)XposedHelpers.getObjectField(mSettings, "mPermissions");
					
					Object bpWriteMedia = mPermissions.get(SystemValue.strWriteMedia);
					Object settingbase = XposedHelpers.getObjectField(pkg, "mExtras");
					Object permState = XposedHelpers.callMethod(settingbase, "getPermissionsState", new Object[]{});
					if(!(boolean)XposedHelpers.callMethod(permState, "hasInstallPermission", SystemValue.strWriteMedia))
					{
						DebugLog("no WriteMedia, fix"+para.thisObject.toString());
						XposedHelpers.callMethod(permState, "grantInstallPermission", bpWriteMedia);
					}
					
				}
			};
			XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService", cl, "grantPermissionsLPw",
					XposedHelpers.findClass("android.content.pm.PackageParser.Package", cl), boolean.class, String.class,
					fixperm);
			
			/*XC_MethodHook fixperm = new XC_MethodHook()
			{
				@Override
				public void afterHookedMethod(MethodHookParam para)
				{
					DebugLog("do fixsdperm6 at "+para.thisObject.toString());
					
					String pkgname = (String) para.args[0];
					String permname = (String) para.args[1];
					int userId = (int)para.args[2];
					
					if(!(permname.equals(SystemValue.strWriteExt) || permname.equals(SystemValue.strWriteAll) ) )
					{
						return;
					}
					
					Object mSettings = XposedHelpers.getObjectField(para.thisObject, "mSettings");
					
					@SuppressWarnings("unchecked")
					android.util.ArrayMap<String, Object> mPermissions = (ArrayMap<String, Object>) XposedHelpers.getObjectField(mSettings, "mPermissions");
					
					Object bpWriteMedia = mPermissions.get(SystemValue.strWriteMedia);
					if(bpWriteMedia == null)
					{
						DebugLog("Error: bpWriteMedia is null");
						return;
					}
					
					@SuppressWarnings("unchecked")
					ArrayMap<String, Object> mPackage = (ArrayMap<String, Object>) XposedHelpers.getObjectField(para.thisObject, "mPackages");
					synchronized(mPackage)
					{
						final Object pkg = mPackage.get(pkgname);
						if(pkg == null) 
						{
							DebugLog("no package "+pkgname);
							return;
						}
						Object settingbase = XposedHelpers.getObjectField(pkg, "mExtras");
						if(settingbase == null)
						{
							DebugLog("no package "+pkgname);
							return;
						}
						Object permState = XposedHelpers.callMethod(settingbase, "getPermissionsState", new Object[]{});
						
						if ((Integer)XposedHelpers.callMethod(permState, "grantInstallPermission", bpWriteMedia) ==
	                            SystemValue.PERMISSION_OPERATION_FAILURE) {
							DebugLog("failed, did it already have permission?");
	                        //Slog.w(PackageManagerService.TAG, "Permission already added: " + name);
	                    } else {
	                    	DebugLog("success, update");
	                        XposedHelpers.callMethod(permState, "updatePermissionFlags", bpWriteMedia, SystemValue.USER_ALL,
	                                SystemValue.MASK_PERMISSION_FLAGS, 0);
	                    }
					}
					
					//Object bpWriteStorage = mPermissions.get("android.permission.WRITE_EXTERNAL_STORAGE");
						
					
				}
			};
			XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService", cl, "grantRuntimePermission",
					String.class,String.class,int.class,
					fixperm);
			*/
			//dead
			/*
			XC_MethodHook hookPackageManagerService = new XC_MethodHook()
			{
				@Override 
				public void afterHookedMethod(MethodHookParam para)
				{
					Object mSettings = XposedHelpers.getObjectField(para.thisObject, "mSettings");
					Object mPermissions = XposedHelpers.getObjectField(mSettings, "mPermissions");
					XposedHelpers.setStaticObjectField(para.thisObject.getClass(), "mPermissionsStatic", mPermissions);
				}
			};
			XposedHelpers.findAndHookConstructor("com.android.server.pm.PackageManagerService", cl,
					Context.class, XposedHelpers.findClass("com.android.server.pm.Installer", cl), boolean.class, boolean.class,
					hookPackageManagerService);
			
			XC_MethodHook hookpermstate = new XC_MethodHook()
			{
				@Override
				public void afterHookedMethod(MethodHookParam para)
				{
					
					@SuppressWarnings("unchecked")
					android.util.ArrayMap<String, Object> mPermissions = (ArrayMap<String, Object>) XposedHelpers.getStaticObjectField(XposedHelpers.findClass("com.android.server.pm.PackageManagerService", cl), "mPermissionsStatic");
					//@SuppressWarnings("unchecked")
					//android.util.ArrayMap<String, Object> mPermissions = (ArrayMap<String, Object>) XposedHelpers.getObjectField(para.thisObject, "mPermissions");
					Object bpWriteMedia = mPermissions.get(SystemValue.strWriteMedia);
					//Object bpWriteExt = mPermissions.get(strWriteExt);
					//Object bpWriteAll = mPermissions.get(strWriteAll);
					
					DebugLog("do fixsdperm6 at "+para.thisObject.toString());
					Object argbp = para.args[0];
					String argpermname = (String) XposedHelpers.getObjectField(argbp, "name");
					int userId = (int) para.args[1];
					
					
					if(argpermname.equals(SystemValue.strWriteExt) || argpermname.equals(SystemValue.strWriteAll))
					{
						DebugLog("at WriteExt or WriteAll, do fix");
						//Method methodGrantPermission = XposedHelpers.findMethodBestMatch(para.thisObject.getClass(), "grantPermission", classbp, int.class);
						//int result = (int)XposedHelpers.callMethod(para.thisObject, "grantInstallPermission", bpWriteMedia);
						//int result = (int)methodGrantPermission.invoke(para.thisObject, bpWriteMedia, userId);
						int result = (int)XposedHelpers.callMethod(para.thisObject, "grantInstallPermission", bpWriteMedia);
						if(result == SystemValue.PERMISSION_OPERATION_FAILURE)
						{
							DebugLog("failed, did it already have permission?");
						}
						else
						{
							DebugLog("success, update");
							XposedHelpers.callMethod(para.thisObject, "updatePermissionFlags", 
									bpWriteMedia, userId, SystemValue.MASK_PERMISSION_FLAGS, 0);
						}
					}
					
				}
			};
			XposedHelpers.findAndHookMethod("com.android.server.pm.PermissionsState", cl, "grantPermission",
					XposedHelpers.findClass("com.android.server.pm.BasePermission", cl), int.class, 
					hookpermstate);*/
			
			/*
			XC_MethodHook fixperm = new XC_MethodHook(){
				@Override
				public void beforeHookedMethod(MethodHookParam para)
				{
					DebugLog("do fixsdperm6 at "+para.thisObject.toString());
					String[] perms = (String[])para.args[2];
					int flag = 0;
					for(String perm: perms)
					{
						if(perm.equals(SystemValue.strWriteExt) || perm.equals(SystemValue.strWriteAll) && flag != 2)
							flag = 1;
						if(perm.equals(SystemValue.strWriteMedia)) flag = 2;
					}
					if(flag == 1)
					{
						DebugLog("at WriteExt or WriteAll, do fix");
						String[] newperms = new String[perms.length+1];
						for(int i=0;i<perms.length;i++)
						{
							newperms[i] = perms[i];
						}
						newperms[newperms.length-1] = SystemValue.strWriteMedia;
						para.args[2] = newperms;
						DebugLog("now permissions="+newperms.toString());
						return;
					}
				}
			};
			XposedHelpers.findAndHookMethod("com.android.server.pm.PackageManagerService", cl, 
					"grantRequestedRuntimePermissions", 
					XposedHelpers.findClass("android.content.pm.PackageParser.Package", cl), int.class, String[].class,
					fixperm);*/
		}
	}
	
	public static int[] appendInt(int[] cur, int val) {
		if (cur == null) {
			return new int[] { val };
		}
		final int N = cur.length;
		for (int i = 0; i < N; i++) {
			if (cur[i] == val) {
				return cur;
			}
		}
		int[] ret = new int[N + 1];
		System.arraycopy(cur, 0, ret, 0, N);
		ret[N] = val;
		return ret;
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable 
	{
		// 
		
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
				if(f == null) return;
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
				DebugLog("File=" + oldf != null ? oldf.getAbsolutePath() : "null");
				
				if(oldf == null || oldf.getAbsolutePath().equals(""))
				{
					DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=null,MEDIA_MOUNTED");
					return Environment.MEDIA_MOUNTED;
				}
				//if(oldf.getAbsolutePath().contains(rPath))
				//{
					//File f = cutHookedPath((File) param.args[0], rPath);
					//param.args[0] = cutHookedPath(f,rPath);
					//old
					if(!oldf.exists())
					{
						try
						{
							oldf.mkdirs();
							if(!oldf.exists()) 
							{
								DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=CreateFailed,MEDIA_REMOVED");
								return Environment.MEDIA_REMOVED;
							}
						}
						catch(Exception e)
						{
							XposedBridge.log(e);
							DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=Exception,MEDIA_REMOVED");
							return Environment.MEDIA_REMOVED;
						}
					}
					
					if(oldf.canRead())
					{
						if(oldf.canWrite())
						{
							DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=RW,MEDIA_MOUNTED");
							return Environment.MEDIA_MOUNTED;
						}
						else
						{
							DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=RO,MEDIA_MOUNTED_READ_ONLY");
							return Environment.MEDIA_MOUNTED_READ_ONLY;
						}
					}
					DebugLogAlways("getExternalStorageState("+oldf.getAbsolutePath()+")=NotExist,MEDIA_REMOVED");
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
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)//5.0 and above
		{
			XposedHelpers.findAndHookMethod("android.os.Environment", cl, "getExternalStorageState", File.class, fixstorage);
		}
		
		
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
		DebugLog("old path="+oldPath+",sdpath="+sdPath+",hookPath="+hookPath);
		String hookedPath;
		if(hookPath.startsWith("/"))//absolute path
		{
			//if path include redirect path then return
			if(oldPath.toLowerCase(Locale.US).startsWith(hookPath.toLowerCase(Locale.US))) return null;
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
			if(oldPath.toLowerCase(Locale.US).startsWith(fixPath(sdPath+"/"+hookPath.toLowerCase(Locale.US)))) return null;
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
		if(oldpath == null) return null;
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
	
	@SuppressWarnings("unused")
	private static final class SystemValue
	{
	    /** The permission operation failed. */
	    public static final int PERMISSION_OPERATION_FAILURE = -1;

	    /** The permission operation succeeded and no gids changed. */
		public static final int PERMISSION_OPERATION_SUCCESS = 0;

	    /** The permission operation succeeded and gids changed. */
	    public static final int PERMISSION_OPERATION_SUCCESS_GIDS_CHANGED = 1;
	    
	    public static final int MASK_PERMISSION_FLAGS = 0xFF;
	    
	    public static final int USER_OWNER = 0;
	    
	    public static final int USER_ALL = -1;
	    
	    public static final int USER_CURRENT = -2;
	    
	    public static final String strWriteMedia = "android.permission.WRITE_MEDIA_STORAGE";
	    public static final String strWriteExt = "android.permission.WRITE_EXTERNAL_STORAGE";
	    public static final String strWriteAll = "android.permission.ACCESS_ALL_EXTERNAL_STORAGE";
	}
	
	private static void DebugLog(String log)
	{
		//if(isDebugger()) XposedBridge.log(log);
		if(isDebugger()) Log.v("Xposed.SDLink", log);
	}
	
	private static void DebugLogAlways(String log)
	{
		Log.v("Xposed.SDLink", log);
	}
	
	private static void XposedLog(String log)
	{
		XposedBridge.log(log);
	}
	
	private static boolean isDebugger()//debug on/off
	{
		return debug;
	}
		
	private class XSettingHandler
	{
		
		public Set<String> allsdpath=new HashSet<String>();
		public Set<String> exdirs=new HashSet<String>();
		public String hookPath;
	}

}
