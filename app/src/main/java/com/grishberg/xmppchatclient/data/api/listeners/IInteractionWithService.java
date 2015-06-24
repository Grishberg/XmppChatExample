package com.grishberg.xmppchatclient.data.api.listeners;

/**
 * Created by grigoriy on 24.06.15.
 */
public interface IInteractionWithService {
	void connect( String login, String password, String server);
	void disconnect();
}
