package com.grishberg.xmppchatclient.data.db;

import android.net.Uri;

/**
 * Created by grigoriy on 26.06.15.
 */
public class SqlQueryBuilderHelper {
	private Uri mUri;
	private String		mSelection;
	private String[] 	mSelectionArgs;
	private String		mSortOrder;

	public SqlQueryBuilderHelper(){

	}

	public void makeMessageHistoryQuery(Long userId){
		mUri			= AppContentProvider.CONTENT_URI_MESSAGES;
		mSelection		= DbHelper.MESSAGES_CHAT_ID + " = ? ";
		mSelectionArgs	= new String[] { userId.toString()};
		mSortOrder		= DbHelper.MESSAGES_CREATED +" ASC";
	}

	public void makeGetUserQuery(String jid){
		mUri			= AppContentProvider.CONTENT_URI_USERS;
		mSelection		= DbHelper.USERS_JID + " = ? ";
		mSelectionArgs	= new String[] { jid };
		mSortOrder		= null;
	}

	//-------------- response ------------------
	public String getSelection() {
		return mSelection;
	}

	public String[] getSelectionArgs() {
		return mSelectionArgs;
	}

	public String getSortOrder() {
		return mSortOrder;
	}

	public Uri getUri() {
		return mUri;
	}
}
