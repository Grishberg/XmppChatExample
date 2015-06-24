package com.grishberg.xmppchatclient.ui.fragments;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.grishberg.xmppchatclient.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChatRoomsFragment extends Fragment {

	public ChatRoomsFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_chat_rooms, container, false);
	}
}
