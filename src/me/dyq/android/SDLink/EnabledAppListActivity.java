package me.dyq.android.SDLink;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.dyq.android.SDLink.SettingValueClass.AppValue;
import me.dyq.android.SDLink.SettingValueClass.hookType;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;
import android.widget.Toast;

public class EnabledAppListActivity extends Activity {
	
	private SettingHandler hdl;
	private AppSettingHandler apphdl;
	private List<Map<String,Object>> viewlist = new ArrayList<Map<String,Object>>();

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_enabled_app_list);
		this.hdl = new SettingHandler(this.getSharedPreferences("Setting", MODE_WORLD_READABLE));
		this.apphdl = new AppSettingHandler();
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume()
	{
		super.onResume();
		this.viewlist.clear();
		this.apphdl.loadSetting(this.getSharedPreferences("PerAppSetting", MODE_WORLD_READABLE));
		List<PackageInfo> pkginfo = this.getPackageManager().getInstalledPackages(0);
		for(Entry<String,AppSettingModel> entry: this.apphdl.getAppSettings().entrySet() )
		{
			String pkgname = entry.getKey();
			boolean found = false;
			AppSettingModel setm = this.apphdl.getAppSetting(pkgname);
			for(PackageInfo tpkg: pkginfo)
			{
				
				if(tpkg.packageName.equalsIgnoreCase(pkgname))
				{
					
					/*EnabledAppModel eam = new EnabledAppModel(pkgname,
							title,
							tpkg.applicationInfo.loadIcon(this.getPackageManager()).getCurrent(),
							this.getDescriptionString(setm));*/
					Map<String,Object> item = new HashMap<String,Object>();
					item.put("pkgname", pkgname);
					item.put("name", tpkg.applicationInfo.loadLabel(this.getPackageManager()).toString());
					item.put("icon", tpkg.applicationInfo.loadIcon(this.getPackageManager()).getCurrent());
					item.put("desc", this.getDescriptionString(setm));
					this.viewlist.add(item);
					found = true;
					break;
				}
				/*String appname = pkg.applicationInfo.loadLabel(this.getPackageManager()).toString();
				String pkgname = pkg.packageName;*/
			}
			if(!found)
			{
				Map<String,Object> item = new HashMap<String,Object>();
				item.put("pkgname", pkgname);
				item.put("name", "未知程序");
				item.put("icon", this.getResources().getDrawable(R.drawable.icon_null));
				item.put("desc", this.getDescriptionString(setm));
				this.viewlist.add(item);
			}
			
		}
		
		SimpleAdapter adapter = new SimpleAdapter(this, this.viewlist, R.layout.item_enabled_app_list_layout, 
				new String[]{"pkgname","name","icon","desc"}, 
				new int[]{R.id.enabledapp_pkgname, R.id.enabledapp_appname,R.id.enabledapp_icon,R.id.enabledapp_desc});
		adapter.setViewBinder(new ViewBinder()
			{
	
				@Override
				public boolean setViewValue(View view, Object data,
						String textRepresentation) {
					if(view instanceof ImageView && data instanceof Drawable)
					{
						((ImageView) view).setImageDrawable((Drawable)data);
						return true;
					}
					return false;
				}
				
			});
		ListView lv = (ListView) this.findViewById(R.id.activity_enabled_list);
		lv.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				String pkgname = ((TextView)view.findViewById(R.id.enabledapp_pkgname)).getText().toString();
				
				Intent i = new Intent(parent.getContext(),AppSettingActivity.class);
				
				if(pkgname != null && !pkgname.isEmpty()) i.putExtra("pkgname", pkgname);
				parent.getContext().startActivity(i);
				
				//Log.i("debug", "text="+pkgname);
				/*Intent i=new Intent();
				i.putExtra("pkgname", pkgname);
				This.setResult(1, i);
				This.finish();*/
			}
			
		});
		lv.setAdapter(adapter);
	}
	
	private String getDescriptionString(AppSettingModel model)
	{
		StringBuilder desc = new StringBuilder();
		desc.append(AppValue.getDescription(model.value)).append(" , ").append(hookType.getDescription(model.hooktype));
		if(model.hooktype == hookType.MODE_DEFAULT) desc.append("(").append(hookType.getDescription(this.hdl.getDefaultHookType())).append(")");
		if(model.value == AppValue.SELECT_PATH)
		{
			desc.append(": ").append(model.customPath);
		}
		desc.append(" , 排除: ");
		if(model.ExcludeDir != null && model.ExcludeDir.size() > 0) for(String s: model.ExcludeDir)
		{
			desc.append(s).append("; ");
		}
		return desc.toString();
	}
	
	public class EnabledAppModel
	{
		public EnabledAppModel(String pkgname, String name, Drawable icon, String desc)
		{
			this.pkgname = pkgname;
			this.name = name;
			this.icon = icon;
			this.description = desc;
		}
		
		public String pkgname;
		public String name;
		public Drawable icon;
		public String description;
	}
	
	public void buttonAdd(View view)
	{
		Intent i = new Intent(this,AppSettingActivity.class);
		
		EditText pkgtext = (EditText) this.findViewById(R.id.activity_enabled_editpkg);
		String pkgname = pkgtext.getText().toString();
		if(pkgname == null || pkgname.isEmpty())
		{
			Toast.makeText(this, "包名不能为空", Toast.LENGTH_SHORT).show();
			return;
		}
		i.putExtra("pkgname", pkgname);
		
		this.startActivity(i);
	}
	
	public void buttonBrowse(View view)
	{
		Intent i = new Intent(this,AppListActivity.class);
		this.startActivityForResult(i, 1);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(requestCode == 1 && resultCode == 1)
		{
			String pkgname = data.getStringExtra("pkgname");
			if(pkgname != null ) ((EditText)this.findViewById(R.id.activity_enabled_editpkg)).setText(pkgname);
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == android.R.id.home) {
			this.finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
