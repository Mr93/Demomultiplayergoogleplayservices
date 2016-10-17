package com.example.rubycell.demomultiplayergoogleplayservices;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.Room;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = MainActivity.class.getSimpleName();
	GridView gridView;
	int data[];
	CustomAdapter customAdapter;
	GoogleApiClient mGoogleApiClient;
	Room mRoom;
	ArrayList<Participant> mParticipants;
	String mMyId;
	int myIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mGoogleApiClient = MenuActivity.mGoogleApiClient;
		mRoom = MenuActivity.mRoom;
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
				sentData(position);
				customAdapter.notifyDataSetChanged();
			}
		});
	}

	public void getMyIndex() {
		myIndex = 0;
		for(Participant p : mParticipants){
			if(p.getParticipantId().equalsIgnoreCase(mMyId)){
				break;
			}else {
				myIndex++;
			}
		}
	}

	public void sentData(int position){
		String data = String.valueOf(myIndex)  + position;
		byte[] message = data.getBytes();
		for (Participant p : mParticipants) {
			Games.RealTimeMultiplayer.sendReliableMessage(mGoogleApiClient, null, message,
					mRoom.getRoomId(), p.getParticipantId());
			/*if (!p.getParticipantId().equals(mMyId)) {

			}*/
		}
	}

	private void createDefaultValue(){
		data = new int[100];
		for(int i = 0 ; i < data.length; i++){
			data[i] = -1;
		}
	}
}
