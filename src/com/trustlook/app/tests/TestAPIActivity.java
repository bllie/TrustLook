package com.trustlook.app.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.util.Log;

import com.trustlook.app.PkgUtils;
import com.trustlook.app.AppInfo;

public class TestAPIActivity extends Activity {
	public static boolean testQuery1() {
		HashMap<String, String> expected = new HashMap<String, String>();
		expected.put("FD304D1A5AEC713A533C0F9A47C17BE4", "10");
		expected.put("F8A6F3DE5255AE3C6750C256559887C5", "9");
		AppInfo a1 = new AppInfo("boat browser", "com.boatbrowser.free", "FD304D1A5AEC713A533C0F9A47C17BE4", "8738D9542C2E25375AC8F14A19F5E5F3D389AEDC");
		AppInfo a2 = new AppInfo("mouthoff", "com.ustwo", "F8A6F3DE5255AE3C6750C256559887C5", "F8A6F3DE5255AE3C6750C256559887C5");
		List<AppInfo> appInfoList = new ArrayList<AppInfo>();
		appInfoList.add(a1);
		appInfoList.add(a2);
		String output = PkgUtils.queryTrustLook("12345", appInfoList);
		appInfoList = PkgUtils.parseQueryResult((ArrayList<AppInfo>)appInfoList, output);
		
		for (AppInfo appInfo : appInfoList) {
			String md5 = appInfo.getMd5();
			if (!expected.get(md5).equals(appInfo.getScore())) {
				Log.e("Test", "error");
				return false;
			}
		}
		return true;
	}
}
