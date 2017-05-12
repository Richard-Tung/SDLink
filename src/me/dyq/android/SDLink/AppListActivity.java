package me.dyq.android.SDLink;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AppListActivity extends Activity {

	Activity This;
	AppListAdapter adapter;
	
	/*
	 * first 第一个字符串
	 * second 第二个字符串
	 * 返回
	 * true: second在前
	 * false: first在前
	 */
	/*private boolean sortText(String first, String second)
	{
		if(first == null) return false;
		if(second == null) return true;

		for(int i=0;i<first.length();i++)
		{
			if(second.length()-1< i) return true;
			int fir = first.codePointAt(i);
			int sec = second.codePointAt(i);
			if(fir < sec) return false;
			else if( fir > sec) return true;
			else continue;
		}
		return false;
	}*/

	
	@SuppressLint("DefaultLocale")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_list);
		
		this.getActionBar().setDisplayHomeAsUpEnabled(true);
		
		List<PackageInfo> pkginfo = this.getPackageManager().getInstalledPackages(0);
		
		//List<Map<String,Object>> list = new ArrayList<Map<String,Object>>(pkginfo.size());
		List<AppInfoModel> list = new ArrayList<AppInfoModel>(pkginfo.size());
		
		for(PackageInfo pkg: pkginfo)
		{
			String appname = pkg.applicationInfo.loadLabel(this.getPackageManager()).toString();
			String pkgname = pkg.packageName;
			//Log.i("debug", "pkg="+pkgname+" app="+appname);
			Drawable icon = pkg.applicationInfo.loadIcon(this.getPackageManager()).getCurrent();
			
			AppInfoModel am = new AppInfoModel(pkgname, appname, icon);
			//list.add(map);
			list.add(am);
			
			//排序
			/*
			if(list.isEmpty()) list.add(am);
			else
			{
				boolean added = false;
				try{
					AppInfoModel fircol = null;
					for(int i=0;i<list.size();i++)
					{
						AppInfoModel seccol = list.get(i);
						
						String thistext = appname.toLowerCase();
						String firtext = null;
						if(fircol != null) firtext = fircol.showname.toLowerCase();
						String sectext = seccol.showname.toLowerCase();
						if(sortText(thistext, firtext) == true && sortText(thistext, sectext) == false)
						{
							list.add(i, am);
							added = true;
							break;
						}
						else
						{
							fircol = seccol;
							continue;
						}
						
					}
				} catch(Exception e){	list.add(am); added = true; }
				if(added == false) list.add(am);
			}*/

			
		}

		Collections.sort(list);
		
		
		this.This = this;
		ListView lv = (ListView) this.findViewById(R.id.activity_applist_listAppSelect);
		adapter = new AppListAdapter(this, list);
		lv.setAdapter(adapter);
		
		/*SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.app_list_layout, 
				new String[]{"appname","pkg","icon"}, 
				new int[]{R.id.applist_AppName,R.id.applist_pkgname,R.id.applist_Icon});
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
				
			});*/
		lv.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				String pkgname = ((TextView)view.findViewById(R.id.applist_pkgname)).getText().toString();
				
				//Log.i("debug", "text="+pkgname);
				Intent i=new Intent();
				i.putExtra("pkgname", pkgname);
				This.setResult(1, i);
				This.finish();
			}
			
		});
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
	
	public void onSearchClicked(View view)
	{
		EditText et = (EditText) this.findViewById(R.id.activity_applist_editsearch);
		adapter.doFilter(et.getText().toString());
	}
	
	private class AppInfoModel implements Comparable<AppInfoModel>
	{
		
		public AppInfoModel(String pack, String showname, Drawable icon)
		{
			this.pkgname=pack;
			this.showname=showname;
			this.icon=icon;
		}
		
		public String pkgname;
		public Drawable icon;
		public String showname;
		
		@Override
		public int compareTo(AppInfoModel another) 
		{
			if(this.showname.equals(another.showname)) return 0;
			Comparator<Object> cmp = Collator.getInstance(java.util.Locale.CHINA);
			List<String> tmparray = new ArrayList<String>(2);
			tmparray.add(this.showname);
			tmparray.add(another.showname);
			Collections.sort(tmparray,cmp);
			if( tmparray.get(0).equals(this.showname) ) return -1;
			else return 1;
		}

	}
	
	@SuppressLint({ "DefaultLocale", "InflateParams" })
	private class AppListAdapter extends BaseAdapter
	{
		
		Context context;
		
		List<AppInfoModel> list;
		List<AppInfoModel> filterlist;
		
		LayoutInflater li;
		
		public AppListAdapter(Context context, List<AppInfoModel> amlist)
		{
			this.context = context;
			this.list = amlist;
			this.filterlist = this.list;
			li = LayoutInflater.from(this.context);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return filterlist.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			ViewHolder holder;
			if(convertView == null)
			{
				convertView = li.inflate(R.layout.item_app_list_layout, null);
				
				holder = new ViewHolder();
				holder.textpkg = (TextView) convertView.findViewById(R.id.applist_pkgname);
				holder.textshow = (TextView) convertView.findViewById(R.id.applist_AppName);
				holder.icon = (ImageView) convertView.findViewById(R.id.applist_Icon);
				
				convertView.setTag(holder);
			}
			else
			{
				holder = (ViewHolder) convertView.getTag();
			}
			
			AppInfoModel am = filterlist.get(position);
			holder.textpkg.setText(am.pkgname);
			holder.textshow.setText(am.showname);
			holder.icon.setImageDrawable(am.icon);
			
			return convertView;
		}
		
		public class ViewHolder
		{
			public ImageView icon;
			public TextView textpkg;
			public TextView textshow;
		}
		
		public void doFilter(String text)
		{
			if(text == null || text.isEmpty())//clear filter
			{
				this.filterlist = this.list;
			}
			else//use filter
			{
				String newtext = text.toLowerCase();
				this.filterlist = new ArrayList<AppInfoModel>(list.size());
				for(AppInfoModel am: list)
				{
					if(am.pkgname.toLowerCase().contains(newtext) || am.showname.toLowerCase().contains(newtext)) this.filterlist.add(am);
				}
			}
			this.notifyDataSetInvalidated();
		}
		
	}
}
