package com.grishberg.xmppchatclient.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.api.ApiService;
import com.grishberg.xmppchatclient.data.api.listeners.IInteractionWithService;

/**
 * Main activity is Chat rooms screen
 */
public class MainActivity extends AppCompatActivity {

	private static final int REQUEST_CODE_LOGIN = 0;
	private IInteractionWithService	mService;
	private boolean mIsBound;
	private boolean mIsLogined;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		startService(new Intent(this,ApiService.class));
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
	}

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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == REQUEST_CODE_LOGIN) {
			if (resultCode == LoginActivity.RESULT_CODE_LOGINED) {
				// we are logined
				mIsLogined = true;
				onLogined();
			} else
			{

			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_connect) {
			if(mIsBound){
				mService.connect("rebbe2015","Qqwe!123","jabber.ru");
			}
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mIsBound){
			mService.disconnect();
		}
	}
}
