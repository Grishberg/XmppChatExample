package com.grishberg.xmppchatclient.data.api;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.GroupContainer;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.data.db.containers.User;
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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


public class ApiService extends Service implements
		ChatManagerListener
		, MessageListener
		, ChatMessageListener
		, RosterListener
		, ConnectionListener{
	private static final String TAG = "XmppChat.ApiService";
	public static final String ACTION_ON_ROSTER_ADD_USER_RESULT 	= "onRosterAddUser";
	public static final String ACTION_ON_CONNECTED_RESULT 			= "onConnectedResult";

	public static final String ACTION_ON_CONNECTION_STATUS_CHANGED 	= "onConnectionChanged";
	public static final String EXTRA_CONNECTION_STATUS 				= "connectionStatus";
	public static final String EXTRA_ADD_USER_STATUS 				= "addUserStatus";

	public static final int CONNECTION_STATUS_OK				= 0;
	public static final int CONNECTION_STATUS_BAD_PASSWORD		= 1;
	public static final int CONNECTION_STATUS_BAD_SERVER		= 2;
	public static final int CONNECTION_STATUS_ERROR_CONNECTION	= 3;
	public static final int CONNECTION_STATUS_ERROR_NORESPONSE	= 4;
	public static final int CONNECTION_STATUS_ERROR_UNKNOWN		= 5;
	public static final int ROSTER_ADD_USER_STATUS_OK 			= 6;

	private ChatManager 		mChatManager;
	private Handler 			mConnectionHandler;
	private Roster 				mRoster;
	private String				mLogin;
	private String				mPassword;
	private String				mServer;
	private Thread				mConnectionThread;
	private int 				mStartMode = START_REDELIVER_INTENT;
	private Map<Long, Chat> 	mPrivateChats;
	private Map<Long, RosterEntry>	mUserList;

	private volatile AbstractXMPPConnection	mConnection;
	private volatile boolean				mIsConnected;
	private MyBinder binder = new MyBinder();

	public ApiService() {
		mConnectionHandler	= new Handler();
		mPrivateChats		= new HashMap<>();
		mUserList			= new HashMap<>();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG,"Start service");
		return mStartMode;
	}


	public void connect(String login, String password, String server ) {
		mLogin		= login;
		mPassword	= password;
		mServer		= server;
		mConnectionThread	= new Thread(new Runnable() {
			@Override
			public void run() {
				doConnect();
			}
		});
		mConnectionThread.start();
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

			// setup chat manager
			mChatManager	= ChatManager.getInstanceFor(mConnection);
			if(mChatManager != null){
				mChatManager.addChatListener(this);
			}

			// setup roster
			mRoster			= Roster.getInstanceFor(mConnection);
			if (!mRoster.isLoaded())
				mRoster.reloadAndWait();
			getGroupsAndUsers();


			// Create a new presence. Pass in false to indicate we're unavailable._
			Presence presence = new Presence(Presence.Type.available);
			presence.setStatus("Working");
			// Send the packet (assume we have an XMPPConnection instance called "con").
			mConnection.sendStanza(presence);

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

	/**
	 * store groups and users to DB
	 */
	private void getGroupsAndUsers() {

		// get groups
		RosterEntries rosterEntriesInterface = new RosterEntries() {
			@Override
			public void rosterEntires(Collection<RosterEntry> rosterEntries) {
				for (RosterEntry user : rosterEntries) {
					long groupId	= 0;
					Presence presence = mRoster.getPresence(user.getUser());
					for(RosterGroup group: user.getGroups()){
						// add group to DB if not exists
						GroupContainer groupContainer = new GroupContainer(group.getName());
						Uri groupUri	= AppController.getAppContext().getContentResolver()
								.insert(AppContentProvider.CONTENT_URI_GROUPS,groupContainer.buildContentValues());
						groupId = Long.valueOf(groupUri.getLastPathSegment());
						Log.d(TAG, "roster group "+group.getName());

					}
					String jid	= Utils.extractJid(user.getUser());
					String name	= user.getName();
					User userContainer = new User(jid, name, groupId, 0, false, 0);

					Uri userUri = AppController.getAppContext().getContentResolver()
							.insert(AppContentProvider.CONTENT_URI_USERS
									,userContainer.buildContentValues() );
					long userId = Long.valueOf( userUri.getLastPathSegment());
					mUserList.put(userId, user);

					Log.d(TAG, "	roster user " + user.getUser());
				}
			}
		};
		mRoster.getEntriesAndAddListener(this, rosterEntriesInterface);

	}

	/**
	 * event when incoming chat
	 * @param chat
	 * @param createdLocally
	 */
	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {

		Log.d(TAG, "on chat created");
		String jid	= Utils.extractJid(chat.getParticipant() );
		long userId	= QueryHelper.getUserByJid(jid);
		mPrivateChats.put(userId, chat);
		chat.addMessageListener(this);

	}

	/**
	 * event when incoming message
	 * @param message
	 */
	@Override
	public void processMessage(Message message) {
		//TODO: send to activity
		Log.d(TAG, "on received message");
	}


	/**
	 * event when icoming message from chat
	 * @param chat
	 * @param message
	 */
	@Override
	public void processMessage(Chat chat, Message message) {
		Log.d(TAG, "on message from chat");

		ContentResolver contentResolver = AppController.getAppContext().getContentResolver();

		// get user id
		long userId = QueryHelper.getUserByJid(Utils.extractJid(chat.getParticipant()));

		// store message to DB
		MessageContainer messageContainer = new MessageContainer(userId,userId, new Date().getTime(),
				true, true
				, message.getBody()
				, message.getSubject());

		contentResolver.insert(AppContentProvider.CONTENT_URI_MESSAGES
				, messageContainer.buildContentValues());

		//TODO: remove
		try {
			chat.sendMessage( message.getBody() );
		} catch (Exception e){

		}
	}

	public void sendMessage(final long userId, final String messageText){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doSendMessage(userId, messageText);
			}
		}).start();
	}

	private void doSendMessage(long userId, String messageText){
		if(mIsConnected){
			Chat currentChat = mPrivateChats.get(userId);
			if( currentChat == null){
				String jid	= QueryHelper.getJidById( userId );
				currentChat = mChatManager.createChat( jid );
				mPrivateChats.put(userId, currentChat);
			}
			try {
				currentChat.sendMessage(messageText);
				QueryHelper.storeMessage(1, userId, new Date(), messageText, null);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	public void deleteUserFromRoster(final long userId){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doDeleteUserFromRoster(userId);
			}
		}).start();
	}

	private void doDeleteUserFromRoster(long userId){
		if(mIsConnected){
			String jid = QueryHelper.getJidById(userId);
			RosterEntry entry = mUserList.get(userId);
			if(entry != null) {
				try {
					mRoster.removeEntry(entry);
					QueryHelper.deleteUser(userId);
				} catch (Exception e){

				}
			}
		}
	}
	//----------- Connection listener ---------------
	@Override
	public void connected(XMPPConnection connection) {
		Log.d(TAG,"on connected" );
	}

	@Override
	public void authenticated(XMPPConnection connection, boolean resumed) {
		mIsConnected = true;
		sendOnConnectionStatusChanged(CONNECTION_STATUS_OK);
		Log.d(TAG, "authenticated resumed="+resumed);
	}

	@Override
	public void connectionClosed() {
		mIsConnected = false;
		sendOnConnectionStatusChanged(CONNECTION_STATUS_ERROR_CONNECTION);

	}

	@Override
	public void connectionClosedOnError(Exception e) {
		mIsConnected = false;
		//sendOnConnectionStatusChanged(CONNECTION_STATUS_ERROR_CONNECTION);
		Log.d(TAG, "connectionClosedOnError err="+e.getMessage() );
	}

	@Override
	public void reconnectionSuccessful() {
		mIsConnected = true;
		sendOnConnectionStatusChanged(CONNECTION_STATUS_OK);
	}

	@Override
	public void reconnectingIn(int seconds) {
		mIsConnected = false;
	}

	@Override
	public void reconnectionFailed(Exception e) {
		mIsConnected = false;
	}

	//------ Roster events ------------

	@Override
	public void entriesAdded(Collection<String> addresses) {
		Log.d(TAG,"on entries added");
	}

	@Override
	public void entriesUpdated(Collection<String> addresses) {
		Log.d(TAG," on entries updated");
	}

	@Override
	public void entriesDeleted(Collection<String> addresses) {
		Log.d(TAG,"entries deleted");
	}

	@Override
	public void presenceChanged(Presence presence) {
		Log.d(TAG,"on presence change");

	}
	public void addUser(String jid, String name){
		//TODO: run in thread
		if(mIsConnected){
			try {

				mRoster.createEntry(jid, name, null);
				sendOnUserAddedToRoster(ROSTER_ADD_USER_STATUS_OK);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}
	//------------------- end roster events --------------------

	public boolean isConnected() {
		return mIsConnected;
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

	private void sendOnUserAddedToRoster(int msg){
		Intent intent = new Intent(ACTION_ON_ROSTER_ADD_USER_RESULT);
		// You can also include some extra data.
		intent.putExtra(EXTRA_ADD_USER_STATUS, msg);
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
}
