package me.dyq.android.SDLink;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class DirectorySelectActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_directory_select);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.directory_select, menu);
		return true;
	}

	@SuppressWarnings("unused")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		return super.onOptionsItemSelected(item);
	}
	
	public class DirectorySelectAdapter extends BaseAdapter
	{
		
		List<String> dirs = new ArrayList<String>();
		DirectorySelectActivity activity;
		File currentdir;
		
		public DirectorySelectAdapter(DirectorySelectActivity activity, String startdir)
		{
			this.activity=activity;
			this.currentdir = new File(startdir);
			this.flushView();
		}
		
		public void flushView()
		{
			
		}

		@Override
		public int getCount() 
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) 
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) 
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
