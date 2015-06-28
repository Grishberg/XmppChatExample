package com.grishberg.xmppchatclient.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.DbHelper;
import com.grishberg.xmppchatclient.ui.activities.FindUserActivity;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithUserListFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractionUserListWithActivity;

public class UserListFragment extends Fragment implements
		IInteractWithUserListFragment
		, LoaderManager.LoaderCallbacks<Cursor> {

	public static final int USERLIST_LOADER = 2;
	private IInteractionUserListWithActivity mListener;
	private ListView 				mListView;
	private SimpleCursorAdapter 	mListViewCursorAdapter;
	private

	// DB cursor settings
	String[] 						mUsersProjection;
	private String 					mUsersFilterSelection;
	private String[]				mUsersFilterSelectionArgs;
	private String					mUsersSortOrder;


	public static UserListFragment newInstance() {
		UserListFragment fragment = new UserListFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	public UserListFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view	=inflater.inflate(R.layout.fragment_user_list, container, false);

		// init ListView
		mListView	= (ListView) view.findViewById(R.id.fragment_userlist_listview);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				onListViewItemClicked(id);
			}
		});
		registerForContextMenu(mListView);


		mUsersProjection 			= null;
		mUsersFilterSelection		= null;
		mUsersFilterSelectionArgs	= null;
		mUsersSortOrder				= DbHelper.USERS_JID + " ASC ";

		// button add
		View addUserButton = view.findViewById(R.id.fragment_userlist_add_button);
		addUserButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onAddNewUser();
			}
		});

		fillData();
		return view;
	}

	private void fillData() {
		// Fields from the database (projection)
		// Must include the _id column for the adapter to work
		String[] from = new String[] {DbHelper.USERS_JID};

		// Fields on the UI to which we map
		int[] to = new int[] { R.id.cell_userlist_username };

		mListViewCursorAdapter = new SimpleCursorAdapter(getActivity(), R.layout.cell_userlist_user
				, null, from, to, 0);
		mListView.setAdapter(mListViewCursorAdapter);
		getLoaderManager().initLoader(USERLIST_LOADER, null, this);

	}

	private void onListViewItemClicked(long id){
		mListener.onUserItemClicked(id);
	}

	private void onAddNewUser(){
		startActivity(new Intent(getActivity(), FindUserActivity.class) );
	}

	//------------- Loader -------------------------------
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id){
			case USERLIST_LOADER:

				// Returns a new CursorLoader
				return new CursorLoader(
						getActivity(),   // Parent activity context
						AppContentProvider.CONTENT_URI_USERS, // Table to query
						null, 					// Projection to return
						mUsersFilterSelection,
						mUsersFilterSelectionArgs,
						mUsersSortOrder
				);
		}
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		switch (loader.getId()){
			case USERLIST_LOADER:
				mListViewCursorAdapter.swapCursor(data);
				break;
		}

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if(loader.getId() == USERLIST_LOADER) {
			mListViewCursorAdapter.changeCursor(null);
		}
	}

	//----------------  ------------------------------

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if( activity instanceof IInteractionUserListWithActivity){
			mListener = (IInteractionUserListWithActivity) activity;
			mListener.onRegister(this);
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener.onUnregister(this);
		mListener = null;
	}
//---------- menu --------
//------------------- context menu ----------------------
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v.getId() == R.id.fragment_userlist_listview) {
			MenuInflater inflater = getActivity().getMenuInflater();
			inflater.inflate(R.menu.menu_main, menu);

		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_delete_user:
				AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
						.getMenuInfo();
				mListener.deleteUserFromRoster(info.id);
				return true;

			default:
				return super.onContextItemSelected(item);
		}
	}
	//--------------------------------------------------
}
