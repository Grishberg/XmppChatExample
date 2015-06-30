package com.grishberg.xmppchatclient.data.api;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.framework.ChatConstants;
import com.grishberg.xmppchatclient.framework.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by grigoriy on 29.06.15.
 */
public class XmppMessageManager implements MessageListener
		, ChatMessageListener
		, ChatManagerListener {

	private static final String TAG	= "XmppChat.MessageManager";
	private Context					mContext;
	private AbstractXMPPConnection	mConnection;
	private ChatManager 			mChatManager;
	private SparseArray 			mPrivateChats;

	public XmppMessageManager(Context context,AbstractXMPPConnection connection){
		mContext	= context;
		mConnection	= connection;

		// setup chat manager
		mChatManager	= ChatManager.getInstanceFor(mConnection);
		if(mChatManager != null){
			mChatManager.addChatListener(this);
		}
		mPrivateChats		= new SparseArray();
	}

	/**
	 * event when incoming chat
	 * @param chat
	 * @param createdLocally
	 */
	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {

		Log.d(TAG, "on chat created");
		String jid	= Utils.extractJid(chat.getParticipant());
		long userId	= QueryHelper.getUserByJid(jid,"", ChatConstants.SINGLE_CHAT_STATE);
		mPrivateChats.put((int)userId, chat);
		chat.addMessageListener(this);

	}

	/**
	 * event when incoming message
	 * @param message
	 */
	@Override
	public void processMessage(Message message) {
		//TODO: send to activity
		Log.d(TAG, "on received message");
	}


	/**
	 * event when incoming message from chat
	 * @param chat
	 * @param message
	 */
	@Override
	public void processMessage(Chat chat, Message message) {
		Log.d(TAG, "on message from chat");

		ContentResolver contentResolver = AppController.getAppContext().getContentResolver();

		// get user id
		long userId = QueryHelper.getUserByJid(Utils.extractJid(chat.getParticipant()),""
				, ChatConstants.SINGLE_CHAT_STATE);

		// store message to DB
		MessageContainer messageContainer = new MessageContainer(userId,userId, new Date().getTime(),
				true, true
				, message.getBody()
				, message.getSubject());

		contentResolver.insert(AppContentProvider.CONTENT_URI_MESSAGES
				, messageContainer.buildContentValues());

	}

	public void sendMessage(final long userId, final String messageText){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doSendMessage(userId, messageText);
			}
		}).start();
	}

	private void doSendMessage(long userId, String messageText){

		if(mConnection.isAuthenticated()){
			Chat currentChat = (Chat)mPrivateChats.get((int)userId);
			if( currentChat == null){
				String jid	= QueryHelper.getJidById( userId );
				currentChat = mChatManager.createChat( jid );
				mPrivateChats.put((int)userId, currentChat);
			}
			try {
				currentChat.sendMessage(messageText);
				QueryHelper.storeMessage(ChatConstants.CURRENT_LOCAL_USER_ID, userId, new Date(), messageText, null);
			} catch (Exception e){
				e.printStackTrace();
			}
		}
	}


}
