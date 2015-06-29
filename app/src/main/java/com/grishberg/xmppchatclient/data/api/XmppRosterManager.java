package com.grishberg.xmppchatclient.data.api;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.GroupContainer;
import com.grishberg.xmppchatclient.data.db.containers.User;
import com.grishberg.xmppchatclient.framework.ChatConstants;
import com.grishberg.xmppchatclient.framework.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntries;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by grigoriy on 29.06.15.
 */
public class XmppRosterManager implements RosterListener {

	private static final String TAG = "XmppChat.RosterManager";

	private Context					mContext;
	private AbstractXMPPConnection 	mConnection;
	private Roster 					mRoster;
	private Map<Long, RosterEntry> mUserList;

	public XmppRosterManager(Context context, AbstractXMPPConnection connection){
		mContext	= context;
		mConnection	= connection;
		// setup roster
		mUserList			= new HashMap<>();
		try {
			mRoster = Roster.getInstanceFor(mConnection);
			if (!mRoster.isLoaded())
				mRoster.reloadAndWait();
		} catch (Exception e){

		}
		getGroupsAndUsers();
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

					Presence presence 	= mRoster.getPresence(user.getUser());
					processPresence(userId, presence);


					Log.d(TAG, "	roster user " + user.getUser());
				}
			}
		};
		mRoster.getEntriesAndAddListener(this, rosterEntriesInterface);

	}

	// delete user
	public void deleteUserFromRoster(final long userId){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doDeleteUserFromRoster(userId);
			}
		}).start();
	}

	private void doDeleteUserFromRoster(long userId){
		if(mConnection.isAuthenticated()){
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

	// add user
	public void addUser(final String jid, final String name, final String group){
		//TODO: run in thread
		if(mConnection.isAuthenticated()){
			new Thread(new Runnable() {
				@Override
				public void run() {
					doAddUser(jid, name, group);
				}
			}).start();
		}
	}

	private void doAddUser(String jid, String name, String group){
		try {

			mRoster.createEntry(jid, name, new String[] { group } );
			sendOnUserAddedToRoster(ApiService.ROSTER_ADD_USER_STATUS_OK);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

	private int processPresence(long userId, Presence presence){
		int presenseDbCode = 0;
		Presence.Type type 	= presence.getType();
		Presence.Mode mode	= presence.getMode();
		switch (type){
			case available:
				presenseDbCode	= ChatConstants.USER_STATUS_AVAILIBLE;
				break;
			case unavailable:
				presenseDbCode	= ChatConstants.USER_STATUS_UNAVAILIBLE;
				break;
			case subscribe:
				break;
			case unsubscribe:
				break;
			case unsubscribed:
				break;

		}

		switch (mode){
			case available:
//				presenseDbCode	= ChatConstants.USER_STATUS_AVAILIBLE;
				break;
			case chat:
				presenseDbCode	= ChatConstants.USER_STATUS_CHAT;
				break;
			case away:
//				presenseDbCode	= ChatConstants.USER_STATUS_AWAY;
				break;
			case xa:
//				presenseDbCode	= ChatConstants.USER_STATUS_XA;
				break;
			case dnd:
//				presenseDbCode	= ChatConstants.USER_STATUS_DND;
				break;

		}
		QueryHelper.setOnlineStatus(userId, presenseDbCode);
		return presenseDbCode;
	}

	/**
	 * change user status in thread
	 * @param presence
	 */
	private void doChangePresence(Presence presence){

		String jid	= Utils.extractJid(presence.getFrom());
		long userId	= QueryHelper.getUserByJid( jid );
		processPresence(userId, presence);

	}
	//------ Roster events ------------

	@Override
	public void entriesAdded(Collection<String> addresses) {
		Log.d(TAG, "on entries added");
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
	public void presenceChanged(final Presence presence) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				doChangePresence(presence);
			}
		}).start();
		Log.d(TAG, "on presence change");

	}

	private void sendOnUserAddedToRoster(int msg){
		Intent intent = new Intent(ApiService.ACTION_ON_ROSTER_ADD_USER_RESULT);
		// You can also include some extra data.
		intent.putExtra(ApiService.EXTRA_ADD_USER_STATUS, msg);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}
	//------------------- end roster events --------------------
}
