package com.grishberg.xmppchatclient.ui.fragments;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.support.v4.widget.CursorAdapter;
import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.DbHelper;
import com.grishberg.xmppchatclient.data.db.SqlQueryBuilderHelper;
import com.grishberg.xmppchatclient.ui.adapters.CustomMessageCursorAdapter;
import com.grishberg.xmppchatclient.ui.listeners.IInteractChatWithActivity;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithChatFragment;


public class ChatFragment extends Fragment implements IInteractWithChatFragment
		, LoaderManager.LoaderCallbacks<Cursor>
		, View.OnClickListener{
	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final int MESSAGES_LOADER = 0;

	private static final String ARG_CHAT_ID		= "chatId";
	private static final String ARG_CHAT_TYPE	= "chatType";
	private static final String ARG_CHAT_JID	= "chatJid";

	private long 				mChatId;
	private int 				mChatType;
	private String 				mChatName;

	private ListView 			mListView;
	private EditText 			mChatText;
	private Button				mSendButton;
	private CursorAdapter 		mListViewCursorAdapter;

	private String		mMessagesSortOrder;
	private String		mMessagesFilterSelection;
	private String[]	mMessagesFilterSelectionArgs;

	private IInteractChatWithActivity mListener;

	public static ChatFragment newInstance(long chatId, int chatType, String chatJid) {
		ChatFragment fragment 	= new ChatFragment();
		Bundle args 			= new Bundle();
		args.putLong(ARG_CHAT_ID, chatId);
		args.putInt(ARG_CHAT_TYPE, chatType);
		args.putString(ARG_CHAT_JID, chatJid);
		fragment.setArguments(args);
		return fragment;
	}

	public ChatFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mChatId		= getArguments().getLong(ARG_CHAT_ID);
			mChatType	= getArguments().getInt(ARG_CHAT_TYPE);
			mChatName	= getArguments().getString(ARG_CHAT_JID);

			if(mChatId > 0){
				SqlQueryBuilderHelper helper	= new SqlQueryBuilderHelper();
				helper.makeMessageHistoryQuery(mChatId);
				mMessagesFilterSelection 		= helper.getSelection();
				mMessagesFilterSelectionArgs	= helper.getSelectionArgs();
				mMessagesSortOrder				= helper.getSortOrder();
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_chat, container, false);

		mSendButton	= (Button) view.findViewById(R.id.fragment_chat_buttonSend);
		mSendButton.setOnClickListener(this);

		mListView 	= (ListView) view.findViewById(R.id.fragment_chat_listview);
		mChatText	= (EditText) view.findViewById(R.id.fragment_chat_edittext);

		mMessagesSortOrder			= DbHelper.MESSAGES_CREATED + " ASC ";
		fillData();
		return view;
	}

	private void fillData() {
		mListViewCursorAdapter = new CustomMessageCursorAdapter(getActivity(), null, mChatType);
		mListView.setAdapter(mListViewCursorAdapter);
		getLoaderManager().initLoader(MESSAGES_LOADER, null, this);

	}
	//on send button click
	@Override
	public void onClick(View v) {
		if(!TextUtils.isEmpty(mChatText.getText().toString()) &&
				mListener != null &&
				mListener.isConnected()){

			mListener.onSendMessage(mChatType, mChatId, mChatText.getText().toString());
			mChatText.setText("");
			// from service update when sended
		}
	}

	//------------ loader -----------------
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id){
			case MESSAGES_LOADER:

				// Returns a new CursorLoader

				return new CursorLoader(
						getActivity(),   // Parent activity context
						AppContentProvider.CONTENT_URI_MESSAGES, // Table to query
						//AppContentProvider.CONTENT_URI_MESSAGES_WITH_JID, // Table to query
						null, 					// Projection to return
						mMessagesFilterSelection,
						mMessagesFilterSelectionArgs,
						mMessagesSortOrder
				);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()){
			case MESSAGES_LOADER:
				mListViewCursorAdapter.swapCursor(data);
				break;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if(loader.getId() == MESSAGES_LOADER) {
			mListViewCursorAdapter.changeCursor(null);
		}
	}

	//------------------------------------------

	@Override
	public void onConnected() {
		mChatText.setEnabled(true);
	}

	@Override
	public void onDisconnected() {
		mChatText.setEnabled(false);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if(activity instanceof IInteractChatWithActivity){
			mListener = (IInteractChatWithActivity) activity;
			mListener.onRegister(this);
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement IInteractChatWithActivity");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener.onUregister(this);
		mListener = null;
	}
}
