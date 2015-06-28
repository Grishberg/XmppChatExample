package com.grishberg.xmppchatclient.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;

public class FindUserActivity extends AppCompatActivity
		implements View.OnClickListener
{

	private Button 			mAddUserButton;
	private EditText 		mLoginEditText;
	private ProgressBar 	mProgress;
	private ApiService 		mService;
	private boolean			mIsBound;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_find_user);
		mAddUserButton	= (Button) findViewById(R.id.login_sign_in_button);
		mAddUserButton.setOnClickListener(this);

		mLoginEditText		= (EditText) findViewById(R.id.login_form_jid);
		mProgress			= (ProgressBar) findViewById(R.id.login_progress);
	}


	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService	= ((ApiService.MyBinder)service).getService();
			mIsBound	= true;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mIsBound	= false;
		}
	};

	@Override
	public void onClick(View v) {
		if(!TextUtils.isEmpty(mLoginEditText.getText().toString()) ){
			// parse login and server
			mProgress.setVisibility(View.VISIBLE);
			if(mIsBound) {
				mService.addUser(mLoginEditText.getText().toString(), null);
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		bindService( new Intent(this, ApiService.class), mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (!mIsBound) return;
		unbindService(mConnection);
		mIsBound = false;
	}

	@Override
	protected void onPause() {
		// Unregister since the activity is paused.
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter(ApiService.ACTION_ON_ROSTER_ADD_USER_RESULT));
		super.onResume();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mProgress.setVisibility(View.GONE);
			if(!intent.getAction().equals(ApiService.ACTION_ON_ROSTER_ADD_USER_RESULT)){
				return;
			}
			final int connectionStatus = intent.getIntExtra(ApiService.EXTRA_ADD_USER_STATUS, -1);

			finish();

		}
	};
}
