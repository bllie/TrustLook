/**
 * Created by Tao Xie on 5/20/13.
 * Copyright (c) 2013 trustlook. All rights reserved.
 */

package com.trustlook.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.trustlook.app.PkgUtils;

public class ApkUploadService extends IntentService {

	private static String TAG = "TL";

	public ApkUploadService() {
		super("ApkUploadService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		if (AppListService.getInstance().isAllowApkUpload() == false) {
			Log.d(TAG, "APK Upload is Turned off.");
			return;
		}
		Log.d(TAG, "ApkUploadService onHandleIntent() triggerd ...");

		Map<String, String> interestMap = PkgUtils.loadInterestMap();
		if (interestMap != null && interestMap.size() > 0) {
			// TODO not working in reboot device
			Map.Entry<String, String> entry = interestMap.entrySet().iterator()
					.next();
			String md5 = entry.getKey();
			String apkFileName = entry.getValue();

			String deviceId = intent.getStringExtra(Constants.DEVICE_ID);

			if (deviceId == null) {
				deviceId = PkgUtils.loadDeviceId();
			}

			Log.d(TAG, "==> uploading " + apkFileName + " from device: "
					+ deviceId + ", md5: " + md5);

			String logInfo = (PkgUtils.isCharged() ? "charged" : "not charged")
					+ ", "
					+ (PkgUtils.isWifiConntected() ? "wifi connected"
							: "no wifi");
			Log.d(TAG, logInfo);

			if (PkgUtils.isCharged() && PkgUtils.isWifiConntected()) {

				String uploadResult = PkgUtils.uploadTrustLook(deviceId,
						new File(apkFileName), md5);
				parseUploadResult(uploadResult);

				Map<String, String> fParams = new HashMap<String, String>();
				fParams.put(Constants.DEVICE_ID, deviceId);
				fParams.put(Constants.APK_FILENAME, apkFileName);
				FlurryAgent.logEvent(Constants.EVENT_APK_UPLOAD, fParams);
			} else {
				Log.d(TAG, "wifi not available or charger not connected");
			}
		}
	}

	public List<String> parseUploadResult(String result) {
		List<String> resultList = new ArrayList<String>();
		try {
			JSONObject json = new JSONObject(result);
			if (json.getBoolean("success") == true) {
				String md5 = json.getString("md5");
				Log.d(TAG, "[Success] APK Upload " + json.getString("md5"));
				Map<String, String> interestMap = PkgUtils.loadInterestMap();
				interestMap.remove(md5);
				PkgUtils.persistInterestMap(interestMap);
			} else {
				Log.d(TAG, "[Fail] APK Upload - No interest list update");
			}
		} catch (JSONException e) {
			Log.d(TAG, "[upload] - parsing error\n" + result);
		}
		return resultList;
	}

}
