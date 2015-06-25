package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

import com.grishberg.xmppchatclient.data.db.DbHelper;

/**
 * Created by G on 25.06.15.
 * relations between user
 */
public class Relation {
	private long id;
	private long userId;
	private long chatId;

	private Relation(long id, long userId, long chatId) {
		this.id = id;
		this.userId	= userId;
		this.chatId	= chatId;
	}

	public Relation(long userId, long chatId) {
		this(-1, userId, chatId);
	}

	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.RELATIONS_USER_ID, userId);
		cv.put(DbHelper.RELATIONS_CHAT_ID, chatId);
		return cv;
	}

	public static Relation fromCursor(Cursor c){
		int idColId 		= c.getColumnIndex(DbHelper.COLUMN_ID);
		int userIdColId 	= c.getColumnIndex(DbHelper.RELATIONS_USER_ID);
		int chatIdColId 	= c.getColumnIndex(DbHelper.RELATIONS_CHAT_ID);

		return new Relation(
				c.getLong(idColId),
				c.getLong(userIdColId),
				c.getLong(chatIdColId));
	}
}
