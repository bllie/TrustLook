package com.trustlook.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import com.facebook.*;
import com.facebook.model.*;

public class FBLoginActivity extends Activity {
	
    private UiLifecycleHelper uiHelper;
    private enum PendingAction {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }
    private PendingAction pendingAction = PendingAction.NONE;
    private final String PENDING_ACTION_BUNDLE_KEY = "com.trustlook.app:PendingAction";

    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("TL", "FBLoginActivity - onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fb_login);

		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		uiHelper = new UiLifecycleHelper(this, callback);
        uiHelper.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
            pendingAction = PendingAction.valueOf(name);
        }
		
		Session.openActiveSession(this, true, new Session.StatusCallback() {
			// callback when session changes state
			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if (session.isOpened()) {
					Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
						// callback after Graph API response with user object
						@Override
						public void onCompleted(GraphUser user, Response response) {
							if (user != null) {
								TextView welcome = (TextView) findViewById(R.id.fb_login_title);
								welcome.setText("Hello " + user.getName() + "!");
							}
						}
					});
				}
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
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();

        // updateUI();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);

        outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
	
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data) {
//		Log.d("TL", "FBLoginActivity - onActivityResult");
//		super.onActivityResult(requestCode, resultCode, data);
//		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
//	}
	
	   private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	        if (pendingAction != PendingAction.NONE &&
	                (exception instanceof FacebookOperationCanceledException ||
	                exception instanceof FacebookAuthorizationException)) {
	                new AlertDialog.Builder(FBLoginActivity.this)
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
	    @SuppressWarnings("incomplete-switch")
	    private void handlePendingAction() {
	        PendingAction previouslyPendingAction = pendingAction;
	        // These actions may re-set pendingAction if they are still pending, but we assume they
	        // will succeed.
	        pendingAction = PendingAction.NONE;

	        switch (previouslyPendingAction) {
	            case POST_PHOTO:
	                // postPhoto();
	                break;
	            case POST_STATUS_UPDATE:
	                // postStatusUpdate();
	                break;
	        }
	    }	   
}
