package com.trustlook.app;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "TL";
	private ListView appListView;
	
	SharedPreferences preferences;
	String deviceId = null;
	
	List<AppInfo> appInfoList = AppListService.getInstance().getAppInfoList();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
				
		preferences = getSharedPreferences("trustlook_app_shared_pref", 0);
		deviceId = preferences.getString("device_id", "NOT_AVAILABLE");
		
		TextView subjectTextView = (TextView)findViewById(R.id.topLabel);
		subjectTextView.setText("Total " + appInfoList.size() + " apps");
		
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		
//		int screenWidth = dm.widthPixels;		
//		Log.d(TAG, "screenWidth: " + screenWidth);

//		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//		View rowView = inflater.inflate(R.layout.list_item, this, false);
//		TextView appNameLabel = (TextView) rowView.findViewById(R.id.appLabel);
//		TextView appDetailsLabel = (TextView)findViewById(R.id.appDetails);
//		appNameLabel.setWidth(screenWidth - 100 - 4);
//		appDetailsLabel.setWidth(screenWidth - 100 - 4);
		
		appListView = (ListView)findViewById(R.id.listView);
		appListView.setAdapter(new AppListAdapter(this, appInfoList));
		
		appListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String toastMsg = "Loading " + appInfoList.get(position).getDisplayName() + " risk report ...";
				Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
			    
			    // launch detail activity
			    Intent intent = new Intent(getApplicationContext(), AppDetailActivity.class);
			    intent.putExtra(Constants.MD5, appInfoList.get(position).getMd5());
			    intent.putExtra(Constants.PACKAGE_NAME, appInfoList.get(position).getPackageName());
			  
                startActivity(intent); 
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, MainScanActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

}
