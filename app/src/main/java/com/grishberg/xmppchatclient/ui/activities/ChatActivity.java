package com.grishberg.xmppchatclient.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.ui.fragments.ChatFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractChatWithActivity;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithChatFragment;

public class ChatActivity extends AppCompatActivity implements IInteractChatWithActivity{

	public static final String EXTRA_CHAT_ID = "extraChatId";
	private IInteractWithChatFragment mChatFragment;
	private ApiService	mService;
	private boolean 	mIsBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		Intent intent	= getIntent();
		long chatId		= intent.getLongExtra(EXTRA_CHAT_ID, 0);

		if(savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.activity_chat_fragmentplaceholder, ChatFragment.newInstance(chatId)
							, ChatFragment.class.getName())
					.commit();
		}
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
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

	//------- binding to service ---------
	@Override
	protected void onStart() {
		super.onStart();
		if(!mIsBound) {
			bindService(new Intent(this, ApiService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (!mIsBound) return;
		unbindService(mServiceConnection);
		mIsBound = false;
	}

	@Override
	public void onRegister(IInteractWithChatFragment fragment) {
		mChatFragment	= fragment;
	}

	@Override
	public void onUregister(IInteractWithChatFragment fragment) {
		mChatFragment	= null;
	}

	@Override
	public void onSendMessage(long userId, String message) {
		if(mIsBound){
			mService.sendMessage(userId, message);
		}
	}

	@Override
	public boolean isConnected() {
		if(mIsBound){
			return mService.isConnected();
		}
		return false;
	}

	// listen service for connect status change
	private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!intent.getAction().equals(ApiService.ACTION_ON_CONNECTION_STATUS_CHANGED )||
					mChatFragment == null){
				return;
			}
			int connectionStatus = intent.getIntExtra(ApiService.EXTRA_CONNECTION_STATUS, -1);
			if (connectionStatus == ApiService.CONNECTION_STATUS_OK) {
				// connected
				mChatFragment.onConnected();
			} else {
				// not connected
				mChatFragment.onDisconnected();
			}
		}
	};
}
