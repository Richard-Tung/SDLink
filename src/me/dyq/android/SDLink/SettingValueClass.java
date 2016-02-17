package me.dyq.android.SDLink;

public class SettingValueClass {

	public static class AppValue 
	{
		public static final int NOT_USE = 0;
		public static final int GLOBAL_SETTING = 1;
		public static final int SELECT_PATH = 2;
		
		public static String getDescription(int hookType)
		{
			if(hookType == NOT_USE) return "禁用";
			else if(hookType == GLOBAL_SETTING) return "开启(全局)";
			else if(hookType == SELECT_PATH) return "开启(自定义)";
			return "";
		}
	}
	
	public static class hookType 
	{
		public static final int MODE_DEFAULT = 0;
		public static final int MODE_ENHANCED = 1;
		public static final int MODE_COMPATIBILITY = 2;
		
		public static String getDescription(int hookType)
		{
			if(hookType == MODE_DEFAULT) return "默认";
			else if(hookType == MODE_ENHANCED) return "增强";
			else if(hookType == MODE_COMPATIBILITY) return "兼容";
			return "";
		}
	}
}
