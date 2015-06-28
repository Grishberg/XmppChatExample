package com.grishberg.xmppchatclient.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;

public class LoginActivity extends AppCompatActivity
implements View.OnClickListener{

	public static final int RESULT_CODE_LOGINED 	= 1;
	public static final int RESULT_CODE_NOT_LOGINED = 2;
	public static final String RESULT_PARAM_NAME	= "extraName";

	private Button mSiginButton;
	private EditText mLoginEditText;
	private EditText mPasswordEditText;
	private String		mLogin;
	private String		mServer;
	private String		mPassword;
	private ProgressBar	mProgress;
	private ApiService mService;
	private boolean			mIsBound;
	private boolean			mIsNeedConnect;
	private boolean			mIsChangeProfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mSiginButton	= (Button) findViewById(R.id.login_sign_in_button);
		mSiginButton.setOnClickListener(this);

		mLoginEditText		= (EditText) findViewById(R.id.login_form_jid);
		mPasswordEditText	= (EditText) findViewById(R.id.login_form_password);
		mProgress			= (ProgressBar) findViewById(R.id.login_progress);
		mLogin				= AppController.getLogin();
		mServer				= AppController.getServer();
		mPassword			= AppController.getPassword();

		if(!TextUtils.isEmpty(mLogin)){
			mLoginEditText.setText(mLogin +"@" + mServer);
			mPasswordEditText.setText(mPassword);
			mIsNeedConnect	= true;
		}

	}

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService	= ((ApiService.MyBinder)service).getService();
			mIsBound	= true;
			if(mIsNeedConnect){
				connect();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mIsBound	= false;
		}
	};


	@Override
	public void onClick(View v) {
		if(!TextUtils.isEmpty(mLoginEditText.getText().toString()) &&
				!TextUtils.isEmpty(mPasswordEditText.getText().toString())){
			// parse login and server
			int atPos = mLoginEditText.getText().toString().indexOf("@");
			if(atPos < 0)
			{
				showMessage(getString(R.string.error_invalid_jid));
				return;
			}
			mLogin		= mLoginEditText.getText().toString().substring(0,atPos);
			mServer		= mLoginEditText.getText().toString().substring(atPos + 1);
			mPassword	= mPasswordEditText.getText().toString();
			connect();
		}
	}

	private void connect() {
		if(mIsBound) {
			mProgress.setVisibility(View.VISIBLE);
			mLoginEditText.setEnabled(false);
			mPasswordEditText.setEnabled(false);
			mSiginButton.setEnabled(false);
			mService.connect(mLogin,mPassword, mServer);
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
				mMessageReceiver, new IntentFilter(ApiService.ACTION_ON_CONNECTED_RESULT));
		super.onResume();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mProgress.setVisibility(View.GONE);
			if(!intent.getAction().equals(ApiService.ACTION_ON_CONNECTED_RESULT)){
				return;
			}
			final int connectionStatus = intent.getIntExtra(ApiService.EXTRA_CONNECTION_STATUS, -1);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					onProcessLoginRequest(connectionStatus);
				}
			});


		}
	};
	private void onProcessLoginRequest(int connectionStatus){
		switch (connectionStatus){
			case ApiService.CONNECTION_STATUS_OK:
				onFinish();
				// close activity
				break;
			case ApiService.CONNECTION_STATUS_ERROR_CONNECTION:
				showMessage(getString(R.string.error_connection_problem));
				enableFields();
				break;
			case ApiService.CONNECTION_STATUS_BAD_PASSWORD:
				showMessage(getString(R.string.error_invalid_login));
				enableFields();
				break;
			case ApiService.CONNECTION_STATUS_BAD_SERVER:
				showMessage(getString(R.string.error_incorrect_server));
				enableFields();
				break;
			case ApiService.CONNECTION_STATUS_ERROR_NORESPONSE:
				showMessage(getString(R.string.error_not_response));
				enableFields();
				break;
			default:
				showMessage(getString(R.string.error_unknown));
				enableFields();
			break;
		}
	}

	private void onFinish(){
		// store login
		AppController.setUserSettings(mLogin, mPasswordEditText.getText().toString()
				,mServer );
		setResult(RESULT_OK);
		finish();
	}

	private void enableFields() {
		mSiginButton.setEnabled(true);
		mLoginEditText.setEnabled(true);
		mPasswordEditText.setEnabled(true);
	}

	private void showMessage(String msg){
		//tODO: Toast
		Toast.makeText(this, msg, Toast.LENGTH_SHORT);
	}

	@Override
	public void onBackPressed() {
		if(mIsChangeProfile) {
			super.onBackPressed();
		}
	}

}
