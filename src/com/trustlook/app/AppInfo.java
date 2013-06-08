package com.trustlook.app;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.drawable.Drawable;

public class AppInfo implements Comparable<AppInfo>{
	private String displayName;
	private String packageName;
	private String md5;
	private String sha1;
	private Drawable icon;
	private String apkPath;
	private long lastUpdate;
	
	// scan result part
	private String score;
	private String summary;
	private String virusName;
	private String reportUrl;

	private long sizeInBytes;
	private String version;
	
	public String getReportUrl() {
		return reportUrl;
	}

	public void setReportUrl(String reportUrl) {
		this.reportUrl = reportUrl;
	}
	
	public long getSizeInBytes() {
		return sizeInBytes;
	}

	public void setSizeInBytes(long sizeInBytes) {
		this.sizeInBytes = sizeInBytes;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public AppInfo(String displayName, String packageName) {
		this(displayName, packageName, "", "");
	}
	
	public AppInfo(String displayName, String packageName, String md5, String sha1) {
		super();
		this.displayName = displayName;
		this.packageName = packageName;
		this.md5 = md5;
		this.sha1 = sha1;
	}
	
	public String getApkPath() {
		return apkPath;
	}
	public void setApkPath(String apkPath) {
		this.apkPath = apkPath;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String name) {
		this.packageName = name;
	}
	public String getMd5() {
		return md5;
	}
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha1) {
		this.sha1 = sha1;
	}
	public Drawable getIcon() {
		return icon;
	}
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getVirusName() {
		return virusName;
	}

	public void setVirusName(String virusName) {
		this.virusName = virusName;
	}

	@Override
	public String toString() {
		return "AppInfo [displayName=" + displayName + ", packageName="
				+ packageName + ", md5=" + md5 + ", score=" + score + "]";
	}
	
//	public JSONObject toJSON2() {
//		JSONObject json = new JSONObject();
//		try {
//			json.put("MD5", getMd5());
//			json.put("score",  getScore());
//		}
//	}
	
	public JSONObject toJSON() {
	    JSONObject jsonObject= new JSONObject();
	    try {
	        jsonObject.put("packageName", getPackageName());
	        jsonObject.put("MD5", getMd5());
	        jsonObject.put("SHA1", getSha1());
	        System.out.println("JSON string: " + jsonObject.toString());
	    } catch (JSONException e) {
	        e.printStackTrace();
	    }
	    return jsonObject;
	}

	@Override
	public int compareTo(AppInfo another) {
		if (this.score == null)
			return 1;
		if (another == null || another.score == null)
			return -1;
		try {
			return (Double.parseDouble(this.score) > Double.parseDouble(another.score)) ? -1 : 1;
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(long lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
}
