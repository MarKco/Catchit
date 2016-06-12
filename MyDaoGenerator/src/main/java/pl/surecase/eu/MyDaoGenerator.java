package pl.surecase.eu;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Index;
import de.greenrobot.daogenerator.Schema;

public class MyDaoGenerator extends AbstractGeneratorHelper  {

    //	Main =)
    public static void main(String[] args) {
        (new MyDaoGenerator()).createClasses();
    }


    public MyDaoGenerator() {
        this(1, "com.ilsecondodasinistra.catchit");
    }

    public MyDaoGenerator(int version, String base_package_name) {
        super(version, base_package_name);

        addEntity("Trips", createTrips());
        addEntity("Stops", createStops());
        addEntity("StopTimes", createStopTimes());
        addEntity("Routes", createRoutes());
        addEntity("CalendarDates", createCalendarDates());
        addEntity("Calendar", createCalendar());
        addEntity("Agency", createAgency());
    }

    private Entity createTrips() {

        Entity trips = createEntity("trips", "trips");

        trips.addIntProperty("route_id").notNull();
        trips.addIntProperty("service_id").notNull();
        trips.addIntProperty("trip_id").notNull().primaryKey().unique();
        trips.addStringProperty("trip_headsign");
        trips.addStringProperty("trip_short_name");
        trips.addIntProperty("direction_id");
        trips.addStringProperty("block_id");
        trips.addIntProperty("shape_id");
        trips.addIntProperty("wheelchair_accessible");

        return trips;
    }

    private Entity createStops() {

        Entity stops = createEntity("stops", "stops");
        stops.addIntProperty("stop_id").notNull().primaryKey().unique();
        stops.addIntProperty("stop_code");
        stops.addStringProperty("stop_name");
        stops.addStringProperty("stop_desc");
        stops.addDoubleProperty("stop_lat");
        stops.addDoubleProperty("stop_lon");
        stops.addIntProperty("zone_id");
        stops.addStringProperty("stop_url");
        stops.addStringProperty("location_type");
        stops.addIntProperty("parent_station");
        stops.addStringProperty("stop_timezone");
        stops.addStringProperty("wheelchair_boarding");

        return stops;
    }

    private Entity createStopTimes() {

        Entity stopTimes = createEntity("stopTimes", "stop_times");
        stopTimes.addIntProperty("trip_id");
        stopTimes.addStringProperty("arrival_time").notNull();
        stopTimes.addStringProperty("departure_time").notNull();
        stopTimes.addIntProperty("stop_id");
        stopTimes.addIntProperty("stop_sequence").notNull().unique().primaryKey();
        stopTimes.addStringProperty("stop_headsign");
        stopTimes.addIntProperty("pickup_type");
        stopTimes.addIntProperty("drop_off_type");
        stopTimes.addFloatProperty("shape_dist_traveled");

        return stopTimes;
    }

    private Entity createRoutes() {

        Entity routes = createEntity("routes", "routes");
        routes.addIntProperty("route_id").unique().primaryKey().notNull();
        routes.addIntProperty("agency_id");
        routes.addStringProperty("route_short_name");
        routes.addStringProperty("route_long_name");
        routes.addStringProperty("route_desc");
        routes.addIntProperty("route_type");
        routes.addStringProperty("route_url");
        routes.addStringProperty("route_color");
        routes.addStringProperty("route_text_color");

        return routes;
    }

    private Entity createCalendarDates() {

        Entity calendarDates = createEntity("calendarDates", "calendar_dates");
        calendarDates.addIntProperty("service_id");
        calendarDates.addStringProperty("date");
        calendarDates.addIntProperty("exception_type");

        return calendarDates;
    }

    private Entity createCalendar() {

        Entity calendar = createEntity("calendar", "calendar");
        calendar.addIntProperty("service_id").primaryKey().notNull().unique();
        calendar.addIntProperty("monday");
        calendar.addIntProperty("tuesday");
        calendar.addIntProperty("wednesday");
        calendar.addIntProperty("thursday");
        calendar.addIntProperty("friday");
        calendar.addIntProperty("saturday");
        calendar.addIntProperty("sunday");
        calendar.addStringProperty("start_date");
        calendar.addStringProperty("end_date");

        return calendar;
    }

    private Entity createAgency() {

        Entity agency = createEntity("agency", "agency");
        agency.addIntProperty("agency_id").notNull().primaryKey().unique();
        agency.addStringProperty("agency_name");
        agency.addStringProperty("agency_url");
        agency.addStringProperty("agency_timezone");
        agency.addStringProperty("agency_lang");
        agency.addStringProperty("agency_phone");
        agency.addStringProperty("agency_fare_url");

        return agency;
    }

}
