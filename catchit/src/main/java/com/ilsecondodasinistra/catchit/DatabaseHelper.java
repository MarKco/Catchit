package com.ilsecondodasinistra.catchit;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by marco on 17/01/17.
 */

public class DatabaseHelper {

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

    static SimpleDateFormat databaseHourFormatter = new SimpleDateFormat("HH:mm:ss"); //DateFormatter four hours.minutes.seconds
    static private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss"); //DateFormatter four hours.minutes.seconds

    static SQLiteDatabase db;

    private static SQLiteDatabase openDatabase(Context context) {
        MyDatabase dbHelper = new MyDatabase(context);
        return dbHelper.getReadableDatabase();
    }

    public static SQLiteDatabase getDb() {
        return db;
    }

    private static void closeDatabase(SQLiteDatabase db) {
        db.close();
    }

    public static List<Bus> getMoreTramToVenice(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramToVeniceTimes = new LinkedList<>();

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

        if(BuildConfig.DEBUG)
            Log.w("MoreTramToVenice", moreTramToVenice);

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
        db.close();

        return tramToVeniceTimes;
    }

    public static List<Bus> getTramToVenice(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramToVeniceTimes = new LinkedList<>();

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

//            if (BuildConfig.DEBUG)
//                Log.w("TramToVenice", tramToVenice);

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
        db.close();

        return tramToVeniceTimes;
    }

    public static List<Bus> getMoreTramFromVenice(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {
        db = openDatabase(context);

        List<Bus> tramToMestreTimes = new LinkedList<>();

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
        db.close();

        return tramToMestreTimes;
    }

    public static List<Bus> getTramFromVenice(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramToMestreTimes = new LinkedList<>();

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
            if (comingCursor.getCount() > 0)
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
        db.close();

        return tramToMestreTimes;
    }

    public static List<Bus> getTramToStation(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramToStationTimes = new LinkedList<>();

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
        if (leavingTramCursor.getCount() > 0)
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
        db.close();

        return tramToStationTimes;
    }

    public static List<Bus> getStationToSansovino(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramToMestreCityCenterTimes = new LinkedList<>();

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
        if (comingTramCursor.getCount() > 0)
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
        db.close();

        return tramToMestreCityCenterTimes;
    }

    public static List<Bus> getSansovinoToStation(Context context, String dayForQuery, String tomorrowForQuery, Date now, String operator, String dayForThisQuery) {

        db = openDatabase(context);

        List<Bus> tramSansovinoCentroTimes = new LinkedList<>();

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

//                if(BuildConfig.DEBUG)
//                    Log.w("TramToVenice", tramSansovinoToCentro);

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
        db.close();
//                    if(BuildConfig.DEBUG)
//                        Log.w("TramSansovinoToCentro", tramSansovinoToCentro);

        return tramSansovinoCentroTimes;

    }

    /*
    *  Convenience method to add a specified number of minutes to a Date object
    *  From: http://stackoverflow.com/questions/9043981/how-to-add-minutes-to-my-date
    *  @param  minutes  The number of minutes to subtract
    *  @param  beforeTime  The time that will have minutes subtracted from it
    *  @return  A date object with the specified number of minutes added to it
    */
    public static java.util.Date subtractMinutesFromDate(int minutes, Date beforeTime) {
        final long ONE_MINUTE_IN_MILLIS = 60000;//millisecs

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs - (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }

}
