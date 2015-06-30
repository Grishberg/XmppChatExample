package com.grishberg.xmppchatclient.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by grigoriy on 25.06.15.
 */

public class DbHelper extends SQLiteOpenHelper {

	private static final String DB_NAME 			= "chat.db";
	private static final int 	DB_VERSION 			= 3;

	public static final String COLUMN_ID 			= "_id";

	// Groups
	public static final String TABLE_GROUPS			= "groups";
	public static final String GROUPS_NAME			= "name";

	//Users
	public static final String TABLE_USERS			= "users";
	public static final String USERS_GROUP_ID		= "group_id";
	public static final String USERS_JID 			= "jid";
	public static final String USERS_NAME			= "name";
	public static final String USERS_MULTIUSER 		= "multiuser";
	public static final String USERS_ONLINE_STATUS	= "online_status";
	public static final String USERS_MUC_ROLE		= "muc_role";
	public static final String USERS_AUTHORIZED		= "authorized";

	public static final String INDEX_USERS			= USERS_JID + "_idx";

	//Messages
	public static final String TABLE_MESSAGES		= "messages";
	public static final String MESSAGES_USER_ID		= "user_id";
	public static final String MESSAGES_CHAT_ID		= "chat_id";
	public static final String MESSAGES_CREATED		= "created";
	public static final String MESSAGES_READED		= "readed";
	public static final String MESSAGES_SENDED		= "sended";
	public static final String MESSAGES_BODY		= "body";
	public static final String MESSAGES_SUBJECT		= "subject";

	// Groups
	private static final String CREATE_TABLE_GROUPS = "" +
			"CREATE TABLE " + TABLE_GROUPS + "(" +
			COLUMN_ID 		+ " integer primary key autoincrement," +
			GROUPS_NAME		+ " text UNIQUE NOT NULL" +
			");";

	//Users
	private static final String CREATE_TABLE_USERS = "" +
			"CREATE TABLE " 	+ TABLE_USERS + "(" +
			COLUMN_ID			+ " integer primary key autoincrement," +
			USERS_GROUP_ID		+ " integer," +
			USERS_JID 			+ " text UNIQUE NOT NULL," +
			USERS_NAME			+ " text," +
			USERS_MULTIUSER 	+ " integer,"+
			USERS_ONLINE_STATUS	+ " integer,"+
			USERS_MUC_ROLE		+ " integger,"+
			USERS_AUTHORIZED	+ " integer"+
			");";

	private static final String CREATE_USERS_INDEX = "CREATE UNIQUE INDEX "
			+ INDEX_USERS + " ON " + TABLE_USERS + " ("
			+ USERS_JID + " ASC);";

	//Messages
	private static final String CREATE_TABLE_MESSAGES = "" +
			"CREATE TABLE " + TABLE_MESSAGES + "(" +
			COLUMN_ID 				+ " integer primary key autoincrement," +
			MESSAGES_USER_ID   	+ " integer," +
			MESSAGES_CHAT_ID   	+ " integer," +
			MESSAGES_CREATED	+ " integer," +
			MESSAGES_READED		+ " integer," +
			MESSAGES_SENDED		+ " integer," +
			MESSAGES_BODY 		+ " text," +
			MESSAGES_SUBJECT	+ " text" +
			");";

	public DbHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String[] CREATES = {
				CREATE_TABLE_GROUPS,
				CREATE_TABLE_USERS,
				CREATE_TABLE_MESSAGES,
				CREATE_USERS_INDEX
		};
		for (final String table : CREATES) {
			db.execSQL(table);
		}
		ContentValues values = new ContentValues();
		values.put(USERS_JID,"CurrentLocalUser");
		db.insert(TABLE_USERS, null, values);

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String[] TABLES = {
				TABLE_GROUPS,
				TABLE_USERS,
				TABLE_MESSAGES
		};
		String[] INDEXES = {
				INDEX_USERS
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