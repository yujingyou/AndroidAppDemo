package com.itheima.mobileguard.activities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.itheima.mobileguard.R;
import com.itheima.mobileguard.db.dao.AntivirusDao;
import com.itheima.mobileguard.domain.Virus;
import com.itheima.mobileguard.utils.StreamUtils;
import com.itheima.mobileguard.utils.UIUtils;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;
import com.lidroid.xutils.http.client.HttpRequest.HttpMethod;

public class SplashActivity extends Activity {
	protected static final int SHOW_UPDATE_DIALOG = 1;
	private static final int LOAD_MAINUI = 2;
	private TextView tv_splash_version;
	private TextView tv_info;
	// 包管理器
	private PackageManager packageManager;
	private int clientVersionCode;

	// 服务器新资源描述信息
	private String desc;
	// 服务器资源的下载路径
	private String downloadurl;

	// 消息处理器
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LOAD_MAINUI:
				loadMainUI();
				break;
			case SHOW_UPDATE_DIALOG:
				// 因为对话框是activity的一部分显示对话框 必须指定activity的环境（令牌）
				AlertDialog.Builder builder = new Builder(SplashActivity.this);
				builder.setTitle("更新提醒");
				builder.setMessage(desc);
				// builder.setCancelable(false);
				builder.setOnCancelListener(new OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						loadMainUI();
					}
				});
				builder.setNegativeButton("下次再说", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						loadMainUI();
					}
				});
				builder.setPositiveButton("立刻更新", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						System.out.println("下载：" + downloadurl);
						download(downloadurl);
					}

				});
				builder.show();
				break;
			}
		};
	};
	private AntivirusDao dao;

	/**
	 * 多线程的下载器
	 * 
	 * @param downloadurl
	 */
	private void download(String downloadurl) {
		// 多线程断点下载。
		HttpUtils http = new HttpUtils();
		http.download(downloadurl, "/mnt/sdcard/temp.apk",
				new RequestCallBack<File>() {
					@Override
					public void onSuccess(ResponseInfo<File> arg0) {
						System.out.println("安装 /mnt/sdcard/temp.apk");
						// <action android:name="android.intent.action.VIEW" />
						// <category
						// android:name="android.intent.category.DEFAULT" />
						// <data android:scheme="content" />
						// <data android:scheme="file" />
						// <data
						// android:mimeType="application/vnd.android.package-archive"
						// />
						Intent intent = new Intent();
						intent.setAction("android.intent.action.VIEW");
						intent.addCategory("android.intent.category.DEFAULT");

						// intent.setType("application/vnd.android.package-archive");
						// intent.setData(Uri.fromFile(new
						// File(Environment.getExternalStorageDirectory(),"temp.apk")));
						intent.setDataAndType(Uri.fromFile(new File(Environment
								.getExternalStorageDirectory(), "temp.apk")),
								"application/vnd.android.package-archive");
						startActivityForResult(intent, 0);
					}

					@Override
					public void onFailure(HttpException arg0, String arg1) {
						Toast.makeText(SplashActivity.this, "下载失败", 0).show();
						System.out.println(arg1);
						arg0.printStackTrace();
						loadMainUI();
					}

					@Override
					public void onLoading(long total, long current,
							boolean isUploading) {
						tv_info.setText(current + "/" + total);
						super.onLoading(total, current, isUploading);
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		loadMainUI();
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		tv_splash_version = (TextView) findViewById(R.id.tv_splash_version);
		tv_info = (TextView) findViewById(R.id.tv_info);
		packageManager = getPackageManager();
		AlphaAnimation aa = new AlphaAnimation(0.0f, 1.0f);
		aa.setDuration(2000);
		findViewById(R.id.rl_splash_root).startAnimation(aa);
		
		//拷贝资产目录下的数据库文件
		copyDB("address.db");
		
		//拷贝资产目录下的病毒数据库文件
		copyDB("antivirus.db");
		
		//创建快捷方式
		createShortcut();
	    //更新病毒数据库
		updataVirus();
		
		

		try {
			PackageInfo packInfo = packageManager.getPackageInfo(
					getPackageName(), 0);
			String version = packInfo.versionName;
			clientVersionCode = packInfo.versionCode;
			tv_splash_version.setText(version);
			// 判断是否需要检查更新版本
			SharedPreferences sp = getSharedPreferences("config", MODE_PRIVATE);
			boolean autoupdate = sp.getBoolean("autoupdate", false);
			if (autoupdate) {
				checkVersion();
			}else{
				//自动更新被关闭。
				new Thread(){
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						loadMainUI();
					};
				}.start();
			}

		} catch (NameNotFoundException e) {
			e.printStackTrace();
			// 不会发生 can't reach
		}

	}

	/**
	 * 进行更新病毒数据库
	 */
    private void updataVirus() {
    	
    	dao = new AntivirusDao();
		
    	//联网从服务器获取到最新数据的md5的特征码
    	
		HttpUtils httpUtils = new HttpUtils();
		
		String url = "http://192.168.13.126:8080/virus.json";
		
		httpUtils.send(HttpMethod.GET, url, new RequestCallBack<String>() {

			@Override
			public void onFailure(HttpException arg0, String arg1) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(ResponseInfo<String> arg0) {
				System.out.println(arg0.result);
//				﻿{"md5":"51dc6ba54cbfbcff299eb72e79e03668","desc":"蝗虫病毒赶快卸载"}

				try {
					JSONObject jsonObject = new JSONObject(arg0.result);
					
					Gson gson = new Gson();
					//解析json
					Virus virus = gson.fromJson(arg0.result, Virus.class);
					
//					String md5 = jsonObject.getString("md5");
//					
//					String desc = jsonObject.getString("desc");
					
					dao.addVirus(virus.md5, virus.desc);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
			
			
		});
	}

	/**
     * 快捷方式
     */
    
	private void createShortcut() {
		
		
		Intent intent = new Intent();
		
		intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		//如果设置为true表示可以创建重复的快捷方式
		intent.putExtra("duplicate", false);
		
		/**
		 * 1 干什么事情
		 * 2 你叫什么名字
		 * 3你长成什么样子
		 */
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "黑马手机卫士");
		//干什么事情
		/**
		 * 这个地方不能使用显示意图
		 * 必须使用隐式意图
		 */
		Intent shortcut_intent = new Intent();
		
		shortcut_intent.setAction("aaa.bbb.ccc");
		
		shortcut_intent.addCategory("android.intent.category.DEFAULT");
		
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut_intent);
		
		sendBroadcast(intent);
		
	}

	/**
	 * 拷贝资产目录下的数据库文件
	 * @param dbname  数据库文件的名称
	 */
	private void copyDB(final String dbname) {
		new Thread(){
			public void run() {
				try {
					File file = new File(getFilesDir(),dbname);
					if(file.exists()&&file.length()>0){
						Log.i("SplashActivity","数据库是存在的。无需拷贝");
						return ;
					}
					InputStream is = getAssets().open(dbname);
					FileOutputStream fos  = openFileOutput(dbname, MODE_PRIVATE);
					byte[] buffer = new byte[1024];
					int len = 0;
					while((len = is.read(buffer))!=-1){
						fos.write(buffer, 0, len);
					}
					is.close();
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
	}

	/**
	 * 连接服务器 检查版本号 是否有更新
	 */
	private void checkVersion() {
		new Thread() {
			public void run() {
				Message msg = Message.obtain();
				// 检查 代码执行的时间。如果时间少于2秒 补足2秒
				long startTime = System.currentTimeMillis();
				try {
					URL url = new URL(getResources().getString(
							R.string.serverurl));
					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.setRequestMethod("GET");
					conn.setConnectTimeout(2000);
					int code = conn.getResponseCode();
					if (code == 200) {
						InputStream is = conn.getInputStream();// json文本
						String json = StreamUtils.readStream(is);
						if (TextUtils.isEmpty(json)) {
							// 服务器json获取失败
							// 错误2016 请联系客服
							UIUtils.showToast(SplashActivity.this,
									"错误2016, 服务器json获取失败,请联系客服");
							msg.what = LOAD_MAINUI;
						} else {
							JSONObject jsonObj = new JSONObject(json);
							downloadurl = jsonObj.getString("downloadurl");
							int serverVersionCode = jsonObj.getInt("version");
							desc = jsonObj.getString("desc");
							if (clientVersionCode == serverVersionCode) {
								// 相同，进入应用程序主界面
								msg.what = LOAD_MAINUI;
							} else {
								// 不同，弹出更新提醒对话框
								msg.what = SHOW_UPDATE_DIALOG;
							}
						}
					} else {
						// 错误2015 请联系客服
						UIUtils.showToast(SplashActivity.this,
								"错误2015, 服务器状态码错误,请联系客服");
						msg.what = LOAD_MAINUI;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					// 错误2011 请联系客服
					UIUtils.showToast(SplashActivity.this,
							"错误2011, url路径不正确,请联系客服");
					msg.what = LOAD_MAINUI;
				} catch (NotFoundException e) {
					e.printStackTrace();
					// 错误2012 请联系客服
					UIUtils.showToast(SplashActivity.this,
							"错误2012, 服务器地址找不到,请联系客服");
					msg.what = LOAD_MAINUI;
				} catch (IOException e) {
					e.printStackTrace();
					// 错误2013 请联系客服
					UIUtils.showToast(SplashActivity.this, "错误2013, 网络错误,请联系客服");
					msg.what = LOAD_MAINUI;
				} catch (JSONException e) {
					e.printStackTrace();
					// 错误2014 请联系客服
					UIUtils.showToast(SplashActivity.this,
							"错误2014, json解析错误,请联系客服");
					msg.what = LOAD_MAINUI;
				} finally {
					long endtime = System.currentTimeMillis();
					long dtime = endtime - startTime;
					if (dtime > 2000) {
						handler.sendMessage(msg);
					} else {
						try {
							Thread.sleep(2000 - dtime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.sendMessage(msg);
					}
				}
			};
		}.start();

	}

	private void loadMainUI() {
		Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
		startActivity(intent);
		finish();
	}

}
