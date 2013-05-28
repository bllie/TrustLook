package com.trustlook.app;

public class Constants {
	
	public static final String PREFERENCE_NAME = "trustlook_app_shared_pref";
	public static final String PREF_KEY_DEVICE_ID = "device_id";
	public static final String PREF_KEY_INTEREST_LIST = "interest_list";
	
	// REST API URLS
	public static final String UPLOAD_URL = "http://www.trustlook.com/drapp/api/v1/upload/";
	public static final String QUERY_URL = "http://www.trustlook.com/drapp/api/v1/query/";
	public static final String ASK_URL = "http://www.trustlook.com/drapp/api/v1/ask/";
	public static final String DEBUG_TAG = "TL";
	
	// Intent extra
	public static final String MD5 = "md5";
	public static final String PACKAGE_NAME = "packageName";
	
	public static final String APK_FILENAME = "apkFileName";
	public static final String DEVICE_ID = "device_Id";			// DONOT use 'deviceId' which somehow conflicts with existing one
	
	// Flurry
	public static final String EVENT_SCAN = "scan_device";
	public static final String EVENT_APK_UPLOAD = "upload_apk";
}
