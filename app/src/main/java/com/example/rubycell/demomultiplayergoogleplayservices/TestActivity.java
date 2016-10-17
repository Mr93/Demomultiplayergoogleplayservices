package com.example.rubycell.demomultiplayergoogleplayservices;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.buddy.sdk.Buddy;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Invitations;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Created by RUBYCELL on 10/6/2016.
 */

public class TestActivity extends AppCompatActivity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		View.OnClickListener, RealTimeMessageReceivedListener,
		RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

	// request code for the "select players" UI
	// can be any number as long as it's unique
	final static int RC_SELECT_PLAYERS = 10000;
	// request code (can be any number, as long as it's unique)
	final static int RC_INVITATION_INBOX = 10001;

	private static final String TAG = MenuActivity.class.getSimpleName();
	private static final int RC_WAITING_ROOM = 10002;
	Button btnInvite, btnShowInvitation, btnQuick, btnJoin;

	//are we already playing?
	boolean mPlaying = false;

	//at least 2 players requierd for our game
	final static int MIN_PLAYERS = 2;

	public GoogleApiClient mGoogleApiClient;
	private static int RC_SIGN_IN = 9001;

	private boolean mResolvingConnectionFailure = false;
	private boolean mAutoStartSignInflow = true;
	private boolean mSignInClicked = false;

	boolean mExplicitSignOut = false;
	boolean mInSignInFlow = false;
	String mRoomId;
	Room mRoom;
	Queue<String> queueSendData;
	ArrayList<Integer> listPing;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		btnInvite = (Button) findViewById(R.id.btn_invite);
		btnShowInvitation = (Button) findViewById(R.id.btn_show_invite);
		btnJoin = (Button) findViewById(R.id.btn_show_join);
		btnQuick = (Button) findViewById(R.id.btn_quick);
		btnInvite.setOnClickListener(this);
		btnShowInvitation.setOnClickListener(this);
		btnQuick.setOnClickListener(this);
		btnJoin.setOnClickListener(this);
		findViewById(R.id.sign_in_button).setOnClickListener(this);
		findViewById(R.id.sign_out_button).setOnClickListener(this);
		createClientAccessPlayGameServices();
	}

	public void createClientAccessPlayGameServices() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Games.API).addScope(Games.SCOPE_GAMES)
				.build();
	}

	@Override
	protected void onStart() {
		if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
			Log.w(TAG,
					"GameHelper: client was already connected on onStart()");
		} else {
			Log.d(TAG, "Connecting client.");
			mGoogleApiClient.connect();
		}
		super.onStart();
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.btn_quick:
				/*FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
				DatabaseReference databaseReference = firebaseDatabase.getReference("room");

				Map<Integer, String> postValues = new HashMap<>();
				postValues.put(0, Games.Players.getCurrentPlayerId(mGoogleApiClient));
				Map<String, Object> childUpdates = new HashMap<>();
				childUpdates.put("/room/" + Games.Players.getCurrentPlayerId(mGoogleApiClient), postValues);
				databaseReference.updateChildren(childUpdates);*/


				ArrayList<String> invitees = new ArrayList<>();
				invitees.add("g08579370587522082113");

				Bundle am = RoomConfig.createAutoMatchCriteria(1,7, 0);

				//create the room and specify a variant if appropriate
				RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
				roomConfigBuilder.addPlayersToInvite(invitees);
				Log.d(TAG, "onActivityResult: ");
				RoomConfig roomConfig = roomConfigBuilder.build();
				Games.RealTimeMultiplayer .create(mGoogleApiClient, roomConfig);

				//prevent screen from sleeping during handshake
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

				break;
			case R.id.btn_show_join:



				Log.d(TAG, "onClick: Id" + Games.Players.getCurrentPlayerId(mGoogleApiClient));
				break;
			case R.id.btn_invite:
				// launch the player selection screen
				// minimum: 1 other player; maximum: 3 other players
				Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(mGoogleApiClient, 1, 3);
				startActivityForResult(intent, RC_SELECT_PLAYERS);
				break;
			case R.id.btn_show_invite:
				// launch the intent to show the invitation inbox screen
				Log.d(TAG, "onClick: btn_show_invite");
				intent = Games.Invitations.getInvitationInboxIntent(mGoogleApiClient);
				startActivityForResult(intent, RC_INVITATION_INBOX);
				break;
			case R.id.sign_in_button:
				// start the asynchronous sign in flow
				Toast.makeText(this, "connect", Toast.LENGTH_SHORT).show();
				mSignInClicked = true;
				mGoogleApiClient.connect();
				break;
			case R.id.sign_out_button:
				// sign out.
				mSignInClicked = false;

				// show sign-in button, hide the sign-out button
				findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
				findViewById(R.id.sign_out_button).setVisibility(View.GONE);

				// user explicitly signed out, so turn off auto sign in
				mExplicitSignOut = true;
				if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
					Games.signOut(mGoogleApiClient);
					mGoogleApiClient.disconnect();
				}
				break;
			case R.id.buttonSend:
				sentData(-1, ((EditText) findViewById(R.id.editText)).getText().toString(), myIndex);
				TextView textView = (TextView) findViewById(R.id.textView);
				textView.setText(textView.getText() + "\n Player" + myIndex + ": "
						+ ((EditText) findViewById(R.id.editText)).getText().toString());
				((EditText) findViewById(R.id.editText)).setText("");
			default:
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == RC_SELECT_PLAYERS) {
			if (resultCode != Activity.RESULT_OK) {
				//user canceld
				return;
			}
			//get the inviter list
			Bundle extras = data.getExtras();
			final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
			for(int i = 0; i < invitees.size(); i++){
				Log.d(TAG, "onActivityResult: id " + invitees.get(i));
			}

			//get auto-match criteria
			Bundle autoMatchCriteria = null;
			int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
			int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
			if (minAutoMatchPlayers > 0) {
				autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
			} else {
				autoMatchCriteria = null;
			}

			//create the room and specify a variant if appropriate
			RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
			roomConfigBuilder.addPlayersToInvite(invitees);
			if (autoMatchCriteria != null) {
				roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
			}
			Log.d(TAG, "onActivityResult: ");
			RoomConfig roomConfig = roomConfigBuilder.build();
			Games.RealTimeMultiplayer.create(mGoogleApiClient, roomConfig);

			//prevent screen from sleeping during handshake
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		if (requestCode == RC_WAITING_ROOM) {
			// ignore response code if the waiting room was dismissed from code:
			if (mWaitingRoomFinishedFromCode) return;
			if (resultCode == Activity.RESULT_OK) {
				// (start game)
				goToGameScreen(R.id.screen_game);
			} else if (resultCode == Activity.RESULT_CANCELED) {
				// Waiting room was dismissed with the back button. The meaning of this
				// action is up to the game. You may choose to leave the room and cancel the
				// match, or do something else like minimize the waiting room and
				// continue to connect in the background.

				// in this example, we take the simple approach and just leave the room:
				Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				// player wants to leave the room.
				Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
		}
		if (requestCode == RC_INVITATION_INBOX) {
			Log.d(TAG, "onActivityResult: RC_INVITATION_INBOX");
			if (resultCode != Activity.RESULT_OK) {
				// canceled
				return;
			}

			// get the selected invitation
			Bundle extras = data.getExtras();
			Invitation invitation =
					extras.getParcelable(Multiplayer.EXTRA_INVITATION);

			// accept it!
			RoomConfig roomConfig = makeBasicRoomConfigBuilder()
					.setInvitationIdToAccept(invitation.getInvitationId())
					.build();
			Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfig);

			// prevent screen from sleeping during handshake
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

			// go to game screen
		}
		if (requestCode == RC_SIGN_IN) {
			mSignInClicked = false;
			mResolvingConnectionFailure = false;
			if (resultCode == RESULT_OK) {
				mGoogleApiClient.connect();
			} else {
				// Bring up an error dialog to alert the user that sign-in
				// failed. The R.string.signin_failure should reference an error
				// string in your strings.xml file that tells the user they
				// could not be signed in, such as "Unable to sign in."
				BaseGameUtils.showActivityResultError(this,
						requestCode, resultCode, R.string.signin_failure);
			}
		}
	}

	//create a RoomConfigBuilder that's appropriate for your implementation
	private RoomConfig.Builder makeBasicRoomConfigBuilder() {
		return RoomConfig.builder(this)
				.setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);
	}



	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		findViewById(R.id.sign_in_button).setVisibility(View.GONE);
		findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
		Games.Invitations.registerInvitationListener(mGoogleApiClient, this);
		if (bundle != null) {
			Invitation inv = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
			if (inv != null) {
				//accept invitation
				RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
				roomConfigBuilder.setInvitationIdToAccept(inv.getInvitationId());
				Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

				//prevent screen from sleeping during handshake
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

				// go to game screen
			}
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		// Attempt to reconnect
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
		if (mResolvingConnectionFailure) {
			// already resolving
			return;
		}

		// if the sign-in button was clicked or if auto sign-in is enabled,
		// launch the sign-in flow
		if (mSignInClicked || mAutoStartSignInflow) {
			mAutoStartSignInflow = false;
			mSignInClicked = false;
			mResolvingConnectionFailure = true;

			// Attempt to resolve the connection failure using BaseGameUtils.
			// The R.string.signin_other_error value should reference a generic
			// error string in your strings.xml file, such as "There was
			// an issue with sign-in, please try again later."
			if (!BaseGameUtils.resolveConnectionFailure(this,
					mGoogleApiClient, connectionResult,
					RC_SIGN_IN, "Error")) {
				mResolvingConnectionFailure = false;
			}
		}

		// Put code here to display the sign-in button
		findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
		findViewById(R.id.sign_out_button).setVisibility(View.GONE);
	}

	@Override
	public void onInvitationReceived(Invitation invitation) {
		Log.d(TAG, "onInvitationReceived: join");
		RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
		roomConfigBuilder.setInvitationIdToAccept(invitation.getInvitationId());
		Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

		//prevent screen from sleeping during handshake
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	public void onInvitationRemoved(String s) {

	}

	boolean mWaitingRoomFinishedFromCode = false;

	@Override
	public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
		{
			byte[] b = realTimeMessage.getMessageData();
			String temp = new String(b, Charset.forName("UTF-8"));
			Log.d(TAG, "onRealTimeMessageReceived: " + temp);
			if (temp.length() > 3) {
				String prefix = temp.substring(0,3);
				if("chat".equalsIgnoreCase(prefix)){
					TextView textView = (TextView) findViewById(R.id.textView);
					textView.setText(textView.getText() + "\n" + getPlayerName(realTimeMessage.getSenderParticipantId())
							+ ": " + temp.substring(4));
				}else {
					if(myIndex != 0){
						responePing(temp, realTimeMessage.getSenderParticipantId());
					}else {
						int indexOfRespone = Integer.valueOf(temp.substring(0,1));
						int ping = (int) (System.currentTimeMillis() - Long.valueOf(temp.substring(5)));
						Log.d(TAG, "onRealTimeMessageReceived: index: " + indexOfRespone);
						Log.d(TAG, "onRealTimeMessageReceived: ping: " + ping + "ms");
						listPing.set(indexOfRespone, ping/2);
					}
				}
			} else {
				int dataIndex = Integer.valueOf(temp.substring(0, 1)) ;
				int dataPosition = Integer.valueOf(temp.substring(1)) ;
				if(myIndex == 0){
					Log.d(TAG, "onRealTimeMessageReceived: sent to client");
					sentData(dataPosition, null, dataIndex);
				}else {
					Toast.makeText(this, "" + dataIndex + ", " + dataPosition, Toast.LENGTH_SHORT).show();
					data[dataPosition] = dataIndex;
					customAdapter.notifyDataSetChanged();
				}
			}
		}
	}



	@Override
	public void onRoomConnecting(Room room) {

	}

	@Override
	public void onRoomAutoMatching(Room room) {

	}

	@Override
	public void onPeerInvitedToRoom(Room room, List<String> list) {

	}

	@Override
	public void onPeerDeclined(Room room, List<String> list) {
		// peer declined invitation -- see if game should be canceled
		if (!mPlaying && shouldCancelGame(room)) {
			Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	public void onPeerJoined(Room room, List<String> list) {

	}

	@Override
	public void onPeerLeft(Room room, List<String> list) {
		// peer left -- see if game should be canceled
		if (!mPlaying && shouldCancelGame(room)) {
			Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	public void onConnectedToRoom(Room room) {

	}

	@Override
	public void onDisconnectedFromRoom(Room room) {
		// leave the room
		Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);

		// clear the flag that keeps the screen on
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// show error message and return to main screen
	}

	@Override
	public void onPeersConnected(Room room, List<String> list) {
		if (mPlaying) {
			//add new player to an ongoing game
		} else if (shouldStartGame(room)) {

			//start game!
		}
	}

	@Override
	public void onPeersDisconnected(Room room, List<String> list) {
		if (mPlaying) {
			// do game-specific handling of this -- remove player's avatar
			// from the screen, etc. If not enough players are left for
			// the game to go on, end the game and leave the room.
		} else if (shouldCancelGame(room)) {
			// cancel the game
			Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	public void onP2PConnected(String s) {

	}

	@Override
	public void onP2PDisconnected(String s) {

	}

	@Override
	public void onRoomCreated(int i, Room room) {
		Log.d(TAG, "onRoomCreated: " + i);
		if(room == null){
			Log.d(TAG, "onRoomCreated: room null");
		}
		if (i != GamesStatusCodes.STATUS_OK) {
			//ldet screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		}else {
			// get waiting room intent
			Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
			startActivityForResult(intent, RC_WAITING_ROOM);
		}
	}

	@Override
	public void onJoinedRoom(int i, Room room) {
		if (i != GamesStatusCodes.STATUS_OK) {
			//let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		}
		Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
		startActivityForResult(intent, RC_WAITING_ROOM);
	}

	@Override
	public void onLeftRoom(int i, String s) {

	}

	@Override
	public void onRoomConnected(int i, Room room) {
		if (i != GamesStatusCodes.STATUS_OK) {
			//let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		} else {
			mRoomId = room.getRoomId();
			mRoom = room;
		}
	}

	//return whether there are enough players to start the game
	boolean shouldStartGame(Room room) {
		int connectedPlayers = 0;
		for (Participant p : room.getParticipants()) {
			if (p.isConnectedToRoom()) {
				++connectedPlayers;
			}
		}
		return connectedPlayers >= MIN_PLAYERS;
	}

	//returns whether the room is in a state where the game should be canceled
	boolean shouldCancelGame(Room room) {
		return true;
	}

	GridView gridView;
	int data[];
	CustomAdapter customAdapter;
	ArrayList<Participant> mParticipants;
	String mMyId;
	int myIndex = 0;

	private void goToGameScreen(int screenId) {
		Toast.makeText(this, "" + mRoomId, Toast.LENGTH_SHORT).show();
		int[] screens = {R.id.screen_game, R.id.screen_menu};
		for (int i = 0; i < screens.length; i++) {
			findViewById(screens[i]).setVisibility(screenId == screens[i] ? View.VISIBLE : View.GONE);
		}
		if (screenId == R.id.screen_game) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			mParticipants = mRoom.getParticipants();
			mMyId = mRoom.getParticipantId(Games.Players.getCurrentPlayerId(mGoogleApiClient));
			Log.d(TAG, "onCreate: " + mMyId);
			getMyIndex();
			createDefaultValue();
			gridView = (GridView) findViewById(R.id.gridView);
			customAdapter = new CustomAdapter(this, data);
			gridView.setAdapter(customAdapter);
			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					data[position] = myIndex;
					sentData(position, null, myIndex);
				}
			});
			findViewById(R.id.buttonSend).setOnClickListener(this);
			createListPing();
			getPing();
		}
	}

	private void createListPing() {
		if(myIndex == 0){
			listPing = new ArrayList<>();
			for (int i = 0; i < mParticipants.size(); i++){
				listPing.add(i, 0);
			}
		}
	}

	public void getMyIndex() {
		myIndex = 0;
		for (Participant p : mParticipants) {
			if (p.getParticipantId().equalsIgnoreCase(mMyId)) {
				break;
			} else {
				myIndex++;
			}
		}
	}

	public String getPlayerName(String id) {
		int temp = 0;
		for (Participant p : mParticipants) {
			if (p.getParticipantId().equalsIgnoreCase(id)) {
				break;
			} else {
				temp++;
			}
		}
		return "Player" + temp;
	}

	public void getPing(){
		if(myIndex == 0){
			String data = "ping" + System.currentTimeMillis();
			byte[] message = data.getBytes(Charset.forName("UTF-8"));
			for (Participant p : mParticipants) {
				if (!p.getParticipantId().equals(mMyId)) {
					Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, message,
							mRoom.getRoomId(), p.getParticipantId());
				}
			}
		}
	}

	public void responePing(String data, String idHost){
		data = myIndex + data;
		Log.d(TAG, "responePing: " + data);
		byte[] message = data.getBytes(Charset.forName("UTF-8"));
		Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, message,
				mRoom.getRoomId(), idHost);
	}

	public synchronized void sentData(int position, String mess, int index) {
		String data;
		if (mess != null) {
			data = "Chat" + mess;
		} else {
			data = String.valueOf(index) + position;
		}
		byte[] message = data.getBytes(Charset.forName("UTF-8"));
		//Games.RealTimeMultiplayer.sendUnreliableMessageToOthers(mGoogleApiClient, message, mRoom.getRoomId());
		if(myIndex != 0){
			Log.d(TAG, "sentData: sent to server");
			Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, message,
					mRoom.getRoomId(), mParticipants.get(0).getParticipantId());
		}else {
			for (Participant p : mParticipants) {
				if (!p.getParticipantId().equals(mMyId)) {
					Games.RealTimeMultiplayer.sendUnreliableMessage(mGoogleApiClient, message,
							mRoom.getRoomId(), p.getParticipantId());
				}
			}
			this.data[Integer.valueOf(position)] = index;
			customAdapter.notifyDataSetChanged();
		}
	}

	private void createDefaultValue() {
		data = new int[100];
		for (int i = 0; i < data.length; i++) {
			data[i] = -1;
		}
	}

	public void test(){

	}
}
