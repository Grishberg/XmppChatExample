package com.grishberg.xmppchatclient.data.db;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.data.db.containers.User;
import com.grishberg.xmppchatclient.framework.ChatConstants;

import java.util.Date;

/**
 * Created by grigoriy on 26.06.15.
 */
public class QueryHelper {

	public static long addOrUpdateMultichatRoom(String jid, String nickname, String password){
		long userId = -1;

		User user = new User(jid, nickname, 0, false, 0, ChatConstants.MULTICHAT_CHAT_STATE );
		Uri uri = AppController.getAppContext().getContentResolver()
				.insert(AppContentProvider.CONTENT_URI_USERS,user.buildContentValues());
		userId = Long.valueOf(uri.getLastPathSegment());
		return userId;

	}

	// get or add user by JID
	public static long getUserByJid(String jid, String nickname, int multichatState){
		long idUser = -1;
		SqlQueryBuilderHelper helper = new SqlQueryBuilderHelper();
		helper.makeGetUserQuery(jid);

		ContentResolver contentResolver =  AppController.getAppContext().getContentResolver();
		Cursor cursor = contentResolver.query(helper.getUri(), new String[]{DbHelper.COLUMN_ID}
				, helper.getSelection()
				, helper.getSelectionArgs()
				, null);
		if(cursor!= null && cursor.getCount() > 0 && cursor.moveToFirst()){

			idUser = cursor.getLong( cursor.getColumnIndex(DbHelper.COLUMN_ID));

		} else {

				ContentValues values = new ContentValues();
			values.put(DbHelper.USERS_JID, jid);
			values.put(DbHelper.USERS_MULTIUSER, multichatState);
			Uri uri = contentResolver.insert(helper.getUri(),values);
			idUser	= Long.valueOf(uri.getLastPathSegment());

		}

		return idUser;
	}

	public static long insertUser(String jid, String nickname,int onlineStatus, int multichatState){
		User user = new User(jid, nickname,0, false, onlineStatus, multichatState);
		Uri uri = AppController.getAppContext().getContentResolver()
				.insert(AppContentProvider.CONTENT_URI_USERS,user.buildContentValues());
		return Long.valueOf(uri.getLastPathSegment());
	}

	public static String getJidById(long userId){
		String result = null;
		SqlQueryBuilderHelper helper = new SqlQueryBuilderHelper();
		Cursor cursor = AppController.getAppContext().getContentResolver()
				.query(AppContentProvider.getUsersUri(userId)
						, new String[]{ DbHelper.USERS_JID}
						, null
						, null
						, null);
		if(cursor!= null && cursor.getCount() > 0 && cursor.moveToFirst()){

			result = cursor.getString(cursor.getColumnIndex(DbHelper.USERS_JID));

		}
		return result;
	}
	public static void updateUser(long userId, String jid, String name){
		ContentValues values = new ContentValues();
		values.put(DbHelper.USERS_JID, jid);
		values.put(DbHelper.USERS_NAME, name);

		AppController.getAppContext().getContentResolver()
				.update(AppContentProvider.getUsersUri(userId), values, null, null);
	}

	public static void deleteUser(long userId){
		AppController.getAppContext().getContentResolver()
				.delete(AppContentProvider.getUsersUri(userId), null, null);
	}

	public static void setOfflineStatus(){
		ContentValues values = new ContentValues();
		values.put(DbHelper.USERS_ONLINE_STATUS, ChatConstants.USER_STATUS_AWAY);
		AppController.getAppContext().getContentResolver()
				.update(AppContentProvider.CONTENT_URI_USERS,values,null,null);
	}

	public static void setOnlineStatus(long userId, int status){
		ContentValues values = new ContentValues();
		values.put(DbHelper.USERS_ONLINE_STATUS, status);
		AppController.getAppContext().getContentResolver()
				.update(AppContentProvider.getUsersUri(userId), values, null, null);
	}

	public static void clearHistoryForChat(Long chatId){
		AppController.getAppContext().getContentResolver()
				.delete(AppContentProvider.CONTENT_URI_MESSAGES
						, DbHelper.MESSAGES_CHAT_ID+" = ? "
						, new String[] {chatId.toString()} );
	}

	public static void storeMessage(long userId, long chatId, Date date, String body, String subject){
		MessageContainer messageContainer = new MessageContainer(userId,chatId
				, date.getTime()
				, false
				, true
				, body
				, subject);
		AppController.getAppContext().getContentResolver()
				.insert(AppContentProvider.CONTENT_URI_MESSAGES,messageContainer.buildContentValues());
	}

	public static void makeMessageReaded(long messageId){
		ContentValues values = new ContentValues();
		values.put(DbHelper.MESSAGES_READED, 1);
		AppController.getAppContext().getContentResolver()
				.update(AppContentProvider.getMessagesUri(messageId),values, null, null);
	}

	public static void makeMessageSended(long messageId){
		ContentValues values = new ContentValues();
		values.put(DbHelper.MESSAGES_READED, 1);
		AppController.getAppContext().getContentResolver()
				.update(AppContentProvider.getMessagesUri(messageId),values, null, null);
	}


}
