package com.grishberg.xmppchatclient.ui.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.DbHelper;
import com.grishberg.xmppchatclient.ui.fragments.ChatFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractChatWithActivity;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithChatFragment;

public class ChatActivity extends AppCompatActivity implements IInteractChatWithActivity{

	public static final String EXTRA_CHAT_ID 	= "extraChatId";
	public static final String EXTRA_CHAT_TYPE 	= "extraChatType";
	public static final String EXTRA_CHAT_NAME 	= "extraChatName";

	private IInteractWithChatFragment mChatFragment;
	private ApiService	mService;
	private boolean 	mIsBound;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		Intent intent	= getIntent();
		long chatId		= intent.getLongExtra(EXTRA_CHAT_ID, 0);

		// start getting from db additional data
		if(savedInstanceState == null) {
			new AsyncTascDbQuer().execute(chatId);
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

	/**
	 * send message
	 * @param chatId id of chat
	 * @param message string message for send
	 */
	@Override
	public void onSendMessage(int chatType, long chatId, String message) {
		if(mIsBound){
			mService.sendMessage(chatType, chatId, message);
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

	private class AsyncTascDbQuer extends AsyncTask<Long, Void, Boolean>{
		private int 	chatType;
		private String 	chatJid;
		private String 	chatNick;
		private long	chatId;

		@Override
		protected Boolean doInBackground(Long... params) {
			chatId	= params.length > 0 ? params[0] : -1;
			if(chatId == -1){
				return false;
			}
			Cursor cursor = AppController.getAppContext().getContentResolver()
					.query(AppContentProvider.getUsersUri( chatId )
							, new String[]{DbHelper.USERS_JID, DbHelper.USERS_NAME, DbHelper.USERS_MULTIUSER}
							, null, null, null);
			if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
				chatType	= cursor.getInt( cursor.getColumnIndex(DbHelper.USERS_MULTIUSER) );
				chatJid		= cursor.getString( cursor.getColumnIndex(DbHelper.USERS_JID) );
				chatNick	= cursor.getString( cursor.getColumnIndex(DbHelper.USERS_NAME) );
			}
			cursor.close();
			return true;
		}

		@Override
		protected void onPostExecute(Boolean param) {
			super.onPostExecute(param);
			if(param && isCancelled()==false){
				// add fragment
				setTitle(chatJid);
				getSupportFragmentManager().beginTransaction()
						.add(R.id.activity_chat_fragmentplaceholder
								, ChatFragment.newInstance(chatId, chatType, chatJid)
								, ChatFragment.class.getName())
						.commit();
			}
		}
	}
}
