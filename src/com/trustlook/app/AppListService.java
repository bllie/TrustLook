package com.trustlook.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppListService {
	private static AppListService instance = null;

	private List<AppInfo> appInfoList = new ArrayList<AppInfo>();
	private Map<String, String> interestMap = new HashMap<String, String>();
	private String deviceId = null;
	private boolean allowApkUpload = true;
	
	protected AppListService() {
		// TODO load from preference?
	}

	public static AppListService getInstance() {
		if (instance == null) {
			instance = new AppListService();
		}
		return instance;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public List<AppInfo> getAppInfoList() {
		return appInfoList;
	}

	public void setAppInfoList(List<AppInfo> appInfoList) {
		this.appInfoList = appInfoList;
	}

	public void resetsetAppInfoList() {
		this.appInfoList.clear();
	}

	public Map<String, String> getInterestMap() {
		return this.interestMap;
	}

	public void setInterestMap(Map<String, String> interestMap) {
		this.interestMap = interestMap;
	}

	public AppInfo getAppInfoByMd5(String md5) {
		for (AppInfo appInfo : appInfoList) {
			if (md5.equals(appInfo.getMd5())) {
				return appInfo;
			}
		}
		return null;
	}

	public String getApkPathByMd5(String md5) {
		AppInfo appInfo = getAppInfoByMd5(md5);
		return (appInfo != null) ? appInfo.getApkPath() : null;
	}

	public void remove(AppInfo appInfo) {
		this.appInfoList.remove(appInfo);
	}
	
	public boolean isAllowApkUpload() {
		return allowApkUpload;
	}
}
