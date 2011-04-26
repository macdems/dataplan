package com.macdems.planactivator;

import java.util.Date;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Simple database access helper class. Defines the basic CRUD operations
 * for the application.
 */
public class DatabaseAdapter {

	public static final String DATABASE_NAME = "planactivator";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_PLANS = "plans";
    public static final String TABLE_CODES = "othercodes";

    public static final String KEY_ROWID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_USSD = "ussd";
    public static final String KEY_PERIOD = "period"; 	// validity period of the code
    public static final String KEY_AUTO = "auto";		// do we automatically call it from the alarm?
    public static final String KEY_DELAY = "delay";		// delay of auto-activation after/before validity expire
    public static final String KEY_LAST = "last";		// last time code used
    public static final String KEY_VALIDUNTIL = "valid";// valid until date (can be adjusted by user or SMS)

    private final Context mCtx;

    private OpenHelper mOpenHelper;
    private SQLiteDatabase mDb;
  
    
    /**
     * Helper class for accessing the database
     *
     */
    private static class OpenHelper extends SQLiteOpenHelper {

    	OpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE " + TABLE_PLANS + " ("
						 + KEY_ROWID+" INTEGER PRIMARY KEY AUTOINCREMENT, "
                         + KEY_USSD+" TEXT NOT NULL,"
                         + KEY_NAME+" TEXT NOT NULL,"
                         + KEY_PERIOD+" INTEGER,"
                         + KEY_AUTO+" INTEGER DEFAULT 0,"
                         + KEY_DELAY+" INTEGER DEFAULT 0,"
                         + KEY_LAST+" INTEGER,"
                         + KEY_VALIDUNTIL+" INTEGER)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS plans");
            onCreate(db);
        }
    }

    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public DatabaseAdapter(Context ctx) {
        this.mCtx = ctx;
    }


    /**
     * Open the notes database. If it cannot be opened, try to create a new
     * instance of the database. If it cannot be created, throw an exception to
     * signal the failure
     * 
     * @return this (self reference, allowing this to be chained in an
     *         initialization call)
     * @throws SQLException if the database could be neither opened or created
     */
    public DatabaseAdapter open() throws SQLException {
        mOpenHelper = new OpenHelper(mCtx);
        mDb = mOpenHelper.getWritableDatabase();
        return this;
    }

    
    /**
     * Closes the database
     */
    public void close() {
        mOpenHelper.close();
    }

    
    /**
     * Return a Cursor over the list of all plans in the database
     * 
     * @return Cursor over all plans
     */
    public Cursor fetchAllPlans() {
        return mDb.query(TABLE_PLANS, new String[] {KEY_ROWID, KEY_USSD, KEY_NAME, KEY_PERIOD,
        		                                    KEY_AUTO, KEY_DELAY, KEY_LAST, KEY_VALIDUNTIL},
        		         null, null, null, null, null);
    }

    
    /**
     * Return a Cursor over the list of all active plans in the database
     * 
     * @returnCursor over all active plans
     */
    public Cursor fetchActivePlans() {
        return mDb.query(TABLE_PLANS, new String[] {KEY_ROWID, KEY_USSD, KEY_NAME, KEY_PERIOD,
                									KEY_AUTO, KEY_DELAY, KEY_LAST, KEY_VALIDUNTIL},
                         KEY_AUTO + "= 1", null, null, null, null);
    }
    
    
    /**
     * Return a Cursor positioned at the plan that matches the given id
     * 
     * @param rowId id of the plan to retrieve
     * @return Cursor positioned to the matching plan, if found
     * @throws SQLException if plan could not be found/retrieved
     */    
    public Cursor fetchPlan(long rowId) throws SQLException {
        Cursor cursor =
        		mDb.query(true, TABLE_PLANS, new String[] {KEY_ROWID, KEY_USSD, KEY_NAME, KEY_PERIOD,
                		                                   KEY_AUTO, KEY_DELAY, KEY_LAST, KEY_VALIDUNTIL},
                		  KEY_ROWID + "=" + rowId, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }


    /**
     * Insert a new plan into database
     * 
     * @param name		name of the new plan
     * @param ussd		USSD code to call to activate the plan
     * @param period	validity period in seconds
     * @param auto		true if the plan should be auto-updated
     * @param delay		delay after/before validity period for auto-update
     * @return rowId or -1 if failed
     */
    public long createPlan(String name, String ussd, Long period, Boolean auto, Long delay) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_USSD, ussd);
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_PERIOD, period);
        initialValues.put(KEY_AUTO, auto?1:0);
        initialValues.put(KEY_DELAY, delay);

        return mDb.insert(TABLE_PLANS, null, initialValues);
    }

    
    /**
     * Delete a plan from the database
     * 
     * @param rowId id of the plan
     * @return true if deleted, false otherwise
     */
    public boolean deletePlan(long rowId) {
        return mDb.delete(TABLE_PLANS, KEY_ROWID + "=" + rowId, null) > 0;
    }

    
    /**
     * Update a plan in the database
     * 
     * @param rowId		id of the plan
     * @param name		name of the new plan
     * @param ussd		USSD code to call to activate the plan
     * @param period	validity period in seconds
     * @param auto		true if the plan should be auto-updated
     * @param delay		delay after/before validity period for auto-update
     * @return rowId or -1 if failed
     */
    public boolean updatePlan(long rowId, String name, String ussd, Long period, Boolean auto, Long delay) {
        ContentValues values = new ContentValues();
        values.put(KEY_USSD, ussd);
        values.put(KEY_NAME, name);
        values.put(KEY_PERIOD, period);
        values.put(KEY_AUTO, auto?1:0);
        values.put(KEY_DELAY, delay);

        return mDb.update(TABLE_PLANS, values, KEY_ROWID + "=" + rowId, null) > 0;
    }

    
    /**
     * Sets the LAST and VALIDUNTIL fields
     * @param rowId		id of the plan
     * @param last		date of the last plan activation
     * @return
     */
    public long updateDates(long rowId, Date last)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_LAST, last.getTime());
        
        Cursor cursor = fetchPlan(rowId);
        Long period = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_PERIOD));
        cursor.deactivate();
        long validuntil;
    	if (period != null) {
    		validuntil = last.getTime() + 1000*period;
			values.put(KEY_VALIDUNTIL, validuntil);
    	} else {
    		validuntil = 0;
    	}    		

    	if (mDb.update(TABLE_PLANS, values, KEY_ROWID + "=" + rowId, null) > 0)
    		return validuntil;
    	else
    		return -1;
    }
    

    
    /**
     * Sets VALIDUNTIL field
     * @param rowId		id of the plan
     * @param valid		date of the plan validity
     * @return
     */
    public boolean updateValidityDate(long rowId, Date valid)
    {
        ContentValues values = new ContentValues();
        values.put(KEY_VALIDUNTIL, valid.getTime());
    	return mDb.update(TABLE_PLANS, values, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
