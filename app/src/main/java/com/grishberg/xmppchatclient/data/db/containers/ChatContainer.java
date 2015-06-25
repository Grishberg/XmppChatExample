package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

import com.grishberg.xmppchatclient.data.db.DbHelper;

/**
 * Created by grigoriy on 25.06.15.
 * MUC
 */
public class ChatContainer {
	private long id;
	private String name;
	private String subject;

	private ChatContainer(long id, String name, String subject){
		this.id			= id;
		this.name		= name;
		this.subject	= subject;
	}
	public ChatContainer(String name, String subject){
		this(-1, name, subject);
	}

	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.CHATS_NAME, name);
		cv.put(DbHelper.CHATS_SUBJECT, subject);
		return cv;
	}

	public static ChatContainer fromCursor(Cursor c){
		int idColId = c.getColumnIndex(DbHelper.COLUMN_ID);
		int nameColId = c.getColumnIndex(DbHelper.CHATS_NAME);
		int subjectColId = c.getColumnIndex(DbHelper.CHATS_SUBJECT);

		return new ChatContainer(
				c.getLong(idColId),
				c.getString(nameColId),
				c.getString(subjectColId));
	}
}
