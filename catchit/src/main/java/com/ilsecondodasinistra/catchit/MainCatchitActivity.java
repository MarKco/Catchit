package com.ilsecondodasinistra.catchit;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;

import com.ilsecondodasinistra.catchit.database.DaoMaster;
import com.ilsecondodasinistra.catchit.database.DaoSession;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainCatchitActivity extends AppCompatActivity {

    public static final String POSITION = "Position";

    Toolbar toolbar;

    List<Bus> tramToVeniceTimes = new LinkedList<>();
    List<Bus> tramToMestreTimes = new LinkedList<>();

    List<Bus> tramToStationTimes = new LinkedList<>();
    List<Bus> tramToMestreCityCenterTimes = new LinkedList<>();

    List<Bus> tramCentroSansovinoTimes = new LinkedList<>();
    List<Bus> tramSansovinoCentroTimes = new LinkedList<>();

    public static final boolean DEBUGHOUR = true; //if true, custom departureTime below is set in the app,
    public static final boolean DEBUGDAY = false; //if true, custom date below is set in the app,
    // regardless of real timestamp
    public static final int DEBUG_HOURS = 13;
    public static final int DEBUG_MINUTES = 33;
    public static final int DEBUG_SECONDS = 0;
    public static final int DEBUG_MILLISECONDS = 0;

    public static String routeForT1MestreVe = "1022, 1024";
    public static String routeForT1VeMestre = "1026, 1028";
    public static String routeForT2MestreMa = "1114, 1115";
    public static String routeForT2MaMestre = "1116, 1118, 1117, 1119";

    public static String routeForN1 = "987, 988, 989, 990";
    public static String routeForN2 = "991, 992";

    public static String routeFor12MestreVe = "693, 694";
    public static String routeFor12VeMestre = "691, 692";

    public static String departingSansovino = "6061";
    public static String returningSansovino = "6062";
    public static String sansovinoForN = departingSansovino + ", " + returningSansovino;
    public static String veniceStops = "510, 6084";
    public static String cialdini = "6080, 6027, 6081";
    public static String stazioneMestre = "6074, 6073";

    int todayWeek;
    int debugDayOfTheWeek = Calendar.SUNDAY;

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;
    SQLiteDatabase db;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss"); //DateFormatter four hours.minutes.seconds
    private SimpleDateFormat databaseHourFormatter = new SimpleDateFormat("HH:mm:ss"); //DateFormatter four hours.minutes.seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        Calendar calendar = Calendar.getInstance();
        todayWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if(DEBUGDAY)
            todayWeek = debugDayOfTheWeek;

        //add the Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        final TextView titleText = (TextView) findViewById(R.id.title);

        List<Fragment> fragments = getFragments();

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), fragments);
        mPager.setAdapter(mPagerAdapter);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                try {
                    switch (position) {
                        case 0:
                            titleText.setText("Mestre -> Venezia");
                            break;
                        case 1:
                            titleText.setText("Venezia -> Mestre");
                            break;
                        case 2:
                            titleText.setText("Tram Centro -> Stazione");
                            break;
                        case 3:
                            titleText.setText("Tram Stazione -> Centro");
                            break;
                        case 4:
                            titleText.setText("Tram Centro -> Sansovino");
                            break;
                        case 5:
                            titleText.setText("Tram Sansovino -> Centro");
                            break;
                    }
                } catch (NullPointerException e) {
                    Log.e("Catchit", "I wonder why" + e.getStackTrace().toString());
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public SQLiteDatabase getDatabase() {
        return db;
    }

    @Override
    protected void onStart() {
        super.onStart();

        MyDatabase dbHelper = new MyDatabase(this.getApplicationContext());
        db = dbHelper.getReadableDatabase();

        populateTimeTable();

        //GreenDAO works: will we use it?
//        ArrayList<calendar> calendarList = (ArrayList<calendar>) Database.getDaoSession(getApplicationContext()).getCalendarDao().queryBuilder().list();

//        testQuery();

//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                if (Database.getDaoSession(getApplicationContext()).getRoutesDao().count() == 0)
//                    Database.fillCacheTables(PagerActivity.this);
//            }
//        }).run();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    private List<Fragment> getFragments() {
        List<Fragment> fList = new ArrayList<Fragment>();

        for (int position = 1; position < 7; position++) {
            Bundle args = new Bundle();
            MainFragment thisFragment = new MainFragment();
            args.putInt(POSITION, position);
            thisFragment.setArguments(args);
            fList.add(thisFragment);
        }

        return fList;
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class ScreenSlidePagerAdapter extends FragmentPagerAdapter {
        private List<Fragment> fragments;

        public ScreenSlidePagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            this.fragments = fragments;
        }

        @Override
        public Fragment getItem(int position) {
            return this.fragments.get(position);
        }

        @Override
        public int getCount() {
            return this.fragments.size();
        }
    }

//    public static class Database {
//
//        private static SQLiteDatabase db = null;
//        private static DaoSession daoSession = null;
//
//        public static SQLiteDatabase getDatabase(Context context) {
//            if (db == null) {
//                // As we are in development we will use the DevOpenHelper which drops the database on a schema update
//                //TODO: To be changed before we go live
//                DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(context, "actv.db", null);
//                // Access the database using the helper
//                db = helper.getWritableDatabase();
//            }
//            return db;
//        }
//
//        public static DaoSession getDaoSession(Context context) {
//
//            if (daoSession == null) {
//                // Construct the DaoMaster which brokers DAOs for the Domain Objects
//                DaoMaster daoMaster = new DaoMaster(getDatabase(context));
//                daoSession = daoMaster.newSession();
//            }
//
//            return daoSession;
//        }

        /**
         * Old method for database filling.
         * Now we use another one, but I keep this as reference
         * @param context
         */
//        public static void fillCacheTables(Context context) {
//
//            List<String> queries = new ArrayList<String>();
//            BufferedReader br = null;
//
//            try {
//                AssetManager assetManager = context.getAssets();
//                String query;
////                br = new BufferedReader(new InputStreamReader(assetManager.open("queries.sql")));
//                br = new BufferedReader(new InputStreamReader(assetManager.open("actv_without_shapes_only_insert.sql")));
//                while ((query = br.readLine()) != null) {
//                    queries.add(query);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                try {
//                    if (br != null)
//                        br.close();
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                }
//            }
//
//            DbUtils.executeSqlStatementsInTx(Database.getDatabase(context), queries.toArray(new String[queries.size()]));
//        }
//
//    }

    private void populateTimeTable() {
        final SQLiteDatabase db = getDatabase();

        final String dayForQuery;
        final String tomorrowForQuery;

        tramToVeniceTimes = new LinkedList<>();
        tramToMestreTimes = new LinkedList<>();

        tramToStationTimes = new LinkedList<>();
        tramToMestreCityCenterTimes = new LinkedList<>();

        tramCentroSansovinoTimes = new LinkedList<>();
        tramSansovinoCentroTimes = new LinkedList<>();

        switch (todayWeek) {
            case Calendar.SUNDAY:
                dayForQuery = "c.sunday";
                tomorrowForQuery = "c.monday";
                break;
            case Calendar.MONDAY:
                dayForQuery = "c.monday";
                tomorrowForQuery = "c.wednesday";
                break;
            case Calendar.WEDNESDAY:
                dayForQuery = "c.wednesday";
                tomorrowForQuery = "c.thursday";
                break;
            case Calendar.THURSDAY:
                dayForQuery = "c.thursday";
                tomorrowForQuery = "c.friday";
                break;
            case Calendar.FRIDAY:
                dayForQuery = "c.friday";
                tomorrowForQuery = "c.saturday";
                break;
            default:
                dayForQuery = "c.saturday";
                tomorrowForQuery = "c.sunday";
                break;
        }

        Thread busPopulateThread = new Thread(new Runnable() {
            @Override
            public void run() {

                /**It seems tricky but it's not. By selecting from the database all the buses AFTER current timestamp
                 * and then the ones BEFORE current timestamp, we avoid sorting objects which is slow and consuming
                 */

                Date now = new Date();
                if (DEBUGHOUR) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(now); //This date is a copy of present datetime (which actually is Linux Epoch)
                    cal.set(Calendar.HOUR_OF_DAY, DEBUG_HOURS); //We just change the hours, minutes, seconds
                    cal.set(Calendar.MINUTE, DEBUG_MINUTES);
                    cal.set(Calendar.SECOND, DEBUG_SECONDS);
                    cal.set(Calendar.MILLISECOND, DEBUG_MILLISECONDS);
                    now.setTime(cal.getTimeInMillis());
                }
                if(DEBUGDAY)
                    now.setDate(debugDayOfTheWeek);

                ArrayList<String> operators = new ArrayList<>();
                operators.add(">");
                operators.add("<");

                for(String operator : operators) {

                    String dayForThisQuery;

                    if(operators.indexOf(operator) == 0)
                        dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                    else
                        dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow

                    String moreTramToVenice =
                            "SELECT t.trip_id,\n" +
                                    "       start_s.stop_name as departure_stop,\n" +
                                    "\t   start_s.stop_id as departure_stop_id,\n" +
                                    "       start_st.departure_time as departure_time,\n" +
                                    "       direction_id as direction,\n" +
                                    "       end_s.stop_name as arrival_stop,\n" +
                                    "\t   end_s.stop_id as arrival_stop_id,\n" +
                                    "       end_st.arrival_time as arrival_time,\n" +
                                    "       r.route_short_name as route_short_name,\n" +
                                    "       end_st.late_night as bus_late_night,\n" +
                                    "       r.route_long_name as route_long_name\n" +
                                    "FROM\n" +
                                    "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                                    "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                                    "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                                    "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                                    "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                                    "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                                    "WHERE " + dayForThisQuery + " = 1\n" +
                                    "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                                    "  and departure_stop_id in (" + sansovinoForN + ")\n" + //For night buses
                                    "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                                    "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" +   //Needed only because N1 and N2 are circular, so you could get paradoxical results
                                    "  and end_s.stop_id in (" + veniceStops + ")\n" +
                                    "  and bus_late_night IS NOT NULL\n" +
                                    "order by start_st.departure_time asc";

//                    if(BuildConfig.DEBUG)
//                        Log.w("MoreTramToVenice", moreTramToVenice);

                    Cursor moreLeavingCursor = db.rawQuery(moreTramToVenice, null);
                    moreLeavingCursor.moveToFirst();
                    if (moreLeavingCursor.getCount() > 0)
                        try {
                            do {
                                String departureStop = moreLeavingCursor.getString(moreLeavingCursor.getColumnIndex("departure_stop"));
                                String departureTime = moreLeavingCursor.getString(moreLeavingCursor.getColumnIndex("departure_time"));
                                String arrivalStop = moreLeavingCursor.getString(moreLeavingCursor.getColumnIndex("arrival_stop"));
                                String arrivalTime = moreLeavingCursor.getString(moreLeavingCursor.getColumnIndex("arrival_time"));
                                String line = moreLeavingCursor.getString(moreLeavingCursor.getColumnIndex("route_short_name"));

                                tramToVeniceTimes.add(new Bus(dateFormatter.parse(departureTime),
                                        line,
                                        departureStop,
                                        arrivalStop,
                                        dateFormatter.parse(arrivalTime)
                                ));
                                //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                            } while (moreLeavingCursor.moveToNext());
                        } catch (ParseException e) {
                            Log.e("Catchit", "Uff, cheppalle");
                        }

                    moreLeavingCursor.close();

                    String tramToVenice = "SELECT t.trip_id,\n" +
                            "       start_s.stop_name as departure_stop,\n" +
                            "\t   start_s.stop_id as departure_stop_id,\n" +
                            "       start_st.departure_time as departure_time,\n" +
                            "       direction_id as direction,\n" +
                            "       end_s.stop_name as arrival_stop,\n" +
                            "\t   end_s.stop_id as arrival_stop_id,\n" +
                            "       end_st.arrival_time as arrival_time,\n" +
                            "       r.route_short_name as route_short_name,\n" +
                            "       r.route_long_name as route_long_name\n" +
                            "FROM\n" +
                            "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                            "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                            "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                            "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                            "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                            "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                            "WHERE " + dayForThisQuery + " = 1\n" +
                            "  and r.route_id in (" + routeForT1MestreVe + ", " + routeFor12MestreVe + ")\n" +      //For ordinary buses
                            "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                            "  and departure_stop_id = " + departingSansovino + "\n" +
                            "  and end_s.stop_id in (" + veniceStops + ")\n" +
                            "  UNION " +
                            "SELECT t.trip_id,\n" +
                            "       start_s.stop_name as departure_stop,\n" +
                            "\t   start_s.stop_id as departure_stop_id,\n" +
                            "       start_st.departure_time as departure_time,\n" +
                            "       direction_id as direction,\n" +
                            "       end_s.stop_name as arrival_stop,\n" +
                            "\t   end_s.stop_id as arrival_stop_id,\n" +
                            "       end_st.arrival_time as arrival_time,\n" +
                            "       r.route_short_name as route_short_name,\n" +
                            "       r.route_long_name as route_long_name\n" +
                            "FROM\n" +
                            "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                            "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                            "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                            "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                            "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                            "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                            "WHERE " + dayForThisQuery + " = 1\n" +
                            "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                            "  and departure_stop_id in (" + sansovinoForN + ")\n" + //For night buses
                            "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                            "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" +   //Needed only because N1 and N2 are circular, so you could get paradoxical results
                            "  and end_s.stop_id in (" + veniceStops + ")\n" +
                            "  and end_st.late_night IS NULL\n" +
                            "order by start_st.departure_time asc";

                    if(BuildConfig.DEBUG)
                        Log.w("TramToVenice", tramToVenice);

                    Cursor leavingCursor = db.rawQuery(tramToVenice, null);
                    leavingCursor.moveToFirst();
                    if (leavingCursor.getCount() > 0)
                        try {
                            do {
                                String departureStop = leavingCursor.getString(leavingCursor.getColumnIndex("departure_stop"));
                                String departureTime = leavingCursor.getString(leavingCursor.getColumnIndex("departure_time"));
                                String arrivalStop = leavingCursor.getString(leavingCursor.getColumnIndex("arrival_stop"));
                                String arrivalTime = leavingCursor.getString(leavingCursor.getColumnIndex("arrival_time"));
                                String line = leavingCursor.getString(leavingCursor.getColumnIndex("route_short_name"));

                                tramToVeniceTimes.add(new Bus(dateFormatter.parse(departureTime),
                                        line,
                                        departureStop,
                                        arrivalStop,
                                        dateFormatter.parse(arrivalTime)
                                ));
                                //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                            } while (leavingCursor.moveToNext());
                        } catch (ParseException e) {
                            Log.e("Catchit", "Uff, cheppalle");
                        }

                    leavingCursor.close();

                    String moreTramFromVenice =
                            "SELECT t.trip_id,\n" +
                                    "       start_s.stop_name as departure_stop,\n" +
                                    "\t   start_s.stop_id as departure_stop_id,\n" +
                                    "       start_st.departure_time as departure_time,\n" +
                                    "       direction_id as direction,\n" +
                                    "       end_s.stop_name as arrival_stop,\n" +
                                    "\t   end_s.stop_id as arrival_stop_id,\n" +
                                    "       end_st.arrival_time as arrival_time,\n" +
                                    "       r.route_short_name as route_short_name,\n" +
                                    "       end_st.late_night as bus_late_night,\n" +
                                    "       r.route_long_name as route_long_name\n" +
                                    "FROM\n" +
                                    "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                                    "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                                    "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                                    "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                                    "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                                    "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                                    "WHERE " + dayForThisQuery + " = 1\n" +
                                    "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                                    "  and departure_stop_id in (" + veniceStops + ")\n" + //For night buses
                                    "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                                    "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" +   //Needed only because N1 and N2 are circular, so you could get paradoxical results
                                    "  and end_s.stop_id in (" + sansovinoForN + ")\n" +
                                    "  and bus_late_night IS NOT NULL\n" +
                                    "order by start_st.departure_time asc";

//                    if(BuildConfig.DEBUG)
//                        Log.w("moreTramFromVenice", moreTramFromVenice);

                    Cursor moreComingCursor = db.rawQuery(moreTramFromVenice, null);
                    moreComingCursor.moveToFirst();
                    if (moreComingCursor.getCount() > 0)
                        try {
                            do {
                                String departureStop = moreComingCursor.getString(moreComingCursor.getColumnIndex("departure_stop"));
                                String departureTime = moreComingCursor.getString(moreComingCursor.getColumnIndex("departure_time"));
                                String arrivalStop = moreComingCursor.getString(moreComingCursor.getColumnIndex("arrival_stop"));
                                String arrivalTime = moreComingCursor.getString(moreComingCursor.getColumnIndex("arrival_time"));
                                String line = moreComingCursor.getString(moreComingCursor.getColumnIndex("route_short_name"));

                                tramToMestreTimes.add(new Bus(dateFormatter.parse(departureTime),
                                        line,
                                        departureStop,
                                        arrivalStop,
                                        dateFormatter.parse(arrivalTime)
                                ));
                                //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                            } while (moreComingCursor.moveToNext());
                        } catch (ParseException e) {
                            Log.e("Catchit", "Uff, cheppalle");
                        }

                    moreComingCursor.close();

                    String tramFromVenice = "SELECT t.trip_id,\n" +
                        "       start_s.stop_name as departure_stop,\n" +
                        "       start_s.stop_id as departure_stop_id,\n" +
                        "       start_st.departure_time as departure_time,\n" +
                        "       direction_id as direction,\n" +
                        "       end_s.stop_name as arrival_stop,\n" +
                        "       end_s.stop_id as arrival_stop_id,\n" +
                        "       end_st.arrival_time as arrival_time,\n" +
                        "       r.route_short_name as short_name,\n" +
                        "       r.route_long_name as long_name\n" +
                        "FROM\n" +
                        "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                        "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                        "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                        "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                        "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                        "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                        "WHERE " + dayForThisQuery + " = 1\n" +
                        "  and r.route_id in (" + routeForT1VeMestre + ", " + routeFor12VeMestre + ")\n" +      //For ordinary buses
                        "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                        "  and departure_stop_id in (" + veniceStops + ")\n" +
                        "  and end_s.stop_id = " + returningSansovino + "\n" +
                        " UNION " +
                        " SELECT t.trip_id, \n" +
                            "       start_s.stop_name as departure_stop,\n" +
                            "       start_s.stop_id as departure_stop_id,\n" +
                            "       start_st.departure_time as departure_time,\n" +
                            "       direction_id as direction,\n" +
                            "       end_s.stop_name as arrival_stop,\n" +
                            "       end_s.stop_id as arrival_stop_id,\n" +
                            "       end_st.arrival_time as arrival_time,\n" +
                            "       r.route_short_name as short_name,\n" +
                            "       r.route_long_name as long_name\n" +
                            "FROM\n" +
                            "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                            "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                            "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                            "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                            "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                            "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                        "WHERE " + dayForThisQuery + " = 1\n" +
                        "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +   //For night buses
                        "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                        "  and departure_stop_id in (" + veniceStops + ")\n" +
                        "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" +   //Needed only because N1 and N2 are circular, so you could get paradoxical results
                        "  and end_s.stop_id in (" + sansovinoForN + ")\n" +
                        "  and end_st.late_night IS NULL\n" +
                        "order by start_st.departure_time asc";

//                    if(BuildConfig.DEBUG)
//                        Log.w("TramFromVenice", tramFromVenice);

                Cursor comingCursor = db.rawQuery(tramFromVenice, null);
                comingCursor.moveToFirst();
                if(comingCursor.getCount() > 0)
                    try {
                        do {
                            String departureStop = comingCursor.getString(comingCursor.getColumnIndex("departure_stop"));
                            String departureTime = comingCursor.getString(comingCursor.getColumnIndex("departure_time"));
                            String arrivalTime = comingCursor.getString(comingCursor.getColumnIndex("arrival_time"));
                            String arrivalStop = comingCursor.getString(comingCursor.getColumnIndex("arrival_stop"));
                            String line = comingCursor.getString(comingCursor.getColumnIndex("short_name"));

                            tramToMestreTimes.add(new Bus(dateFormatter.parse(departureTime),
                                    line,
                                    departureStop,
                                    arrivalStop,
                                    dateFormatter.parse(arrivalTime)));
                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                        } while (comingCursor.moveToNext());
                    } catch (ParseException e) {
                        Log.e("Catchit", "Uff, cheppalle");
                    }

                comingCursor.close();

                //Tram Mestre Centro -> Stazione
                String tramToStation = "SELECT t.trip_id,\n" +
                        "       start_s.stop_name as departure_stop,\n" +
                        "\t   start_s.stop_id as departure_stop_id,\n" +
                        "       start_st.departure_time,\n" +
                        "       direction_id as direction,\n" +
                        "       end_s.stop_name as arrival_stop,\n" +
                        "\t   end_s.stop_id as arrival_stop_id,\n" +
                        "       end_st.arrival_time,\n" +
                        "       r.route_short_name\n" +
                        "FROM\n" +
                        "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                        "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                        "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                        "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                        "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                        "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                        "WHERE " + dayForThisQuery + " = 1\n" +
                        "  and r.route_id in (" + routeForT2MestreMa + ")\n" +
                        "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                        "  and arrival_stop_id in (" + stazioneMestre + ")\n" +
                        "  and departure_stop_id in (" + cialdini + ")\n" +
                        "order by start_st.departure_time asc\t";

//                if(BuildConfig.DEBUG)
//                    Log.w("TramToStation", tramToStation);

                Cursor leavingTramCursor = db.rawQuery(tramToStation, null);
                leavingTramCursor.moveToFirst();
                if(leavingTramCursor.getCount() > 0)
                    try {
                        do {
                            String departureStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_stop"));
                            String departureTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_time"));
                            String arrivalTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_time"));
                            String arrivalStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_stop"));
                            String line = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("route_short_name"));

                            tramToStationTimes.add(new Bus(dateFormatter.parse(departureTime),
                                    line,
                                    departureStop,
                                    arrivalStop,
                                    dateFormatter.parse(arrivalTime)
                                    ));

                        } while (leavingTramCursor.moveToNext());
                    } catch (ParseException e) {
                        Log.e("Catchit", "Uff, cheppalle");
                    }

                leavingTramCursor.close();

                String tramFromStation = "SELECT t.trip_id,\n" +
                        "       start_s.stop_name as departure_stop,\n" +
                        "\t   start_s.stop_id as departure_stop_id,\n" +
                        "       start_st.departure_time,\n" +
                        "       direction_id as direction,\n" +
                        "       end_s.stop_name as arrival_stop,\n" +
                        "\t   end_s.stop_id as arrival_stop_id,\n" +
                        "       end_st.arrival_time,\n" +
                        "       r.route_short_name\n" +
                        "FROM\n" +
                        "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                        "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                        "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                        "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                        "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                        "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                        "WHERE " + dayForThisQuery + " = 1\n" +
                        "  and r.route_id in (" + routeForT2MaMestre + ")\n" +
                        "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                        "  and departure_stop_id in (" + stazioneMestre + ")\n" +
                        "  and arrival_stop_id in (" + cialdini + ")\n" +
                        "order by start_st.departure_time asc\t";

//                if(BuildConfig.DEBUG)
//                    Log.w("tramFromStation", tramFromStation);

                Cursor comingTramCursor = db.rawQuery(tramFromStation, null);
                comingTramCursor.moveToFirst();
                if(comingTramCursor.getCount() > 0)
                    try {
                        do {
                            String departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"));
                            String departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"));
                            String arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"));
                            String arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"));
                            String line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"));

                            tramToMestreCityCenterTimes.add(new Bus(dateFormatter.parse(departureTime),
                                    line,
                                    departureStop,
                                    arrivalStop,
                                    dateFormatter.parse(arrivalTime)
                            ));

                        } while (comingTramCursor.moveToNext());
                    } catch (ParseException e) {
                        Log.e("Catchit", "Uff, cheppalle");
                    }

                comingTramCursor.close();

                    String tramSansovinoToCentro = "SELECT t.trip_id,\n" +
                            "       start_s.stop_name as departure_stop,\n" +
                            "\t   start_s.stop_id as departure_stop_id,\n" +
                            "       start_st.departure_time as departure_time,\n" +
                            "       direction_id as direction,\n" +
                            "       end_s.stop_name as arrival_stop,\n" +
                            "\t   end_s.stop_id as arrival_stop_id,\n" +
                            "       end_st.arrival_time as arrival_time,\n" +
                            "       r.route_short_name as route_short_name,\n" +
                            "       r.route_long_name as route_long_name\n" +
                            "FROM\n" +
                            "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                            "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                            "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                            "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                            "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                            "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                            "WHERE " + dayForThisQuery + " = 1\n" +
                            "  and r.route_id in (" + routeForT1MestreVe + ", " + routeFor12MestreVe + ")\n" +      //For ordinary buses
                            "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                            "  and departure_stop_id = " + returningSansovino + "\n" +
                            "  and end_s.stop_id in (" + cialdini + ")\n" +
                            "  UNION " +
                            "SELECT t.trip_id,\n" +
                            "       start_s.stop_name as departure_stop,\n" +
                            "\t   start_s.stop_id as departure_stop_id,\n" +
                            "       start_st.departure_time as departure_time,\n" +
                            "       direction_id as direction,\n" +
                            "       end_s.stop_name as arrival_stop,\n" +
                            "\t   end_s.stop_id as arrival_stop_id,\n" +
                            "       end_st.arrival_time as arrival_time,\n" +
                            "       r.route_short_name as route_short_name,\n" +
                            "       r.route_long_name as route_long_name\n" +
                            "FROM\n" +
                            "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                            "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                            "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                            "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                            "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                            "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                            "WHERE " + dayForThisQuery + " = 1\n" +
                            "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                            "  and departure_stop_id in (" + sansovinoForN + ")\n" + //For night buses
                            "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                            "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" +   //Needed only because N1 and N2 are circular, so you could get paradoxical results
                            "  and end_s.stop_id in (" + cialdini + ")\n" +
                            "order by start_st.departure_time asc";

//                    if(BuildConfig.DEBUG)
//                        Log.w("TramToVenice", tramSansovinoToCentro);

                    Cursor leavingToCentroCursor = db.rawQuery(tramSansovinoToCentro, null);
                    leavingToCentroCursor.moveToFirst();
                    if (leavingToCentroCursor.getCount() > 0)
                        try {
                            do {
                                String departureStop = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("departure_stop"));
                                String departureTime = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("departure_time"));
                                String arrivalStop = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("arrival_stop"));
                                String arrivalTime = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("arrival_time"));
                                String line = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("route_short_name"));

                                tramSansovinoCentroTimes.add(new Bus(dateFormatter.parse(departureTime),
                                        line,
                                        departureStop,
                                        arrivalStop,
                                        dateFormatter.parse(arrivalTime)
                                ));
                                //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                            } while (leavingToCentroCursor.moveToNext());
                        } catch (ParseException e) {
                            Log.e("Catchit", "Uff, cheppalle");
                        }

                    leavingToCentroCursor.close();

//                    if(BuildConfig.DEBUG)
//                        Log.w("TramSansovinoToCentro", tramSansovinoToCentro);

                }
                db.close();
            }
        });

        busPopulateThread.run();
    }

    public List<Bus> getTramToVeniceTimes() {
        return tramToVeniceTimes;
    }

    public List<Bus> getTramToMestreTimes() {
        return tramToMestreTimes;
    }

    public List<Bus> getTramToStationTimes() {
        return tramToStationTimes;
    }

    public List<Bus> getTramToMestreCityCenterTimes() {
        return tramToMestreCityCenterTimes;
    }

    public List<Bus> getTramCentroSansovinoTimes() {
        return tramCentroSansovinoTimes;
    }

    public List<Bus> getTramSansovinoCentroTimes() {
        return tramSansovinoCentroTimes;
    }

    /*
     * Sorts lists starting with NOW
     */
    private void sortAndFilterThoseLists(int weekDay) throws ParseException {
        List<Bus> passed = new LinkedList<Bus>();
        List<Bus> toPass = new LinkedList<Bus>();

        Bus saveItToBePutLast = null;

        Date now = dateFormatter.parse(dateFormatter.format(new Date()));

        /**
         * DEBUG: Set specific departureTime, if needed
         */
        if (DEBUGDAY) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(now); //This date is a copy of present datetime (which actually is Linux Epoch)
            cal.set(Calendar.HOUR_OF_DAY, DEBUG_HOURS); //We just change the hours, minutes, seconds
            cal.set(Calendar.MINUTE, DEBUG_MINUTES);
            cal.set(Calendar.SECOND, DEBUG_SECONDS);
            cal.set(Calendar.MILLISECOND, DEBUG_MILLISECONDS);
            Date wasNow = (Date) now.clone();
            now.setTime(cal.getTimeInMillis());
        }
        /**
         * End Debug Block
         */

        int sevenMinutes = 1000 * 60 * 7;

        for (int i = 0; i < tramToVeniceTimes.size(); i++) {
            if (tramToVeniceTimes.get(i).getDeparture().getTime() > (now.getTime() - sevenMinutes)) {
                toPass.add(tramToVeniceTimes.get(i));
                //If the bus has passed in last five minutes it should be grey
                if (tramToVeniceTimes.get(i).getDeparture().getTime() < now.getTime()) {
                    //					toPass.get(toPass.size()-1).setColor(50, 131, 139, 131);
                    toPass.get(toPass.size() - 1).setColor(200, 205, 201, 201);
                    toPass.get(toPass.size() - 1).setTextColor(255, 119, 136, 153);
                    toPass.get(toPass.size() - 1).setToBePutLast(true);    //Questo bus al prossimo giro
                    //dovrà finire in fondo alla lista
                } else {
                    toPass.get(toPass.size() - 1).resetColors();
                    toPass.get(toPass.size() - 1).setToBePutLast(false);    //Non deve essere in fondo alla lista
                }
            } else {
                if (tramToVeniceTimes.get(i).getToBePutLast()) {
                    saveItToBePutLast = tramToVeniceTimes.get(i);            //Salviamolo per metterlo in fondo alla lista stavolta
                    tramToVeniceTimes.get(i).setToBePutLast(false);            //D'ora in poi non deve più essere in fondo alla lista
                } else {
                    passed.add(tramToVeniceTimes.get(i));
                    passed.get(passed.size() - 1).resetColors();
                }
            }
        }

        tramToVeniceTimes = new LinkedList<Bus>();
        tramToVeniceTimes.addAll(toPass);
        tramToVeniceTimes.addAll(passed);
        if (saveItToBePutLast != null)    //Se c'è un bus salvato per essere messo in fondo
        {
            saveItToBePutLast.resetColors();
            tramToVeniceTimes.add(saveItToBePutLast);
            saveItToBePutLast = null;
        }

        toPass = new LinkedList<Bus>();
        passed = new LinkedList<Bus>();

        for (int i = 0; i < tramToMestreTimes.size(); i++) {
            if (tramToMestreTimes.get(i).getDeparture().getTime() > (now.getTime() - sevenMinutes)) {
                toPass.add(tramToMestreTimes.get(i));
                //If the bus has passed in last five minutes it should be grey
                if (tramToMestreTimes.get(i).getDeparture().getTime() < now.getTime()) {
                    //					toPass.get(toPass.size()-1).setColor(50, 131, 139, 131);
                    toPass.get(toPass.size() - 1).setColor(200, 205, 201, 201);
                    toPass.get(toPass.size() - 1).setTextColor(255, 119, 136, 153);
                    toPass.get(toPass.size() - 1).setToBePutLast(true);    //Questo bus al prossimo giro
                    //dovrà finire in fondo alla lista
                } else {
                    toPass.get(toPass.size() - 1).resetColors();
                    toPass.get(toPass.size() - 1).setToBePutLast(false);    //Non deve essere in fondo alla lista
                }
            } else {
                if (tramToMestreTimes.get(i).getToBePutLast()) {
                    saveItToBePutLast = tramToMestreTimes.get(i);            //Salviamolo per metterlo in fondo alla lista stavolta
                    tramToMestreTimes.get(i).setToBePutLast(false);            //D'ora in poi non deve più essere in fondo alla lista
                } else {
                    passed.add(tramToMestreTimes.get(i));
                    passed.get(passed.size() - 1).resetColors();
                }
            }
        }

        tramToMestreTimes = new LinkedList<Bus>();
        tramToMestreTimes.addAll(toPass);
        tramToMestreTimes.addAll(passed);
        if (saveItToBePutLast != null)    //Se c'è un bus salvato per essere messo in fondo
        {
            saveItToBePutLast.resetColors();
            tramToMestreTimes.add(saveItToBePutLast);
            saveItToBePutLast = null;
        }


		/*
         * TRAM
		 */
        toPass = new LinkedList<Bus>();
        passed = new LinkedList<Bus>();

        for (int i = 0; i < tramToStationTimes.size(); i++) {
            if (tramToStationTimes.get(i).getDeparture().getTime() > (now.getTime())) { //No "sevenminutes", because tram is always on departureTime
                toPass.add(tramToStationTimes.get(i));
                //If the bus has passed in last five minutes it should be grey
                if (tramToStationTimes.get(i).getDeparture().getTime() < now.getTime()) {
                    //					toPass.get(toPass.size()-1).setColor(50, 131, 139, 131);
                    toPass.get(toPass.size() - 1).setColor(200, 205, 201, 201);
                    toPass.get(toPass.size() - 1).setTextColor(255, 119, 136, 153);
                    toPass.get(toPass.size() - 1).setToBePutLast(true);    //Questo bus al prossimo giro
                    //dovrà finire in fondo alla lista
                } else {
                    toPass.get(toPass.size() - 1).resetColors();
                    toPass.get(toPass.size() - 1).setToBePutLast(false);    //Non deve essere in fondo alla lista
                }
            } else {
                if (tramToStationTimes.get(i).getToBePutLast()) {
                    saveItToBePutLast = tramToStationTimes.get(i);            //Salviamolo per metterlo in fondo alla lista stavolta
                    tramToStationTimes.get(i).setToBePutLast(false);            //D'ora in poi non deve più essere in fondo alla lista
                } else {
                    passed.add(tramToStationTimes.get(i));
                    passed.get(passed.size() - 1).resetColors();
                }
            }
        }

        tramToStationTimes = new LinkedList<Bus>();
        tramToStationTimes.addAll(toPass);
        tramToStationTimes.addAll(passed);
        if (saveItToBePutLast != null)    //Se c'è un bus salvato per essere messo in fondo
        {
            saveItToBePutLast.resetColors();
            tramToStationTimes.add(saveItToBePutLast);
            saveItToBePutLast = null;
        }

        toPass = new LinkedList<Bus>();
        passed = new LinkedList<Bus>();

        for (int i = 0; i < tramToMestreCityCenterTimes.size(); i++) {
            if (tramToMestreCityCenterTimes.get(i).getDeparture().getTime() > (now.getTime())) { //No "sevenminutes", because tram is always on departureTime
                toPass.add(tramToMestreCityCenterTimes.get(i));
                //If the bus has passed in last seven minutes it should be grey
                if (tramToMestreCityCenterTimes.get(i).getDeparture().getTime() < now.getTime()) {
                    //					toPass.get(toPass.size()-1).setColor(50, 131, 139, 131);
                    toPass.get(toPass.size() - 1).setColor(200, 205, 201, 201);
                    toPass.get(toPass.size() - 1).setTextColor(255, 119, 136, 153);
                    toPass.get(toPass.size() - 1).setToBePutLast(true);    //Questo bus al prossimo giro
                    //dovrà finire in fondo alla lista
                } else {
                    toPass.get(toPass.size() - 1).resetColors();
                    toPass.get(toPass.size() - 1).setToBePutLast(false);    //Non deve essere in fondo alla lista
                }
            } else {
                if (tramToMestreCityCenterTimes.get(i).getToBePutLast()) {
                    saveItToBePutLast = tramToMestreCityCenterTimes.get(i);            //Salviamolo per metterlo in fondo alla lista stavolta
                    tramToMestreCityCenterTimes.get(i).setToBePutLast(false);            //D'ora in poi non deve più essere in fondo alla lista
                } else {
                    passed.add(tramToMestreCityCenterTimes.get(i));
                    passed.get(passed.size() - 1).resetColors();
                }
            }
        }

        tramToMestreCityCenterTimes = new LinkedList<Bus>();
        tramToMestreCityCenterTimes.addAll(toPass);
        tramToMestreCityCenterTimes.addAll(passed);
        if (saveItToBePutLast != null)    //Se c'è un bus salvato per essere messo in fondo
        {
            saveItToBePutLast.resetColors();
            tramToMestreCityCenterTimes.add(saveItToBePutLast);
            saveItToBePutLast = null;
        }
    }

    /** We don't need it anymore since we get information from the database **/
//    private boolean busIsGoodForToday(Bus thisBus, int weekDay) {
////		Log.i("CatchIt", "Il bus passa di sabato? " + thisBus.doesItPassOnSaturdays() + " e oggi è il " + weekDay);
//
//        switch (weekDay) {
//            case 1:        //sunday
//                if (thisBus.doesItPassOnSundays())    //Al momento non ho ancora inserito
//                    //gli autobus della domenica
//                    return true;
//                break;
//            case 2:        //workdays
//            case 3:
//            case 4:
//            case 5:
//            case 6:
//                if (thisBus.doesItPassOnWorkdays())
//                    return true;
//                break;
//            case 7:        //saturday
//                if (thisBus.doesItPassOnSaturdays())
//                    return true;
//                break;
//        }
//
//        return false;
//    }

    /*
*  Convenience method to add a specified number of minutes to a Date object
*  From: http://stackoverflow.com/questions/9043981/how-to-add-minutes-to-my-date
*  @param  minutes  The number of minutes to subtract
*  @param  beforeTime  The time that will have minutes subtracted from it
*  @return  A date object with the specified number of minutes added to it
*/
    private static Date subtractMinutesFromDate(int minutes, Date beforeTime){
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs - (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }

}