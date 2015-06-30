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
import android.widget.Toast;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.framework.ChatConstants;

public class NewMucActivity extends AppCompatActivity
		implements View.OnClickListener {

	public static final String EXTRA_NEW_MUC_STATUS	= "newMucStatus";

	private Button 		mNewMucButton;
	private EditText 	mHostEdit;
	private EditText 	mRoomEdit;
	private EditText 	mNicknameEdit;
	private EditText 	mPasswordEdit;
	private ProgressBar mProgress;
	private ApiService 	mService;
	private boolean		mIsBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_muc);

		mHostEdit		= (EditText) findViewById(R.id.activity_new_muc_host);
		mRoomEdit		= (EditText) findViewById(R.id.activity_new_muc_room);
		mNicknameEdit	= (EditText) findViewById(R.id.activity_new_muc_nickname);
		mPasswordEdit	= (EditText) findViewById(R.id.activity_new_muc_password);
		mHostEdit.setText(AppController.getMucHost());
		mRoomEdit.setText(AppController.getMucRoom());
		mNicknameEdit.setText(AppController.getMucNick());
		mPasswordEdit.setText(AppController.getMucPassword());

		mProgress		= (ProgressBar) findViewById(R.id.activity_new_muc_progress);
		mNewMucButton	= (Button) findViewById(R.id.activity_new_user_apply_button);
		mNewMucButton.setOnClickListener(this);
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
		if(!TextUtils.isEmpty(mHostEdit.getText().toString()) &&
				!TextUtils.isEmpty(mRoomEdit.getText().toString()) &&
				!TextUtils.isEmpty(mNicknameEdit.getText().toString()) ){
			// parse login and server
			mProgress.setVisibility(View.VISIBLE);
			if(mIsBound) {
				disableFields();
				mService.joinMuc(mHostEdit.getText().toString()
						, mRoomEdit.getText().toString()
						, mNicknameEdit.getText().toString()
						, mPasswordEdit.getText().toString());
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
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mNewMucReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mNewMucReceiver, new IntentFilter(ApiService.ACTION_ON_NEW_MUC_RESULT));
		super.onResume();
	}

	private BroadcastReceiver mNewMucReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!intent.getAction().equals(ApiService.ACTION_ON_NEW_MUC_RESULT)){
				return;
			}
			final int joinStatus	= intent.getIntExtra(ApiService.EXTRA_JOIN_MUC_STATUS, -1);
			final long chatId		= intent.getLongExtra(ApiService.EXTRA_MUC_CHAT_ID, -1);
			switch (joinStatus){
				case ApiService.MUC_JOIN_STATUS_OK:
					onFinish(chatId);

					break;
				default:
					showMessage("Some error");
					enableFields();
					break;
			}

		}
	};

	/**
	 * start activity with Chat screen
	 * @param chatId id of multichat in table users
	 */
	private void onFinish(long chatId){

		AppController.setMucSettings(mHostEdit.getText().toString()
				, mRoomEdit.getText().toString()
				, mNicknameEdit.getText().toString()
				, mPasswordEdit.getText().toString() );
		Intent startActivityIntent	= new Intent(this, ChatActivity.class);
		startActivityIntent.putExtra(ChatActivity.EXTRA_CHAT_ID, chatId);
		startActivityIntent.putExtra(ChatActivity.EXTRA_CHAT_TYPE, ChatConstants.MULTICHAT_CHAT_STATE);
		startActivityIntent.putExtra(ChatActivity.EXTRA_CHAT_NAME, mRoomEdit.getText().toString() +
		"@" + mHostEdit.getText().toString());

		startActivity(startActivityIntent);

		finish();
	}

	private void disableFields(){
		mProgress.setVisibility(View.VISIBLE);
		mHostEdit.setEnabled(false);
		mRoomEdit.setEnabled(false);
		mNicknameEdit.setEnabled(false);
		mPasswordEdit.setEnabled(false);
		mNewMucButton.setEnabled(false);
	}

	private void enableFields() {
		mProgress.setVisibility(View.GONE);
		mHostEdit.setEnabled(true);
		mRoomEdit.setEnabled(true);
		mNicknameEdit.setEnabled(true);
		mPasswordEdit.setEnabled(true);
		mNewMucButton.setEnabled(true);
	}

	private void showMessage(String msg){
		//tODO: Toast
		Toast.makeText(this, msg, Toast.LENGTH_SHORT);
	}
}
