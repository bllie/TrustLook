package com.trustlook.app.tests;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.trustlook.app.AppInfo;
import com.trustlook.app.MainScanActivity;
import com.trustlook.app.PkgUtils;
import com.trustlook.app.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TestActivity extends Activity {
	private TextView testResult;
	private Button testQuery1Button;
	private Button testSortButton;
	private Button testParseQueryButton;
	private Button testParseAskButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFFFFF")));
		
		testResult = (TextView)findViewById(R.id.testResult);
		
		testQuery1Button = (Button) findViewById(R.id.testQuery1Button);
		testQuery1Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                boolean r = TestAPI.testQuery1();
                testResult.setText("testQuery1 - " + (r ? "Success" : "Falure"));
            }
        });
		
		testSortButton = (Button) findViewById(R.id.testSortButton);
		testSortButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                boolean r = TestAPI.testSort();
                testResult.setText("testSort - " + (r ? "Success" : "Falure"));
            }
        });
		
		testParseQueryButton = (Button)findViewById(R.id.testParseQueryButton);
		testParseQueryButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {
            	ArrayList<AppInfo> appInfoList = new ArrayList<AppInfo>();
    			AppInfo a1 = new AppInfo("test1", "test.package.1", "465B21B6DE403637AFBCE7E04DFF9F58", "");
    			AppInfo a2 = new AppInfo("test2", "test.package.2", "C3A0F5D584CC2C3221BBD79486578208", "");
    			appInfoList.add(a1);
    			appInfoList.add(a2);
    			String output = getSample("query1.json");
    			Log.d("TEST", "query1.json:\n" + output);
                TestAPI.testParseQuery(appInfoList, getSample("query1.json"));
                TestAPI.testParseQuery(appInfoList, getSample("query2.json"));
                TestAPI.testParseQuery(appInfoList, getSample("query3.json"));

            }
        });
		
		testParseAskButton = (Button)findViewById(R.id.testParseAskButton);
		testParseAskButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				TestAPI.testParseAsk(getSample("ask1.json"));	
				TestAPI.testParseAsk(getSample("ask2.json"));	
			}
		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent intent = new Intent(this, MainScanActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				this.finish();
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	private String getSample(String key) {
		Properties props = readFileFromInternalStorage();
		if (props != null) {
			return (String)props.get(key);
		}
		return "";
	}
	private Properties readFileFromInternalStorage() {
		try {
			InputStream rawResource = this.getResources().openRawResource(R.raw.testcases);
			Properties properties = new Properties();
			properties.load(rawResource);
			return properties;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
