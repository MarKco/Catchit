package com.ilsecondodasinistra.catchit

import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.GravityCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.toolbar.*
import java.util.*

class MainCatchitActivity : AppCompatActivity() {

    internal var tramToVeniceTimes: MutableList<Bus> = LinkedList()
    internal var tramToMestreTimes: MutableList<Bus> = LinkedList()

    internal var tramToStationTimes: MutableList<Bus> = LinkedList()
    internal var tramToMestreCityCenterTimes: MutableList<Bus> = LinkedList()

    internal var tramCentroSansovinoTimes: MutableList<Bus> = LinkedList()
    internal var tramSansovinoCentroTimes: MutableList<Bus> = LinkedList()

    internal var busHermadaToStationTimes: MutableList<Bus> = LinkedList()
    internal var busStationToHermadaTimes: MutableList<Bus> = LinkedList()

    internal var busAirportToHermada: MutableList<Bus> = LinkedList()
    internal var busHermadaToAirport: MutableList<Bus> = LinkedList()

    internal var todayWeek: Int = 0
    internal var debugDayOfTheWeek = Calendar.SUNDAY

    internal lateinit var dayForQuery: String
    internal lateinit var tomorrowForQuery: String
    internal lateinit var yesterdayForQuery: String
    internal var now = Date()

    internal var fList: MutableList<Fragment> = ArrayList()

    /**
     * The pager widget, which handles animation and allows swiping horizontally to access previous
     * and next wizard steps.
     */
    private var mPager: ViewPager? = null

    /**
     * Navigation drawer
     */

    //First We Declare Titles And Icons For Our Navigation mDrawerLayout List View
    //This Icons And Titles Are holded in an Array as you can see

    internal var ICONS = intArrayOf(R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_subway_black_24dp, R.drawable.ic_directions_bus_black_24dp, R.drawable.ic_directions_bus_black_24dp, R.drawable.ic_directions_bus_black_24dp, R.drawable.ic_directions_bus_black_24dp)

    internal lateinit var mRecyclerView: RecyclerView                           // Declaring RecyclerView
    internal lateinit var mAdapter: RecyclerView.Adapter<*>                        // Declaring Adapter For Recycler View
    internal lateinit var mLayoutManager: RecyclerView.LayoutManager            // Declaring Layout Manager as a linear layout manager
    internal lateinit var mDrawerLayout: DrawerLayout                                  // Declaring DrawerLayout

    //Similarly we Create a String Resource for the name and email in the header view
    //And we also create a int resource for profile picture in the header view

    internal var NAME = "Catchit"
    internal var EMAIL = "Non perdere la pazienza per prendere il tram"
    internal var PROFILE = R.drawable.ic_launcher

    /**
     * The pager adapter, which provides the pages to the view pager widget.
     */
    private var mPagerAdapter: PagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pager)

        //add the Toolbar
        setSupportActionBar(toolbar)

        /* Actions for Navigation mDrawerLayout */
        val TITLES = arrayOf(getString(R.string.mestre_venezia), getString(R.string.venezia_mestre), getString(R.string.centro_stazione), getString(R.string.stazione_centro), getString(R.string.centro_casa), getString(R.string.casa_centro), getString(R.string.casa_stazione), getString(R.string.stazione_casa), getString(R.string.casa_aeroporto), getString(R.string.aeroporto_casa))

        mRecyclerView = findViewById<View>(R.id.navigationDrawer) as RecyclerView // Assigning the RecyclerView Object to the xml View
        mRecyclerView.setHasFixedSize(true)                            // Letting the system know that the list objects are of fixed size

        mAdapter = NavigationAdapter(TITLES, ICONS, NAME, EMAIL, PROFILE)       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)
        // And passing the titles,icons,header view name, header view email,
        // and header view profile picture

        mRecyclerView.adapter = mAdapter                              // Setting the adapter to RecyclerView
        mLayoutManager = LinearLayoutManager(this)                 // Creating a layout Manager
        mRecyclerView.layoutManager = mLayoutManager                 // Setting the layout Manager

        mDrawerLayout = findViewById<View>(R.id.drawer_layout) as DrawerLayout        // mDrawerLayout object Assigned to the view

        val calendar = Calendar.getInstance()
        todayWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (DEBUGDAY)
            todayWeek = debugDayOfTheWeek

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        if (Build.VERSION.SDK_INT >= 18) {
            supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)    //  Required API level 18 :(
        }

        val mGestureDetector = GestureDetector(this@MainCatchitActivity, object : GestureDetector.SimpleOnGestureListener() {

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                return true
            }
        })

        mRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent): Boolean {
                val child = recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y)

                val position: Int
                if (child != null && mGestureDetector.onTouchEvent(motionEvent)) {
                    mDrawerLayout.closeDrawers()
                    //                    Toast.makeText(MainCatchitActivity.this,"The Item Clicked is: "+recyclerView.getChildPosition(child),Toast.LENGTH_SHORT).show();
                    position = recyclerView.getChildPosition(child)
                    if (position > 0)
                        mPager!!.currentItem = position - 1
                    return true
                }

                return false
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {

            }

            override fun onTouchEvent(recyclerView: RecyclerView, motionEvent: MotionEvent) {

            }
        })


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById<View>(R.id.pager) as ViewPager
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, fragments)
        mPager!!.adapter = mPagerAdapter

        /* Actions for Navigation mDrawerLayout */
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
    }

    override fun onStart() {
        super.onStart()

        reloadBusAndTrams()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                mDrawerLayout.closeDrawer(Gravity.LEFT)
            } else {
                mDrawerLayout.openDrawer(Gravity.LEFT)
            }

        } else if (item.itemId == R.id.action_settings) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
    }

    fun refreshDateTime() {
        now = Date()

        if (DEBUGHOUR) {
            val cal = Calendar.getInstance()
            cal.time = now //This date is a copy of present datetime (which actually is Linux Epoch)
            cal.set(Calendar.HOUR_OF_DAY, DEBUG_HOURS) //We just change the hours, minutes, seconds
            cal.set(Calendar.MINUTE, DEBUG_MINUTES)
            cal.set(Calendar.SECOND, DEBUG_SECONDS)
            cal.set(Calendar.MILLISECOND, DEBUG_MILLISECONDS)
            now.time = cal.timeInMillis
        }

        if (DEBUGDAY)
            now.date = debugDayOfTheWeek

        selectRightDayOfTheWeek()

    }

    private fun reloadBusAndTrams() {

        refreshDateTime()

        mPager!!.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                reloadSingleFragmentData(position, false)  //We don't want to force reload, of course
                when (position) {
                    0 -> titleText.text = getString(R.string.mestre_venezia)
                    1 -> titleText.text = getString(R.string.venezia_mestre)
                    2 -> titleText.text = getString(R.string.centro_stazione)
                    3 -> titleText.text = getString(R.string.stazione_centro)
                    4 -> titleText.text = getString(R.string.centro_casa)
                    5 -> titleText.text = getString(R.string.casa_centro)
                    6 -> titleText.text = getString(R.string.casa_stazione)
                    7 -> titleText.text = getString(R.string.stazione_casa)
                    8 -> titleText.text = getString(R.string.casa_aeroporto)
                    9 -> titleText.text = getString(R.string.aeroporto_casa)
                }
            }

            override fun onPageSelected(position: Int) {}

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    /**
     * This gets called by the fragment when the user swipes down to refresh
     * @param fragment
     * * TODO: Use an interface and a callback, calling this method directly is shit!
     */
    fun reloadSingleFragmentDataFromFragment(fragment: MainFragment) {
        refreshDateTime()  //We update the "now" variable

        //Code for debugging reload. Just put the time you wand and uncomment. It will be passed only upon refresh.
        //        now = new Date();
        //
        //        Calendar cal = Calendar.getInstance();
        //        cal.setTime(now); //This date is a copy of present datetime (which actually is Linux Epoch)
        //        cal.set(Calendar.HOUR_OF_DAY, 1); //We just change the hours, minutes, seconds
        //        cal.set(Calendar.MINUTE, 29);
        //        cal.set(Calendar.SECOND, 00);
        //        cal.set(Calendar.MILLISECOND, 00);
        //        now.setTime(cal.getTimeInMillis());

        val position = fList!!.indexOf(fragment)
        if (fList!!.contains(fragment)) {
            reloadSingleFragmentData(position, true)    //This is called when the user swipes down the list for refreshing, so we force refresh!
        }

        /**
         * After having reloaded "the important one", we reload the others as well
         */
        Thread(Runnable {
            for (i in fList!!.indices) {
                if (i != position)
                    reloadSingleFragmentData(i, true)
            }
        }).run()
    }

    /**
     * Reloads the single data of a fragment
     * @param position  the position of the fragment
     * *
     * @param forceRefresh  whether we want the data to be refreshed even though a fragment is already there and filled up
     */
    private fun reloadSingleFragmentData(position: Int, forceRefresh: Boolean) {
        try {
            val operators = ArrayList<String>()
            operators.add(">")
            operators.add("<")

            /**
             * If we're forcing refresh, we must start from scratch
             */
            if (forceRefresh) {
                busHermadaToAirport = LinkedList<Bus>()
                busStationToHermadaTimes = LinkedList<Bus>()
                busHermadaToStationTimes = LinkedList<Bus>()
                tramSansovinoCentroTimes = LinkedList<Bus>()
                tramCentroSansovinoTimes = LinkedList<Bus>()
                tramToMestreCityCenterTimes = LinkedList<Bus>()
                tramToStationTimes = LinkedList<Bus>()
                tramToMestreTimes = LinkedList<Bus>()
                tramToVeniceTimes = LinkedList<Bus>()
                busAirportToHermada = LinkedList<Bus>()
            }

            when (position) {
                0 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(Runnable {
                        if (forceRefresh || tramToVeniceTimes == null || tramToVeniceTimes!!.size == 0) {
                            var dayForThisQuery: String
                            var yesterdayForThisQuery: String

                            for (operator in operators) {

                                if (operators.indexOf(operator) == 0) {
                                    dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                    yesterdayForThisQuery = yesterdayForQuery
                                } else {
                                    dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                    yesterdayForThisQuery = dayForQuery
                                }

                                tramToVeniceTimes!!.addAll(DatabaseHelper.getTramAndBusesToVenice(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                            }
                        }

                        (fList!![position] as MainFragment).populate(tramToVeniceTimes, forceRefresh)
                        prefetchTimes(position + 1)
                        (fList!![position] as MainFragment).setRefresingForFragment(false)
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                1 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(Runnable {
                        if (forceRefresh || tramToMestreTimes == null || tramToMestreTimes!!.size == 0) {
                            var dayForThisQuery: String
                            var yesterdayForThisQuery: String

                            for (operator in operators) {

                                if (operators.indexOf(operator) == 0) {
                                    dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                    yesterdayForThisQuery = yesterdayForQuery
                                } else {
                                    dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                    yesterdayForThisQuery = dayForQuery
                                }

                                tramToMestreTimes!!.addAll(DatabaseHelper.getTramAndBusesFromVenice(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                            }
                        }
                        (fList!![position] as MainFragment).populate(tramToMestreTimes, forceRefresh)
                        prefetchTimes(position + 1)
                        (fList!![position] as MainFragment).setRefresingForFragment(false)
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                2 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(Runnable {
                        if (forceRefresh || tramToStationTimes == null || tramToStationTimes!!.size == 0) {
                            var dayForThisQuery: String
                            var yesterdayForThisQuery: String

                            for (operator in operators) {

                                if (operators.indexOf(operator) == 0) {
                                    dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                    yesterdayForThisQuery = yesterdayForQuery
                                } else {
                                    dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                    yesterdayForThisQuery = dayForQuery
                                }

                                tramToStationTimes!!.addAll(DatabaseHelper.getTramToStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                            }
                        }
                        (fList!![position] as MainFragment).populate(tramToStationTimes, forceRefresh)
                        prefetchTimes(position + 1)
                        (fList!![position] as MainFragment).setRefresingForFragment(false)
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                3 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || tramToMestreCityCenterTimes == null || tramToMestreCityCenterTimes!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ tramToMestreCityCenterTimes!!.addAll(DatabaseHelper.getTramFromStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(tramToMestreCityCenterTimes, forceRefresh)
                            prefetchTimes(position + 1)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                4 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(Runnable {
                        if (forceRefresh || tramCentroSansovinoTimes == null || tramCentroSansovinoTimes!!.size == 0) {
                            var dayForThisQuery: String
                            var yesterdayForThisQuery: String

                            for (operator in operators) {

                                if (operators.indexOf(operator) == 0) {
                                    dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                    yesterdayForThisQuery = yesterdayForQuery
                                } else {
                                    dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                    yesterdayForThisQuery = dayForQuery
                                }
                                tramCentroSansovinoTimes!!.addAll(DatabaseHelper.getCenterToSansovino(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                            }
                        }
                        (fList!![position] as MainFragment).populate(tramCentroSansovinoTimes, forceRefresh)
                        prefetchTimes(position + 1)
                        (fList!![position] as MainFragment).setRefresingForFragment(false)
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                5 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || tramSansovinoCentroTimes == null || tramSansovinoCentroTimes!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ tramSansovinoCentroTimes!!.addAll(DatabaseHelper.getSansovinoToCenter(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(tramSansovinoCentroTimes, forceRefresh)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                6 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || busHermadaToStationTimes == null || busHermadaToStationTimes!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ busHermadaToStationTimes!!.addAll(DatabaseHelper.getHermadaToStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(busHermadaToStationTimes, forceRefresh)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                7 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || busStationToHermadaTimes == null || busStationToHermadaTimes!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ busStationToHermadaTimes!!.addAll(DatabaseHelper.getStationToHermada(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(busStationToHermadaTimes, forceRefresh)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                8 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || busHermadaToAirport == null || busHermadaToAirport!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ busHermadaToAirport!!.addAll(DatabaseHelper.getHermadaToAirport(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(busHermadaToAirport, forceRefresh)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
                9 -> if (forceRefresh || !(fList!![position] as MainFragment).isPopulated) {
                    val busPopulateThread = Thread(object : Runnable {
                        override fun run() {
                            if (forceRefresh || busAirportToHermada == null || busAirportToHermada!!.size == 0) {
                                var dayForThisQuery: String
                                var yesterdayForThisQuery: String

                                for (operator in operators) {

                                    if (operators.indexOf(operator) == 0) {
                                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                                        yesterdayForThisQuery = yesterdayForQuery
                                    } else {
                                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                                        yesterdayForThisQuery = dayForQuery
                                    }
                                    run({ busAirportToHermada!!.addAll(DatabaseHelper.getAirportToHermada(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery)) })
                                }
                            }
                            (fList!![position] as MainFragment).populate(busAirportToHermada, forceRefresh)
                            (fList!![position] as MainFragment).setRefresingForFragment(false)
                        }
                    })
                    busPopulateThread.run()
                } else {
                    (fList!![position] as MainFragment).show()
                    (fList!![position] as MainFragment).setRefresingForFragment(false)
                }
            }
        } catch (e: NullPointerException) {
            Log.e("Catchit", "I wonder why" + e.stackTrace.toString())
        }

    }

    /**
     * Pre-loads data for next Fragment
     * @param position
     */
    private fun prefetchTimes(position: Int) {
        if (true)
            return

        val operators = ArrayList<String>()
        operators.add(">")
        operators.add("<")

        when (position) {
            0 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramToVeniceTimes!!.addAll(DatabaseHelper.getTramAndBusesToVenice(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            1 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramToMestreTimes!!.addAll(DatabaseHelper.getTramAndBusesFromVenice(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            2 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramToStationTimes!!.addAll(DatabaseHelper.getTramToStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            3 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramToMestreCityCenterTimes!!.addAll(DatabaseHelper.getTramFromStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            4 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramCentroSansovinoTimes!!.addAll(DatabaseHelper.getCenterToSansovino(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            5 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    tramSansovinoCentroTimes!!.addAll(DatabaseHelper.getSansovinoToCenter(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            6 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    busHermadaToStationTimes!!.addAll(DatabaseHelper.getHermadaToStation(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            7 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    busStationToHermadaTimes!!.addAll(DatabaseHelper.getStationToHermada(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            8 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    busHermadaToAirport!!.addAll(DatabaseHelper.getHermadaToAirport(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
            9 -> Thread(Runnable {
                var dayForThisQuery: String
                var yesterdayForThisQuery: String

                for (operator in operators) {

                    if (operators.indexOf(operator) == 0) {
                        dayForThisQuery = dayForQuery //We want the timetable of next buses today
                        yesterdayForThisQuery = yesterdayForQuery
                    } else {
                        dayForThisQuery = tomorrowForQuery //And remote buses tomorrow
                        yesterdayForThisQuery = dayForQuery
                    }

                    busAirportToHermada!!.addAll(DatabaseHelper.getAirportToHermada(applicationContext, yesterdayForThisQuery, now, operator, dayForThisQuery))
                }
            }).run()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        if (mPager!!.currentItem == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START)
            } else {
                super.onBackPressed()
            }
        } else {
            // Otherwise, select the previous step.
            mPager!!.currentItem = mPager!!.currentItem - 1
        }
    }

    private val fragments: List<Fragment>
        get() {

            if (fList == null || fList!!.size == 0) {
                for (position in 1..10) {
                    val args = Bundle()
                    val thisFragment = MainFragment()
                    args.putInt(POSITION, position)
                    thisFragment.arguments = args
                    thisFragment.retainInstance = true
                    fList!!.add(thisFragment)
                }
            }

            return fList
        }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager, private val fragments: List<Fragment>) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return this.fragments[position]
        }

        override fun getCount(): Int {
            return this.fragments.size
        }
    }

    private fun selectRightDayOfTheWeek() {
        when (todayWeek) {
            Calendar.SUNDAY -> {
                dayForQuery = "c.sunday"
                tomorrowForQuery = "c.monday"
                yesterdayForQuery = "c.saturday"
            }
            Calendar.MONDAY -> {
                dayForQuery = "c.monday"
                tomorrowForQuery = "c.wednesday"
                yesterdayForQuery = "c.sunday"
            }
            Calendar.TUESDAY -> {
                dayForQuery = "c.tuesday"
                tomorrowForQuery = "c.wednesday"
                yesterdayForQuery = "c.monday"
            }
            Calendar.WEDNESDAY -> {
                dayForQuery = "c.wednesday"
                tomorrowForQuery = "c.thursday"
                yesterdayForQuery = "c.tuesday"
            }
            Calendar.THURSDAY -> {
                dayForQuery = "c.thursday"
                tomorrowForQuery = "c.friday"
                yesterdayForQuery = "c.wednesday"
            }
            Calendar.FRIDAY -> {
                dayForQuery = "c.friday"
                tomorrowForQuery = "c.saturday"
                yesterdayForQuery = "c.thursday"
            }
            else -> {
                dayForQuery = "c.saturday"
                tomorrowForQuery = "c.sunday"
                yesterdayForQuery = "c.friday"
            }
        }
    }

    companion object {

        val POSITION = "Position"

        val DEBUGHOUR = false //if true, custom departureTime below is set in the app,
        val DEBUGDAY = false //if true, custom date below is set in the app,

        // regardless of real timestamp
        val DEBUG_HOURS = 23
        val DEBUG_MINUTES = 55
        val DEBUG_SECONDS = 0
        val DEBUG_MILLISECONDS = 0
    }
}