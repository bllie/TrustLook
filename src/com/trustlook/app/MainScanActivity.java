package com.trustlook.app;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.flurry.android.FlurryAgent;

public class MainScanActivity extends Activity {

	private static final String TAG = "TL";

	private int mProgressStatus = 0;
	private int totalApps = 0;

	private boolean isIncludeSystem = true;
	SharedPreferences preferences;
	private String deviceId = null;

	private ImageView amplifierImageView;
	private ProgressBar mProgressBar;
	private Button scanButton;
	private TextView scanLabel;
	private TextView resultText;
	private TextView percentTextView;
	private RelativeLayout progressComboLayout;

	private boolean scanMode;

	ScanApps scanTask;
	
	private GraphUser user;

	List<PackageInfo> pkgInfoList = new ArrayList<PackageInfo>();
	List<AppInfo> appInfoList = AppListService.getInstance().getAppInfoList();
	AppListService service = null;
	
    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }
    private PendingAction pendingAction = PendingAction.NONE;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_scan);

		getActionBar().setBackgroundDrawable(
				new ColorDrawable(Color.parseColor("#FFFFFF")));

		service = AppListService.getInstance();
		service.setInterestMap(PkgUtils.loadInterestMap());

		// UI widgets
		scanLabel = (TextView) findViewById(R.id.scanLabel);
		resultText = (TextView) findViewById(R.id.resultText);
		percentTextView = (TextView) findViewById(R.id.tv_progress_circle);
		amplifierImageView = (ImageView) findViewById(R.id.scan_image);
		mProgressBar = (ProgressBar) findViewById(R.id.scanBar);
		progressComboLayout = (RelativeLayout) findViewById(R.id.progressComboLayout);

		scanLabel.setTypeface(PkgUtils.getRegularFont());
		resultText.setTypeface(PkgUtils.getRegularFont());
		percentTextView.setTypeface(PkgUtils.getLightFont());

		preferences = getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();

		boolean isAlreadyLaunched = preferences.getBoolean("already_launched",
				false);

		deviceId = preferences.getString(Constants.PREF_KEY_DEVICE_ID, null);
		if (deviceId == null) {
			deviceId = Secure.getString(this.getContentResolver(),
					Secure.ANDROID_ID);
			editor.putString("devide_id", deviceId);
			editor.commit();
			service.setDeviceId(deviceId);
			PkgUtils.persistDeviceId(deviceId);
		}

		Log.d(TAG, "deviceId: " + deviceId);

		// setup trigger every 15 seconds
		Log.d(TAG, "setting up ApkUploadService for device: " + deviceId);
		Intent apkUploadIntent = new Intent(getApplicationContext(),
				ApkUploadService.class);
		apkUploadIntent.putExtra(Constants.DEVICE_ID, deviceId);

		PendingIntent pendingIntent = PendingIntent.getService(
				getApplicationContext(), 0, apkUploadIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
				Constants.CHECK_INTERVAL, pendingIntent);

		if (!isAlreadyLaunched) {
			launchEULADialog();
			// save shared preference value
			editor.putBoolean("already_launched", true);
			editor.commit();
		}

		// Hack to force overflow button show-up in action bar on
		// hard-menu-button devices
		// Source:
		// http://stackoverflow.com/questions/9286822/how-to-force-use-of-overflow-menu-on-devices-with-menu-button
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception ex) {
			// Ignore
		}

		isIncludeSystem = Boolean
				.parseBoolean(getString(R.string.includeSystem));

		pkgInfoList = getLocalAppsPkgInfo(isIncludeSystem);
		totalApps = pkgInfoList.size();

		mProgressBar.setMax(totalApps);

		scanButton = (Button) findViewById(R.id.scanButton);
		scanButton.setTypeface(PkgUtils.getLightFont());
		scanMode = true;

		scanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(TAG, "scanButton - onClick");

				if (scanMode) {
					// visibility control
					amplifierImageView.setVisibility(View.GONE);
					progressComboLayout.setVisibility(View.VISIBLE);
					scanLabel.setVisibility(View.VISIBLE);
					resultText.setVisibility(View.VISIBLE);

					// set progressBar to initial state
					mProgressStatus = 0;
					mProgressBar.setProgress(mProgressStatus);
					percentTextView.setText("");

					scanButton.setText("Cancel");
					scanMode = false;

					appInfoList.clear();
					AppListService.getInstance().resetsetAppInfoList();

					scanTask = new ScanApps();
					scanTask.execute();

				} else {
					// visibility control
					amplifierImageView.setVisibility(View.VISIBLE);
					progressComboLayout.setVisibility(View.GONE);
					scanLabel.setVisibility(View.GONE);
					resultText.setVisibility(View.GONE);

					// set progressBar to initial state
					mProgressStatus = 0;
					mProgressBar.setProgress(mProgressStatus);
					percentTextView.setText("");

					scanButton.setText("Scan");
					scanMode = true;

					if (scanTask != null)
						scanTask.cancel(true);
				}
			}
		});
	}

	private class ScanApps extends AsyncTask<Void, String, String> {
		@Override
		protected String doInBackground(Void... params) {
			if (isCancelled())
				return "";

			Map<String, String> fParams = new HashMap<String, String>();
			fParams.put("deviceId", deviceId);
			FlurryAgent.logEvent(Constants.EVENT_SCAN, fParams);

			for (PackageInfo pi : pkgInfoList) {
				if (isSystemPackage(pi)) {
					continue;
				}
				AppInfo appInfo = new AppInfo(pi.applicationInfo.loadLabel(
						getPackageManager()).toString(),
						pi.applicationInfo.processName);
				appInfo.setIcon(pi.applicationInfo.loadIcon(getPackageManager()));
				appInfo.setApkPath(pi.applicationInfo.publicSourceDir);

				File apkFile = new File(appInfo.getApkPath());
				String md5 = PkgUtils.MD5(apkFile);
				String sha1 = PkgUtils.SHA1(apkFile);
				appInfo.setMd5(md5);
				appInfo.setSha1(sha1);
				appInfo.setSizeInBytes(apkFile.length());
				appInfo.setVersion(pi.versionName);
				appInfo.setLastUpdate(pi.lastUpdateTime);

				Log.d(TAG, "Scanning " + appInfo.getApkPath());

				appInfoList.add(appInfo);

				publishProgress(appInfo.getApkPath());
			}
			return PkgUtils.queryTrustLook(deviceId, appInfoList);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			resultText.setText("" + PkgUtils.shortName(values[0]));
			mProgressStatus++;

			// Log.d(TAG, "mProgressStatus: " + mProgressStatus);
			mProgressBar.setProgress(mProgressStatus);
			String s = String.format("%.0f", (float) (100 * mProgressStatus) / totalApps);
			percentTextView.setText(Html.fromHtml("<big>" + s + "</big>" + "<small>%</small>"));
		}

		@Override
		protected void onPostExecute(String results) {
			mProgressBar.setProgress(100);

			scanButton.setText("Scan");
			resultText.setText("Completed");

			Log.d(TAG, "TrustLook Results: " + results);
			scanLabel.setText("Checking ...");
			resultText.setText("");
			appInfoList = PkgUtils.parseQueryResult((ArrayList<AppInfo>)appInfoList, results);

			// make the 'ask' request
			new AskTask().execute();

			Intent intent = new Intent(getApplicationContext(), MainActivity.class);
			startActivity(intent);
			finish();
		}

		@Override
		protected void onCancelled() {
			Log.d(TAG, "ScanApps - onCancelled");
		}
	}

	private class AskTask extends AsyncTask<Void, String, String> {
		@Override
		protected String doInBackground(Void... params) {
			Log.d(TAG, "Executing AskTask");
			return PkgUtils.askTrustLook(deviceId, appInfoList);
		}

		@Override
		protected void onProgressUpdate(String... values) {
		}

		@Override
		protected void onPostExecute(String results) {
			Log.d(TAG, "Parsing Ask Results");
			AppListService.getInstance().setInterestMap(PkgUtils.parseAskResult(results, service));
			PkgUtils.persistInterestMap(AppListService.getInstance().getInterestMap());

			scanLabel.setText("Done.");
			resultText.setText("");
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {

		}
		return super.onKeyDown(keyCode, event);
	}

	// ======================= Option Menu =======================

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		// new MenuInflater(this).inflate(R.menu.context, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about_drapp:

			final Dialog aboutDialog = new Dialog(this);
			aboutDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
			aboutDialog.setContentView(R.layout.about);
			aboutDialog.setTitle("About TrustLook");
			aboutDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,R.drawable.ic_launcher);

			TextView aboutMainLabel = (TextView) aboutDialog.findViewById(R.id.aboutMainLabel);
			String mainTemplate = aboutMainLabel.getText().toString();
			aboutMainLabel.setText(mainTemplate.replace("[Version]", PkgUtils.getAppVersion()));

			TextView aboutDetailLabel = (TextView) aboutDialog.findViewById(R.id.aboutDetailLabel);
			Button feedbackButton = (Button) aboutDialog.findViewById(R.id.feedbackButton);
			feedbackButton.setTypeface(PkgUtils.getLightFont());
			feedbackButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					sendFeedback();
				}
			});

			aboutMainLabel.setTypeface(PkgUtils.getLightFont());
			aboutDetailLabel.setTypeface(PkgUtils.getLightFont());

			aboutDialog.show();
			return true;
		case R.id.help:
			// launch detail activity
			Intent intent = new Intent(getApplicationContext(), EULAActivity.class);
			startActivity(intent);
			return (true);
//		case R.id.action_account:
//			Intent fbIntent = new Intent(getApplicationContext(), FBLoginActivity.class);
//			fbIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//			// Intent fbIntent = new Intent(MainScanActivity.this, FBLoginActivity.class);
//			startActivity(fbIntent);
//			return (true);
//		case R.id.action_settings:
//			Intent fb2Intent = new Intent(getApplicationContext(), HelloFacebookSampleActivity.class);
//			startActivity(fb2Intent);
//			return (true);
			
			/*
			final Dialog fbLoginDialog = new Dialog(this);
			fbLoginDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
			fbLoginDialog.setContentView(R.layout.hello_facebook_main);
			fbLoginDialog.setTitle("Facebook Login");
			fbLoginDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);
			fbLoginDialog.show();
			return (true);
			*/
			/*
			final Dialog fbLoginDialog = new Dialog(this);
			fbLoginDialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
			fbLoginDialog.setContentView(R.layout.fb_login);
			fbLoginDialog.setTitle("Facebook Login");
			fbLoginDialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.ic_launcher);

			TextView fbLoginTitle = (TextView)fbLoginDialog.findViewById(R.id.fb_login_title);
			fbLoginTitle.setTypeface(PkgUtils.getLightFont());
			
			LoginButton loginButton = (LoginButton) fbLoginDialog.findViewById(R.id.fb_login_button);
			Log.d(TAG, "loginButton: " + loginButton);
			
	        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
	            @Override
	            public void onUserInfoFetched(GraphUser user) {
	            	Log.d(TAG, "onUserInfoFetched - user: " + user);
	            	
	            	if (user == null) {
	            		
	            	}
	                MainScanActivity.this.user = user;
	                // updateUI();
	                // It's possible that we were waiting for this.user to be populated in order to post a
	                // status update.
	                handlePendingAction();
	            }
	        });
	        
			fbLoginDialog.show();
			return (true);
			*/
			
		}

		return (super.onOptionsItemSelected(item));
	}

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
    	Log.d(TAG, "- MainScanActivity - onSessionStateChange");
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) 
        {
                new AlertDialog.Builder(MainScanActivity.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
        }
    }
    
    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_STATUS_UPDATE:
                // postStatusUpdate();
                Log.d(TAG, "POST_STATUS_UPDATE");
            	break;
        }
    }
    
	public List<PackageInfo> getLocalAppsPkgInfo(boolean includeSystem) {
		Context context = getApplicationContext();
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<PackageInfo> pkgInfoList = context.getPackageManager()
				.getInstalledPackages(0);
		List<PackageInfo> resultPkgInfoList = new ArrayList<PackageInfo>();

		if (!includeSystem) {
			for (PackageInfo pkgInfo : pkgInfoList) {
				if (!isSystemPackage(pkgInfo)) {
					resultPkgInfoList.add(pkgInfo);
				}
			}
		}
		Log.d(TAG, "Total packages: " + resultPkgInfoList.size());

		return resultPkgInfoList;
	}

	private boolean isSystemPackage(PackageInfo pkgInfo) {
		return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
				: false;
	}

	public void pkgInfo() {
		try {
			Context ctx = this.getApplicationContext();
			PackageManager pkgMgr = ctx.getPackageManager();
			PackageInfo pkgInfo = pkgMgr
					.getPackageInfo(ctx.getPackageName(), 0);
			Log.d(TAG, "PkgInfo: " + pkgInfo.toString());
		} catch (Exception e) {
			Log.d(TAG, e.toString());
		}
	}

	public void launchEULADialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("EULA");

		// set dialog message
		alertDialogBuilder
				.setMessage(getString(R.string.EULA))
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// if this button is clicked, close current
								// activity
								// EULAActivity.this.finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// if this button is clicked, just close
						// the dialog box and do nothing
						dialog.cancel();
					}
				});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	@Override
	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "9HR6QKYMCF3BJQJMZJSJ");
	}

	@Override
	protected void onStop() {
		super.onStop();
		FlurryAgent.onEndSession(this);
	}

	private void sendFeedback() {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("message/rfc822");
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { "support@trustlook.com" });
		i.putExtra(Intent.EXTRA_SUBJECT,
				"Feedback for trustlook" + PkgUtils.getAppVersion());
		i.putExtra(Intent.EXTRA_TEXT, "trustlook " + PkgUtils.getAppVersion()
				+ "\n");

		try {
			startActivity(Intent.createChooser(i, "Send mail..."));
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(this, "There are no email clients installed.",
					Toast.LENGTH_SHORT).show();
		}
	}
}