package com.grishberg.xmppchatclient.data.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.grishberg.xmppchatclient.data.api.listeners.IInteractionWithService;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.util.Collection;

public class ApiService extends Service implements IInteractionWithService
		, ChatManagerListener
		, MessageListener
		, ChatMessageListener
		, RosterListener {
	private static final String TAG = "XmppChat.ApiService";
	public static final String EVENT_ON_CONNECTED_RESULT 	= "onConnectedResult";
	public static final String EXTRA_CONNECTION_STATUS 		= "connectionStatus";

	public static final int CONNECTION_STATUS_OK				= 0;
	public static final int CONNECTION_STATUS_BAD_PASSWORD		= 1;
	public static final int CONNECTION_STATUS_BAD_SERVER		= 2;
	public static final int CONNECTION_STATUS_ERROR_CONNECTION	= 3;

	private AbstractXMPPConnection 	mConnection;
	private ChatManager 		mChatManager;
	private Handler 			mConnectionHandler;
	private Roster 				mRoster;
	private String				mLogin;
	private String				mPassword;
	private String				mServer;
	private Thread				mConnectionThread;

	private MyBinder binder = new MyBinder();

	public ApiService() {
		mConnectionHandler	= new Handler();
	}

	@Override
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

	@Override
	public void disconnect() {
		mConnection.disconnect();
	}

	private void doConnect(){
		try {
			// Create the configuration for this new connection
			XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
			configBuilder.setUsernameAndPassword(mLogin, mPassword);
			configBuilder.setResource("mobile");
			configBuilder.setServiceName(mServer);
			configBuilder.setHost(mServer);

			mConnection = new XMPPTCPConnection(configBuilder.build());
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
			Collection<RosterEntry> entries = mRoster.getEntries();
			for (RosterEntry entry : entries) {
				Log.d(TAG, "roster element "+entry.getName());
			}

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
		catch (Exception e){
			e.printStackTrace();
			sendOnConnectedMessage(CONNECTION_STATUS_BAD_PASSWORD);
		}
	}

	/**
	 * event when incoming chat
	 * @param chat
	 * @param createdLocally
	 */
	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		//TODO:
		Log.d(TAG, "on chat created");
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

	//------------------- end roster events --------------------

	private void sendOnConnectedMessage(int msg){
		Intent intent = new Intent(EVENT_ON_CONNECTED_RESULT);
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
		public IInteractionWithService getService() {
			return ApiService.this;
		}
	}
}
