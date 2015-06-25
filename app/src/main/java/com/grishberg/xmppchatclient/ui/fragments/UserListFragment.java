package com.grishberg.xmppchatclient.ui.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.grishberg.xmppchatclient.R;
import com.grishberg.xmppchatclient.ui.listeners.IInteractWithUserListFragment;
import com.grishberg.xmppchatclient.ui.listeners.IInteractionUserListWithActivity;

public class UserListFragment extends Fragment implements IInteractWithUserListFragment {
	private IInteractionUserListWithActivity mListener;
	private ListView 						mListView;
	private SimpleCursorAdapter 			mListViewCursorAdapter;

	// DB cursor settings
	String[] 								mProjection;
	String[]								mCategoryProjection;
	private String 							mArticlesSortOrder;
	private String 							mCategoriesSortOrder;
	private String							mChildArticlesSortOrder;

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

		return view;
	}

	private void onListViewItemClicked(long id){
		mListener.onUserItemClicked(id);
	}

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

}
