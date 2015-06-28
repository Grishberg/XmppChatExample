package com.grishberg.xmppchatclient.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.grishberg.xmppchatclient.R;

/**
 * Created by G on 27.06.15.
 */
public class CustomUserCursorAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public CustomUserCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub

	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mInflater.inflate(R.layout.cell_userlist_user, parent, false);
	}

	static class ViewHolder {
		ImageView statusImage;
		TextView jid;
		TextView ureadMessageCount;
	}

}
