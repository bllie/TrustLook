package com.trustlook.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.util.Log;

public class PkgUtils {

	private static final String TAG = "TL";

	public enum RISK_LEVEL {
		HIGH, MEDIUM, LOW
	};

	public static RISK_LEVEL getRiskLevel(String scoreString) {
		int scoreInt = 0;
		try {
			scoreInt = Math.round(Float.parseFloat(scoreString));
		} catch (Exception e) {
			scoreInt = 0;
		}
		if (scoreInt >= 7)
			return RISK_LEVEL.HIGH;
		else if (scoreInt >= 3)
			return RISK_LEVEL.MEDIUM;
		else
			return RISK_LEVEL.LOW;
	}

	public List<AppInfo> getAllPkgInfo(Context context, PackageManager packageManager) {
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		final List<ResolveInfo> pkgAppsList = packageManager
				.queryIntentActivities(mainIntent, 0);

		List<AppInfo> appInfoList = new ArrayList<AppInfo>();

		for (ResolveInfo ri : pkgAppsList) {
			if (ri.activityInfo != null) {
				String appDisplayName = ri.activityInfo.loadLabel(packageManager).toString();
				String packageName = ri.activityInfo.packageName;
				AppInfo appInfo = new AppInfo(appDisplayName, packageName);
				appInfo.setIcon(ri.activityInfo.loadIcon(packageManager));

				appInfoList.add(appInfo);
				// TODO populate the progress, update the progressbar
			}
		}
		return appInfoList;
	}

	public static String MD5(File file) {
		return generateDigest(file, "MD5");
	}

	public static String SHA1(File file) {
		return generateDigest(file, "SHA1");
	}

	/**
	 * Generate signature from given file, with "MD5" and "SHA1" as valid
	 * algorithm options Source:
	 * 
	 * @param file
	 * @param algorithm
	 * @return
	 */
	private static String generateDigest(File file, String algorithm) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Exception while getting Digest", e);
			return null;
		}

		InputStream is;
		try {
			is = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Exception while getting FileInputStream", e);
			return null;
		}

		byte[] buffer = new byte[8192];
		int read;
		try {
			while ((read = is.read(buffer)) > 0) {
				digest.update(buffer, 0, read);
			}
			byte[] md5sum = digest.digest();
			BigInteger bigInt = new BigInteger(1, md5sum);
			String output = bigInt.toString(16);
			// Fill to 32 chars
			output = String.format("%32s", output).replace(' ', '0');
			return output.toUpperCase(Locale.US);
		} catch (IOException e) {
			throw new RuntimeException("Unable to process file for MD5", e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				Log.e(TAG, "Exception on closing MD5 input stream", e);
			}
		}
	}

	public static String shortName(String pathName) {
		return pathName.substring(pathName.lastIndexOf("/") + 1);
	}

	public static String formatFileSize(long size) {

		String unit = "Bytes";
		double sizeInUnit = 0d;

		if (size > 1024 * 1024 * 1024) { // Gigabyte
			sizeInUnit = (double) size / (1024 * 1024 * 1024);
			unit = "GB";
		} else if (size > 1024 * 1024) { // Megabyte
			sizeInUnit = (double) size / (1024 * 1024);
			unit = "MB";
		} else if (size > 1024) { // Kilobyte
			sizeInUnit = (double) size / 1024;
			unit = "KB";
		} else { // Byte
			sizeInUnit = (double) size;
		}

		// only show two digits after the comma
		return new DecimalFormat("###.##").format(sizeInUnit) + " " + unit;
	}

	public static void printList(List<AppInfo> list) {
		for (Object item : list) {
			Log.d(TAG, item.toString());
		}
	}

	// ------- network ------
	public static String queryTrustLook(String deviceId, List<AppInfo> appInfoList) {
		HttpPost request = new HttpPost(Constants.QUERY_URL);
		request.setHeader("Accept", "application/json");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		try {
			JSONArray jsArray = new JSONArray();
			for (AppInfo appInfo : appInfoList) {
				jsArray.put(appInfo.toJSON());
			}
			Log.d(TAG, "Posting to " + Constants.QUERY_URL);

			List<NameValuePair> nvps = new ArrayList<NameValuePair>();
			nvps.add(new BasicNameValuePair("devid", deviceId));
			nvps.add(new BasicNameValuePair("action", "QUERY"));
			nvps.add(new BasicNameValuePair("data", jsArray.toString()));
			request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

			return post2TrustLook(request);
		} catch (Exception e) {
			return "";
		}
	}

	public static String askTrustLook(String deviceId, JSONArray md5Array) {
		HttpPost request = new HttpPost(Constants.ASK_URL);

		request.setHeader("Accept", "application/json");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		Log.d(TAG, "Posting to " + Constants.ASK_URL);

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("devid", deviceId));
		nvps.add(new BasicNameValuePair("action", "ASK"));
		nvps.add(new BasicNameValuePair("data", md5Array.toString()));

		try {
			request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			return post2TrustLook(request);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	public static String askTrustLook(String deviceId, List<AppInfo> appInfoList) {
		HttpPost request = new HttpPost(Constants.ASK_URL);

		request.setHeader("Accept", "application/json");
		request.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

		Log.d(TAG, "Posting to " + Constants.ASK_URL);

		JSONArray arr = new JSONArray();
		for (AppInfo appInfo : appInfoList) {
			arr.put(appInfo.getMd5());
		}

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("devid", deviceId));
		nvps.add(new BasicNameValuePair("action", "ASK"));
		nvps.add(new BasicNameValuePair("data", arr.toString()));

		try {
			request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			return post2TrustLook(request);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * Upload APK file to cloud for analysis
	 * 
	 * @param apkFile
	 * @return
	 */
	public static String uploadTrustLook(String deviceId, File apkFile, String md5) {
		Log.d(TAG, "[uploadTrustLook] deviceId: " + deviceId + ", md5: " + md5);
		try {
			HttpPost post = new HttpPost(Constants.UPLOAD_URL);
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

			reqEntity.addPart("devid", new StringBody(deviceId));
			reqEntity.addPart("md5", new StringBody(md5));
			reqEntity.addPart("action", new StringBody("UPLOAD"));

			FileBody bin = new FileBody(apkFile);
			reqEntity.addPart("file", bin);
			post.setEntity(reqEntity);

			String response = post2TrustLook(post);
			Log.d(TAG, "response: " + response);
			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String post2TrustLook(HttpPost request) {
		try {
			HttpResponse httpResponse = new DefaultHttpClient().execute(request);
			String retSrc = EntityUtils.toString(httpResponse.getEntity());
			return retSrc;
		} 
		catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
			return "";
		}
	}

	public static ArrayList<AppInfo> parseQueryResult(ArrayList<AppInfo> appInfoList, String queryResult) {
		try {
			JSONObject result = new JSONObject(queryResult);
			JSONArray resultList = result.getJSONArray("results");
			for (int i = 0; i < resultList.length(); i++) {
				try {
					JSONObject item = resultList.getJSONObject(i);
					String md5 = item.getString("md5");
					Double score = item.isNull("score") ? Double.valueOf(0.0) : item.getDouble("score");
					String virusName = (item.has("virusname")) ? item.getString("virusname") : null;
					String summary = item.isNull("summary") ? "" : item.getString("summary");
					String reportUrl = item.getString("url");
	
					Log.d(TAG, md5 + " " + score);
	
					for (AppInfo ai : appInfoList) {
						if (ai.getMd5().equals(md5)) {
							ai.setScore("" + score);
							ai.setVirusName(virusName);
							ai.setReportUrl(reportUrl);
							ai.setSummary(summary);
							break;
						}
					}
				} catch (JSONException jsonException) {
					jsonException.printStackTrace();
				}
				
			}
			// sort based on risk score descending
			Collections.sort(appInfoList);
		} catch (JSONException e) {
			Log.d(TAG, "query - parsing error of \n" + queryResult);
			e.printStackTrace();
		}
		return appInfoList;
	}
	
	public static Map<String, String> parseAskResult(String askResult, AppListService service) {
		Log.d(TAG, "[ask result] " + askResult);
		Map<String, String> resultMap = new HashMap<String, String>();
		try {
			JSONObject result = new JSONObject(askResult);
			
			// server has no interest to your apk uploading			
			if (!result.has("results")) {
				Log.d(TAG, "server has no interest in your apks");
				return resultMap;
			}
			JSONArray interestList = result.getJSONArray("results");
			
			for (int i = 0; i < interestList.length(); i++) {
				// Log.d(TAG, "ASK result: " + interestList.getString(i));
				// TODO
				String md5 = interestList.getString(i);
				String apkPath = service.getApkPathByMd5(md5);
				resultMap.put(md5, apkPath);
				Log.d(TAG, "putting (" + md5 + ", " + apkPath + ")");
			}
			Log.d(TAG, "Total interest apps: " + interestList.length());
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d(TAG, "[ask] - parsing error of: " + askResult);
		}
		return resultMap;
	}

	public static boolean isWifiConntected() {
		Context context = TrustApp.getContext();
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		return mWifi.isConnected();
	}

	public static boolean isCharged() {
		Context context = TrustApp.getContext();
		Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
	}

	public boolean isNetworkAvailable() {
		Context context = TrustApp.getContext();
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			Log.d(TAG, "getSystemService rend null");
		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static String loadDeviceId() {
		Context context = TrustApp.getContext();
		SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		return preferences.getString(Constants.PREF_KEY_DEVICE_ID, null);
	}

	public static void persistDeviceId(String deviceId) {
		Context context = TrustApp.getContext();
		SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(Constants.PREF_KEY_DEVICE_ID, deviceId);
		editor.commit();
	}

	public static void persistInterestMap(Map<String, String> interestMap) {
		Context context = TrustApp.getContext();
		Log.d(TAG, "--> persist interest set of " + interestMap.size());
		JSONArray arr = new JSONArray();
		Iterator<String> it = interestMap.keySet().iterator();
		try {
			while (it.hasNext()) {
				JSONObject obj = new JSONObject();
				String md5 = (String) it.next();
				obj.put("md5", md5);
				obj.put("apkPath", (String) interestMap.get(md5));

				arr.put(obj);
			}
		} catch (JSONException e) {
			Log.d(TAG, "generate json error.");
		}

		SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(Constants.PREF_KEY_INTEREST_LIST, arr.toString());
		Log.d(TAG, "generated json " + arr.toString());
		editor.commit();
	}

	public static Map<String, String> loadInterestMap() {
		Context context = TrustApp.getContext();
		SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		Map<String, String> resultMap = parsePersistedInterestMap(preferences.getString(Constants.PREF_KEY_INTEREST_LIST, "[]"));
		Log.d(TAG, "--> loaded interest of " + resultMap.size());

		return resultMap;
	}

	private static Map<String, String> parsePersistedInterestMap(String s) {
		Map<String, String> resultMap = new HashMap<String, String>();
		try {
			JSONArray interestList = new JSONArray(s);
			for (int i = 0; i < interestList.length(); i++) {
				JSONObject obj = interestList.getJSONObject(i);
				resultMap.put(obj.getString("md5"), obj.getString("apkPath"));
			}
			Log.d(TAG, "Total interest apps: " + resultMap.size());
		} catch (JSONException e) {
			Log.d(TAG, "[persist-interest-map] - parsing error of: " + s);
		}
		return resultMap;
	}

	public static Typeface getLightFont() {
		Context context = TrustApp.getContext();
		return Typeface.createFromAsset(context.getAssets(), "MyriadPro-Light.otf");
	}

	public static Typeface getRegularFont() {
		Context context = TrustApp.getContext();
		return Typeface.createFromAsset(context.getAssets(), "MyriadPro-Regular.otf");
	}

	public static String getAppVersion() {
		Context context = TrustApp.getContext();
		try {
			String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			return versionName;
		} catch (Exception e) {
			return "0.0.0";
		}
	}
}
