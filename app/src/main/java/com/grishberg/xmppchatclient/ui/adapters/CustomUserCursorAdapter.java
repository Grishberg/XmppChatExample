package com.grishberg.xmppchatclient.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.db.containers.User;
import com.grishberg.xmppchatclient.framework.ChatConstants;

/**
 * Created by G on 27.06.15.
 */
public class CustomUserCursorAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public CustomUserCursorAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		// TODO Auto-generated method stub
		ViewHolder holder 	= (ViewHolder) view.getTag();

		User user	= User.fromCursor(cursor);
		int position = cursor.getPosition();

		switch (user.getOnlineStatus())
		{
			case ChatConstants.USER_STATUS_UNAVAILIBLE:
				holder.statusImage.setImageResource(R.drawable.jabber_offline);
				break;
			case ChatConstants.USER_STATUS_AVAILIBLE:
				holder.statusImage.setImageResource(R.drawable.jabber_online);
				break;
			case ChatConstants.USER_STATUS_CHAT:
				holder.statusImage.setImageResource(R.drawable.jabber_chat);
				break;
			default:
				break;
		}

		holder.jid.setText(user.getLogin());
		//TODO: отобразить количество непрочитанных сообщений
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		View view = mInflater.inflate(R.layout.cell_userlist_user, parent, false);

		ViewHolder holder = new ViewHolder();
		holder.statusImage 		= (ImageView) view.findViewById(R.id.cell_userlist_statusicon);
		holder.jid 				= (TextView) view.findViewById(R.id.cell_userlist_username);
		holder.unreadMessageCount	= (TextView) view.findViewById(R.id.cell_userlist_new_messages_count);
		view.setTag(holder);
		return view;
	}

	static class ViewHolder {
		ImageView statusImage;
		TextView jid;
		TextView unreadMessageCount;
	}

}
