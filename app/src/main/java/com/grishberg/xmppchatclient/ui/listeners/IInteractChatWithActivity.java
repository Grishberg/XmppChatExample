package com.grishberg.xmppchatclient.ui.listeners;

/**
 * Created by G on 25.06.15.
 */
public interface IInteractChatWithActivity {
	void onRegister(IInteractWithChatFragment fragment);
	void onUregister(IInteractWithChatFragment fragment);
	void onSendMessage(int chatType, long chatId, String message);
	boolean isConnected();
}
