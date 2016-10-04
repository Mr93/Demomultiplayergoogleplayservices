package com.example.rubycell.demomultiplayergoogleplayservices;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

/**
 * Created by RUBYCELL on 10/3/2016.
 */
public class CustomAdapter extends BaseAdapter {

	Context context;
	int[] data;
	LayoutInflater inflater;

	public CustomAdapter(Context context, int[] data ) {
		this.context = context;
		this.data = data;
		inflater = (LayoutInflater.from(context));
	}

	@Override
	public int getCount() {
		return data.length;
	}

	@Override
	public Object getItem(int i) {
		return null;
	}

	@Override
	public long getItemId(int i) {
		return 0;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		view = inflater.inflate(R.layout.activity_gridview, null);
		ImageView icon = (ImageView) view.findViewById(R.id.icon);
		icon.setBackgroundColor(getColor(data[i]));
		return view;
	}

	private int getColor(int colorCode){
		if(colorCode == 0){
			return context.getResources().getColor(R.color.firstColor);
		}else if(colorCode == 1){
			return context.getResources().getColor(R.color.secondColor);
		}else if(colorCode == 2){
			return context.getResources().getColor(R.color.thirdColor);
		}else if(colorCode == 3){
			return context.getResources().getColor(R.color.fourthColor);
		}else if(colorCode == 4){
			return context.getResources().getColor(R.color.fiveThColor);
		}else if(colorCode == 5){
			return context.getResources().getColor(R.color.sixThColor);
		}else if(colorCode == 6){
			return context.getResources().getColor(R.color.sevenThColor);
		} else {
			return context.getResources().getColor(R.color.eightThColor);
		}
	}
}
