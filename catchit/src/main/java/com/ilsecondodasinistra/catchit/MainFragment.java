package com.ilsecondodasinistra.catchit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;

public class MainFragment extends Fragment {

    public static final String POSITION = "Position";

    int todayWeek;

    ListView timeTable;
    CustomAdapter leavingAdapter;

    List<Bus> times;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        timeTable = (ListView) rootView.findViewById(R.id.timeTable);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        //Preferences
        final SharedPreferences settings = getActivity().getPreferences(0);

//        Toast.makeText(rootView.getContext(), "Invocato onCreateView", Toast.LENGTH_SHORT).show();

        if (settings.getLong("lastWeekDay", 0) == 0) {
            saveToday(todayWeek);    //Prima volta che si avvia l'app, salviamo
            //il giorno corrente
        }
        // If current day is Sunday, todayWeek=1

//        populateTimeTableOld();
    }

    private void saveToday(int todayWeek) {
        //Preferences
        final SharedPreferences settings = getActivity().getPreferences(0);

		/*
         * Salviamo l'ultimo giorno in cui l'app è stata usata
		 * Se quando verrà riaperta il giorno sarà diverso
		 * allora verranno ricaricati gli autobus
		 */
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("lastWeekDay", todayWeek);
        editor.commit();
    }

    private void populateTimeTableOld() {

        /**
         * In base all'attributo "position" capisce in quale tab siamo
         */
        int position = getArguments().getInt(POSITION, 0);

        leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, times);
        timeTable.setAdapter(leavingAdapter);
    }

    public void populate(List<Bus> list) {
        if(timeTable.getAdapter() == null) { //If we already have an adapter, we don't need another. Or do we???
            times = list;
            leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, list);
            timeTable.setAdapter(leavingAdapter);
        }
    }

    public boolean isPopulated() {
        if(times != null && times.size() > 0)
            return true;

        return false;
    }

    public void show() {
        if(timeTable.getAdapter() == null)
            populate(times);
    }
}
