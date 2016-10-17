package com.example.rubycell.demomultiplayergoogleplayservices;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.util.ArrayList;
import java.util.List;

public class MenuActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
		View.OnClickListener, RealTimeMessageReceivedListener,
		RoomStatusUpdateListener, RoomUpdateListener, OnInvitationReceivedListener {

	// request code for the "select players" UI
	// can be any number as long as it's unique
	final static int RC_SELECT_PLAYERS = 10000;
	// request code (can be any number, as long as it's unique)
	final static int RC_INVITATION_INBOX = 10001;

	private static final String TAG = MenuActivity.class.getSimpleName();
	Button btnInvite, btnShowInvitation;

	//are we already playing?
	boolean mPlaying = false;

	//at least 2 players requierd for our game
	final static int MIN_PLAYERS = 2;

	public static GoogleApiClient mGoogleApiClient;
	private static int RC_SIGN_IN = 9001;

	private boolean mResolvingConnectionFailure = false;
	private boolean mAutoStartSignInflow = true;
	private boolean mSignInClicked = false;

	boolean mExplicitSignOut = false;
	boolean mInSignInFlow = false;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		btnInvite = (Button)findViewById(R.id.btn_invite);
		btnShowInvitation = (Button)findViewById(R.id.btn_show_invite);
		btnInvite.setOnClickListener(this);
		btnShowInvitation.setOnClickListener(this);
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
		super.onStart();
		if (!mInSignInFlow && !mExplicitSignOut && !mGoogleApiClient.isConnected()) {
			// auto sign in
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		Log.d(TAG, "onStop: ");
		//mGoogleApiClient.disconnect();
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()){
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
			default:
				break;
		}
	}

	/**
	 * Dispatch incoming result to the correct fragment.
	 *
	 * @param requestCode
	 * @param resultCode
	 * @param data
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == RC_SELECT_PLAYERS){
			if(resultCode != Activity.RESULT_OK){
				//user canceld
				return;
			}
			//get the inviter list
			Bundle extras = data.getExtras();
			final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

			//get auto-match criteria
			Bundle autoMatchCriteria = null;
			int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
			int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
			if(minAutoMatchPlayers > 0){
				autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
			}else {
				autoMatchCriteria = null;
			}

			//create the room and specify a variant if appropriate
			RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
			roomConfigBuilder.addPlayersToInvite(invitees);
			if(autoMatchCriteria != null){
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
				goToGameScreen();
			}
			else if (resultCode == Activity.RESULT_CANCELED) {
				// Waiting room was dismissed with the back button. The meaning of this
				// action is up to the game. You may choose to leave the room and cancel the
				// match, or do something else like minimize the waiting room and
				// continue to connect in the background.

				// in this example, we take the simple approach and just leave the room:
				Games.RealTimeMultiplayer.leave(mGoogleApiClient, null, mRoomId);
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			}
			else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
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
			goToGameScreen();
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
	private RoomConfig.Builder makeBasicRoomConfigBuilder (){
		return RoomConfig.builder(this)
				.setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);
	}

	String mRoomId;
	public static Room mRoom;

	@Override
	public void onRoomCreated(int i, Room room) {
		Log.d(TAG, "onRoomCreated: " + i);
		if(i != GamesStatusCodes.STATUS_OK){
			//ldet screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		}
		mRoomId = room.getRoomId();
		mRoom = room;
		// get waiting room intent
		Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
		startActivityForResult(intent, RC_WAITING_ROOM);
	}



	// arbitrary request code for the waiting room UI.
	// This can be any integer that's unique in your Activity.
	final static int RC_WAITING_ROOM = 10002;

	@Override
	public void onJoinedRoom(int i, Room room) {
		if(i != GamesStatusCodes.STATUS_OK){
			//let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		}
		mRoom = room;
		Intent intent = Games.RealTimeMultiplayer.getWaitingRoomIntent(mGoogleApiClient, room, MIN_PLAYERS);
		startActivityForResult(intent, RC_WAITING_ROOM);
	}

	@Override
	public void onLeftRoom(int i, String s) {

	}

	@Override
	public void onRoomConnected(int i, Room room) {
		if(i != GamesStatusCodes.STATUS_OK){
			//let screen go to sleep
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			Toast.makeText(this, "Error create room", Toast.LENGTH_SHORT).show();
		}
	}

	boolean mWaitingRoomFinishedFromCode = false;
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
		// if "start game" message is received:
		if(!mWaitingRoomFinishedFromCode){
			mWaitingRoomFinishedFromCode = true;
			finishActivity(RC_WAITING_ROOM);
		}else {
			byte[] b = realTimeMessage.getMessageData();
			Log.d(TAG, "onRealTimeMessageReceived: " + b);
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

	//return whether there are enough players to start the game
	boolean shouldStartGame(Room room){
		int connectedPlayers = 0;
		for(Participant p : room.getParticipants()){
			if(p.isConnectedToRoom()){
				++connectedPlayers;
			}
		}
		return connectedPlayers >= MIN_PLAYERS;
	}

	//returns whether the room is in a state where the game should be canceled
	boolean shouldCancelGame(Room room){
		return true;
	}

	@Override
	public void onPeersConnected(Room room, List<String> list) {
		if(mPlaying){
			//add new player to an ongoing game
		}else if(shouldStartGame(room)){
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
	public void onConnected(@Nullable Bundle bundle) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		findViewById(R.id.sign_in_button).setVisibility(View.GONE);
		findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
		if(bundle != null){
			Invitation inv = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
			if(inv != null){
				//accept invitation
				RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
				roomConfigBuilder.setInvitationIdToAccept(inv.getInvitationId());
				Games.RealTimeMultiplayer.join(mGoogleApiClient, roomConfigBuilder.build());

				//prevent screen from sleeping during handshake
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

				// go to game screen
				goToGameScreen();
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

	}

	@Override
	public void onInvitationRemoved(String s) {

	}

	private void goToGameScreen(){
		startActivity(new Intent(this, MainActivity.class));
	}
}
