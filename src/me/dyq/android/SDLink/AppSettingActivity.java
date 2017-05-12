package me.dyq.android.SDLink;

import java.util.HashSet;
import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class AppSettingActivity extends Activity {

	@SuppressWarnings("unused")
	private SettingHandler hdl;
	private AppSettingHandler apphdl;
	private SharedPreferences sett;
	private SharedPreferences appsett;

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle para) 
	{
		super.onCreate(para);
		setContentView(R.layout.activity_app_setting);
		
		ActionBar bar = this.getActionBar();
		bar.setDisplayHomeAsUpEnabled(true);//back key
		bar.setIcon(this.getResources().getDrawable(R.drawable.icon_null));
		bar.setTitle("未知程序");
		
		
		
		
		//settings
		this.sett = this.getSharedPreferences("Setting", MODE_WORLD_READABLE);
		this.appsett = this.getSharedPreferences("PerAppSetting", MODE_WORLD_READABLE);
		//setting handlers
		this.hdl = new SettingHandler(this.sett);
		this.apphdl = new AppSettingHandler();
		this.apphdl.loadSetting(this.appsett);
		
		String pkgname = null;
		
		Bundle extra = this.getIntent().getExtras();
		if(extra != null)
		{
			
			pkgname = extra.getString("pkgname", "");
			AppSettingModel m = this.apphdl.getAppSetting(pkgname);
			int value = m.value;
			int hooktype = m.hooktype;
			String path = m.customPath;
			
			
			TextView textpkg = (TextView) this.findViewById(R.id.activity_appsetting_textpkg);
			EditText editPath = (EditText) this.findViewById(R.id.activity_appsetting_editpath);
			EditText editExclude = (EditText) this.findViewById(R.id.activity_appsetting_editExclude);
			RadioGroup setrg = (RadioGroup) this.findViewById(R.id.activity_appsetting_rg_set);
			RadioGroup hooktyperg = (RadioGroup) this.findViewById(R.id.activity_appsetting_rg_hooktype);
			textpkg.setText(pkgname);
			editPath.setText(path);
			/*if(value == AppValue.NOT_USE) ((RadioButton) this.findViewById(R.id.rb1)).setSelected(true);
			else if(value == AppValue.GLOBAL_SETTING) ((RadioButton) this.findViewById(R.id.rb2)).setSelected(true);
			else if(value == AppValue.SELECT_PATH) ((RadioButton) this.findViewById(R.id.rb3)).setSelected(true);*/
			if(value == AppValue.NOT_USE) setrg.check(R.id.activity_appsetting_set_rb1);
			else if(value == AppValue.GLOBAL_SETTING) setrg.check(R.id.activity_appsetting_set_rb2);
			else if(value == AppValue.SELECT_PATH) setrg.check(R.id.activity_appsetting_set_rb3);
			
			if(hooktype == hookType.MODE_DEFAULT) hooktyperg.check(R.id.activity_appsetting_hooktype_rb_default);
			else if(hooktype == hookType.MODE_ENHANCED) hooktyperg.check(R.id.activity_appsetting_hooktype_rb_enhanced);
			else if(hooktype == hookType.MODE_COMPATIBILITY) hooktyperg.check(R.id.activity_appsetting_hooktype_rb_compatibility);
			
		
			if(m.ExcludeDir != null)
			{
				StringBuilder dirsb = new StringBuilder();
				for(String p: m.ExcludeDir)
				{
					if(dirsb.length() != 0) dirsb.append("\n");
					dirsb.append(p);
				}
				editExclude.setText(dirsb.toString());
			}
			
			for(PackageInfo pkg: this.getPackageManager().getInstalledPackages(0))
			{
				if(pkg.packageName.equalsIgnoreCase(pkgname))
				{
					String appname = pkg.applicationInfo.loadLabel(this.getPackageManager()).toString();
					Drawable icon = pkg.applicationInfo.loadIcon(this.getPackageManager()).getCurrent();
					bar.setIcon(icon);
					bar.setTitle(appname);
					break;
				}
			}
			
		}
	}
	
	private void doSave()
	{
		String pkgname;
		int value = AppValue.NOT_USE;
		int hooktype = hookType.MODE_DEFAULT;
		String path;
		TextView textpkg = (TextView) this.findViewById(R.id.activity_appsetting_textpkg);
		EditText editPath = (EditText) this.findViewById(R.id.activity_appsetting_editpath);
		EditText editExclude = (EditText) this.findViewById(R.id.activity_appsetting_editExclude);
		
		pkgname = textpkg.getText().toString();
		path = editPath.getText().toString();
		
		RadioGroup setrg = (RadioGroup) this.findViewById(R.id.activity_appsetting_rg_set);
		RadioGroup hooktyperg = (RadioGroup) this.findViewById(R.id.activity_appsetting_rg_hooktype);
		int setChecked = setrg.getCheckedRadioButtonId();
		if( setChecked == R.id.activity_appsetting_set_rb1 ) value = AppValue.NOT_USE;
		else if( setChecked == R.id.activity_appsetting_set_rb2 ) value = AppValue.GLOBAL_SETTING;
		else if( setChecked == R.id.activity_appsetting_set_rb3 ) value = AppValue.SELECT_PATH;;
		
		int hooktypeChecked = hooktyperg.getCheckedRadioButtonId();
		if( hooktypeChecked == R.id.activity_appsetting_hooktype_rb_default ) hooktype = hookType.MODE_DEFAULT;
		else if( hooktypeChecked == R.id.activity_appsetting_hooktype_rb_enhanced ) hooktype = hookType.MODE_ENHANCED;
		else if( hooktypeChecked == R.id.activity_appsetting_hooktype_rb_compatibility ) hooktype = hookType.MODE_COMPATIBILITY;
		
		String[] exdirs;
		try
		{
			exdirs= editExclude.getText().toString().replaceAll("\r", "").split("\n");
		}
		catch(Exception e)
		{
			exdirs = null;
		}
		
		AppSettingModel m = this.apphdl.getAppSetting(pkgname);
		//m.Package = pkgname;
		m.value = value;
		m.customPath = path;
		m.hooktype = hooktype;
		
		Set<String> exdirset = new HashSet<String>();
		StringBuilder exsb = new StringBuilder();
		
		if(exdirs != null) for(String exdir: exdirs)
		{
			exdirset.add(exdir);
			exsb.append(exdir).append("; ");
		}
		m.ExcludeDir = exdirset;
		
		Log.i("debug", "package="+pkgname+" value="+m.value+" hooktype="+hooktype+" path="+m.customPath+" exdir="+exsb.toString());
		
		this.apphdl.setAppSetting(pkgname, m);
		this.apphdl.saveSetting(this.appsett);
		
		Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
		this.finish();
	}
	
	private void doDelete()
	{
		String pkgname = ((TextView) this.findViewById(R.id.activity_appsetting_textpkg)).getText().toString();
		this.apphdl.removeAppSetting(pkgname);
		this.apphdl.saveSetting(appsett);
		Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
		this.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.app_setting, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if(id == android.R.id.home)
		{
			this.finish();
			return true;
		}
		else if (id == R.id.menu_perapp_save) 
		{
			this.doSave();
			return true;
		}
		else if(id == R.id.menu_perapp_delete)
		{
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setMessage("确认删除吗");
			ab.setPositiveButton("确定", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
					AppSettingActivity.this.doDelete();
				}
				
			});
			ab.setNegativeButton("取消", new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					dialog.dismiss();
				}
				
			});
			ab.create().show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
