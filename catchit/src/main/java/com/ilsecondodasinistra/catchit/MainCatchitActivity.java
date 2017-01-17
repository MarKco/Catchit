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

import static com.ilsecondodasinistra.catchit.DatabaseHelper.subtractMinutesFromDate;

public class MainCatchitActivity extends AppCompatActivity {

    public static final String POSITION = "Position";

    Toolbar toolbar;

    List<Bus> tramToVeniceTimes = new LinkedList<>();
    List<Bus> tramToMestreTimes = new LinkedList<>();

    List<Bus> tramToStationTimes = new LinkedList<>();
    List<Bus> tramToMestreCityCenterTimes = new LinkedList<>();

    List<Bus> tramCentroSansovinoTimes = new LinkedList<>();
    List<Bus> tramSansovinoCentroTimes = new LinkedList<>();

    public static final boolean DEBUGHOUR = false; //if true, custom departureTime below is set in the app,
    public static final boolean DEBUGDAY = false; //if true, custom date below is set in the app,
    // regardless of real timestamp
    public static final int DEBUG_HOURS = 13;
    public static final int DEBUG_MINUTES = 33;
    public static final int DEBUG_SECONDS = 0;
    public static final int DEBUG_MILLISECONDS = 0;

    TextView titleText;
    int todayWeek;
    int debugDayOfTheWeek = Calendar.SUNDAY;

    String dayForQuery;
    String tomorrowForQuery;
    final Date now = new Date();

    List<Fragment> fList = new ArrayList<Fragment>();

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

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

        if (DEBUGDAY)
            todayWeek = debugDayOfTheWeek;

        //add the Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        titleText = (TextView) findViewById(R.id.title);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), getFragments());
        mPager.setAdapter(mPagerAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();

//        tramToVeniceTimes = new LinkedList<>();
//        tramToMestreTimes = new LinkedList<>();
//
//        tramToStationTimes = new LinkedList<>();
//        tramToMestreCityCenterTimes = new LinkedList<>();
//
//        tramCentroSansovinoTimes = new LinkedList<>();
//        tramSansovinoCentroTimes = new LinkedList<>();

        if (DEBUGHOUR) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(now); //This date is a copy of present datetime (which actually is Linux Epoch)
            cal.set(Calendar.HOUR_OF_DAY, DEBUG_HOURS); //We just change the hours, minutes, seconds
            cal.set(Calendar.MINUTE, DEBUG_MINUTES);
            cal.set(Calendar.SECOND, DEBUG_SECONDS);
            cal.set(Calendar.MILLISECOND, DEBUG_MILLISECONDS);
            now.setTime(cal.getTimeInMillis());
        }
        if (DEBUGDAY)
            now.setDate(debugDayOfTheWeek);

        selectRightDayOfTheWeek();

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
                try {
                    switch (position) {
                        case 0:
                            titleText.setText("Mestre -> Venezia");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramToVeniceTimes == null || tramToVeniceTimes.size() == 0) {
                                            tramToVeniceTimes = DatabaseHelper.getMoreTramToVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now);
                                            tramToVeniceTimes.addAll(DatabaseHelper.getTramToVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                                        }

                                        ((MainFragment) fList.get(position)).populate(tramToVeniceTimes);
                                        prefetchTimes(position + 1);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
                            break;
                        case 1:
                            titleText.setText("Venezia -> Mestre");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramToMestreTimes == null || tramToMestreTimes.size() == 0 ) {
                                            tramToMestreTimes.addAll(DatabaseHelper.getMoreTramFromVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                                            tramToMestreTimes.addAll(DatabaseHelper.getTramFromVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                                        }

                                        ((MainFragment) fList.get(position)).populate(tramToMestreTimes);
                                        prefetchTimes(position + 1);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
                            break;
                        case 2:
                            titleText.setText("Tram Centro -> Stazione");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramToStationTimes == null || tramToStationTimes.size() == 0)
                                            tramToStationTimes.addAll(DatabaseHelper.getTramToStation(getApplicationContext(), dayForQuery, tomorrowForQuery, now));

                                        ((MainFragment) fList.get(position)).populate(tramToStationTimes);
                                        prefetchTimes(position + 1);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
                            break;
                        case 3:
                            titleText.setText("Tram Stazione -> Centro");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramToMestreCityCenterTimes == null || tramToMestreCityCenterTimes.size() == 0)
                                            tramToMestreCityCenterTimes.addAll(DatabaseHelper.getStationToSansovino(getApplicationContext(), dayForQuery, tomorrowForQuery, now));

                                        ((MainFragment) fList.get(position)).populate(tramToMestreCityCenterTimes);
                                        prefetchTimes(position + 1);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
                            break;
                        case 4:
                            titleText.setText("Tram Centro -> Sansovino");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramCentroSansovinoTimes == null || tramCentroSansovinoTimes.size() == 0)
                                            tramCentroSansovinoTimes.addAll(DatabaseHelper.getStationToSansovino(getApplicationContext(), dayForQuery, tomorrowForQuery, now));

                                        ((MainFragment) fList.get(position)).populate(tramCentroSansovinoTimes);
                                        prefetchTimes(position + 1);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
                            break;
                        case 5:
                            titleText.setText("Tram Sansovino -> Centro");
                            if(!((MainFragment)fList.get(position)).isPopulated()) {
                                Thread busPopulateThread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if(tramSansovinoCentroTimes == null || tramSansovinoCentroTimes.size() == 0)
                                            tramSansovinoCentroTimes.addAll(DatabaseHelper.getSansovinoToStation(getApplicationContext(), dayForQuery, tomorrowForQuery, now));

                                        ((MainFragment) fList.get(position)).populate(tramSansovinoCentroTimes);
                                    }
                                });
                                busPopulateThread.run();
                            }
                            else
                                ((MainFragment)fList.get(position)).show();
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

//        populateTimeTable();

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

    /**
     * Pre-loads data for next Fragment
     * @param position
     */
    private void prefetchTimes(int position) {
        switch(position) {
            case 0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramToVeniceTimes = DatabaseHelper.getMoreTramToVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now);
                        tramToVeniceTimes.addAll(DatabaseHelper.getTramToVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
            case 1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramToMestreTimes.addAll(DatabaseHelper.getMoreTramFromVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                        tramToMestreTimes.addAll(DatabaseHelper.getTramFromVenice(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
            case 2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramToStationTimes.addAll(DatabaseHelper.getTramToStation(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
            case 3:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramToMestreCityCenterTimes.addAll(DatabaseHelper.getStationToSansovino(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
            case 4:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramCentroSansovinoTimes.addAll(DatabaseHelper.getStationToSansovino(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
            case 5:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        tramSansovinoCentroTimes.addAll(DatabaseHelper.getSansovinoToStation(getApplicationContext(), dayForQuery, tomorrowForQuery, now));
                    }
                }).run();
                break;
        }
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

        if(fList == null || fList.size() == 0) {
            for (int position = 1; position < 7; position++) {
                Bundle args = new Bundle();
                MainFragment thisFragment = new MainFragment();
                args.putInt(POSITION, position);
                thisFragment.setArguments(args);
                fList.add(thisFragment);
            }
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
     *
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

    private void selectRightDayOfTheWeek() {
        switch (todayWeek) {
            case Calendar.SUNDAY:
                dayForQuery = "c.sunday";
                tomorrowForQuery = "c.monday";
                break;
            case Calendar.MONDAY:
                dayForQuery = "c.monday";
                tomorrowForQuery = "c.wednesday";
                break;
            case Calendar.TUESDAY:
                dayForQuery = "c.tuesday";
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

}