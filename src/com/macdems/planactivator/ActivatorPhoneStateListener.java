package com.macdems.planactivator;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

public class ActivatorPhoneStateListener extends PhoneStateListener {

	private Context mContext;
	private long mRowId;
	private boolean mOnline;
	private boolean mIdle;
	
	/**
	 * @param context	application context
	 * @param rowId		id of the plan
	 */
	public ActivatorPhoneStateListener(Context context, long rowId) {
		super();
		this.mContext = context;
		this.mRowId = rowId;
		this.mOnline = false;
		this.mIdle = false;
	}

	
	/**
	 * Fire the plan activation and stop listening
	 */
	void fireActivation() {
		if (mOnline && mIdle) {
			ActivateEvent.activate(mContext, mRowId);
	        TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
	        telephony.listen(this, LISTEN_NONE);
		}
	}

	@Override
	public void onServiceStateChanged(ServiceState serviceState) {
		mOnline = (serviceState.getState() == ServiceState.STATE_IN_SERVICE);
		fireActivation();
	}
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		mIdle = (state == TelephonyManager.CALL_STATE_IDLE);
		fireActivation();
	}

}
