package com.ilsecondodasinistra.catchit;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

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

    List<Bus> busHermadaToStationTimes = new LinkedList<>();
    List<Bus> busStationToHermadaTimes = new LinkedList<>();

    List<Bus> busAirportToHermada = new LinkedList<>();
    List<Bus> busHermadaToAirport = new LinkedList<>();

    public static final boolean DEBUGHOUR = false; //if true, custom departureTime below is set in the app,
    public static final boolean DEBUGDAY = false; //if true, custom date below is set in the app,

    // regardless of real timestamp
    public static final int DEBUG_HOURS = 23;
    public static final int DEBUG_MINUTES = 55;
    public static final int DEBUG_SECONDS = 0;
    public static final int DEBUG_MILLISECONDS = 0;

    TextView titleText;
    int todayWeek;
    int debugDayOfTheWeek = Calendar.SUNDAY;

    String dayForQuery;
    String tomorrowForQuery;
    String yesterdayForQuery;
    Date now = new Date();

    List<Fragment> fList = new ArrayList<Fragment>();

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private ViewPager mPager;

    /**
     * Navigation drawer
     */

    //First We Declare Titles And Icons For Our Navigation mDrawerLayout List View
    //This Icons And Titles Are holded in an Array as you can see

    int ICONS[] = {
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_subway_black_24dp,
            R.drawable.ic_directions_bus_black_24dp,
            R.drawable.ic_directions_bus_black_24dp,
            R.drawable.ic_directions_bus_black_24dp,
            R.drawable.ic_directions_bus_black_24dp};

    RecyclerView mRecyclerView;                           // Declaring RecyclerView
    RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    DrawerLayout mDrawerLayout;                                  // Declaring DrawerLayout

    //Similarly we Create a String Resource for the name and email in the header view
    //And we also create a int resource for profile picture in the header view

    String NAME = "Catchit";
    String EMAIL = "Non perdere la pazienza per prendere il tram";
    int PROFILE = R.drawable.ic_launcher;

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private PagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        //add the Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /* Actions for Navigation mDrawerLayout */
        String TITLES[]= {
            getString(R.string.mestre_venezia),
                    getString(R.string.venezia_mestre),
                    getString(R.string.centro_stazione),
                    getString(R.string.stazione_centro),
                    getString(R.string.centro_casa),
                    getString(R.string.casa_centro),
                    getString(R.string.casa_stazione),
                    getString(R.string.stazione_casa),
                    getString(R.string.casa_aeroporto),
                    getString(R.string.aeroporto_casa)};

        mRecyclerView = (RecyclerView) findViewById(R.id.navigationDrawer); // Assigning the RecyclerView Object to the xml View
        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size

        mAdapter = new NavigationAdapter(TITLES,ICONS,NAME,EMAIL,PROFILE);       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)
        // And passing the titles,icons,header view name, header view email,
        // and header view profile picture

        mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView
        mLayoutManager = new LinearLayoutManager(this);                 // Creating a layout Manager
        mRecyclerView.setLayoutManager(mLayoutManager);                 // Setting the layout Manager

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);        // mDrawerLayout object Assigned to the view

        Calendar calendar = Calendar.getInstance();
        todayWeek = calendar.get(Calendar.DAY_OF_WEEK);

        if (DEBUGDAY)
            todayWeek = debugDayOfTheWeek;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        if (Build.VERSION.SDK_INT >= 18) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);    //  Required API level 18 :(
        }
        titleText = (TextView) findViewById(R.id.title);

        final GestureDetector mGestureDetector = new GestureDetector(MainCatchitActivity.this, new GestureDetector.SimpleOnGestureListener() {

            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(),motionEvent.getY());

                int position;
                if(child!=null && mGestureDetector.onTouchEvent(motionEvent)){
                    mDrawerLayout.closeDrawers();
//                    Toast.makeText(MainCatchitActivity.this,"The Item Clicked is: "+recyclerView.getChildPosition(child),Toast.LENGTH_SHORT).show();
                    position = recyclerView.getChildPosition(child);
                    if(position > 0)
                        mPager.setCurrentItem(position - 1);
                    return true;
                }

                return false;
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

            }
        });


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), getFragments());
        mPager.setAdapter(mPagerAdapter);

        /* Actions for Navigation mDrawerLayout */
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
    }

    @Override
    protected void onStart() {
        super.onStart();

        reloadBusAndTrams();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == android.R.id.home) {
            if(mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
            }else{
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }

        }else if (item.getItemId()== R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void refreshDateTime() {
        now = new Date();

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

    }

    private void reloadBusAndTrams() {

        refreshDateTime();

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(final int position, float positionOffset, int positionOffsetPixels) {
                reloadSingleFragmentData(position, false);  //We don't want to force reload, of course
                switch(position) {
                    case 0:
                        titleText.setText(getString(R.string.mestre_venezia));
                        break;
                    case 1:
                        titleText.setText(getString(R.string.venezia_mestre));
                        break;
                    case 2:
                        titleText.setText(getString(R.string.centro_stazione));
                        break;
                    case 3:
                        titleText.setText(getString(R.string.stazione_centro));
                        break;
                    case 4:
                        titleText.setText(getString(R.string.centro_casa));
                        break;
                    case 5:
                        titleText.setText(getString(R.string.casa_centro));
                        break;
                    case 6:
                        titleText.setText(getString(R.string.casa_stazione));
                        break;
                    case 7:
                        titleText.setText(getString(R.string.stazione_casa));
                        break;
                    case 8:
                        titleText.setText(getString(R.string.casa_aeroporto));
                        break;
                    case 9:
                        titleText.setText(getString(R.string.aeroporto_casa));
                        break;
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

    /**
     * This gets called by the fragment when the user swipes down to refresh
     * @param fragment
     * TODO: Use an interface and a callback, calling this method directly is shit!
     */
    public void reloadSingleFragmentDataFromFragment(MainFragment fragment) {
        refreshDateTime();  //We update the "now" variable
        final int position = fList.indexOf(fragment);
        if(fList.contains(fragment)) {
            reloadSingleFragmentData(position, true);    //This is called when the user swipes down the list for refreshing, so we force refresh!
        }

        /**
         * After having reloaded "the important one", we reload the others as well
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                for(int i = 0; i < fList.size(); i++) {
                    if(i != position)
                        reloadSingleFragmentData(i, true);
                }
            }
        }).run();
    }

    /**
     * Reloads the single data of a fragment
     * @param position  the position of the fragment
     * @param forceRefresh  whether we want the data to be refreshed even though a fragment is already there and filled up
     */
    private void reloadSingleFragmentData(final int position, final boolean forceRefresh) {
        try {
            final ArrayList<String> operators = new ArrayList<>();
            operators.add(">");
            operators.add("<");

            /**
             * If we're forcing refresh, we must start from scratch
             */
            if(forceRefresh) {
                busHermadaToAirport = new LinkedList<>();
                busStationToHermadaTimes = new LinkedList<>();
                busHermadaToStationTimes = new LinkedList<>();
                tramSansovinoCentroTimes = new LinkedList<>();
                tramCentroSansovinoTimes = new LinkedList<>();
                tramToMestreCityCenterTimes = new LinkedList<>();
                tramToStationTimes = new LinkedList<>();
                tramToMestreTimes = new LinkedList<>();
                tramToVeniceTimes = new LinkedList<>();
                busAirportToHermada = new LinkedList<>();
            }

            switch (position) {
                case 0:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (tramToVeniceTimes == null || tramToVeniceTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for(String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }

                                        tramToVeniceTimes.addAll(DatabaseHelper.getTramAndBusesToVenice(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                                    }
                                }

                                ((MainFragment) fList.get(position)).populate(tramToVeniceTimes, forceRefresh);
                                prefetchTimes(position + 1);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 1:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (forceRefresh || (tramToMestreTimes == null || tramToMestreTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }

                                        tramToMestreTimes.addAll(DatabaseHelper.getTramAndBusesFromVenice(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(tramToMestreTimes, forceRefresh);
                                prefetchTimes(position + 1);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 2:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (tramToStationTimes == null || tramToStationTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }

                                        tramToStationTimes.addAll(DatabaseHelper.getTramToStation(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(tramToStationTimes, forceRefresh);
                                prefetchTimes(position + 1);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 3:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (tramToMestreCityCenterTimes == null || tramToMestreCityCenterTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            tramToMestreCityCenterTimes.addAll(DatabaseHelper.getTramFromStation(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(tramToMestreCityCenterTimes, forceRefresh);
                                prefetchTimes(position + 1);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 4:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (tramCentroSansovinoTimes == null || tramCentroSansovinoTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }
                                        tramCentroSansovinoTimes.addAll(DatabaseHelper.getCenterToSansovino(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(tramCentroSansovinoTimes, forceRefresh);
                                prefetchTimes(position + 1);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 5:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (tramSansovinoCentroTimes == null || tramSansovinoCentroTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            tramSansovinoCentroTimes.addAll(DatabaseHelper.getSansovinoToCenter(getApplicationContext(), yesterdayForThisQuery,  now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(tramSansovinoCentroTimes, forceRefresh);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 6:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (busHermadaToStationTimes == null || busHermadaToStationTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            busHermadaToStationTimes.addAll(DatabaseHelper.getHermadaToStation(getApplicationContext(), yesterdayForThisQuery,  now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(busHermadaToStationTimes, forceRefresh);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 7:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (busStationToHermadaTimes == null || busStationToHermadaTimes.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            busStationToHermadaTimes.addAll(DatabaseHelper.getStationToHermada(getApplicationContext(), yesterdayForThisQuery,  now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(busStationToHermadaTimes, forceRefresh);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 8:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (busHermadaToAirport == null || busHermadaToAirport.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            busHermadaToAirport.addAll(DatabaseHelper.getHermadaToAirport(getApplicationContext(), yesterdayForThisQuery,  now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(busHermadaToAirport, forceRefresh);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
                case 9:
                    if(forceRefresh || !((MainFragment)fList.get(position)).isPopulated()) {
                        Thread busPopulateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if(forceRefresh || (busAirportToHermada == null || busAirportToHermada.size() == 0)) {
                                    String dayForThisQuery;
                                    String yesterdayForThisQuery;

                                    for (String operator : operators) {

                                        if (operators.indexOf(operator) == 0) {
                                            dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                            yesterdayForThisQuery = yesterdayForQuery;
                                        }
                                        else {
                                            dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                            yesterdayForThisQuery = dayForQuery;
                                        }                                                {
                                            busAirportToHermada.addAll(DatabaseHelper.getAirportToHermada(getApplicationContext(), yesterdayForThisQuery,  now, operator, dayForThisQuery));
                                        }
                                    }
                                }
                                ((MainFragment) fList.get(position)).populate(busAirportToHermada, forceRefresh);
                                ((MainFragment)fList.get(position)).setRefresingForFragment(false);
                            }
                        });
                        busPopulateThread.run();
                    }
                    else {
                        ((MainFragment) fList.get(position)).show();
                        ((MainFragment) fList.get(position)).setRefresingForFragment(false);
                    }
                    break;
            }
        } catch (NullPointerException e) {
            Log.e("Catchit", "I wonder why" + e.getStackTrace().toString());
        }
    }

    /**
     * Pre-loads data for next Fragment
     * @param position
     */
    private void prefetchTimes(int position) {
        if(true)
            return;

        final ArrayList<String> operators = new ArrayList<>();
        operators.add(">");
        operators.add("<");

        switch(position) {
            case 0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramToVeniceTimes.addAll(DatabaseHelper.getTramAndBusesToVenice(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                            }
                    }
                }).run();
                break;
            case 1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramToMestreTimes.addAll(DatabaseHelper.getTramAndBusesFromVenice(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 2:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramToStationTimes.addAll(DatabaseHelper.getTramToStation(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 3:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramToMestreCityCenterTimes.addAll(DatabaseHelper.getTramFromStation(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 4:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramCentroSansovinoTimes.addAll(DatabaseHelper.getCenterToSansovino(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 5:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            tramSansovinoCentroTimes.addAll(DatabaseHelper.getSansovinoToCenter(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 6:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            busHermadaToStationTimes.addAll(DatabaseHelper.getHermadaToStation(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 7:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            busStationToHermadaTimes.addAll(DatabaseHelper.getStationToHermada(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 8:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            busHermadaToAirport.addAll(DatabaseHelper.getHermadaToAirport(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
                    }
                }).run();
                break;
            case 9:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String dayForThisQuery;
                        String yesterdayForThisQuery;

                        for(String operator : operators) {

                            if (operators.indexOf(operator) == 0) {
                                dayForThisQuery = dayForQuery; //We want the timetable of next buses today
                                yesterdayForThisQuery = yesterdayForQuery;
                            }
                            else {
                                dayForThisQuery = tomorrowForQuery; //And remote buses tomorrow
                                yesterdayForThisQuery = dayForQuery;
                            }

                            busAirportToHermada.addAll(DatabaseHelper.getAirportToHermada(getApplicationContext(), yesterdayForThisQuery, now, operator, dayForThisQuery));
                        }
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
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        } else {
            // Otherwise, select the previous step.
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    private List<Fragment> getFragments() {

        if(fList == null || fList.size() == 0) {
            for (int position = 1; position < 11; position++) {
                Bundle args = new Bundle();
                MainFragment thisFragment = new MainFragment();
                args.putInt(POSITION, position);
                thisFragment.setArguments(args);
                thisFragment.setRetainInstance(true);
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

    private void selectRightDayOfTheWeek() {
        switch (todayWeek) {
            case Calendar.SUNDAY:
                dayForQuery = "c.sunday";
                tomorrowForQuery = "c.monday";
                yesterdayForQuery = "c.saturday";
                break;
            case Calendar.MONDAY:
                dayForQuery = "c.monday";
                tomorrowForQuery = "c.wednesday";
                yesterdayForQuery = "c.sunday";
                break;
            case Calendar.TUESDAY:
                dayForQuery = "c.tuesday";
                tomorrowForQuery = "c.wednesday";
                yesterdayForQuery = "c.monday";
                break;
            case Calendar.WEDNESDAY:
                dayForQuery = "c.wednesday";
                tomorrowForQuery = "c.thursday";
                yesterdayForQuery = "c.tuesday";
                break;
            case Calendar.THURSDAY:
                dayForQuery = "c.thursday";
                tomorrowForQuery = "c.friday";
                yesterdayForQuery = "c.wednesday";
                break;
            case Calendar.FRIDAY:
                dayForQuery = "c.friday";
                tomorrowForQuery = "c.saturday";
                yesterdayForQuery = "c.thursday";
                break;
            default:
                dayForQuery = "c.saturday";
                tomorrowForQuery = "c.sunday";
                yesterdayForQuery = "c.friday";
                break;
        }
    }
}