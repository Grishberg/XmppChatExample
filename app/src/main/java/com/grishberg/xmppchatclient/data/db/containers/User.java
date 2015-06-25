package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

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
		this(-1, name, n);
	}


	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(AppSQLiteOpenHelper.COLUMN_ID, id);
		}
		cv.put(AppSQLiteOpenHelper.USERS_COLUMN_NAME, name);
		cv.put(AppSQLiteOpenHelper.USERS_COLUMN_CODE, code);
		return cv;
	}

	public static User fromCursor(Cursor c){
		int idColId = c.getColumnIndex(AppSQLiteOpenHelper.COLUMN_ID);
		int nameColId = c.getColumnIndex(AppSQLiteOpenHelper.USERS_COLUMN_NAME);
		int codeColId = c.getColumnIndex(AppSQLiteOpenHelper.USERS_COLUMN_CODE);

		return new User(
				c.getInt(idColId),
				c.getString(nameColId),
				c.getInt(codeColId));
	}
}
