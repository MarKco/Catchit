package com.ilsecondodasinistra.catchit;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomAdapter extends ArrayAdapter<Bus> {

	final SimpleDateFormat parser = new SimpleDateFormat("kk:mm");
	
	public CustomAdapter(Context context, int resource,
			List<Bus> leavingTimes) {
		super(context, resource, leavingTimes);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		return getViewOptimize(position, convertView, parent);
	}
	
	public View getViewOptimize(int position, View convertView, ViewGroup parent)
	{
		ViewHolder viewHolder = null;
		if (convertView == null)
		{
			LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.tram_row, parent, false);

//            Typeface font = Typeface.createFromAsset(parent.getContext().getAssets(), "FuturaStd-Medium.otf");

			viewHolder = new ViewHolder();
			viewHolder.line = (TextView)convertView.findViewById(R.id.bus_line);
//            viewHolder.line.setTypeface(font);

            viewHolder.busArrivalTime = (TextView) convertView.findViewById(R.id.bus_arrival_time);

            viewHolder.departureTime = (TextView)convertView.findViewById(R.id.bus_departure_time);
//            viewHolder.departureTime.setTypeface(font);

            viewHolder.departureStop = (TextView)convertView.findViewById(R.id.bus_departure_stop);
//            viewHolder.departureStop.setTypeface(font);

            viewHolder.arrivalStop = (TextView)convertView.findViewById(R.id.bus_arrival_stop);
//            viewHolder.arrivalStop.setTypeface(font);

			convertView.setTag(viewHolder);
		}
		else
		{
			viewHolder = (ViewHolder)convertView.getTag();
		}
			
		Bus bus = getItem(position);
		
		viewHolder.line.setText(bus.getLine());
        viewHolder.line.setTextColor(bus.getTextColor());

        viewHolder.departureTime.setText(bus.getDepartureString());
        viewHolder.departureTime.setTextColor(bus.getTextColor());

        viewHolder.departureStop.setText(bus.getDepartureStop());
        viewHolder.arrivalStop.setText(bus.getArrivalStop());

        viewHolder.busArrivalTime.setText(bus.getArrivalString());

        if(position < 5) //In the first places of the list there could be some probably-already-passed buses we should emphasize
            if(bus.getDepartureStringForComparison().compareTo(parser.format(new Date())) < 0)
                bus.setColor(String.valueOf((getContext().getResources().getString(0+R.color.already_passed))));

		convertView.setBackgroundColor(bus.getColor());
		
		return convertView;
	}
	
	private class ViewHolder {
		public TextView busArrivalTime;
		public TextView line;
		public TextView departureTime;
        public TextView departureStop;
        public TextView arrivalStop;
	}
}
