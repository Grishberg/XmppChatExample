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
	private String login;
	private String name;


	private User(long id, String login, String name, long groupId) {
		this.id = id;
		this.login	= login;
		this.name = name;
		this.groupId	= groupId;
	}

	public User(String login, String name, long groupId) {
		this(-1, login, name, groupId);
	}


	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.USERS_LOGIN, login);
		cv.put(DbHelper.USERS_NAME, name);
		cv.put(DbHelper.USERS_GROUP_ID, groupId);
		return cv;
	}

	public static User fromCursor(Cursor c){
		int idColId 		= c.getColumnIndex(DbHelper.COLUMN_ID);
		int loginColId 		= c.getColumnIndex(DbHelper.USERS_LOGIN);
		int nameColId 		= c.getColumnIndex(DbHelper.USERS_NAME);
		int groupIdColId 	= c.getColumnIndex(DbHelper.USERS_GROUP_ID);

		return new User(
				c.getLong(idColId),
				c.getString(loginColId),
				c.getString(nameColId),
				c.getLong(groupIdColId));
	}
}
