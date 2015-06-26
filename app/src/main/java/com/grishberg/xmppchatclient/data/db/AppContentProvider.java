package com.grishberg.xmppchatclient.data.db;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

/**
 * Created by grigoriy on 25.06.15.
 */
public class AppContentProvider  extends ContentProvider {

	private static final String AUTHORITY = "com.grishberg.xmppchatclient.content_provider";

	private static final String PATH_GROUPS		= DbHelper.TABLE_GROUPS;
	private static final String PATH_USERS		= DbHelper.TABLE_USERS;
	private static final String PATH_MESSAGES	= DbHelper.TABLE_MESSAGES;

	private static final String PATH_GROUPS_NOT_EMPTY	= "notEmptyGroups";

	public static final Uri CONTENT_URI_GROUPS		= Uri.parse("content://" + AUTHORITY + "/" + PATH_GROUPS);
	public static final Uri CONTENT_URI_USERS		= Uri.parse("content://" + AUTHORITY + "/" + PATH_USERS);
	public static final Uri CONTENT_URI_MESSAGES	= Uri.parse("content://" + AUTHORITY + "/" + PATH_MESSAGES);

	public static final Uri CONTENT_URI_CATEGORIES_NOT_EMPTY	= Uri.parse("content://" + AUTHORITY + "/" + PATH_GROUPS_NOT_EMPTY);

	private static final int CODE_GROUPS			= 0;
	private static final int CODE_GROUPS_ID			= 1;
	private static final int CODE_USERS				= 4;
	private static final int CODE_USERS_ID			= 5;
	private static final int CODE_MESSAGES			= 6;
	private static final int CODE_MESSAGES_ID		= 7;
	private static final int CODE_GROUPS_NOT_EMPTY	= 8;

	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		URI_MATCHER.addURI(AUTHORITY, PATH_GROUPS, 			CODE_GROUPS);
		URI_MATCHER.addURI(AUTHORITY, PATH_USERS, 			CODE_USERS);
		URI_MATCHER.addURI(AUTHORITY, PATH_MESSAGES, 		CODE_MESSAGES);

		URI_MATCHER.addURI(AUTHORITY, PATH_GROUPS+ "/#", 	CODE_GROUPS_ID);
		URI_MATCHER.addURI(AUTHORITY, PATH_USERS+ "/#", 	CODE_USERS_ID);
		URI_MATCHER.addURI(AUTHORITY, PATH_MESSAGES+ "/#",	CODE_MESSAGES_ID);

		URI_MATCHER.addURI(AUTHORITY, PATH_GROUPS_NOT_EMPTY,	CODE_GROUPS_NOT_EMPTY);
	}

	private static DbHelper dbHelper;

	public static Uri getGroupsUri(Long id){
		return Uri.withAppendedPath(AppContentProvider.CONTENT_URI_GROUPS, id.toString());
	}

	public static Uri getUsersUri(Long id){
		return Uri.withAppendedPath(AppContentProvider.CONTENT_URI_USERS, id.toString());
	}

	public static Uri getMessagesUri(Long id){
		return Uri.withAppendedPath(AppContentProvider.CONTENT_URI_MESSAGES, id.toString());
	}

	public synchronized static DbHelper getDbHelper(Context context) {
		if (null == dbHelper) {
			dbHelper = new DbHelper(context);
		}
		return dbHelper;
	}

	@Override
	public boolean onCreate() {
		getDbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		int uriId = URI_MATCHER.match(uri);
		Cursor cursor = null;
		switch (uriId) {
			case CODE_USERS:
			case CODE_GROUPS:
			case CODE_MESSAGES:
				cursor = dbHelper.getReadableDatabase()
						.query(getTableName(uriId), projection, selection, selectionArgs, null, null, sortOrder);
				break;
			case CODE_GROUPS_ID:
			case CODE_USERS_ID:
			case CODE_MESSAGES_ID:
				cursor = dbHelper.getReadableDatabase()
						.query(getTableName(uriId), projection
								, DbHelper.COLUMN_ID + " = ?"
								, new String[] { uri.getLastPathSegment() }, null, null, sortOrder);
				break;

			case CODE_GROUPS_NOT_EMPTY:
				cursor	= getCategories();
				break;
			default:
				throw new IllegalArgumentException("Unknown URI: " + uri);
		}

		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		long id		= -1;
		int uriId 	= URI_MATCHER.match(uri);
		SQLiteDatabase db 	= dbHelper.getWritableDatabase();
		switch (uriId){
			case CODE_GROUPS:
				id = insertOrUpdateById(db,uri, uriId, values, DbHelper.GROUPS_NAME);
				break;
			case CODE_USERS:
				id = insertOrUpdateById(db,uri, uriId, values, DbHelper.USERS_JID);
				break;

			case CODE_MESSAGES:
				id = db.insert(getTableName(uriId), null, values);
				break;
		}
		if (-1 == id) {
			throw new RuntimeException("Record wasn't saved.");
		}
		Uri resultUri = ContentUris.withAppendedId(uri, id);
		getContext().getContentResolver().notifyChange(resultUri, null);

		return resultUri;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int result 	= 0;
		int uriId 	= URI_MATCHER.match(uri);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriId){
			case CODE_GROUPS:
			case CODE_MESSAGES:
			case CODE_USERS:
				// delete all articles
				result	= db.delete(getTableName(uriId), selection, selectionArgs);
				break;

			case CODE_GROUPS_ID:
			case CODE_MESSAGES_ID:
			case CODE_USERS_ID:
				// delete by ID
				String id	= uri.getLastPathSegment();
				result = db.delete(getTableName(uriId)
						, DbHelper.COLUMN_ID + " = ?", new String[] {id});
				break;
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int result	= 0;
		int uriId	= URI_MATCHER.match(uri);
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		switch (uriId){
			case CODE_GROUPS:
			case CODE_MESSAGES:
			case CODE_USERS:
				// update all articles
				result	= db.update(getTableName(uriId), values, selection, selectionArgs);
				break;

			case CODE_GROUPS_ID:
			case CODE_MESSAGES_ID:
			case CODE_USERS_ID:
				// update by ID
				String id	= uri.getLastPathSegment();
				if (TextUtils.isEmpty(selection)) {
					result = db.update(getTableName(uriId)
							, values, DbHelper.COLUMN_ID + " = ?", new String[]{id});
				} else {
					result = db.update(getTableName(uriId)
							, values, DbHelper.COLUMN_ID + " = " + id + " AND "
							+ selection, selectionArgs);
				}
				break;
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return result;
	}

	private String getTableName(int match) {
		String table = null;
		switch (match) {
			case CODE_GROUPS:
			case CODE_GROUPS_ID:
				table = dbHelper.TABLE_GROUPS;
				break;
			case CODE_USERS:
			case CODE_USERS_ID:
				table = dbHelper.TABLE_USERS;
				break;
			case CODE_MESSAGES:
			case CODE_MESSAGES_ID:
				table = dbHelper.TABLE_MESSAGES;
				break;

			default:
				throw new IllegalArgumentException("Invalid DB code: " + match);
		}
		return table;
	}

	/**
	 *
	 * @param db -current db
	 * @param uri - uri
	 * @param uriId uri index
	 * @param values values
	 * @param column column for condition
	 * @return
	 * @throws SQLiteConstraintException
	 */
	private long insertOrUpdateById(SQLiteDatabase db,Uri uri, int uriId,
									ContentValues values, String column) throws SQLiteConstraintException{
		long result	= -1;

		try {
			result	= db.insertOrThrow(getTableName(uriId), null, values);
		} catch (SQLiteConstraintException e) {
			int nrRows = update(uri, values, column + "=?",
					new String[]{values.getAsString(column)});

			Cursor cursor = db.query(getTableName(uriId)
					, new String[]{DbHelper.COLUMN_ID}
					, column + "=?"
					, new String[]{ values.getAsString(column) }
					,null,null,null);
			if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()){
				result = cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_ID));
			}

			if (nrRows == 0) {
				throw e;
			}
		}
		return result;
	}

	private Cursor getCategories(){
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		StringBuilder sqlBuilder = new StringBuilder("SELECT usr.");
		sqlBuilder.append(DbHelper.USERS_GROUP_ID);
		sqlBuilder.append(" AS ");
		sqlBuilder.append(DbHelper.COLUMN_ID);
		sqlBuilder.append(", cat.");
		sqlBuilder.append(DbHelper.GROUPS_NAME);
		sqlBuilder.append(" FROM ");
		sqlBuilder.append(DbHelper.TABLE_USERS);
		sqlBuilder.append(" AS usr INNER JOIN ");
		sqlBuilder.append(DbHelper.TABLE_GROUPS);
		sqlBuilder.append(" AS cat ON usr.");
		sqlBuilder.append(DbHelper.USERS_GROUP_ID);
		sqlBuilder.append(" = cat.");
		sqlBuilder.append(DbHelper.COLUMN_ID);
		sqlBuilder.append(" GROUP BY cat.");
		sqlBuilder.append(DbHelper.GROUPS_NAME);

		String sql = sqlBuilder.toString();
		return db.rawQuery(sql, null);
	}
}

