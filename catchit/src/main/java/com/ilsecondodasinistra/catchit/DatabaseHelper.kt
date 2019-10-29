package com.ilsecondodasinistra.catchit

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList

/**
 * Created by marco on 17/01/17.
 */

object DatabaseHelper {

    val routeForT1MestreVe = "731"
    val routeForT1VeMestre = "733"
    val routeForT2MestreMa = "779"
    val routeForT2MaMestre = "780, 781"

    val routeForN1 = "712, 713"
    val routeForN2 = "714"

    val routeFor12MestreVe = "63, 530"
    val routeFor12VeMestre = "66, 529"

    val routeFor15AirportStation = "545, 546"
    val routeFor15StationAirport = "547, 548"

    val departingSansovino = "6061"
    val returningSansovino = "6062"
    val sansovinoForN = departingSansovino + ", " + returningSansovino
    val veniceStops = "510, 6084"
    val veniceStopsFor12 = "501"
    val cialdini = "6080, 6027, 6081"
    val stazioneMestre = "6074, 6073"
    val stazioneMestreFor15 = "164, 612, 613"
    val viaHermadaStreetSide = "172"
    val viaHermadaCanalSide = "1172"
    val airport = "3626, 13626"

    internal var databaseHourFormatter = SimpleDateFormat("HH:mm:ss") //DateFormatter four hours.minutes.seconds
    private val dateFormatter = SimpleDateFormat("HH:mm:ss") //DateFormatter four hours.minutes.seconds

    lateinit var db: SQLiteDatabase

    private fun openDatabase(context: Context): SQLiteDatabase {
        val dbHelper = MyDatabase(context)
        dbHelper.setForcedUpgrade()
        return dbHelper.writableDatabase
    }

    private fun closeDatabase(db: SQLiteDatabase) {
        db.close()
    }

    fun getTramAndBusesToVenice(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramToVeniceTimes = LinkedList<Bus>()

        val tramToVenice = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeForT1MestreVe + ", " + routeFor12MestreVe + ")\n" + //For ordinary buses

                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id = " + departingSansovino + "\n" +
                "  and end_s.stop_id in (" + veniceStops + ", " + veniceStopsFor12 + ")\n" +
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
                "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" + //Needed only because N1 and N2 are circular, so you could get paradoxical results

                "  and end_s.stop_id in (" + veniceStops + ")\n" +
                "  and end_st.late_night IS NULL\n" +
                " UNION " +
                "SELECT t.trip_id,\n" +
                "       start_s.stop_name as departure_stop,\n" +
                "\t   start_s.stop_id as departure_stop_id,\n" +
                "       start_st.departure_time as departure_time,\n" +
                "       direction_id as direction,\n" +
                "       end_s.stop_name as arrival_stop,\n" +
                "\t   end_s.stop_id as arrival_stop_id,\n" +
                "       end_st.arrival_time as arrival_time,\n" +
                "       r.route_short_name as route_short_name,\n" +
                //                "       end_st.late_night as bus_late_night,\n" +
                "       r.route_long_name as route_long_name\n" +
                "FROM\n" +
                "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                "WHERE " + yesterdayForQuery + " = 1\n" +
                "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                "  and departure_stop_id in (" + sansovinoForN + ")\n" + //For night buses

                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" + //Needed only because N1 and N2 are circular, so you could get paradoxical results

                "  and end_s.stop_id in (" + veniceStops + ")\n" +
                "  and end_st.late_night IS NOT NULL\n" +
                "order by start_st.departure_time asc"

        //            if (BuildConfig.DEBUG)
        //                Log.w("TramToVenice", tramToVenice);

        val leavingCursor = db.rawQuery(tramToVenice, null)
        leavingCursor.moveToFirst()
        if (leavingCursor.count > 0)
            try {
                do {
                    val departureStop = leavingCursor.getString(leavingCursor.getColumnIndex("departure_stop"))
                    val departureTime = leavingCursor.getString(leavingCursor.getColumnIndex("departure_time"))
                    val arrivalStop = leavingCursor.getString(leavingCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = leavingCursor.getString(leavingCursor.getColumnIndex("arrival_time"))
                    val line = leavingCursor.getString(leavingCursor.getColumnIndex("route_short_name"))

                    tramToVeniceTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))
                    //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                } while (leavingCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        leavingCursor.close()
        db.close()

        return tramToVeniceTimes
    }

    fun getTramAndBusesFromVenice(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramToMestreTimes = LinkedList<Bus>()

        val tramFromVenice = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeForT1VeMestre + ", " + routeFor12VeMestre + ")\n" + //For ordinary buses

                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + veniceStops + ", " + veniceStopsFor12 + ")\n" +
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
                "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" + //For night buses

                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + veniceStops + ")\n" +
                "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" + //Needed only because N1 and N2 are circular, so you could get paradoxical results

                "  and end_s.stop_id in (" + sansovinoForN + ")\n" +
                "  and end_st.late_night IS NULL\n" +
                " UNION " +
                "SELECT t.trip_id,\n" +
                "       start_s.stop_name as departure_stop,\n" +
                "\t   start_s.stop_id as departure_stop_id,\n" +
                "       start_st.departure_time as departure_time,\n" +
                "       direction_id as direction,\n" +
                "       end_s.stop_name as arrival_stop,\n" +
                "\t   end_s.stop_id as arrival_stop_id,\n" +
                "       end_st.arrival_time as arrival_time,\n" +
                "       r.route_short_name as route_short_name,\n" +
                //                "       end_st.late_night as bus_late_night,\n" +
                "       r.route_long_name as route_long_name\n" +
                "FROM\n" +
                "trips t INNER JOIN calendar c ON t.service_id = c.service_id\n" +
                "        INNER JOIN routes r ON t.route_id = r.route_id\n" +
                "        INNER JOIN stop_times start_st ON t.trip_id = start_st.trip_id\n" +
                "        INNER JOIN stops start_s ON start_st.stop_id = start_s.stop_id\n" +
                "        INNER JOIN stop_times end_st ON t.trip_id = end_st.trip_id\n" +
                "        INNER JOIN stops end_s ON end_st.stop_id = end_s.stop_id\n" +
                "WHERE " + yesterdayForQuery + " = 1\n" +
                "  and r.route_id in (" + routeForN1 + ", " + routeForN2 + ")\n" +
                "  and departure_stop_id in (" + veniceStops + ")\n" + //For night buses

                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and DATETIME(start_st.departure_time) < DATETIME(end_st.arrival_time)\n" + //Needed only because N1 and N2 are circular, so you could get paradoxical results

                "  and end_s.stop_id in (" + sansovinoForN + ")\n" +
                "  and end_st.late_night IS NOT NULL\n" +
                "order by start_st.departure_time asc"

        //                    if(BuildConfig.DEBUG)
        //                        Log.w("TramFromVenice", tramFromVenice);

        val comingCursor = db.rawQuery(tramFromVenice, null)
        comingCursor.moveToFirst()
        if (comingCursor.count > 0)
            try {
                do {
                    val departureStop = comingCursor.getString(comingCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingCursor.getString(comingCursor.getColumnIndex("departure_time"))
                    val arrivalTime = comingCursor.getString(comingCursor.getColumnIndex("arrival_time"))
                    val arrivalStop = comingCursor.getString(comingCursor.getColumnIndex("arrival_stop"))
                    val line = comingCursor.getString(comingCursor.getColumnIndex("short_name"))

                    tramToMestreTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)))
                    //                        Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                } while (comingCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingCursor.close()
        db.close()

        return tramToMestreTimes
    }

    fun getTramToStation(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramToStationTimes = LinkedList<Bus>()

        //Tram Mestre Centro -> Stazione
        val tramToStation = "SELECT t.trip_id,\n" +
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
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("TramToStation", tramToStation);

        val leavingTramCursor = db.rawQuery(tramToStation, null)
        leavingTramCursor.moveToFirst()
        if (leavingTramCursor.count > 0)
            try {
                do {
                    val departureStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_time"))
                    val arrivalTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_time"))
                    val arrivalStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_stop"))
                    val line = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("route_short_name"))

                    tramToStationTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (leavingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        leavingTramCursor.close()
        db.close()

        return tramToStationTimes
    }

    fun getTramFromStation(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramToStationTimes = LinkedList<Bus>()

        //Tram Mestre Centro -> Stazione
        val tramToStation = "SELECT t.trip_id,\n" +
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
                "  and arrival_stop_id in (" + cialdini + ")\n" +
                "  and departure_stop_id in (" + stazioneMestre + ")\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("TramToStation", tramToStation);

        val leavingTramCursor = db.rawQuery(tramToStation, null)
        leavingTramCursor.moveToFirst()
        if (leavingTramCursor.count > 0)
            try {
                do {
                    val departureStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("departure_time"))
                    val arrivalTime = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_time"))
                    val arrivalStop = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("arrival_stop"))
                    val line = leavingTramCursor.getString(leavingTramCursor.getColumnIndex("route_short_name"))

                    tramToStationTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (leavingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        leavingTramCursor.close()
        db.close()

        return tramToStationTimes
    }

    fun getCenterToSansovino(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramCentroSansovinoTimes = LinkedList<Bus>()

        val tramFromStation = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeForT1MestreVe + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + cialdini + ")\n" +
                "  and arrival_stop_id in (" + departingSansovino + ")\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("tramFromStation", tramFromStation);

        val comingTramCursor = db.rawQuery(tramFromStation, null)
        comingTramCursor.moveToFirst()
        if (comingTramCursor.count > 0)
            try {
                do {
                    val departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"))
                    val arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"))
                    val line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"))

                    tramCentroSansovinoTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (comingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingTramCursor.close()
        db.close()

        return tramCentroSansovinoTimes
    }

    fun getSansovinoToCenter(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramSansovinoCentroTimes = LinkedList<Bus>()

        val tramSansovinoToCentro = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeForT1VeMestre + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + returningSansovino + ")\n" +
                "  and arrival_stop_id in (" + cialdini + ")\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("TramToVenice", tramSansovinoToCentro);

        val leavingToCentroCursor = db.rawQuery(tramSansovinoToCentro, null)
        leavingToCentroCursor.moveToFirst()
        if (leavingToCentroCursor.count > 0)
            try {
                do {
                    val departureStop = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("departure_stop"))
                    val departureTime = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("departure_time"))
                    val arrivalStop = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("arrival_time"))
                    val line = leavingToCentroCursor.getString(leavingToCentroCursor.getColumnIndex("route_short_name"))

                    tramSansovinoCentroTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))
                    //                    Log.i("Catchit", "Added a new item: " + departureStop + " " + departureTime + " " + line);

                } while (leavingToCentroCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        leavingToCentroCursor.close()
        db.close()
        //                    if(BuildConfig.DEBUG)
        //                        Log.w("TramSansovinoToCentro", tramSansovinoToCentro);

        return tramSansovinoCentroTimes

    }

    fun getHermadaToStation(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramCentroSansovinoTimes = LinkedList<Bus>()

        val tramFromStation = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeFor15AirportStation + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id = " + viaHermadaStreetSide + "\n" +
                "  and arrival_stop_id IN (" + stazioneMestreFor15 + ")\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("tramFromStation", tramFromStation);

        val comingTramCursor = db.rawQuery(tramFromStation, null)
        comingTramCursor.moveToFirst()
        if (comingTramCursor.count > 0)
            try {
                do {
                    val departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"))
                    val arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"))
                    val line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"))

                    tramCentroSansovinoTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (comingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingTramCursor.close()
        db.close()

        return tramCentroSansovinoTimes
    }

    fun getStationToHermada(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramCentroSansovinoTimes = LinkedList<Bus>()

        val tramFromStation = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeFor15StationAirport + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id IN (" + stazioneMestreFor15 + ")\n" +
                "  and arrival_stop_id = " + viaHermadaCanalSide + "\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("tramFromStation", tramFromStation);

        val comingTramCursor = db.rawQuery(tramFromStation, null)
        comingTramCursor.moveToFirst()
        if (comingTramCursor.count > 0)
            try {
                do {
                    val departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"))
                    val arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"))
                    val line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"))

                    tramCentroSansovinoTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (comingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingTramCursor.close()
        db.close()

        return tramCentroSansovinoTimes
    }

    fun getHermadaToAirport(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramCentroSansovinoTimes = LinkedList<Bus>()

        val tramFromStation = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeFor15StationAirport + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + viaHermadaCanalSide + ")\n" +
                "  and arrival_stop_id in (" + airport + ")\n" +
                "order by start_st.departure_time asc\t"

        //                if(BuildConfig.DEBUG)
        //                    Log.w("tramFromStation", tramFromStation);

        val comingTramCursor = db.rawQuery(tramFromStation, null)
        comingTramCursor.moveToFirst()
        if (comingTramCursor.count > 0)
            try {
                do {
                    val departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"))
                    val arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"))
                    val line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"))

                    tramCentroSansovinoTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (comingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingTramCursor.close()
        db.close()

        return tramCentroSansovinoTimes
    }

    fun getAirportToHermada(context: Context, yesterdayForQuery: String, now: Date, operator: String, dayForThisQuery: String): List<Bus> {

        db = openDatabase(context)

        val tramCentroSansovinoTimes = LinkedList<Bus>()

        val tramFromStation = "SELECT t.trip_id,\n" +
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
                "  and r.route_id in (" + routeFor15AirportStation + ")\n" +
                "  and DATETIME(start_st.departure_time) " + operator + " DATETIME('" + databaseHourFormatter.format(subtractMinutesFromDate(4, now)) + "')\n" +
                "  and departure_stop_id in (" + airport + ")\n" +
                "  and arrival_stop_id in (" + viaHermadaStreetSide + ")\n" +
                "order by start_st.departure_time asc\t"

        if (BuildConfig.DEBUG)
            Log.w("tramFromStation", tramFromStation)

        val comingTramCursor = db.rawQuery(tramFromStation, null)
        comingTramCursor.moveToFirst()
        if (comingTramCursor.count > 0)
            try {
                do {
                    val departureStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_stop"))
                    val departureTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("departure_time"))
                    val arrivalStop = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_stop"))
                    val arrivalTime = comingTramCursor.getString(comingTramCursor.getColumnIndex("arrival_time"))
                    val line = comingTramCursor.getString(comingTramCursor.getColumnIndex("route_short_name"))

                    tramCentroSansovinoTimes.add(Bus(dateFormatter.parse(departureTime),
                            line,
                            departureStop,
                            arrivalStop,
                            dateFormatter.parse(arrivalTime)
                    ))

                } while (comingTramCursor.moveToNext())
            } catch (e: ParseException) {
                Log.e("Catchit", "Uff, cheppalle")
            }

        comingTramCursor.close()
        db.close()

        return tramCentroSansovinoTimes
    }

    /*
    *  Convenience method to add a specified number of minutes to a Date object
    *  From: http://stackoverflow.com/questions/9043981/how-to-add-minutes-to-my-date
    *  @param  minutes  The number of minutes to subtract
    *  @param  beforeTime  The time that will have minutes subtracted from it
    *  @return  A date object with the specified number of minutes added to it
    */
    fun subtractMinutesFromDate(minutes: Int, beforeTime: Date): java.util.Date {
        val ONE_MINUTE_IN_MILLIS: Long = 60000//millisecs

        val curTimeInMs = beforeTime.time
        val afterAddingMins = Date(curTimeInMs - minutes * ONE_MINUTE_IN_MILLIS)
        return afterAddingMins
    }

}
