package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

import com.grishberg.xmppchatclient.data.db.DbHelper;

/**
 * Created by grigoriy on 25.06.15.
 */
public class User {
	private long id;
	private long groupId;
	private long multiuserId;
	private String login;
	private String name;
	private boolean authorized;
	private int	onlineStatus;


	private User(long id, String login, String name, long groupId, long multiuserId
			,boolean authorized,int onlineStatus ) {
		this.id = id;
		this.login			= login;
		this.name 			= name;
		this.groupId		= groupId;
		this.multiuserId	= multiuserId;
		this.authorized		= authorized;
		this.onlineStatus	= onlineStatus;
	}

	public User(String login, String name, long groupId, long multiuserId, boolean authorized
		,int onlineStatus) {
		this(-1, login, name, groupId, multiuserId, authorized, onlineStatus);
	}


	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.USERS_JID, 		login);
		cv.put(DbHelper.USERS_NAME, 		name);
		cv.put(DbHelper.USERS_GROUP_ID, 	groupId);
		cv.put(DbHelper.USERS_MULTIUSER_ID, multiuserId);
		cv.put(DbHelper.USERS_AUTHORIZED, 	authorized ? 1: 0);
		cv.put(DbHelper.USERS_ONLINE_STATUS, onlineStatus);
		return cv;
	}

	public static User fromCursor(Cursor c){
		int idColId 		= c.getColumnIndex(DbHelper.COLUMN_ID);
		int loginColId 		= c.getColumnIndex(DbHelper.USERS_JID);
		int multiuserIdColId= c.getColumnIndex(DbHelper.USERS_MULTIUSER_ID);
		int nameColId 		= c.getColumnIndex(DbHelper.USERS_NAME);
		int groupIdColId 	= c.getColumnIndex(DbHelper.USERS_GROUP_ID);
		int authorizedColId = c.getColumnIndex(DbHelper.USERS_AUTHORIZED);
		int onlineStatusColId	= c.getColumnIndex(DbHelper.USERS_ONLINE_STATUS);

		return new User(
				c.getLong(idColId),
				c.getString(loginColId),
				c.getString(nameColId),
				c.getLong(groupIdColId),
				c.getLong(multiuserIdColId),
				c.getLong(authorizedColId) == 1,
				c.getInt(onlineStatusColId)
		);
	}

	public long getId() {
		return id;
	}

	public long getGroupId() {
		return groupId;
	}

	public long getMultiuserId() {
		return multiuserId;
	}

	public String getLogin() {
		return login;
	}

	public String getName() {
		return name;
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public int getOnlineStatus() {
		return onlineStatus;
	}
}
