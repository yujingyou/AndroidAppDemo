package com.itheima.mobileguard.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.itheima.mobileguard.R;
import com.itheima.mobileguard.services.CallSmsSafeService;
import com.itheima.mobileguard.services.ShowLocationService;
import com.itheima.mobileguard.services.WatchDogService;
import com.itheima.mobileguard.ui.SettingView;
import com.itheima.mobileguard.utils.SystemInfoUtils;

public class SettingCenterActivity extends Activity {

	private static final String[] items = { "半透明", "活力橙", "卫士蓝", "金属灰", "苹果绿" };

	private SharedPreferences sp;
	private SettingView sv_autoupdate;
	/**
	 * 黑名单
	 */
	private SettingView sv_blacknumber;
	private Intent callSmsSafeIntent;
	/**
	 * 归属地显示
	 */
	private SettingView sv_showaddress;
	private Intent showAddressIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		// 显示的是默认的风格
		tv_title_style = (TextView) findViewById(R.id.tv_title_style);
		sp = getSharedPreferences("config", MODE_PRIVATE);
		int which = sp.getInt("which", 0);
		tv_title_style.setText(items[which]);

		sv_autoupdate = (SettingView) findViewById(R.id.sv_autoupdate);
		sv_autoupdate.setChecked(sp.getBoolean("autoupdate", false));
		sv_autoupdate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				System.out.println("hahah ，被点击了。");
				Editor editor = sp.edit();
				// 判断勾选的状态。
				if (sv_autoupdate.isChecked()) {
					sv_autoupdate.setChecked(false);
					editor.putBoolean("autoupdate", false);
				} else {
					sv_autoupdate.setChecked(true);
					editor.putBoolean("autoupdate", true);
				}
				editor.commit();
			}
		});
		// 黑名单
		sv_blacknumber = (SettingView) findViewById(R.id.sv_blacknumber);
		callSmsSafeIntent = new Intent(this, CallSmsSafeService.class);
		sv_blacknumber.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (sv_blacknumber.isChecked()) {
					sv_blacknumber.setChecked(false);
					// 停止拦截服务
					stopService(callSmsSafeIntent);
				} else {
					sv_blacknumber.setChecked(true);
					// 开启拦截服务
					startService(callSmsSafeIntent);
				}
			}
		});

		// 看萌狗

		sv_watch_dog = (SettingView) findViewById(R.id.sv_watch_dog);
		watchDogIntent = new Intent(this, WatchDogService.class);
		sv_watch_dog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (sv_watch_dog.isChecked()) {
					sv_watch_dog.setChecked(false);
					// 停止拦截服务
					stopService(watchDogIntent);
				} else {
					sv_watch_dog.setChecked(true);
					// 开启拦截服务
					startService(watchDogIntent);
				}
			}
		});

		// 归属地
		sv_showaddress = (SettingView) findViewById(R.id.sv_showaddress);
		showAddressIntent = new Intent(this, ShowLocationService.class);
		sv_showaddress.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (sv_showaddress.isChecked()) {
					sv_showaddress.setChecked(false);
					// 停止拦截服务
					stopService(showAddressIntent);
				} else {
					sv_showaddress.setChecked(true);
					// 开启拦截服务
					startService(showAddressIntent);
				}
			}
		});
	}

	@Override
	protected void onStart() {
		// 判断服务的运行状态
		boolean running = SystemInfoUtils.isServiceRunning(this,
				"com.itheima.mobileguard.services.CallSmsSafeService");
		if (running) {
			sv_blacknumber.setChecked(true);
		} else {
			sv_blacknumber.setChecked(false);
		}

		running = SystemInfoUtils.isServiceRunning(this,
				"com.itheima.mobileguard.services.ShowLocationService");
		if (running) {
			sv_showaddress.setChecked(true);
		} else {
			sv_showaddress.setChecked(false);
		}
		
		running = SystemInfoUtils.isServiceRunning(this,
				"com.itheima.mobileguard.services.WatchDogService");
		if (running) {
			sv_watch_dog.setChecked(true);
		} else {
			sv_watch_dog.setChecked(false);
		}
		
		
		
		
		super.onStart();
	}

	private TextView tv_title_style;

	private SettingView sv_watch_dog;

	private Intent watchDogIntent;

	/**
	 * 修改归属地显示的背景风格
	 * 
	 * @param view
	 */
	public void changeBgStyle(View view) {
		AlertDialog.Builder builder = new Builder(this);
		builder.setIcon(R.drawable.main_icon_36);
		builder.setSingleChoiceItems(items, sp.getInt("which", 0),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Editor editor = sp.edit();
						editor.putInt("which", which);
						editor.commit();
						tv_title_style.setText(items[which]);
						dialog.dismiss();
					}
				});
		builder.setTitle("归属地提示框风格");
		builder.show();
	}
	

}
