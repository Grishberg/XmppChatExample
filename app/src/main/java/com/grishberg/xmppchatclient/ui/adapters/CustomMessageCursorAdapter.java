package com.grishberg.xmppchatclient.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.db.DbHelper;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.framework.ChatConstants;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by G on 27.06.15.
 */
public class CustomMessageCursorAdapter extends CursorAdapter {

	private LayoutInflater mInflater;

	public CustomMessageCursorAdapter(Context context, Cursor c) {
		super(context, c);
		mAutoRequery	= true;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		ViewHolder holder 	= (ViewHolder) view.getTag();
		int jidCol 			= cursor.getColumnIndexOrThrow(DbHelper.USERS_JID);
		String jid 			= cursor.getString(jidCol);
		MessageContainer messageContainer	= MessageContainer.fromCursor(cursor);
		DateFormat df = new SimpleDateFormat("[dd.MM.yyyy HH:mm:ss]");
		String createdAtDate = df.format(messageContainer.getCreated());

		if(messageContainer.getUserId() != ChatConstants.CURRENT_LOCAL_USER_ID){
			// other's messages
			holder.chatContainer.setBackgroundResource(R.drawable.bubble_b);
			holder.chatContainer.setGravity( Gravity.LEFT);
			holder.leftTab.setVisibility(View.GONE);
			holder.rightTab.setVisibility(View.VISIBLE);

		} else {
			//myMessages
			holder.chatContainer.setBackgroundResource(R.drawable.bubble_a);
			holder.chatContainer.setGravity(Gravity.RIGHT);
			holder.rightTab.setVisibility(View.GONE);
			holder.leftTab.setVisibility(View.VISIBLE);
		}
		holder.from.setText(jid);
		holder.messageText.setText(messageContainer.getBody());
		holder.messageDate.setText(createdAtDate);
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.cell_chat_single_msg, parent, false);

		ViewHolder holder = new ViewHolder();
		holder.statusImage 		= (ImageView) v.findViewById(R.id.cell_chat_statusicon);
		holder.from 			= (TextView) v.findViewById(R.id.cell_chat_from);
		holder.messageText 		= (TextView) v.findViewById(R.id.cell_chat_message_text);
		holder.messageDate 		= (TextView) v.findViewById(R.id.cell_chat_date);
		holder.chatContainer	= (LinearLayout) v.findViewById(R.id.cell_chat_message_container);
		holder.leftTab			= (LinearLayout) v.findViewById(R.id.cell_chat_lefttab);
		holder.rightTab			= (LinearLayout) v.findViewById(R.id.cell_chat_righttab);

		v.setTag(holder);
		return v;
	}

	private static class ViewHolder {
		LinearLayout chatContainer;
		LinearLayout leftTab;
		LinearLayout rightTab;
		TextView from;
		TextView messageText;
		TextView messageDate;
		ImageView statusImage;
	}
}
