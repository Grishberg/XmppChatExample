package com.grishberg.xmppchatclient.data.api;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.data.db.containers.User;
import com.grishberg.xmppchatclient.framework.ChatConstants;
import com.grishberg.xmppchatclient.framework.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.Affiliate;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCNotJoinedException;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.muc.Occupant;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.util.Date;
import java.util.List;

/**
 * Created by grigoriy on 29.06.15.
 */
public class XmppMucManager implements InvitationListener
		, MessageListener {

	private static final String TAG	= "XmppChat.MucManager";

	private Context					mContext;
	private MultiUserChat 			mCurrentMuc;
	private MultiUserChatManager 	mMucManager;
	private AbstractXMPPConnection	mConnection;
	private long 					mCurrentChatId;
	private String 					mMyMucJid;

	public XmppMucManager(Context context, AbstractXMPPConnection connection){
		mContext	= context;
		mConnection	= connection;
		mMucManager	= MultiUserChatManager.getInstanceFor(mConnection);
		if(mMucManager != null){
			mMucManager.addInvitationListener(this);
		}
	}

	// MUC
	public void addJoinMuc(final String host, final String room, final String nickname, final String password){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doJoinMuc(host, room, nickname, password);
			}
		}).start();
	}

	private void doJoinMuc(String host, String room, String nickname, String password){
		if(mMucManager == null){
			return;
		}

		if(mCurrentMuc != null){
			leaveMuc();
		}
		String mucJid = room +"@"+host;
		mMyMucJid	= mucJid+"/"+nickname;
		mCurrentMuc	= mMucManager.getMultiUserChat(mucJid);
		if(!mCurrentMuc.isJoined()){
			try {
				long timeout = 20000;
				DiscussionHistory histroy = new DiscussionHistory();
				histroy.setMaxStanzas(0);
				boolean status = mCurrentMuc.createOrJoin(nickname, password, histroy, timeout) ;

				setConfig(mCurrentMuc);
				getOccupants(mCurrentMuc);
			}
			catch (SmackException.NoResponseException e){
				sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_NOT_RESPONSE, -1);
				return;
			}
			catch (Exception e){
				sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_OTHER_ERROR, -1);
				e.printStackTrace();
				return;
			}
		}
		mCurrentChatId	= QueryHelper.addOrUpdateMultichatRoom(mucJid, nickname, password);



		// clear from history old messages
		//QueryHelper.clearHistoryForChat(mCurrentChatId);

		mCurrentMuc.addMessageListener(this);
		sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_OK, mCurrentChatId);
	}

	/**
	 * get user list in current MUC
	 * @param chat
	 * @throws SmackException.NoResponseException
	 * @throws XMPPException.XMPPErrorException
	 * @throws SmackException.NotConnectedException
	 */
	private void getOccupants(MultiUserChat chat)throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException {
		for(String occupant: chat.getOccupants()){
			String nickname	= Utils.extractResource(occupant);
			QueryHelper.insertUser(occupant,nickname,ChatConstants.USER_STATUS_AVAILIBLE
					, ChatConstants.MULTICHAT_PARTICIPATE_STATE);
			Log.d(TAG, "getOccupants: jid="+occupant);
		}
		/*

		for(Occupant occupant: chat.getParticipants()){
			String jid		= occupant.getJid();
			String nick		= occupant.getNick();
			MUCRole role	= occupant.getRole();
			Log.d(TAG, "getParticipants: jid="+jid +" nick="+nick+" role="+role);
		}
		*/
/*
		for(Affiliate member: chat.getMembers()){
			String jid		= member.getJid();
			String nick		= member.getNick();
			MUCRole role	= member.getRole();
			Log.d(TAG, "participans: jid="+jid +" nick="+nick+" role="+role);
		}
*/
	}

	private void setConfig(MultiUserChat multiUserChat) {
		try {
			Form form = multiUserChat.getConfigurationForm();
			Form submitForm = form.createAnswerForm();
			for (FormField field:submitForm.getFields()) {
				if (!FormField.FORM_TYPE.equals(field.getType()) && field.getVariable() != null) {
					submitForm.setDefaultAnswer(field.getVariable());
				}
			}
			submitForm.setAnswer("muc#roomconfig_publicroom", true);
			submitForm.setAnswer("muc#roomconfig_persistentroom", true);
			multiUserChat.sendConfigurationForm(submitForm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processMessage(Message message) {
		// get user id
		String jid 		= Utils.extractJid(message.getFrom());
		String nickname	= Utils.extractResource(message.getFrom());

		long userId = QueryHelper.getUserByJid(message.getFrom(), nickname
				, ChatConstants.MULTICHAT_PARTICIPATE_STATE);

		if(!message.getFrom().equals(mMyMucJid)) {
			QueryHelper.storeMessage(userId, mCurrentChatId
					, new Date(), message.getBody(), message.getSubject());
		}

	}

	public void sendMessage(final long chatId, final String messageText){
		new Thread(new Runnable() {
			@Override
			public void run() {
				doSendMessage(chatId, messageText);
			}
		}).start();
	}

	private void doSendMessage(long chatId, String messageText){

		if(mConnection.isAuthenticated()){
			//TODO: check chatId
			if( mCurrentMuc != null) {
				try {
					mCurrentMuc.sendMessage(messageText);
					QueryHelper.storeMessage(ChatConstants.CURRENT_LOCAL_USER_ID, chatId, new Date()
							, messageText, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}



	/**
	 * need call when leave chat
	 */
	public void leaveMuc(){
		if(mCurrentMuc != null){
			mCurrentMuc.removeMessageListener(this);
			try {
				mCurrentMuc.leave();
				//TODO: delete history and USERS
			}catch (Exception e){

			} finally {
				mCurrentMuc	= null;
			}
		}
	}


	//Muc invation
	@Override
	public void invitationReceived(XMPPConnection conn
			, MultiUserChat room
			, String inviter
			, String reason
			, String password
			, Message message) {

	}

	private void sendOnMucResponseMessage(int msg, long chatId){
		Intent intent = new Intent(ApiService.ACTION_ON_NEW_MUC_RESULT);
		// You can also include some extra data.
		intent.putExtra(ApiService.EXTRA_JOIN_MUC_STATUS, msg);
		intent.putExtra(ApiService.EXTRA_MUC_CHAT_ID, chatId);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}
}
