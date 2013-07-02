package com.trustlook.app.tests;

import com.trustlook.app.PkgUtils;
import com.trustlook.app.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TestActivity extends Activity {
	private TextView testResult;
	private Button testQuery1Button;
	private Button testSortButton;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
		
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
	}
}
