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

    public static final MainFragment newInstance(int i) {
        MainFragment f = new MainFragment();
        Bundle bdl = new Bundle(1);
        bdl.putInt(POSITION, i);
        f.setArguments(bdl);
        return f;
    }

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("H.mm"); //DateFormatter four hours.minutes

//    List<Bus> origLeavingTimes;
//    List<Bus> origComingTimes;

    ListView timeTable;
    CustomAdapter leavingAdapter;

    List<Bus> leavingTimes;
    List<Bus> comingTimes;

    List<Bus> tramLeavingTimes;
    List<Bus> tramComingTimes;

    List<Bus> tramCentroSansovinoTimes;
    List<Bus> tramSansovinoCentroTimes;

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

//            populateTimes();
        leavingTimes = ((MainCatchitActivity) getActivity()).getTramToVeniceTimes();
        comingTimes = ((MainCatchitActivity) getActivity()).getTramToMestreTimes();
        tramLeavingTimes = ((MainCatchitActivity) getActivity()).getTramToStationTimes();
        tramComingTimes = ((MainCatchitActivity) getActivity()).getTramToMestreCityCenterTimes();
        tramCentroSansovinoTimes = ((MainCatchitActivity) getActivity()).getTramCentroSansovinoTimes();
        tramSansovinoCentroTimes = ((MainCatchitActivity) getActivity()).getTramSansovinoCentroTimes();

        populateTimeTableOld();
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

//        tramToVeniceTimes = new LinkedList<Bus>();
//        tramToMestreTimes = new LinkedList<Bus>();
//
//        tramToStationTimes = new LinkedList<Bus>();
//        tramToMestreCityCenterTimes = new LinkedList<Bus>();

        //Preferences
        final SharedPreferences settings = getActivity().getPreferences(0);

//        int position = getArguments().getInt("position", 0);
        /**
         * In base all'attributo "position" capisce in quale tab siamo
         */
        int position = getArguments().getInt(POSITION, 0);

        switch (position) {
            case 1:
                leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, leavingTimes);
                break;
            case 2:
                leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, comingTimes);
                break;
            case 3:
                leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, tramLeavingTimes);
                break;
            case 4:
                leavingAdapter = new CustomAdapter(getActivity(), R.layout.row, tramComingTimes);
                break;
        }
        timeTable.setAdapter(leavingAdapter);

//        Toast.makeText(getActivity().getBaseContext(), "Invocato onStart", Toast.LENGTH_SHORT).show();
    }
}
