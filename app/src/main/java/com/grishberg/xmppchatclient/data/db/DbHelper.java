package com.grishberg.xmppchatclient.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by grigoriy on 25.06.15.
 */

public class DbHelper extends SQLiteOpenHelper {

	private static final String DB_NAME 			= "chat.db";
	private static final int 	DB_VERSION 			= 2;

	public static final String COLUMN_ID 			= "_id";

	public static final String TABLE_GROUPS			= "groups";
	public static final String GROUPS_NAME			= "name";
	public static final String INDEX_GROUPS			= GROUPS_NAME + "_idx";


	public static final String TABLE_USERS			= "users";
	public static final String USERS_GROUP_ID		= "group_id";
	public static final String USERS_LOGIN			= "login";
	public static final String USERS_NAME			= "name";
	public static final String INDEX_USERS			= USERS_LOGIN + "_idx";

	public static final String TABLE_CHATS			= "chats";
	public static final String CHATS_NAME			= "name";
	public static final String INDEX_CHATS			= CHATS_NAME + "_idx";
	//TODO: add some fields

	public static final String TABLE_MESSAGES		= "messages";
	public static final String MESSAGES_USER_ID		= "user_id";
	public static final String MESSAGES_CHAT_ID		= "chat_id";
	public static final String MESSAGES_CREATED		= "created";
	public static final String MESSAGES_VIEWED		= "viewed";
	public static final String MESSAGES_TEXT		= "text";


	private static final String CREATE_TABLE_GROUPS = "" +
			"CREATE TABLE " + TABLE_GROUPS + "(" +
			COLUMN_ID 		+ " integer primary key autoincrement," +
			GROUPS_NAME	+ " text" +
			");";

	private static final String CREATE_GROUPS_INDEX = "CREATE UNIQUE INDEX "
			+ INDEX_GROUPS + " ON " + TABLE_GROUPS + " ("
			+ GROUPS_NAME + " ASC);";

	private static final String CREATE_TABLE_USERS = "" +
			"CREATE TABLE " + TABLE_USERS + "(" +
			COLUMN_ID		+ " integer primary key autoincrement," +
			USERS_GROUP_ID	+ " integer," +
			USERS_LOGIN		+ " text," +
			USERS_NAME		+ " text" +
			");";

	private static final String CREATE_USERS_INDEX = "CREATE UNIQUE INDEX "
			+ INDEX_USERS + " ON " + TABLE_USERS + " ("
			+ USERS_LOGIN + " ASC);";

	private static final String CREATE_TABLE_CHATS = "" +
			"CREATE TABLE " + TABLE_CHATS + "(" +
			COLUMN_ID		+ " integer primary key autoincrement," +
			CHATS_NAME	+ " text" +
			");";

	private static final String CREATE_CHATS_INDEX = "CREATE UNIQUE INDEX "
			+ INDEX_CHATS + " ON " + TABLE_CHATS + " ("
			+ CHATS_NAME + " ASC);";

	private static final String CREATE_TABLE_MESSAGES = "" +
			"CREATE TABLE " + TABLE_MESSAGES + "(" +
			COLUMN_ID 				+ " integer primary key autoincrement," +
			MESSAGES_USER_ID   	+ " integer," +
			MESSAGES_CHAT_ID	+ " integer," +
			MESSAGES_CREATED	+ " integer," +
			MESSAGES_VIEWED 	+ " integer," +
			MESSAGES_TEXT 		+ " text" +
			");";

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String[] CREATES = {
				CREATE_TABLE_GROUPS,
				CREATE_TABLE_USERS,
				CREATE_TABLE_CHATS,
				CREATE_TABLE_MESSAGES,
				CREATE_CHATS_INDEX,
				CREATE_GROUPS_INDEX,
				CREATE_USERS_INDEX

		};
		for (final String table : CREATES) {
			db.execSQL(table);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String[] TABLES = {
				TABLE_GROUPS,
				TABLE_USERS,
				TABLE_CHATS,
				TABLE_MESSAGES
		};
		String[] INDEXES = {
				INDEX_CHATS,
				INDEX_USERS,
				INDEX_GROUPS
		};

		for (final String table : TABLES) {
			db.execSQL("DROP TABLE IF EXISTS " + table);
		}

		for (final String index : INDEXES) {
			db.execSQL("DROP INDEX IF EXISTS " + index);
		}
		onCreate(db);
	}

}