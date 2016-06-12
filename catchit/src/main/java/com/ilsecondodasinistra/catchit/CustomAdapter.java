package com.ilsecondodasinistra.catchit;

import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomAdapter extends ArrayAdapter<Bus> {

	final int MINUTES_IT_TAKES_THE_TRAM_FROM_A_TO_B_OR_BACK = 9;
	final SimpleDateFormat parser = new SimpleDateFormat("HH.mm");
	
	public CustomAdapter(Context context, int resource,
			List<Bus> leavingTimes) {
		super(context, resource, leavingTimes);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
//		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//		convertView = inflater.inflate(R.layout.row, null);
//		TextView line = (TextView)convertView.findViewById(R.id.bus_line);		
//		TextView time = (TextView)convertView.findViewById(R.id.bus_time);
//		
//		Bus bus = getItem(position);
//		
//		line.setText(bus.getLine());
//		time.setText(bus.getDepartureString());
//		
//		return convertView;
		return getViewOptimize(position, convertView, parent);
	}
	
	public View getViewOptimize(int position, View convertView, ViewGroup parent)
	{
		ViewHolder viewHolder = null;
		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.tram_row, parent, false);

            Typeface font = Typeface.createFromAsset(parent.getContext().getAssets(), "FuturaStd-Medium.otf");

			viewHolder = new ViewHolder();
			viewHolder.line = (TextView)convertView.findViewById(R.id.bus_line);
            viewHolder.line.setTypeface(font);

			viewHolder.time = (TextView)convertView.findViewById(R.id.bus_time);
            viewHolder.time.setTypeface(font);

			viewHolder.busArrivingTime = (TextView) convertView.findViewById(R.id.busArrivingTime);
			
//			TextView line = (TextView)convertView.findViewById(R.id.bus_line);		
//			TextView time = (TextView)convertView.findViewById(R.id.bus_time);
			
			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder)convertView.getTag();
		}
			
		Bus bus = getItem(position);
		
		viewHolder.line.setText(bus.getLine());
		viewHolder.time.setText(bus.getDepartureString());
		viewHolder.line.setTextColor(bus.getTextColor());
		viewHolder.time.setTextColor(bus.getTextColor());

		viewHolder.busArrivingTime.setText("-> " + bus.getArrivalString());
		viewHolder.busArrivingTime.setVisibility(View.VISIBLE);

		convertView.setBackgroundColor(bus.getColor());
		
		return convertView;
	}
	
	private class ViewHolder {
		public TextView busArrivingTime;
		public TextView line;
		public TextView time;
	}
}
