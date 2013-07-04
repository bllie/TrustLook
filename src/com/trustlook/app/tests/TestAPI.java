package com.trustlook.app.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.trustlook.app.AppListService;
import com.trustlook.app.PkgUtils;
import com.trustlook.app.AppInfo;

public class TestAPI {
	private static String TAG = "Test";
	
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
	
	public static boolean testSort() {
		Map<String, String> srcMap = new HashMap<String, String>();
		srcMap.put("D2A96669370D61119BBD2934F99330D3", "0.0");
		srcMap.put("8F46EDD0B59F6F46B637849B3967008B", "0.0");
		srcMap.put("8F46EDD0B59F6F46B637849B3967008B", "0.0");
		srcMap.put("D76AD6BE138B2A353866066DE64150DF", "0.0");
		srcMap.put("DF7AA470FD9952FF7029CFA11514D697", "0.0");
		srcMap.put("8640139600FA34D87268036CA76C55EC", "1.0");
		srcMap.put("B4A4D5D211B411167A58038EEDAFCEC4", "10.0");
		srcMap.put("46EE9398151678EEF4B26880AC349950", "0.0");
		srcMap.put("C8B56F332D6383D09661F559AB493D40", "0.0");
		srcMap.put("8B17C13A13D8005C6C4A20DD40CB1371", "0.0");
		srcMap.put("99E93B0E52F39DB9AA8569E85CA4A093", "0.0");
		srcMap.put("2C5129DFF9B53A7E3432371DB62CB323", "0.0");
		srcMap.put("A025DE6F8DFD19F0CE54AD9F052A4FFF", "0.0");
		srcMap.put("B83E35E8ACF96D3EB2022BAE156BEE3B", "0.0");
		srcMap.put("967B5DEB9703F0A1AD46312924C78DE3", "0.0");
		srcMap.put("3C4602D5E88C9D77B9271127308650D3", "0.0");
		srcMap.put("1B6EB98B5F29DF214E7AC1032CC3C295", "1.0");
		srcMap.put("D672CC5BDD8C4B21E49FA38376C4972E", "0.0");
		srcMap.put("E20A785064D92E2B1E050F11F8316DB1", "0.0");
		
		List<AppInfo> appInfoList = new ArrayList<AppInfo>();
		for (String key : srcMap.keySet()) {
			AppInfo appInfo = new AppInfo(key, "pkg");
			appInfo.setMd5(key);
			appInfo.setScore(srcMap.get(key));
			appInfoList.add(appInfo);
		}
		
		Collections.sort(appInfoList);
		Log.d(TAG, "Sorted by risk score (highest - lowest)");
		for (AppInfo appInfo : appInfoList) {
			Log.d(TAG, appInfo.getMd5() + " " + appInfo.getScore());
		}
		return true;
	}
	
	public static boolean testParseQuery(ArrayList<AppInfo> appInfoList, String queryResult) {
		PkgUtils.parseQueryResult(appInfoList, queryResult);
		return false;
	}
	
	public static boolean testParseAsk(String askResult) {
		PkgUtils.parseAskResult(askResult, AppListService.getInstance());
		return true;
	}
}
