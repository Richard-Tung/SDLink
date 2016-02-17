package me.dyq.android.SDLink;

import java.util.Map.Entry;
import java.util.Set;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private SettingHandler hdl;
	private AppSettingHandler apphdl;
	private SharedPreferences sett;
	private SharedPreferences appsett;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		
	}
	
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume()
	{
		super.onResume();
		PackageManager pm = this.getPackageManager();
		
		//settings
		this.sett = this.getSharedPreferences("Setting", MODE_WORLD_READABLE);
		this.appsett = this.getSharedPreferences("PerAppSetting", MODE_WORLD_READABLE);
		//setting handlers
		this.hdl = new SettingHandler(this.sett);
		this.apphdl = new AppSettingHandler();
		this.apphdl.loadSetting(this.appsett);
		
		CheckBox cbenable = (CheckBox) this.findViewById(R.id.activity_main_checkEnabled);
		cbenable.setChecked(hdl.isEnable());
		
		CheckBox cbautosetting = (CheckBox) this.findViewById(R.id.activity_main_checkAutoSetting);
		cbautosetting.setChecked(pm.getComponentEnabledSetting(new ComponentName(this,Receiver_onPackageInstall.class)) == 
				PackageManager.COMPONENT_ENABLED_STATE_ENABLED ? true : false );
		
		RadioGroup hooktyperg = (RadioGroup) this.findViewById(R.id.activity_main_hooktype_rg);
		int hooktype = hdl.getDefaultHookType();
		if(hooktype == hookType.MODE_ENHANCED) hooktyperg.check(R.id.activity_main_hooktype_rb_enhanced);
		else if(hooktype == hookType.MODE_COMPATIBILITY) hooktyperg.check(R.id.activity_main_hooktype_rb_compatibility);
		
		EditText et = (EditText) this.findViewById(R.id.activity_main_editSDPath);
		StringBuilder sb = new StringBuilder();
		for(String p: this.hdl.getSDPath())
		{
			if(sb.length() != 0) sb.append("\n");
			sb.append(p);
		}
		et.setText(sb.toString());
		//Environment.getExternalStorageDirectory();

		
		
		TextView tv = (TextView) this.findViewById(R.id.activity_main_textSelected);
		this.apphdl.loadSetting(this.appsett);
		StringBuilder sb1 = new StringBuilder();
		for(Entry<String,AppSettingModel> e: this.apphdl.getAppSettings().entrySet())
		{
			AppSettingModel m = e.getValue();
			sb1.append(e.getKey()).append(": ").append(AppValue.getDescription(m.value)).append(" , ").append(hookType.getDescription(m.hooktype));
			if(m.hooktype == hookType.MODE_DEFAULT) sb1.append("(").append(hookType.getDescription(this.hdl.getDefaultHookType())).append(")");
			if(m.value == AppValue.SELECT_PATH)
			{
				sb1.append(": ").append(m.customPath);
			}
			sb1.append(" , 排除: ");
			if(m.ExcludeDir != null && m.ExcludeDir.size() > 0) for(String s: m.ExcludeDir)
			{
				sb1.append(s).append("; ");
			}
			sb1.append("\n");
		}
		String s = sb1.toString();
		if(s == null || s.isEmpty()) s = "无条目";
		tv.setText(s);
	}

	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.menu_main_about) 
		{
			Intent i = new Intent(this,AboutActivity.class);
			this.startActivity(i);
			return true;
		}
		else if(id == R.id.menu_main_setting)
		{
			Intent i = new Intent(this,EnabledAppListActivity.class);
			this.startActivity(i);
			return true;
		}
		else if(id == R.id.menu_main_save)
		{
			this.doSave();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void doSave()
	{
		PackageManager pm = this.getPackageManager();
		CheckBox cb = (CheckBox) this.findViewById(R.id.activity_main_checkEnabled);
		CheckBox cbautosetting = (CheckBox) this.findViewById(R.id.activity_main_checkAutoSetting);
		boolean isEnable = cb.isChecked();
		this.hdl.setEnable(isEnable);
		Log.i("debug", "enable="+isEnable);
		
		EditText et = (EditText) this.findViewById(R.id.activity_main_editSDPath);
		String[] pathList = et.getText().toString().replaceAll("\r", "").split("\n");
		Set<String> set = this.hdl.getSDPath();
		set.clear();
		for(String s: pathList)
		{
			if(s != null && !s.equals("")) set.add(s);
		}
		Log.i("debug", "sdlist="+set.toString());
		
		RadioGroup hooktyperg = (RadioGroup) this.findViewById(R.id.activity_main_hooktype_rg);
		int hooktypeChecked = hooktyperg.getCheckedRadioButtonId();
		int hooktype = hookType.MODE_ENHANCED;
		if( hooktypeChecked == R.id.activity_main_hooktype_rb_enhanced ) hooktype = hookType.MODE_ENHANCED;
		else if( hooktypeChecked == R.id.activity_main_hooktype_rb_compatibility ) hooktype = hookType.MODE_COMPATIBILITY;
		this.hdl.setDefaultHookType(hooktype);
		
		this.hdl.save();
		pm.setComponentEnabledSetting(new ComponentName(this,Receiver_onPackageInstall.class), 
				cbautosetting.isChecked() ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 
						PackageManager.DONT_KILL_APP);
		Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
	}

	
	
	/*public void selectEnabled(View view)
	{
		CheckBox cb = (CheckBox) view;
		boolean isEnable = cb.isChecked();
		Log.i("debug", "enable="+isEnable);
		this.hdl.setEnable(isEnable);
		this.hdl.save();
		Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
	}*/
	
}
