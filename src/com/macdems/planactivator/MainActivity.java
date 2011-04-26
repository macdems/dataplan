package com.macdems.planactivator;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class MainActivity extends ListActivity {

	private static final int ACTIVITY_CREATEPACKET = 0;
	private static final int ACTIVITY_EDITPACKET = 1;
	
	
    /** Object for accessing the database */
	private DatabaseAdapter mDbHelper;

	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.packet_list);
        registerForContextMenu(getListView());
        
        mDbHelper = new DatabaseAdapter(this);
        mDbHelper.open();
        fillData();
    }

    
    /** Fill the list with defined packets */
    private void fillData() {
        // Get all of the notes from the database and create the item list
        Cursor cursor = mDbHelper.fetchAllPlans();
        startManagingCursor(cursor);

        //int[] to = new int[] { android.R.id.text1, android.R.id.text2 };
        //String[] from = new String[] { PacketActivatorDbHelper.KEY_TITLE, PacketActivatorDbHelper.KEY_CODE };
        int[] to = new int[] { android.R.id.text1 };
        String[] from = new String[] { DatabaseAdapter.KEY_NAME };
        
        // Now create an array adapter and set it to display using our row
        SimpleCursorAdapter packets = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, cursor, from, to);
        setListAdapter(packets);
	}

   
    
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean result = super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
        return result;        
    }


	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	
    	case R.id.newplan:	// Start an activity for creating a new packet
    		Intent intent = new Intent(this, PlanEditActivity.class);
    		startActivityForResult(intent, ACTIVITY_CREATEPACKET);
    		return true;
    	}
    	
        return super.onOptionsItemSelected(item);
    }

    
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        final long rowId = id;
        final Context context = (Context) this;
        
        Cursor cursor = mDbHelper.fetchPlan(id);
        
		AlertDialog dialog = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_Light)).create();
		
		dialog.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_NAME)));
		
		String text =
				getString(R.string.ussd_code) + " " +
				cursor.getString(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_USSD)) + "\n";
		
		Long period =  cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_PERIOD));
		
		if (period != null) {
			text += getString(R.string.validity_period) + " ";
			int i = 0;
			for (long unit : PlanEditActivity.Units) {
				if (period % unit == 0) {
					Long p = period / unit;
					text += p.toString() + " " + getResources().getStringArray(R.array.time_units)[i];
					break;
				}
				i++;
			}
		}
		
		Long last = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_LAST));
		if (last != null && last != 0) {
			text += "\n" + getString(R.string.last_used) + " " + 
				    DateFormat.getDateFormat(this).format(last) + " " + DateFormat.getTimeFormat(this).format(last);
		}
		
		Long valid = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_VALIDUNTIL));
		if (valid != null && valid != 0) {
			text += "\n" + getString(R.string.valid_until) + " " + 
			        DateFormat.getDateFormat(this).format(valid) + " " + DateFormat.getTimeFormat(this).format(valid);
		}
		
		if (cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseAdapter.KEY_AUTO)) != 0)
			text += "\n" + getString(R.string.automatic);
		
		dialog.setMessage(text);
		
		dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.activate), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
	       		Intent activateIntent = new Intent(context, ActivateEvent.class);
	       		activateIntent.putExtra("id", rowId);
	       		sendBroadcast(activateIntent);
	       		dialog.dismiss();
			}}
		);

		dialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.edit), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
	       		Intent editIntent = new Intent(context, PlanEditActivity.class);
	            editIntent.putExtra(DatabaseAdapter.KEY_ROWID, rowId);
	            startActivityForResult(editIntent, ACTIVITY_EDITPACKET);
	       		dialog.dismiss();
			}}
		);

		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.delete), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
	       		dialog.dismiss();
	    		AlertDialog alert = new AlertDialog.Builder(context).create();
	    		alert.setMessage(getString(R.string.delete_plan));
	    		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				dialog.dismiss();
	    	    		mDbHelper.deletePlan(rowId);
	    	    		fillData();
	    			}}
	    		);
	    		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
	    			public void onClick(DialogInterface dialog, int which) {
	    				dialog.dismiss();
	    			}}
	    		);		 
	    		alert.show();
			}}
		);

		dialog.show();
    }

    
     
    @Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_context, menu);
	}


    @Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final long selectedItem = info.id;
		
       	switch (item.getItemId()) {
    	
       	case R.id.activateplan:
       		Intent activateIntent = new Intent(this, ActivateEvent.class);
       		activateIntent.putExtra("id", selectedItem);
       		sendBroadcast(activateIntent);
       		break;
       	
       	case R.id.editplan:
            Intent editIntent = new Intent(this, PlanEditActivity.class);
            editIntent.putExtra(DatabaseAdapter.KEY_ROWID, selectedItem);
            startActivityForResult(editIntent, ACTIVITY_EDITPACKET);
            break;
    	
       	case R.id.deleteplan:	// Delete selected packet
    		AlertDialog alert = new AlertDialog.Builder(this).create();
    		alert.setMessage(getString(R.string.delete_plan));
    		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    	    		mDbHelper.deletePlan(selectedItem);
    	    		fillData();
    			}}
    		);
    		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int which) {
    				dialog.dismiss();
    			}}
    		);		 
    		alert.show();
    		break;
       	}
    	
 		return super.onContextItemSelected(item);
	}


    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        fillData();
    }
    
    
}
