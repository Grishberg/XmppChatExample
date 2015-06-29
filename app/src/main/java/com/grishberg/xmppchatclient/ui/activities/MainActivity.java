package com.grishberg.xmppchatclient.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.ui.fragments.UserListFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithUserListFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractionUserListWithActivity;

/**
 * Main activity is Chat rooms screen
 */
public class MainActivity extends AppCompatActivity implements
		IInteractionUserListWithActivity {

	private static final int REQUEST_CODE_LOGIN = 0;
	private static final int REQUEST_CODE_NEW_MUC = 1;
	private ApiService	mService;
	private boolean 	mIsBound;
	private boolean 	mIsLogined;
	private String		mCurrentLogin;
	private IInteractWithUserListFragment mUserListFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getSupportFragmentManager().beginTransaction()
				.add(R.id.userlist_fragment_container,UserListFragment.newInstance())
				.commit();

		startService(new Intent(this, ApiService.class));
		bindService( new Intent(this, ApiService.class), mServiceConnection, Context.BIND_AUTO_CREATE);

		//if not logined - open Login activity
		startActivityForResult(new Intent(this, LoginActivity.class), REQUEST_CODE_LOGIN);
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


	private void onLogined(){
		// download Roster
		setTitle(mCurrentLogin);
	}

	@Override
	public void onRegister(IInteractWithUserListFragment fragment) {
		mUserListFragment	= fragment;
	}

	@Override
	public void onUnregister(IInteractWithUserListFragment fragment) {
		mUserListFragment	= null;
	}

	/**
	 * event when user click on user's list
	 * @param id
	 */
	@Override
	public void onUserItemClicked(long id) {
		Intent intent	= new Intent(this, ChatActivity.class);
		intent.putExtra(ChatActivity.EXTRA_CHAT_ID, id);
		startActivity(intent);
	}

	/**
	 * delete jid from roster
	 * @param userId
	 */
	@Override
	public void deleteUserFromRoster(long userId) {
		if(mIsBound){
			mService.deleteUserFromRoster(userId);
		}
	}

	// binding/ unbinding to service
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

	// response from LoginActivity
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_CODE_LOGIN) {
			if (resultCode == LoginActivity.RESULT_CODE_LOGINED) {
				// we are logined
				mIsLogined 		= true;
				mCurrentLogin	= data.getStringExtra(LoginActivity.EXTRA_JID);
				onLogined();
			} else
			{

			}
		}
	}

	//------ menu -----
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		switch (id){
			case R.id.action_online:
			if(mIsBound){
				mService.connect();
			}
			return true;

			case R.id.action_offline:
				if(mIsBound){
					mService.disconnect();
				}
				return true;

			case R.id.action_new_muc:
				startActivityForResult(new Intent(this, NewMucActivity.class), REQUEST_CODE_NEW_MUC);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	//-- end menu ----------------


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mIsBound){
			mService.disconnect();
		}
	}
}
