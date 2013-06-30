package com.trustlook.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

public class PowerNetworkReceiver extends BroadcastReceiver {
	final String TAG = "TL";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "wifireceiver: " + intent.toString());
		String action = intent.getAction();

		if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
			Log.d(TAG, "Power on - consider of uploading binary");
		} else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
			Log.d(TAG, "Finish current upload task, then stop");
		} else if (action
				.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
			Log.d(TAG, "network change");
			if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED,
					false)) {
				Log.d(TAG, "wifi connected - consider of starting upload task");
				// final Handler handler = new Handler();
				// handler.postDelayed(new Runnable() {
				// public void run() {
				// new UploadTask().execute("22344445");
				// }
				// }, 1000*5);
				//
				// new UploadTask().execute("22344445");
			} else {
				// wifi connection was lost
				Log.d(TAG, "wifi disconnected - stop task");
			}
		} else if (action.equals(Intent.ACTION_SCREEN_ON)) {
			Log.d(TAG, "ACTION_SCREEN_ON");

		} else if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.d(TAG, "ACTION_BOOT_COMPLETED");
			String deviceId = PkgUtils.loadDeviceId();

			// setup trigger every 15 seconds

			Log.d(TAG, "setting up ApkUploadService for device: " + deviceId);
			Intent apkUploadIntent = new Intent(context, ApkUploadService.class);
			apkUploadIntent.putExtra(Constants.DEVICE_ID, deviceId);

			PendingIntent pendingIntent = PendingIntent.getService(context, 0,
					apkUploadIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			AlarmManager am = (AlarmManager) context
					.getSystemService(Context.ALARM_SERVICE);
			am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(),
					Constants.CHECK_INTERVAL, pendingIntent);
		}
	}
	/*
	 * private class UploadTask extends AsyncTask<String, Void, String> {
	 * 
	 * @Override protected String doInBackground(String... appMd5) {
	 * 
	 * Log.d(TAG, "Uploading app (md5: " + appMd5 + ")"); Map<String, String>
	 * interestMap = AppListService.getInstance().getInterestMap();
	 * 
	 * JSONArray arr = new JSONArray(); for (String md5 : interestSet) {
	 * arr.put(md5); } return PkgUtils.askTrustLook("", arr); }
	 * 
	 * protected void onPostExecute(String results) { Log.d(TAG,
	 * "TrustLook Results: " + results); } }
	 */
}