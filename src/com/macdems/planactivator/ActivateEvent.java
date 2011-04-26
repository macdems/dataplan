/**
 * Class responsible for activating given packet
 * as a result of user action or scheduled alarm.
 */
package com.macdems.planactivator;

import java.util.Date;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class ActivateEvent extends BroadcastReceiver {

	/**
	 * Schedule the next automatic call
	 * 
	 * @param context	application context
	 * @param rowId		id of the plan to schedule
	 * @param next		time of the closest validity expire
	 * @param delay		delay before/after the expire of the reactivation (in seconds)
	 * @return
	 */
	public static void setNewAlarm(Context context, long rowId, long next, Long delay) {
		if (delay != null) next += (delay * 1000);

		Intent intent = new Intent(context, ActivateEvent.class);
		intent.putExtra("id", rowId);
		
        PendingIntent sender = PendingIntent.getBroadcast(context, (int)rowId, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, next, sender);
	}

	/**
	 * Cancel the scheduled alarm
	 * 
	 * @param context	application context
	 * @param rowId		id of the plan to schedule
	 */
	public static void cancelAlarm(Context context, long rowId) {
		Intent intent = new Intent(context, ActivateEvent.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, (int)rowId, intent, 0);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        alarm.cancel(sender);
	}
	
	
	
	/** Send USSD code activating the service and schedule next call if necessary
	 * 
	 * @param context	application context
	 * @param rowId		id of the plan to fire
	 */
	protected static void activate(Context context, long rowId) {
        DatabaseAdapter dbHelper = new DatabaseAdapter(context);
        dbHelper.open();
        Cursor cursor = dbHelper.fetchPlan(rowId);
        if (cursor.getCount() == 0) return; // apparently the plan has been deleted so nothing is left to do
        
        String ussd = Uri.encode(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_USSD)));
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+ussd));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { 
          context.startActivity(intent);
        } catch (Exception eExcept) {
            Toast.makeText(context, eExcept.toString(), Toast.LENGTH_LONG).show();
        } 
        
        // TODO notify user that we have sent the code (?)
        
        Date now = new Date();
        long next = dbHelper.updateDates(rowId, now);
        
        boolean auto = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_AUTO)) == 1;
		if (auto && next > 0) {
			Long delay = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_DELAY));
			setNewAlarm(context, rowId, next, delay);
		}		

		cursor.deactivate();
	}

	
	/** Begin activation process
	 * 
	 * Set listeners for the phone state to make sure we can send the code and nothing will interrupt us
	 * 
	 * @param context	application context
	 * @param rowId		id of the plan to fire
	 */
	protected static void beginActivation(Context context, long rowId) {
		ActivatorPhoneStateListener phoneListener = new ActivatorPhoneStateListener(context, rowId);
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE|PhoneStateListener.LISTEN_CALL_STATE);
	}
	
	
	/* Called either on user request or by alarm scheduler */
	@Override
	public void onReceive(Context context, Intent intent) {
		long rowId = intent.getLongExtra("id", -1);
		if (rowId != -1) beginActivation(context, rowId);
	}

}
