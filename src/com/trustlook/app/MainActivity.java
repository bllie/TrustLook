package com.trustlook.app;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookAuthorizationException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.android.*;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;

import com.flurry.android.FlurryAgent;

public class MainActivity extends Activity implements OnItemClickListener {

	private static final String TAG = "TL";

	private ListView appListView;
	TextView subjectTextView;

	SharedPreferences preferences;
	String deviceId = null;

	List<AppInfo> appInfoList = AppListService.getInstance().getAppInfoList();
	AppListAdapter adapter = null;
	AppInfo selectedApp = null;
    private PendingAction pendingAction = PendingAction.NONE;

    private GraphUser user;
    private final String PENDING_ACTION_BUNDLE_KEY = "com.trustlook.app:PendingAction";
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }
    private UiLifecycleHelper uiHelper;

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (pendingAction != PendingAction.NONE &&
                (exception instanceof FacebookOperationCanceledException ||
                exception instanceof FacebookAuthorizationException)) {
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
        }
        // updateUI();
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
        uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            Log.d(TAG, "pending name: " + name);
            if (name != null)
            	pendingAction = PendingAction.valueOf(name);
        }

		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFFFFF")));

		preferences = getSharedPreferences(Constants.PREFERENCE_NAME, 0);
		deviceId = preferences.getString("device_id", "NOT_AVAILABLE");

		subjectTextView = (TextView) findViewById(R.id.totalAppsLabel);
		subjectTextView.setTypeface(PkgUtils.getLightFont());
		subjectTextView.setText(Html.fromHtml("Total: <big>" + appInfoList.size() + "</big>"));

		TextView riskLabel = (TextView) findViewById(R.id.riskLabel);
		TextView lowRiskLabel = (TextView) findViewById(R.id.lowRiskLabel);
		TextView modRiskLabel = (TextView) findViewById(R.id.moderateRiskLabel);
		TextView highRiskLabel = (TextView) findViewById(R.id.highRiskLabel);

		riskLabel.setTypeface(PkgUtils.getRegularFont());
		lowRiskLabel.setTypeface(PkgUtils.getLightFont());
		modRiskLabel.setTypeface(PkgUtils.getLightFont());
		highRiskLabel.setTypeface(PkgUtils.getLightFont());

		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);

		int screenWidth = dm.widthPixels;
		// Log.d(TAG, "==> screenWidth: " + screenWidth);

		appListView = (ListView) findViewById(R.id.listView);
		adapter = new AppListAdapter(this, appInfoList);
		appListView.setAdapter(adapter);

		appListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				selectedApp = appInfoList.get(position);
				launchAppOPDialog();
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			Intent intent = new Intent(this, MainScanActivity.class);
			startActivity(intent);
			Log.d(TAG, "onKeyDown");
			this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

	public void launchAppOPDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).create();

		String message = "Version: " + selectedApp.getVersion() + "\n"
				+ selectedApp.getApkPath() + "\n"
				+ PkgUtils.formatFileSize(selectedApp.getSizeInBytes());

		dialog.setTitle(selectedApp.getDisplayName());
		dialog.setMessage(message);
		dialog.setCancelable(true);
		dialog.setIcon(selectedApp.getIcon());

		// Uninstall App
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Uninstall",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Intent intent = new Intent(Intent.ACTION_DELETE);
						intent.setData(Uri.parse("package:"
								+ selectedApp.getPackageName()));
						startActivity(intent);
						AppListService.getInstance().remove(selectedApp);

						Map<String, String> fParams = new HashMap<String, String>();
						fParams.put("app_name", selectedApp.getPackageName());
						fParams.put("app_md5", selectedApp.getMd5());
						FlurryAgent.logEvent("delete_app", fParams);

						adapter.notifyDataSetChanged(); // update the UI
					}
				});

		// Facebook Share
		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Share", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						Log.d(TAG, "Share button clicked");
						
		                performPublish(PendingAction.POST_STATUS_UPDATE);
						
						Map<String, String> fParams = new HashMap<String, String>();
						fParams.put("app_name", selectedApp.getPackageName());
						fParams.put("app_md5", selectedApp.getMd5());
						FlurryAgent.logEvent("share_app_report", fParams);

					}
				});
		
//		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Cancel",
//				new DialogInterface.OnClickListener() {
//					public void onClick(DialogInterface dialog, int id) {
//						dialog.cancel();
//					}
//				});
		dialog.show();
	}
	
    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        if (session == null) 
        	Log.d(TAG, "> Session is null");
        Log.d(TAG, "permissions: " + session.getPermissions());
        return session != null && session.getPermissions().contains("publish_actions");
    }
    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }
    private void showPublishResult(String message, GraphObject result, FacebookRequestError error) {
        String title = null;
        String alertMessage = null;
        if (error == null) {
            title = getString(R.string.success);
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = getString(R.string.successfully_posted_post, "");
        } else {
            title = getString(R.string.error);
            alertMessage = error.getErrorMessage();
        }
        Log.d(TAG, "message: " + message);
        Toast.makeText(getApplicationContext(), alertMessage, Toast.LENGTH_SHORT).show();
    }
    
    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_PHOTO:
                break;
            case POST_STATUS_UPDATE:
                postStatusUpdate();
                break;
        }
    }
    
	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
	    for (String string : subset) {
	        if (!superset.contains(string)) {
	            return false;
	        }
	    }
	    return true;
	}
	
    private void postStatusUpdate() {
    	Log.d(TAG, "postStatusUpdate");
        Bundle postParams = new Bundle();
        postParams.putString("name", selectedApp.getDisplayName() + " Risk Report");
        postParams.putString("caption", "- powered by TrustLook Antivirus");
        postParams.putString("description", "Find the latest threat");
        postParams.putString("link", "https://www.trustlook.com");
        postParams.putString("picture", "http://www.trustlook.com/static/img/trustlook-logo3.png");
        postParams.putString("message", "");

        Session session = Session.getActiveSession();
        if (session == null) {
        	Log.d(TAG, "session is null");
        }
        List<String> permissions = session.getPermissions();
        if (!isSubsetOf(PERMISSIONS, permissions)) {
            // pendingPublishReauthorization = true;
            Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(this, PERMISSIONS);
            session.requestNewPublishPermissions(newPermissionsRequest);
            Log.d(TAG, "requested publish permission");
        }
        
        if (hasPublishPermission()) {
        	Request.Callback callback= new Request.Callback() {
	            public void onCompleted(Response response) {
	            	showPublishResult("success!", response.getGraphObject(), response.getError());
                }
	        };
        	Request request = new Request(Session.getActiveSession(), "me/feed", postParams, 
                    HttpMethod.POST, callback);
            Log.d(TAG, "publishing " + postParams.getString("name") + "...");
            request.executeAsync();
        } else {
        	Log.d(TAG, "No publish permission, no op");
            // pendingAction = PendingAction.POST_STATUS_UPDATE;
        }
    }
    
    private void performPublish(PendingAction action) {
        Session session = Session.getActiveSession();
        if (session != null) {
        	Log.d(TAG, "1) Session: " + session + ", state: " + session.getState());
        	if (session.getState() != SessionState.OPENED) {
        		Session.openActiveSession(this, true, null);
        	}
            pendingAction = action;
            
            if (session.getState() != SessionState.OPENED) {
        		// start Facebook Login
        		Session.openActiveSession(this, true, new Session.StatusCallback() {

        			// callback when session changes state
        			@Override
        			public void call(Session session, SessionState state, Exception exception) {
        				if (session.isOpened()) {

        					// make request to the /me API
        					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {

        						// callback after Graph API response with user
        						// object
        						@Override
        						public void onCompleted(GraphUser user, Response response) {
        							if (user != null) {
        								Log.d(TAG, "user: " + user.getName());
        							}
        						}
        					});
        				}
        			}
        		});            	
            }
            Log.d(TAG, "2) Session: " + session + ", state: " + session.getState());
            if (hasPublishPermission()) {
                handlePendingAction();
            } else {
            	// handlePendingAction();
                // We need to get new permissions, then complete the action when we get called back.
                session.requestNewPublishPermissions(new Session.NewPermissionsRequest(this, PERMISSIONS));
            }
        }
    }
    /*
     * callback for the feed dialog which updates the profile status
     */
    public class UpdateStatusListener extends BaseDialogListener {
        @Override
        public void onComplete(Bundle values) {
        	Log.d(TAG, "UpdateStatusListener - onComplete");
            final String postId = values.getString("post_id");
            if (postId != null) {
            	Toast.makeText(getApplicationContext(), "Updaet Status executed", Toast.LENGTH_SHORT)
            	.show();
            } else {
            	Toast.makeText(getApplicationContext(), "No wall post made", Toast.LENGTH_SHORT)
            	.show();
            }
        }

        @Override
        public void onFacebookError(FacebookError error) {
            Toast.makeText(getApplicationContext(), "Facebook Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel() {
            Toast.makeText(getApplicationContext(), "Update status cancelled", Toast.LENGTH_SHORT)
            		.show();
        }
    }

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// TODO Auto-generated method stub
		selectedApp = appInfoList.get(position);
		launchAppOPDialog();
	}
	@Override
	public void onResume() {
	    super.onResume();
	    uiHelper.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    uiHelper.onDestroy();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}

}
