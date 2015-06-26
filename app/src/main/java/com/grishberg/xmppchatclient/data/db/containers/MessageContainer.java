package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

import com.grishberg.xmppchatclient.data.db.DbHelper;

import java.util.Date;

/**
 * Created by grigoriy on 25.06.15.
 */
public class MessageContainer {
	private long id;
	private long userId;
	private Date created;
	private String body;
	private String subject;

	private MessageContainer(long id, long userId, long created
			,String body, String subject){
		this.id			= id;
		this.userId		= userId;
		this.created	= new Date(created);
		this.body		= body;
		this.subject	= subject;
	}
	public MessageContainer(long userId,  long created
			,String body, String subject){
		this(-1, userId, created
				, body, subject);
	}

	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.MESSAGES_USER_ID, 	userId);
		cv.put(DbHelper.MESSAGES_CREATED, 	created.getTime());
		cv.put(DbHelper.MESSAGES_BODY, 		body);
		cv.put(DbHelper.MESSAGES_SUBJECT, 	subject);

		return cv;
	}

	public static MessageContainer fromCursor(Cursor c){
		int idColId 		= c.getColumnIndex(DbHelper.COLUMN_ID);
		int userIdColId 	= c.getColumnIndex(DbHelper.MESSAGES_USER_ID);
		int createdIdColId 	= c.getColumnIndex(DbHelper.MESSAGES_CREATED);
		int bodyIdColId 	= c.getColumnIndex(DbHelper.MESSAGES_BODY);
		int subjectColId 	= c.getColumnIndex(DbHelper.MESSAGES_SUBJECT);

		return new MessageContainer(
				c.getLong(idColId),
				c.getLong(userIdColId),
				c.getLong(createdIdColId),
				c.getString(bodyIdColId),
				c.getString(subjectColId));
	}
}
