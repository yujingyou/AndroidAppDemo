package com.itheima.mobileguard.services;

import java.lang.reflect.Method;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Address;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.itheima.mobileguard.db.dao.BlackNumberDao;
import com.itheima.mobileguard.db.dao.NumberAddressDao;

public class CallSmsSafeService extends Service {
	private InnerSmsReceiver receiver;
	private BlackNumberDao dao;
	//系统提供的电话管理器，电话管理的服务
	private TelephonyManager tm;
	private MyPhoneListener listener;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		dao = new BlackNumberDao(this);
		tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		listener = new MyPhoneListener();
		tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
		receiver  = new InnerSmsReceiver();
		IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		filter.setPriority(Integer.MAX_VALUE);
		registerReceiver(receiver, filter);
		super.onCreate();
	}
	
	private class MyPhoneListener extends PhoneStateListener{
		@Override
		public void onCallStateChanged(int state, final String incomingNumber) {
			super.onCallStateChanged(state, incomingNumber);
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE://空闲状态
				
				break;
			case TelephonyManager.CALL_STATE_RINGING://响铃状态
				String mode = dao.findBlockMode(incomingNumber);
				if("1".equals(mode)||"3".equals(mode)){
					Log.i("MyPhoneListener","挂断电话");
					//观察（另外一个应用程序数据库的变化）呼叫记录的变化，如果呼叫记录生成了，就把呼叫记录给删除掉。
					Uri uri = Uri.parse("content://call_log/calls");
					getContentResolver().registerContentObserver(uri, true, new CallLogObserver(new Handler(), incomingNumber));
					//用代码挂断电话。
					endCall();//电话挂断之后，会在另外一个应用程序里面生成呼叫记录。
					//清除黑名单号码产生的呼叫记录
					
				}
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK://接通状态
				
				break;
			}
		}
	}
	
	
	private class CallLogObserver extends ContentObserver{
		private String incomingNumber;
		public CallLogObserver(Handler handler,String incomingNumber) {
			super(handler);
			this.incomingNumber = incomingNumber;
		}
		//观察到数据库内容变化调用的方法
		@Override
		public void onChange(boolean selfChange) {
			Log.i("CallLogObserver","呼叫记录数据库的内容变化了。");
			getContentResolver().unregisterContentObserver(this);
			deleteCallLog(incomingNumber);
			super.onChange(selfChange);
		}
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(receiver);
		receiver = null;
		tm.listen(listener, PhoneStateListener.LISTEN_NONE);
		listener = null;
		super.onDestroy();
	}
	
	/**
	 * 清除呼叫记录
	 * @param incomingNumber
	 */
	public void deleteCallLog(String incomingNumber) {
		ContentResolver resolver = getContentResolver();
		Uri uri = Uri.parse("content://call_log/calls");
		resolver.delete(uri, "number=?", new String[]{incomingNumber});
	}

	/**
	 * 挂断电话
	 */
	public void endCall() {
		try {
			Class clazz = getClassLoader().loadClass("android.os.ServiceManager");
			Method method = clazz.getDeclaredMethod("getService", String.class);
			IBinder iBinder = (IBinder) method.invoke(null, TELEPHONY_SERVICE);
			ITelephony itelephony = ITelephony.Stub.asInterface(iBinder);
			itelephony.endCall();
			//开通呼叫转移 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class InnerSmsReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.i("InnerSmsReceiver","短信到来了。");
			//判断短信的发件人是否在黑名单列表里面，
			Object[] objs = (Object[]) intent.getExtras().get("pdus");
			for(Object obj :objs){
				SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj);
				String sender = smsMessage.getOriginatingAddress();
				String mode = dao.findBlockMode(sender);
				if("1".equals(mode)||"2".equals(mode)){
					Log.i("InnerSmsReceiver","黑名单短信被拦截。");
					abortBroadcast();//终止短信的广播 ，短信就被拦截 
				}
				//智能拦截。
				String body = smsMessage.getMessageBody();
				if(body.contains("发票")){ //你的头发票亮极了。
					Log.i("InnerSmsReceiver","拦截到垃圾发票短信，终止");
					abortBroadcast();//终止短信的广播 ，短信就被拦截 
				}
			}
			
		}
	}
	
	
}
