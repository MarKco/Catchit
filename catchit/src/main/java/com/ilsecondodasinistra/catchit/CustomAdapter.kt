package com.ilsecondodasinistra.catchit

import java.text.SimpleDateFormat
import java.util.Date

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CustomAdapter(context: Context, resource: Int,
                    leavingTimes: List<Bus>) : ArrayAdapter<Bus>(context, resource, leavingTimes) {

    internal val parser = SimpleDateFormat("kk:mm")

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getViewOptimize(position, convertView, parent)
    }

    fun getViewOptimize(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        var viewHolder: ViewHolder? = null
        if (convertView == null) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            convertView = inflater.inflate(R.layout.tram_row, parent, false)

            //            Typeface font = Typeface.createFromAsset(parent.getContext().getAssets(), "FuturaStd-Medium.otf");

            viewHolder = ViewHolder()
            viewHolder.line = convertView!!.findViewById<View>(R.id.bus_line) as TextView
            //            viewHolder.line.setTypeface(font);

            viewHolder.busArrivalTime = convertView.findViewById<View>(R.id.bus_arrival_time) as TextView

            viewHolder.departureTime = convertView.findViewById<View>(R.id.bus_departure_time) as TextView
            //            viewHolder.departureTime.setTypeface(font);

            viewHolder.departureStop = convertView.findViewById<View>(R.id.bus_departure_stop) as TextView
            //            viewHolder.departureStop.setTypeface(font);

            viewHolder.arrivalStop = convertView.findViewById<View>(R.id.bus_arrival_stop) as TextView
            //            viewHolder.arrivalStop.setTypeface(font);

            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }

        val bus = getItem(position)

        viewHolder.line!!.text = bus!!.line
        viewHolder.line!!.setTextColor(bus.textColor)

        viewHolder.departureTime!!.text = bus.departureString
        viewHolder.departureTime!!.setTextColor(bus.textColor)

        viewHolder.departureStop!!.text = bus.departureStop
        viewHolder.arrivalStop!!.text = bus.arrivalStop

        viewHolder.busArrivalTime!!.text = bus.arrivalString

        if (position < 5)
        //In the first places of the list there could be some probably-already-passed buses we should emphasize
            if (bus.departureStringForComparison.compareTo(parser.format(Date())) < 0)
                bus.setColor(context.resources.getString(0 + R.color.already_passed).toString())

        convertView.setBackgroundColor(bus.color)

        return convertView
    }

    private inner class ViewHolder {
        var busArrivalTime: TextView? = null
        var line: TextView? = null
        var departureTime: TextView? = null
        var departureStop: TextView? = null
        var arrivalStop: TextView? = null
    }
}
