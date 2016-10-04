package com.example.rubycell.demomultiplayergoogleplayservices;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.GridView;

public class MainActivity extends AppCompatActivity {

	GridView gridView;
	int data[];
	CustomAdapter customAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		createDefaultValue();
		gridView = (GridView) findViewById(R.id.gridView);
		customAdapter = new CustomAdapter(this, data);
		gridView.setAdapter(customAdapter);
	}

	private void createDefaultValue(){
		data = new int[100];
		for(int i = 0 ; i < data.length; i++){
			data[i] = 0;
		}
	}
}
