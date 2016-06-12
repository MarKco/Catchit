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
    CustomAdapter comingAdapter;
    CustomAdapter tramLeavingAdapter;
    CustomAdapter tramComingAdapter;
//    private TextView mSelected;

    List<Bus> leavingTimes;
    List<Bus> comingTimes;

    List<Bus> tramLeavingTimes;
    List<Bus> tramComingTimes;

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

        populateTimeTableOld();
    }

    /*
     * Meglio riordinare l'array ogni volta
     * che si riapre l'app, sai mai...
     */
//    @Override
//	public void onResume() {
//		super.onResume();
//
//		//Preferences
//		final SharedPreferences settings = getActivity().getPreferences(0);
//
//		Calendar calendar = Calendar.getInstance();
//		int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
//		// If current day is Sunday, weekDay=1
//
//		try {
//			/*
//			 * Decommenta queste righe per far sì che la lista
//			 * degli autobus NON venga ricaricata ad ogni avvio
//			 * Considera però che questo porterà, per qualche motivo,
//			 * a far sì che eventuali autobus appena passati vadano,
//			 * dopo più di 7 minuti, in fondo alla giornata anziché
//			 * in fondo alla lista
//			 */
//			if(settings.getLong("lastWeekDay", 0) != weekDay)
//			{
//				populateTimes();
////				Log.i("CatchIt", "Ricarico gli autobus, l'ultima volta il giorno era " + settings.getLong("lastWeekDay", 0) + " ed ora è " + weekDay);
//			}
//			sortAndFilterThoseLists(weekDay);
//        	leavingAdapter = new CustomAdapter(getActivity().getBaseContext(), R.layout.row, tramToVeniceTimes);
//        	comingAdapter = new CustomAdapter(getActivity().getBaseContext(), R.layout.row, tramToMestreTimes);
//        	tramLeavingAdapter = new CustomAdapter(getActivity().getBaseContext(), R.layout.tram_row, tramToStationTimes);
//        	tramComingAdapter = new CustomAdapter(getActivity().getBaseContext(), R.layout.tram_row, tramToMestreCityCenterTimes);
////        	switch(getSupportActionBar().getSelectedTab().getPosition())
////        	{
////        		case(0):
////                	timeTable.setAdapter(leavingAdapter);
////        			break;
////        		case(1):
////                	timeTable.setAdapter(comingAdapter);
////        			break;
////        		case(2):
////        			timeTable.setAdapter(tramLeavingAdapter);
////        			break;
////        		case(3):
////        			timeTable.setAdapter(tramComingAdapter);
////        			break;
////        	}
////			Log.i("catchIt", "Ordinata!");
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			Log.i("catchIt", e.getStackTrace().toString());
//		}
//	}

//	@Override
//	public void onPause() {
//		super.onPause();
//
//		Calendar calendar = Calendar.getInstance();
//		int todayWeek = calendar.get(Calendar.DAY_OF_WEEK);
//		// If current day is Sunday, todayWeek=1
//
//		//Preferences
//		final SharedPreferences settings = getActivity().getPreferences(0);
//
//		SharedPreferences.Editor editor = settings.edit();
////	    editor.putInt("tabSelected", getSupportActionBar().getSelectedNavigationIndex());
//
////	    Log.i("Catchit", "Il tab selezionato è " + getSupportActionBar().getSelectedNavigationIndex());
//
//	    editor.commit();
//
//		saveToday(todayWeek);
//	}

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

//    private void populateTimes() throws ParseException {
//        //Liste che verranno ordinate
//        leavingTimes = new LinkedList<Bus>();
//        comingTimes = new LinkedList<Bus>();
//
//        tramLeavingTimes = new LinkedList<Bus>();
//        tramComingTimes = new LinkedList<Bus>();
//
//		/*
//         * Inserisco le partenze da Mestre
//		 */
//
//        leavingTimes.add(new Bus(dateFormatter.parse("00.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.00"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.15"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.14"),
//                "NOALE",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.30"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("05.58"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.00"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.03"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.15"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.30"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.28"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.30"),
//                "NOALE'",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.43"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.00"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("06.58"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.07"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.15"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.22"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.23"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.30"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.37"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.52"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.53"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.00"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("07.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.07"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.15"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.22"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.30"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("08.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.05"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("09.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("10.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("11.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("12.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("13.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("14.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.53"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("15.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.23"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("16.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("17.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.48"),
//                "NOALE",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("18.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.00"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.15"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.30"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.45"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("19.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.08"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.35"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("20.58"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("21.05"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("21.38"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("21.19"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("21.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("21.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("22.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("22.28"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("22.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("23.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("23.28"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        leavingTimes.add(new Bus(dateFormatter.parse("23.49"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//
//		/*
//         * PARTENZE DA VENEZIA (feriali e festive mescolate)
//		 */
//
//        comingTimes.add(new Bus(dateFormatter.parse("00.00"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("00.11"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("00.15"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("00.31"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("00.45"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("00.51"),
//                "N2",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("01.11"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("01.21"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("01.51"),
//                "N2",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("02.11"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("02.31"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("02.51"),
//                "N2",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("03.11"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("03.31"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("03.51"),
//                "N2",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("04.11"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("04.31"),
//                "N1",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("04.51"),
//                "N2",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("05.25"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("05.43"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("05.55"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("05.58"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.13"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.25"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.28"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.35"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.43"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("06.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("07.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("08.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("09.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("10.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("11.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("12.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.10"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("13.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("14.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("15.58"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.28"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.50"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("16.58"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.10"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.13"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.20"),
//                "SCORZE'",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.28"),
//                "12L",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.43"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("17.58"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.13"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.28"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.43"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("18.58"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.13"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.25"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.28"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.43"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.55"),
//                "12L",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("19.58"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.13"),
//                "12L",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.22"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.37"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.45"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("20.52"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("21.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("21.07"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("21.15"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("21.30"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("21.45"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("22.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("22.15"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("22.30"),
//                "NOALE",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("22.45"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("23.00"),
//                "SCORZE'",
//                true, //Feriale
//                true, //Sabato
//                true, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("23.15"),
//                "12",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("23.30"),
//                "NOALE",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("23.31"),
//                "N1",
//                true, //Feriale
//                false, //Sabato
//                false, //Domenica
//                false));
//        comingTimes.add(new Bus(dateFormatter.parse("23.45"),
//                "12/",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//
//
//        /********
//         *  TRAM *
//         ********/
//
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.35"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.45"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.55"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("21.05"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("21.15"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("21.25"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                true));
//
//        /** TRAM FESTIVI PIAZZA -> STAZIONE **/
//
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.25"),
//                "T1",
//                false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("6.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.05"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("7.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("8.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("9.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("10.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("11.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("12.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("13.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("14.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("15.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("16.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("17.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("18.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("19.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.40"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("20.55"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("21.10"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//        tramLeavingTimes.add(new Bus(dateFormatter.parse("21.25"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                true));
//
//        /** TRAM STAZIONE -> PIAZZA FERIALI **/
//
//        tramComingTimes.add(new Bus(dateFormatter.parse("6.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("6.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.53"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.03"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.13"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.23"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.33"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.43"),
//                "T1",
//                true, //Feriale
//                true, //Sabato
//                false, //Domenica
//                false));
//
//        tramComingTimes.add(new Bus(dateFormatter.parse("6.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("6.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("7.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("8.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("9.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("10.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("11.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("12.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("13.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("14.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("15.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("16.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("17.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("18.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("19.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("20.53"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.08"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.23"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//        tramComingTimes.add(new Bus(dateFormatter.parse("21.38"), "T1", false, //Feriale
//                false, //Sabato
//                true, //Domenica
//                false));
//
//
//        //Liste che non verranno ordinate
////		origLeavingTimes  = new LinkedList<Bus>(tramToVeniceTimes);
////		origComingTimes = new LinkedList<Bus>(tramToMestreTimes);
//
//        //		for(int i = 0; i < tramToVeniceTimes.size(); i++)
//        //		{
//        //			Log.i("catchIt", tramToVeniceTimes.get(i).toString());
//        //		}
//
//    }

}
