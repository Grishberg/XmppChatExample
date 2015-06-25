package com.grishberg.xmppchatclient.data.db.containers;

import android.content.ContentValues;
import android.database.Cursor;

import com.grishberg.xmppchatclient.data.db.DbHelper;

/**
 * Created by grigoriy on 25.06.15.
 */
public class Group {
	private long id;
	private String name;

	private Group(long id, String name){
		this.id		= id;
		this.name	= name;
	}
	private Group(String name){
		this(-1, name);
	}

	public ContentValues buildContentValues() {
		ContentValues cv = new ContentValues();
		if (id >= 0) {
			cv.put(DbHelper.COLUMN_ID, id);
		}
		cv.put(DbHelper.GROUPS_NAME, name);
		return cv;
	}

	public static Group fromCursor(Cursor c){
		int idColId = c.getColumnIndex(DbHelper.COLUMN_ID);
		int nameColId = c.getColumnIndex(DbHelper.GROUPS_NAME);

		return new Group(
				c.getLong(idColId),
				c.getString(nameColId));
	}
}
