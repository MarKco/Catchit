package com.ilsecondodasinistra.catchit.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.ilsecondodasinistra.catchit.database.stops;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table stops.
*/
public class stopsDao extends AbstractDao<stops, Integer> {

    public static final String TABLENAME = "stops";

    /**
     * Properties of entity stops.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Stop_id = new Property(0, int.class, "stop_id", true, "STOP_ID");
        public final static Property Stop_code = new Property(1, Integer.class, "stop_code", false, "STOP_CODE");
        public final static Property Stop_name = new Property(2, String.class, "stop_name", false, "STOP_NAME");
        public final static Property Stop_desc = new Property(3, String.class, "stop_desc", false, "STOP_DESC");
        public final static Property Stop_lat = new Property(4, Double.class, "stop_lat", false, "STOP_LAT");
        public final static Property Stop_lon = new Property(5, Double.class, "stop_lon", false, "STOP_LON");
        public final static Property Zone_id = new Property(6, Integer.class, "zone_id", false, "ZONE_ID");
        public final static Property Stop_url = new Property(7, String.class, "stop_url", false, "STOP_URL");
        public final static Property Location_type = new Property(8, String.class, "location_type", false, "LOCATION_TYPE");
        public final static Property Parent_station = new Property(9, Integer.class, "parent_station", false, "PARENT_STATION");
        public final static Property Stop_timezone = new Property(10, String.class, "stop_timezone", false, "STOP_TIMEZONE");
        public final static Property Wheelchair_boarding = new Property(11, String.class, "wheelchair_boarding", false, "WHEELCHAIR_BOARDING");
    };


    public stopsDao(DaoConfig config) {
        super(config);
    }
    
    public stopsDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'stops' (" + //
                "'STOP_ID' INTEGER PRIMARY KEY NOT NULL UNIQUE ," + // 0: stop_id
                "'STOP_CODE' INTEGER," + // 1: stop_code
                "'STOP_NAME' TEXT," + // 2: stop_name
                "'STOP_DESC' TEXT," + // 3: stop_desc
                "'STOP_LAT' REAL," + // 4: stop_lat
                "'STOP_LON' REAL," + // 5: stop_lon
                "'ZONE_ID' INTEGER," + // 6: zone_id
                "'STOP_URL' TEXT," + // 7: stop_url
                "'LOCATION_TYPE' TEXT," + // 8: location_type
                "'PARENT_STATION' INTEGER," + // 9: parent_station
                "'STOP_TIMEZONE' TEXT," + // 10: stop_timezone
                "'WHEELCHAIR_BOARDING' TEXT);"); // 11: wheelchair_boarding
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'stops'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, stops entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getStop_id());
 
        Integer stop_code = entity.getStop_code();
        if (stop_code != null) {
            stmt.bindLong(2, stop_code);
        }
 
        String stop_name = entity.getStop_name();
        if (stop_name != null) {
            stmt.bindString(3, stop_name);
        }
 
        String stop_desc = entity.getStop_desc();
        if (stop_desc != null) {
            stmt.bindString(4, stop_desc);
        }
 
        Double stop_lat = entity.getStop_lat();
        if (stop_lat != null) {
            stmt.bindDouble(5, stop_lat);
        }
 
        Double stop_lon = entity.getStop_lon();
        if (stop_lon != null) {
            stmt.bindDouble(6, stop_lon);
        }
 
        Integer zone_id = entity.getZone_id();
        if (zone_id != null) {
            stmt.bindLong(7, zone_id);
        }
 
        String stop_url = entity.getStop_url();
        if (stop_url != null) {
            stmt.bindString(8, stop_url);
        }
 
        String location_type = entity.getLocation_type();
        if (location_type != null) {
            stmt.bindString(9, location_type);
        }
 
        Integer parent_station = entity.getParent_station();
        if (parent_station != null) {
            stmt.bindLong(10, parent_station);
        }
 
        String stop_timezone = entity.getStop_timezone();
        if (stop_timezone != null) {
            stmt.bindString(11, stop_timezone);
        }
 
        String wheelchair_boarding = entity.getWheelchair_boarding();
        if (wheelchair_boarding != null) {
            stmt.bindString(12, wheelchair_boarding);
        }
    }

    /** @inheritdoc */
    @Override
    public Integer readKey(Cursor cursor, int offset) {
        return cursor.getInt(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public stops readEntity(Cursor cursor, int offset) {
        stops entity = new stops( //
            cursor.getInt(offset + 0), // stop_id
            cursor.isNull(offset + 1) ? null : cursor.getInt(offset + 1), // stop_code
            cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2), // stop_name
            cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3), // stop_desc
            cursor.isNull(offset + 4) ? null : cursor.getDouble(offset + 4), // stop_lat
            cursor.isNull(offset + 5) ? null : cursor.getDouble(offset + 5), // stop_lon
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6), // zone_id
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7), // stop_url
            cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8), // location_type
            cursor.isNull(offset + 9) ? null : cursor.getInt(offset + 9), // parent_station
            cursor.isNull(offset + 10) ? null : cursor.getString(offset + 10), // stop_timezone
            cursor.isNull(offset + 11) ? null : cursor.getString(offset + 11) // wheelchair_boarding
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, stops entity, int offset) {
        entity.setStop_id(cursor.getInt(offset + 0));
        entity.setStop_code(cursor.isNull(offset + 1) ? null : cursor.getInt(offset + 1));
        entity.setStop_name(cursor.isNull(offset + 2) ? null : cursor.getString(offset + 2));
        entity.setStop_desc(cursor.isNull(offset + 3) ? null : cursor.getString(offset + 3));
        entity.setStop_lat(cursor.isNull(offset + 4) ? null : cursor.getDouble(offset + 4));
        entity.setStop_lon(cursor.isNull(offset + 5) ? null : cursor.getDouble(offset + 5));
        entity.setZone_id(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setStop_url(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
        entity.setLocation_type(cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8));
        entity.setParent_station(cursor.isNull(offset + 9) ? null : cursor.getInt(offset + 9));
        entity.setStop_timezone(cursor.isNull(offset + 10) ? null : cursor.getString(offset + 10));
        entity.setWheelchair_boarding(cursor.isNull(offset + 11) ? null : cursor.getString(offset + 11));
     }
    
    /** @inheritdoc */
    @Override
    protected Integer updateKeyAfterInsert(stops entity, long rowId) {
        return entity.getStop_id();
    }
    
    /** @inheritdoc */
    @Override
    public Integer getKey(stops entity) {
        if(entity != null) {
            return entity.getStop_id();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
