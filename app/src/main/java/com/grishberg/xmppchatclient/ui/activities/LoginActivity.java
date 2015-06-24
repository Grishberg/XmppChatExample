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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.data.api.listeners.IInteractionWithService;

public class LoginActivity extends AppCompatActivity
implements View.OnClickListener{

	private Button mSiginButton;
	private EditText mLoginEditText;
	private EditText mPasswordEditText;
	private String		mLogin;
	private String		mServer;
	private String		mPassword;
	private ProgressBar	mProgress;
	private IInteractionWithService mService;
	private boolean			mIsBound;
	private boolean			mIsChaneProfile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		mSiginButton	= (Button) findViewById(R.id.login_sign_in_button);
		mSiginButton.setOnClickListener(this);

		mLoginEditText		= (EditText) findViewById(R.id.login_form_jid);
		mPasswordEditText	= (EditText) findViewById(R.id.login_form_password);
		mProgress			= (ProgressBar) findViewById(R.id.login_progress);
		mLogin	= AppController.getLogin();
		mServer	= AppController.getServer();
		mPassword	= AppController.getPassword();

		if(!TextUtils.isEmpty(mLogin)){
			mLoginEditText.setText(mLogin +"@" + mServer);
			mPasswordEditText.setText(mPassword);
		}
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
			if(mIsBound) {
				mProgress.setVisibility(View.VISIBLE);
				mSiginButton.setEnabled(false);
				mService.connect(mLogin,mPasswordEditText.getText().toString(), mServer);
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
		// Register to receive messages.
		// We are registering an observer (mMessageReceiver) to receive Intents
		// with actions named "custom-event-name".
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mMessageReceiver, new IntentFilter(ApiService.EVENT_ON_CONNECTED_RESULT));
		super.onResume();
	}

	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			mProgress.setVisibility(View.GONE);
			int connectionStatus = intent.getIntExtra(ApiService.EXTRA_CONNECTION_STATUS, -1);
			switch (connectionStatus){
				case ApiService.CONNECTION_STATUS_OK:
					// store login
					AppController.setUserSettings(mLogin, mPasswordEditText.getText().toString()
							,mServer );
					finish();
					// close activity
					break;
				case ApiService.CONNECTION_STATUS_ERROR_CONNECTION:
					showMessage(getString(R.string.error_connection_problem));
					mSiginButton.setEnabled(true);
					break;
				case ApiService.CONNECTION_STATUS_BAD_PASSWORD:
					showMessage(getString(R.string.error_invalid_login));
					mSiginButton.setEnabled(true);
					break;
				case ApiService.CONNECTION_STATUS_BAD_SERVER:
					showMessage(getString(R.string.error_incorrect_server));
					mSiginButton.setEnabled(true);
					break;
			}
		}
	};

	private void showMessage(String msg){
		//tODO: Toast
		Toast.makeText(this,msg, Toast.LENGTH_SHORT);
	}

	@Override
	public void onBackPressed() {
		if(mIsChaneProfile) {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_login, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

}
