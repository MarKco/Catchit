package com.ilsecondodasinistra.catchit.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.ilsecondodasinistra.catchit.database.calendar;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table calendar.
*/
public class calendarDao extends AbstractDao<calendar, Integer> {

    public static final String TABLENAME = "calendar";

    /**
     * Properties of entity calendar.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Service_id = new Property(0, int.class, "service_id", true, "SERVICE_ID");
        public final static Property Monday = new Property(1, Integer.class, "monday", false, "MONDAY");
        public final static Property Tuesday = new Property(2, Integer.class, "tuesday", false, "TUESDAY");
        public final static Property Wednesday = new Property(3, Integer.class, "wednesday", false, "WEDNESDAY");
        public final static Property Thursday = new Property(4, Integer.class, "thursday", false, "THURSDAY");
        public final static Property Friday = new Property(5, Integer.class, "friday", false, "FRIDAY");
        public final static Property Saturday = new Property(6, Integer.class, "saturday", false, "SATURDAY");
        public final static Property Sunday = new Property(7, Integer.class, "sunday", false, "SUNDAY");
        public final static Property Start_date = new Property(8, String.class, "start_date", false, "START_DATE");
        public final static Property End_date = new Property(9, String.class, "end_date", false, "END_DATE");
    };


    public calendarDao(DaoConfig config) {
        super(config);
    }
    
    public calendarDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'calendar' (" + //
                "'SERVICE_ID' INTEGER PRIMARY KEY NOT NULL UNIQUE ," + // 0: service_id
                "'MONDAY' INTEGER," + // 1: monday
                "'TUESDAY' INTEGER," + // 2: tuesday
                "'WEDNESDAY' INTEGER," + // 3: wednesday
                "'THURSDAY' INTEGER," + // 4: thursday
                "'FRIDAY' INTEGER," + // 5: friday
                "'SATURDAY' INTEGER," + // 6: saturday
                "'SUNDAY' INTEGER," + // 7: sunday
                "'START_DATE' TEXT," + // 8: start_date
                "'END_DATE' TEXT);"); // 9: end_date
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'calendar'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, calendar entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getService_id());
 
        Integer monday = entity.getMonday();
        if (monday != null) {
            stmt.bindLong(2, monday);
        }
 
        Integer tuesday = entity.getTuesday();
        if (tuesday != null) {
            stmt.bindLong(3, tuesday);
        }
 
        Integer wednesday = entity.getWednesday();
        if (wednesday != null) {
            stmt.bindLong(4, wednesday);
        }
 
        Integer thursday = entity.getThursday();
        if (thursday != null) {
            stmt.bindLong(5, thursday);
        }
 
        Integer friday = entity.getFriday();
        if (friday != null) {
            stmt.bindLong(6, friday);
        }
 
        Integer saturday = entity.getSaturday();
        if (saturday != null) {
            stmt.bindLong(7, saturday);
        }
 
        Integer sunday = entity.getSunday();
        if (sunday != null) {
            stmt.bindLong(8, sunday);
        }
 
        String start_date = entity.getStart_date();
        if (start_date != null) {
            stmt.bindString(9, start_date);
        }
 
        String end_date = entity.getEnd_date();
        if (end_date != null) {
            stmt.bindString(10, end_date);
        }
    }

    /** @inheritdoc */
    @Override
    public Integer readKey(Cursor cursor, int offset) {
        return cursor.getInt(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public calendar readEntity(Cursor cursor, int offset) {
        calendar entity = new calendar( //
            cursor.getInt(offset + 0), // service_id
            cursor.isNull(offset + 1) ? null : cursor.getInt(offset + 1), // monday
            cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2), // tuesday
            cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3), // wednesday
            cursor.isNull(offset + 4) ? null : cursor.getInt(offset + 4), // thursday
            cursor.isNull(offset + 5) ? null : cursor.getInt(offset + 5), // friday
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6), // saturday
            cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7), // sunday
            cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8), // start_date
            cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9) // end_date
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, calendar entity, int offset) {
        entity.setService_id(cursor.getInt(offset + 0));
        entity.setMonday(cursor.isNull(offset + 1) ? null : cursor.getInt(offset + 1));
        entity.setTuesday(cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2));
        entity.setWednesday(cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3));
        entity.setThursday(cursor.isNull(offset + 4) ? null : cursor.getInt(offset + 4));
        entity.setFriday(cursor.isNull(offset + 5) ? null : cursor.getInt(offset + 5));
        entity.setSaturday(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setSunday(cursor.isNull(offset + 7) ? null : cursor.getInt(offset + 7));
        entity.setStart_date(cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8));
        entity.setEnd_date(cursor.isNull(offset + 9) ? null : cursor.getString(offset + 9));
     }
    
    /** @inheritdoc */
    @Override
    protected Integer updateKeyAfterInsert(calendar entity, long rowId) {
        return entity.getService_id();
    }
    
    /** @inheritdoc */
    @Override
    public Integer getKey(calendar entity) {
        if(entity != null) {
            return entity.getService_id();
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
