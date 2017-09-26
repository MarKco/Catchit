package com.ilsecondodasinistra.catchit

import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView

class MainFragment : Fragment() {

    internal var todayWeek: Int = 0

    internal lateinit var timeTable: ListView
    internal lateinit var leavingAdapter: CustomAdapter

    private var swipeContainer: SwipeRefreshLayout? = null
    internal var times: List<Bus> = ArrayList<Bus>()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_main, container, false)
        timeTable = rootView.findViewById<View>(R.id.timeTable) as ListView
        setHasOptionsMenu(true)
        swipeContainer = rootView.findViewById<View>(R.id.swipeContainer) as SwipeRefreshLayout
        // Setup refresh listener which triggers new data loading
        swipeContainer!!.setOnRefreshListener {
            // Your code to refresh the list here.
            // Make sure you call swipeContainer.setRefreshing(false)
            // once the network request has completed successfully.
            (activity as MainCatchitActivity).reloadSingleFragmentDataFromFragment(this@MainFragment)
        }
        return rootView
    }

    fun setRefresingForFragment(refresingForFragment: Boolean) {
        swipeContainer!!.isRefreshing = false
    }

    override fun onStart() {
        super.onStart()

        //Preferences
        val settings = activity.getPreferences(0)

        //        Toast.makeText(rootView.getContext(), "Invocato onCreateView", Toast.LENGTH_SHORT).show();

        if (settings.getLong("lastWeekDay", 0) == 0L) {
            saveToday(todayWeek)    //Prima volta che si avvia l'app, salviamo
            //il giorno corrente
        }
        // If current day is Sunday, todayWeek=1

        //        populateTimeTableOld();
    }

    private fun saveToday(todayWeek: Int) {
        //Preferences
        val settings = activity.getPreferences(0)

        /*
         * Salviamo l'ultimo giorno in cui l'app è stata usata
		 * Se quando verrà riaperta il giorno sarà diverso
		 * allora verranno ricaricati gli autobus
		 */
        val editor = settings.edit()
        editor.putLong("lastWeekDay", todayWeek.toLong())
        editor.commit()
    }

    private fun populateTimeTableOld() {

        /**
         * In base all'attributo "position" capisce in quale tab siamo
         */
        val position = arguments.getInt(POSITION, 0)

        leavingAdapter = CustomAdapter(activity, R.layout.row, times!!)
        timeTable.adapter = leavingAdapter
    }

    fun populate(list: List<Bus>, force: Boolean) {
        if (force || timeTable.adapter == null) { //If we already have an adapter, we don't need another. Or do we???
            times = list
            leavingAdapter = CustomAdapter(activity, R.layout.row, list)
            timeTable.adapter = leavingAdapter
            timeTable.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
                AlertDialog.Builder(context)
                        .setTitle(times!![position].line)
                        .setMessage("Partenza da " + times!![position].departureStop + " alle " + times!![position].departureString + " e arrivo a " + times!![position].arrivalStop + " alle " + times!![position].arrivalString)
                        .setPositiveButton(android.R.string.yes) { dialog, which ->
                            // continue with delete
                        }
                        .setIcon(R.drawable.bus)
                        .show()
            }
        }
    }

    val isPopulated: Boolean
        get() {
            if (times != null && times!!.size > 0)
                return true

            return false
        }

    fun show() {
        if (timeTable.adapter == null)
            populate(times, false)
    }

    companion object {

        val POSITION = "Position"
    }
}
