package com.itheima.mobileguard.db.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ListView;

import com.itheima.mobileguard.db.AppLockOpenHelper;

/**
 * ============================================================
 * 
 * 版 权 ： 黑马程序员教育集团 版权所有 (c) 2015
 * 
 * 作 者 : 马伟奇
 * 
 * 版 本 ： 1.0
 * 
 * 创建日期 ： 2015-3-7 上午10:14:24
 * 
 * 描 述 ：
 * 
 * 程序所的数据库 修订历史 ：
 * 
 * ============================================================
 **/
public class AppLockDao {

	private AppLockOpenHelper helper;

	public AppLockDao(Context context) {
		helper = new AppLockOpenHelper(context);
	}

	/**
	 * 添加到程序所里面
	 * 
	 * @param packageName
	 *            包名
	 */
	public void add(String packageName) {
		SQLiteDatabase db = helper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("packagename", packageName);
		db.insert("info", null, values);
		db.close();
	}

	/**
	 * 从程序锁里面删除当前的包
	 * 
	 * @param packageName
	 */
	public void delete(String packageName) {
		SQLiteDatabase db = helper.getWritableDatabase();
		db.delete("info", "packagename=?", new String[] { packageName });
		db.close();
	}
    /**
     * 查询当前的包是否在程序锁里面
     * @param packageName
     * @return
     */
	public boolean find(String packageName) {
		boolean result = false;
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query("info", null, "packagename=?",
				new String[] { packageName }, null, null, null);
		if (cursor.moveToNext()) {
			result = true;
		}
		cursor.close();
		db.close();
		return result;

	}
	/**
	 * 查询全部的锁定的包名
	 * @return
	 */
	public List<String> findAll(){
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query("info", new String[]{"packagename"}, null, null, null, null, null);
		List<String> packnames = new ArrayList<String>();
		while(cursor.moveToNext()){
			packnames.add(cursor.getString(0));
		}
		cursor.close();
		db.close();
		return packnames;
	}
	
}
