package com.trustlook.app;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;

@SuppressLint("SetJavaScriptEnabled")
public class AppDetailActivity extends Activity {
	
	private static String TAG = "TL";
	private WebView reportWebView;
	
	private Button uninstallButton;
	private Button trustButton;
	
	private AppInfo currentApp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_detail);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		final String pkgName = getIntent().getStringExtra(Constants.PACKAGE_NAME);
		final String md5 = getIntent().getStringExtra(Constants.MD5);
		currentApp = AppListService.getInstance().getAppInfoByMd5(md5);
		Log.d(TAG, "packageName: " + pkgName);
		
		ImageView logoView = (ImageView)findViewById(R.id.appLogo);
		TextView appNameLabel = (TextView)findViewById(R.id.appNameLabel);
		
		logoView.setImageDrawable(currentApp.getIcon());
		appNameLabel.setText(currentApp.getDisplayName());
		
		trustButton = (Button)findViewById(R.id.trustButton);
		trustButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				// onBackPressed();
				new UploadApkTask().execute();
			}
		});		
		
		uninstallButton = (Button)findViewById(R.id.uninstallButton);
		uninstallButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				launchAppOPDialog();
			}
		});
		reportWebView = (WebView)findViewById(R.id.detailWebView);
		// progressBar = (ProgressBar)findViewById(R.id.reportWebProgressBar);
		reportWebView.setWebViewClient(new SimpleReportWebClient());
		reportWebView.getSettings().setJavaScriptEnabled(true);
		reportWebView.loadUrl(currentApp.getReportUrl());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, MainActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	public void launchAppOPDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		
		String message = "Version: " + currentApp.getVersion() + "\n" 
				+ currentApp.getApkPath() + "\n" 
				+ PkgUtils.formatFileSize(currentApp.getSizeInBytes());
				
		dialog.setTitle(currentApp.getDisplayName());			      
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setIcon(currentApp.getIcon());

		/*
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Launch", new DialogInterface.OnClickListener() {
	         public void onClick(DialogInterface dialog, int id) {
	        	 // start the app by invoking its launch intent
	        	 Intent i = getPackageManager().getLaunchIntentForPackage(currentApp.getPackageName());
	        	 try {
	        		 if (i != null) {
	        			 startActivity(i);
	        		 } 
	        		 else {
	        			 i = new Intent(currentApp.getPackageName());
	        			 startActivity(i);
	        		 }
	        	 } 
	        	 catch (ActivityNotFoundException err) {
	        		 // Toast.makeText(ListInstalledApps.this, "Error launching app", Toast.LENGTH_SHORT).show();
	        	 }
	         }
		});
		*/
		
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Uninstall", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
	             Intent intent = new Intent(Intent.ACTION_DELETE);
	             intent.setData(Uri.parse("package:" + currentApp.getPackageName()));
	             startActivity(intent);
	             AppListService.getInstance().remove(currentApp);	  
	             
	             Map<String, String> fParams = new HashMap<String, String>();
	             fParams.put("app_name", currentApp.getPackageName());
	             fParams.put("app_md5", currentApp.getMd5());
	             FlurryAgent.logEvent("delete_app", fParams);
	             
	             // TODO
	             // mApps.remove(appPosition);
	             // mAdapter.notifyDataSetChanged(); // update the UI
	          }
		});
	       
		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});		
		dialog.show();
	}
	
	public class SimpleReportWebClient extends WebViewClient
    {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
        }
 
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
 
        }
 
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // progressBar.setVisibility(View.GONE);
        }
    }
	
	private class UploadApkTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void... params) {
			String apkPath = currentApp.getApkPath();
			File apkFile = new File(apkPath);
	
			Log.d(TAG, "Post2Cloud " + apkPath);
			return PkgUtils.uploadTrustLook(AppListService.getInstance().getDeviceId(), apkFile, currentApp.getMd5());
		}
		
		protected void onPostExecute(String results) {
			Log.d(TAG, "TrustLook Results: " + results);
			// PkgUtils.parseQueryResult(results);
		}		
	}
}
