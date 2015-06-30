package com.grishberg.xmppchatclient.data.api;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.grishberg.xmppchatclient.AppController;
import com.grishberg.xmppchatclient.data.db.AppContentProvider;
import com.grishberg.xmppchatclient.data.db.QueryHelper;
import com.grishberg.xmppchatclient.data.db.containers.MessageContainer;
import com.grishberg.xmppchatclient.framework.Utils;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.InvitationListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import java.util.Date;

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
		mCurrentMuc	= mMucManager.getMultiUserChat(room +"@"+host);
		if(!mCurrentMuc.isJoined()){
			try {
				mCurrentMuc.createOrJoin(nickname);
				setConfig(mCurrentMuc);
				DiscussionHistory histroy = new DiscussionHistory();
				histroy.setMaxStanzas(10);

				mCurrentMuc.createOrJoin(nickname, null, histroy, SmackConfiguration.getDefaultPacketReplyTimeout());
				mCurrentMuc.nextMessage();

			}
			catch (SmackException.NoResponseException e){
				sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_NOT_RESPONSE);
				return;
			}
			catch (Exception e){
				sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_OTHER_ERROR);
				e.printStackTrace();
				return;
			}
		}
		mCurrentMuc.addMessageListener(this);
		sendOnMucResponseMessage(ApiService.MUC_JOIN_STATUS_OK);
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
		long userId = QueryHelper.getUserByJid(Utils.extractJid(message.getFrom()));

		// store message to DB
		MessageContainer messageContainer = new MessageContainer(userId,userId, new Date().getTime(),
				true, true
				, message.getBody()
				, message.getSubject());

		AppController.getAppContext().getContentResolver()
				.insert(AppContentProvider.CONTENT_URI_MESSAGES
				, messageContainer.buildContentValues());
	}

	/**
	 * need call when leave chat
	 */
	public void leaveMuc(){
		if(mCurrentMuc != null){
			mCurrentMuc.removeMessageListener(this);
			try {
				mCurrentMuc.leave();
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

	private void sendOnMucResponseMessage(int msg){
		Intent intent = new Intent(ApiService.ACTION_ON_NEW_MUC_RESULT);
		// You can also include some extra data.
		intent.putExtra(ApiService.EXTRA_JOIN_MUC_STATUS, msg);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}
}
