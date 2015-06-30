package com.grishberg.xmppchatclient.data.api;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;


import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.GroupContainer;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.data.db.containers.User;
import com.grishberg.xmppchatclient.framework.ChatConstants;
import com.grishberg.xmppchatclient.framework.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntries;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


public class ApiService extends Service implements
	ConnectionListener {
	private static final String TAG = "XmppChat.ApiService";
	public static final String ACTION_ON_CONNECTED_RESULT 			= "onConnectedResult";

	public static final String ACTION_ON_CONNECTION_STATUS_CHANGED 	= "onConnectionChanged";
	public static final String ACTION_ON_NEW_MUC_RESULT 			= "onNewMucResult";

	//roster
	public static final String ACTION_ON_ROSTER_ADD_USER_RESULT 	= "onRosterAddUser";
	public static final int ROSTER_ADD_USER_STATUS_OK 				= 6;

	//muc
	public static final String ACTION_ON_MUC_JOIN_RESULT 			= "onMucJoin";
	public static final int MUC_JOIN_STATUS_OK 						= 1;
	public static final String EXTRA_JOIN_MUC_STATUS 				= "joinMucStatus";
	public static final String EXTRA_MUC_CHAT_ID	 				= "mucChatId";
	public static final int MUC_JOIN_STATUS_NOT_RESPONSE 			= 2;
	public static final int MUC_JOIN_STATUS_OTHER_ERROR				= 10;

	public static final String EXTRA_CONNECTION_STATUS 				= "connectionStatus";
	public static final String EXTRA_ADD_USER_STATUS 				= "addUserStatus";

	public static final int CONNECTION_STATUS_OK				= 0;
	public static final int CONNECTION_STATUS_BAD_PASSWORD		= 1;
	public static final int CONNECTION_STATUS_BAD_SERVER		= 2;
	public static final int CONNECTION_STATUS_ERROR_CONNECTION	= 3;
	public static final int CONNECTION_STATUS_ERROR_NORESPONSE	= 4;
	public static final int CONNECTION_STATUS_ERROR_UNKNOWN		= 5;


	private Handler 			mConnectionHandler;
	private String				mLogin;
	private String				mPassword;
	private String				mServer;
	private Thread				mConnectionThread;
	private int 				mStartMode = START_REDELIVER_INTENT;



	private volatile AbstractXMPPConnection	mConnection;
	private XmppMessageManager 				mMessageManager;
	private XmppRosterManager				mRosterManager;
	private XmppMucManager					mMucManager;



	private MyBinder binder = new MyBinder();

	public ApiService() {
		mConnectionHandler	= new Handler();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG,"Start service");
		return mStartMode;
	}

	public void connect(){
		if(!TextUtils.isEmpty(mLogin)){
			connect(mLogin, mPassword, mServer);
		}
	}

	public void connect(String login, String password, String server ) {
		mLogin		= login;
		mPassword	= password;
		mServer		= server;
		if(mConnection != null && mConnection.isAuthenticated()){
			// not need connect
			sendOnConnectedMessage(CONNECTION_STATUS_OK);
			Log.d(TAG,"not need connect, connected");
		} else {
			mConnectionThread = new Thread(new Runnable() {
				@Override
				public void run() {
					doConnect();
				}
			});
			mConnectionThread.start();
		}
	}

	public void disconnect() {
		mConnection.disconnect();
	}

	private void doConnect(){
		try {
			QueryHelper.setOfflineStatus();
			// Create the configuration for this new connection
			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword(mLogin, mPassword);
			configBuilder.setResource("mobile");
			configBuilder.setServiceName(mServer);
			configBuilder.setSendPresence(true);
			configBuilder.setHost(mServer);

			mConnection = new XMPPTCPConnection(configBuilder.build());
			mConnection.addConnectionListener(this);
			// Connect to the server
			mConnection.connect();
			// Log into the server
			mConnection.login();

			mRosterManager 	= new XmppRosterManager(this, mConnection);
			mMessageManager	= new XmppMessageManager(this, mConnection);
			mMucManager		= new XmppMucManager(this, mConnection);

			// Create a new presence. Pass in false to indicate we're unavailable._
			Presence presence = new Presence(Presence.Type.available);
			presence.setStatus("Working");
			// Send the packet (assume we have an XMPPConnection instance called "con").
			mConnection.sendStanza(presence);
			QueryHelper.updateUser(ChatConstants.CURRENT_LOCAL_USER_ID, mLogin+"@"+mServer, mLogin);
			sendOnConnectedMessage(CONNECTION_STATUS_OK);
			Log.d(TAG,"on connected");
		}
		catch (SmackException.ConnectionException e){
			sendOnConnectedMessage(CONNECTION_STATUS_ERROR_CONNECTION);
		}
		catch (SmackException.NoResponseException e){
			sendOnConnectedMessage(CONNECTION_STATUS_ERROR_NORESPONSE);
		}
		catch (Exception e){
			e.printStackTrace();
			sendOnConnectedMessage(CONNECTION_STATUS_ERROR_UNKNOWN);
		}
	}


	public void sendMessage(int chatType, long chatId, String messageText){
		switch (chatType) {
			case ChatConstants.SINGLE_CHAT_STATE:
			if (mMessageManager != null) {
				mMessageManager.sendMessage(chatId, messageText);
			}
				break;

			case ChatConstants.MULTICHAT_CHAT_STATE:
				if(mMucManager != null){
					mMucManager.sendMessage(chatId, messageText);
				}
				break;
		}
	}

 	public void addUser(String jid, String name, String group){
		if(mRosterManager!= null){
			mRosterManager.addUser( jid, name, group);
		}
	}

	// delete user
	public void deleteUserFromRoster(long userId){
		if(mRosterManager != null){
			mRosterManager.deleteUserFromRoster(userId);
		}
	}

	// MUC
	public void joinMuc( String host,  String room,  String nickname, String password){
		if(mMucManager != null){
			mMucManager.addJoinMuc(host,room, nickname, password);
		}
	}

	/**
	 * need call when leave chat
	 */
	public void leaveMuc(){
		if(mMucManager != null){
			mMucManager.leaveMuc();
		}
	}



	//----------- Connection listener ---------------
	@Override
	public void connected(XMPPConnection connection) {
		Log.d(TAG,"on connected" );
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		sendOnConnectionStatusChanged(CONNECTION_STATUS_OK);
		Log.d(TAG, "authenticated resumed="+resumed);
	}

	@Override
	public void connectionClosed() {
		sendOnConnectionStatusChanged(CONNECTION_STATUS_ERROR_CONNECTION);

	}

	@Override
	public void connectionClosedOnError(Exception e) {
		//sendOnConnectionStatusChanged(CONNECTION_STATUS_ERROR_CONNECTION);
		Log.d(TAG, "connectionClosedOnError err="+e.getMessage() );
	}

	@Override
	public void reconnectionSuccessful() {
		sendOnConnectionStatusChanged(CONNECTION_STATUS_OK);
	}

	@Override
	public void reconnectingIn(int seconds) {
		Log.d(TAG, "reconnectingIn sec="+seconds);
	}

	@Override
	public void reconnectionFailed(Exception e) {
		Log.d(TAG, "reconnectionFailed e="+e.getMessage());
	}

	public boolean isConnected() {
		return mConnection.isAuthenticated();
	}

	private void sendOnConnectionStatusChanged(int connectionStatus){
		Intent intent = new Intent(ACTION_ON_CONNECTION_STATUS_CHANGED);
		// You can also include some extra data.
		intent.putExtra(EXTRA_CONNECTION_STATUS, connectionStatus);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

	}

	private void sendOnConnectedMessage(int msg){
		Intent intent = new Intent(ACTION_ON_CONNECTED_RESULT);
		// You can also include some extra data.
		intent.putExtra(EXTRA_CONNECTION_STATUS, msg);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;

	}

	// service container for Activity
	public class MyBinder extends Binder{
		public ApiService getService() {
			return ApiService.this;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if(mConnection != null && mConnection.isConnected()){
			mConnection.disconnect();
		}

		if(mMucManager != null){
			mMucManager.leaveMuc();
		}
	}
}
