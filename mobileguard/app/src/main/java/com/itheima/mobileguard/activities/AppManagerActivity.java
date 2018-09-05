package com.itheima.mobileguard.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.itheima.mobileguard.R;
import com.itheima.mobileguard.domain.AppInfo;
import com.itheima.mobileguard.engine.AppInfoParser;
import com.stericson.RootTools.RootTools;

public class AppManagerActivity extends Activity implements OnClickListener {
	public static final String TAG = "AppManagerActivity";
	private TextView tv_avail_rom;
	private TextView tv_avail_sd;
	private LinearLayout ll_loading;
	private Button button;
	private TextView textView = null;    
	
	/**
	 * 使用的应用程序信息集合
	 */
	private List<AppInfo> infos;
	/**
	 * 用户程序集合
	 */
	private List<AppInfo> userAppInfos;

	/**
	 * 系统程序集合
	 */
	private List<AppInfo> systemAppInfos;
         
	
	
	private TextView tv_appsize_lable;

	private ListView lv_appmanger;

	private LinearLayout ll_start;
	private LinearLayout ll_share;
	private LinearLayout ll_uninstall;
	private LinearLayout ll_setting;

	/**
	 * 被点击的条目对应的，appinfo对象
	 */
	private AppInfo clickedAppInfo;

	/**
	 * 悬浮窗体
	 */
	private PopupWindow popupwindow;

	private UninstallReceiver receiver;

	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			// 隐藏正在加载的界面
			ll_loading.setVisibility(View.INVISIBLE);
			lv_appmanger.setAdapter(new AppManagerAdapter());
		};
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_manager);
		tv_appsize_lable = (TextView) findViewById(R.id.tv_appsize_lable);
		ll_loading = (LinearLayout) findViewById(R.id.ll_loading);
		tv_avail_rom = (TextView) findViewById(R.id.tv_avail_rom);
		tv_avail_sd = (TextView) findViewById(R.id.tv_avail_sd);
		lv_appmanger = (ListView) findViewById(R.id.lv_appmanger);
		long avail_sd = Environment.getExternalStorageDirectory()
				.getFreeSpace();
		long avail_rom = Environment.getDataDirectory().getFreeSpace();
		String str_avail_sd = Formatter.formatFileSize(this, avail_sd);
		String str_avail_rom = Formatter.formatFileSize(this, avail_rom);
		tv_avail_rom.setText("剩余手机内部：" + str_avail_rom);
		tv_avail_sd.setText("剩余SD卡：" + str_avail_sd);

		fillData();
		// 给listview设置一个滚动状态的监听器
		lv_appmanger.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {

			}

			// 当listview被滚动的时候调用的方法
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				dismissPopupWindow();
				if (userAppInfos != null && systemAppInfos != null) {
					if (firstVisibleItem >= (userAppInfos.size() + 1)) {
						tv_appsize_lable.setText("系统程序："
								+ systemAppInfos.size() + "个");
					} else {
						tv_appsize_lable.setText("用户程序：" + userAppInfos.size()
								+ "个");
					}
				}
			}
		});

		// 给listview注册一个点击事件
		lv_appmanger.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Object obj = lv_appmanger.getItemAtPosition(position);
				if (obj != null && obj instanceof AppInfo) {
					clickedAppInfo = (AppInfo) obj;
					View contentView = View.inflate(getApplicationContext(),
							R.layout.popup_item, null);
					ll_uninstall = (LinearLayout) contentView
							.findViewById(R.id.ll_uninstall);
					ll_start = (LinearLayout) contentView
							.findViewById(R.id.ll_start);
					ll_share = (LinearLayout) contentView
							.findViewById(R.id.ll_share);
					ll_setting = (LinearLayout) contentView
							.findViewById(R.id.ll_setting);
					ll_share.setOnClickListener(AppManagerActivity.this);
					ll_start.setOnClickListener(AppManagerActivity.this);
					ll_uninstall.setOnClickListener(AppManagerActivity.this);
					ll_setting.setOnClickListener(AppManagerActivity.this);
					dismissPopupWindow();
					popupwindow = new PopupWindow(contentView, -2, -2);
					// 动画播放有一个前提条件： 窗体必须要有背景资源。 如果窗体没有背景，动画就播放不出来。
					popupwindow.setBackgroundDrawable(new ColorDrawable(
							Color.TRANSPARENT));
					int[] location = new int[2];
					view.getLocationInWindow(location);
					popupwindow.showAtLocation(parent, Gravity.LEFT
							+ Gravity.TOP, 60, location[1]);
					ScaleAnimation sa = new ScaleAnimation(0.5f, 1.0f, 0.5f,
							1.0f, Animation.RELATIVE_TO_SELF, 0,
							Animation.RELATIVE_TO_SELF, 0.5f);
					sa.setDuration(200);
					AlphaAnimation aa = new AlphaAnimation(0.5f, 1.0f);
					aa.setDuration(200);
					AnimationSet set = new AnimationSet(false);
					set.addAnimation(aa);
					set.addAnimation(sa);
					contentView.startAnimation(set);
				}

			}
		});

		receiver = new UninstallReceiver();
		IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(receiver, filter);

	}

	// 填充数据的业务方法
	private void fillData() {
		ll_loading.setVisibility(View.VISIBLE);
		new Thread() {
			public void run() {
				infos = AppInfoParser.getAppInfos(AppManagerActivity.this);
				userAppInfos = new ArrayList<AppInfo>();
				systemAppInfos = new ArrayList<AppInfo>();
				for (AppInfo info : infos) {
					if (info.isUserApp()) {
						// 用户程序
						userAppInfos.add(info);
					} else {
						// 系统程序
						systemAppInfos.add(info);
					}
				}
				// 设置界面了。
				handler.sendEmptyMessage(0);
			};
		}.start();
	}

	private void dismissPopupWindow() {
		if (popupwindow != null && popupwindow.isShowing()) {
			popupwindow.dismiss();
			popupwindow = null;
		}
	}

	static class ViewHolder {
		ImageView iv_app_icon;
		TextView tv_app_name;
		TextView tv_app_size;
		TextView tv_app_location;
	}

	private class AppManagerAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			// return infos.size();
			// 多了两个显示条目个数的textview小标签 所有加2
			return userAppInfos.size() + systemAppInfos.size() + 2;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (position == 0) {
				// 第0个位置显示的应该是 用户程序的个数的标签。
				TextView tv = new TextView(getApplicationContext());
				tv.setBackgroundColor(Color.GRAY);
				tv.setTextColor(Color.WHITE);
				tv.setText("用户程序：" + userAppInfos.size() + "个");
				return tv;
			} else if (position == (userAppInfos.size() + 1)) {
				TextView tv = new TextView(getApplicationContext());
				tv.setBackgroundColor(Color.GRAY);
				tv.setTextColor(Color.WHITE);
				tv.setText("系统程序：" + systemAppInfos.size() + "个");
				return tv;
			}
			AppInfo appInfo;
			if (position < (userAppInfos.size() + 1)) {
				// 用户程序
				appInfo = userAppInfos.get(position - 1);// 多了一个textview的标签 ，
															// 位置需要-1
			} else {
				// 系统程序
				int location = position - 1 - userAppInfos.size() - 1;
				appInfo = systemAppInfos.get(location);
			}
			View view;
			ViewHolder holder;
			if (convertView != null && convertView instanceof LinearLayout) {
				view = convertView;
				holder = (ViewHolder) view.getTag();
			} else {
				view = View.inflate(getApplicationContext(),
						R.layout.item_app_manager, null);
				holder = new ViewHolder();
				holder.iv_app_icon = (ImageView) view
						.findViewById(R.id.iv_app_icon);
				holder.tv_app_name = (TextView) view
						.findViewById(R.id.tv_app_name);
				holder.tv_app_size = (TextView) view
						.findViewById(R.id.tv_app_size);
				holder.tv_app_location = (TextView) view
						.findViewById(R.id.tv_app_location);
				view.setTag(holder);
			}
			// 得到当前位置的appinfo对象
			holder.iv_app_icon.setImageDrawable(appInfo.getIcon());
			holder.tv_app_name.setText(appInfo.getName());
			holder.tv_app_size.setText(Formatter.formatFileSize(
					getApplicationContext(), appInfo.getAppSize()));
			if (appInfo.isInRom()) {
				holder.tv_app_location.setText("手机内存");
			} else {
				holder.tv_app_location.setText("外部存储");
			}
			return view;
		}

		@Override
		public Object getItem(int position) {
			if (position == 0) {
				// 第0个位置显示的应该是 用户程序的个数的标签。
				return null;
			} else if (position == (userAppInfos.size() + 1)) {
				return null;
			}
			AppInfo appInfo;
			if (position < (userAppInfos.size() + 1)) {
				// 用户程序
				appInfo = userAppInfos.get(position - 1);// 多了一个textview的标签 ，
															// 位置需要-1
			} else {
				// 系统程序
				int location = position - 1 - userAppInfos.size() - 1;
				appInfo = systemAppInfos.get(location);
			}
			return appInfo;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	@Override
	protected void onDestroy() {
		dismissPopupWindow();
		unregisterReceiver(receiver);
		receiver = null;
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.ll_share:
			Log.i(TAG, "分享：" + clickedAppInfo.getPackname());
			shareApplication();
			break;
		case R.id.ll_uninstall:
			Log.i(TAG, "卸载：" + clickedAppInfo.getPackname());
			uninstallApplication();
			break;
		case R.id.ll_start:
			Log.i(TAG, "开启：" + clickedAppInfo.getPackname());
			startApplication();
			break;
		case R.id.ll_setting:
			Log.i(TAG, "设置：" + clickedAppInfo.getPackname());
			viewAppDetail();
			break;
		}
		dismissPopupWindow();
	}

	private void viewAppDetail() {
		Intent intent = new Intent();
		intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		// dat=package:com.itheima.mobileguard
		intent.setData(Uri.parse("package:" + clickedAppInfo.getPackname()));
		startActivity(intent);
	}

	/**
	 * 卸载软件
	 */
	private void uninstallApplication() {
		if (clickedAppInfo.isUserApp()) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_DELETE);
			intent.setData(Uri.parse("package:" + clickedAppInfo.getPackname()));
			startActivity(intent);
		}else{
			//系统应用 ，root权限 利用linux命令删除文件。
			if(!RootTools.isRootAvailable()){
				Toast.makeText(this, "卸载系统应用，必须要root权限", 0).show();
				return ;
			}
			try {
				if(!RootTools.isAccessGiven()){
					Toast.makeText(this, "请授权黑马小护卫root权限", 0).show();
					return ;
				}
				RootTools.sendShell("mount -o remount ,rw /system", 3000);
				RootTools.sendShell("rm -r "+clickedAppInfo.getApkpath(), 30000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
	}

	/**
	 * 开启应用程序
	 */
	private void startApplication() {
		// 打开这个应用程序的入口activity。
		PackageManager pm = getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(clickedAppInfo
				.getPackname());
		if (intent != null) {
			startActivity(intent);
		} else {
			Toast.makeText(this, "该应用没有启动界面", 0).show();
		}
	}

	/**
	 * 分享应用
	 */
	private void shareApplication() {
		Intent intent = new Intent("android.intent.action.SEND");
		intent.addCategory("android.intent.category.DEFAULT");
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT,
				"推荐您使用一款软件，名称叫：" + clickedAppInfo.getName()
						+ "下载路径：https://play.google.com/store/apps/details?id="
						+ clickedAppInfo.getPackname());
		startActivity(intent);
	}

	private class UninstallReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String info = intent.getData().toString();
			System.out.println(info);
			fillData();
		}
	}
}
