package com.example.rubycell.demomultiplayergoogleplayservices;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

public class LobbyActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient
		.OnConnectionFailedListener {

	private static final String TAG = LobbyActivity.class.getSimpleName();
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
		setContentView(R.layout.activity_lobby);
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
		if (!mInSignInFlow && !mExplicitSignOut) {
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

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
		findViewById(R.id.sign_in_button).setVisibility(View.GONE);
		findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
		startActivity(new Intent(this, MenuActivity.class));
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

	// Call when the sign-in button is clicked
	private void signInClicked() {
		mSignInClicked = true;
		mGoogleApiClient.connect();
	}

	// Call when the sign-out button is clicked
	private void signOutclicked() {
		mSignInClicked = false;
		Games.signOut(mGoogleApiClient);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v The view that was clicked.
	 */
	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.sign_in_button) {
			// start the asynchronous sign in flow
			Toast.makeText(this, "connect", Toast.LENGTH_SHORT).show();
			mSignInClicked = true;
			mGoogleApiClient.connect();
		}
		else if (view.getId() == R.id.sign_out_button) {
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
}
