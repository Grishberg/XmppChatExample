package com.grishberg.xmppchatclient.ui.listeners;

/**
 * Created by G on 25.06.15.
 */
public interface IInteractionUserListWithActivity {
	void onRegister(IInteractWithUserListFragment fragment);
	void onUnregister(IInteractWithUserListFragment fragment);
	void onUserItemClicked(long id);
	void deleteUserFromRoster(long userId);
}
