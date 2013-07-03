package com.trustlook.app;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

public class MainActivity extends Activity implements OnItemClickListener {

	private static final String TAG = "TL";

	private ListView appListView;
	TextView subjectTextView;

	SharedPreferences preferences;
	String deviceId = null;

	List<AppInfo> appInfoList = AppListService.getInstance().getAppInfoList();
	AppListAdapter adapter = null;
	AppInfo selectedApp = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFFFFF")));

		preferences = getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		deviceId = preferences.getString("device_id", "NOT_AVAILABLE");

		subjectTextView = (TextView) findViewById(R.id.totalAppsLabel);
		subjectTextView.setTypeface(PkgUtils.getLightFont());
		subjectTextView.setText(Html.fromHtml("Total: <big>" + appInfoList.size() + "</big>"));

		TextView riskLabel = (TextView) findViewById(R.id.riskLabel);
		TextView lowRiskLabel = (TextView) findViewById(R.id.lowRiskLabel);
		TextView modRiskLabel = (TextView) findViewById(R.id.moderateRiskLabel);
		TextView highRiskLabel = (TextView) findViewById(R.id.highRiskLabel);

		riskLabel.setTypeface(PkgUtils.getRegularFont());
		lowRiskLabel.setTypeface(PkgUtils.getLightFont());
		modRiskLabel.setTypeface(PkgUtils.getLightFont());
		highRiskLabel.setTypeface(PkgUtils.getLightFont());

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		int screenWidth = dm.widthPixels;
		// Log.d(TAG, "==> screenWidth: " + screenWidth);

		appListView = (ListView) findViewById(R.id.listView);
		adapter = new AppListAdapter(this, appInfoList);
		appListView.setAdapter(adapter);

		appListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedApp = appInfoList.get(position);
				launchAppOPDialog();
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
			this.finish();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			Intent intent = new Intent(this, MainScanActivity.class);
			startActivity(intent);
			Log.d(TAG, "onKeyDown");
			this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void launchAppOPDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).create();

		String message = "Version: " + selectedApp.getVersion() + "\n"
				+ selectedApp.getApkPath() + "\n"
				+ PkgUtils.formatFileSize(selectedApp.getSizeInBytes());

		dialog.setTitle(selectedApp.getDisplayName());
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setIcon(selectedApp.getIcon());

		// Uninstall App
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Uninstall",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent = new Intent(Intent.ACTION_DELETE);
						intent.setData(Uri.parse("package:"
								+ selectedApp.getPackageName()));
						startActivity(intent);
						AppListService.getInstance().remove(selectedApp);

						Map<String, String> fParams = new HashMap<String, String>();
						fParams.put("app_name", selectedApp.getPackageName());
						fParams.put("app_md5", selectedApp.getMd5());
						FlurryAgent.logEvent("delete_app", fParams);

						adapter.notifyDataSetChanged(); // update the UI
					}
				});

		// Share
		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Share", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.d(TAG, "Share button clicked");
						
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_SUBJECT, "Android App \"" + selectedApp.getDisplayName() + "\"(" 
								+ selectedApp.getPackageName() + ") Risk Report");
						intent.putExtra(Intent.EXTRA_TEXT, "Details at: \n" 
								+ "http://www.trustlook.com/" + selectedApp.getMd5().toUpperCase()
								+ "\ntrustlook - next generation mobile security platform");
						startActivity(Intent.createChooser(intent, "Share with"));
						
		                // performPublish(PendingAction.POST_STATUS_UPDATE);
						
						Map<String, String> fParams = new HashMap<String, String>();
						fParams.put("app_name", selectedApp.getPackageName());
						fParams.put("app_md5", selectedApp.getMd5());
						FlurryAgent.logEvent("share_app_report", fParams);

					}
				});
		dialog.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		selectedApp = appInfoList.get(position);
		launchAppOPDialog();
	}
}
