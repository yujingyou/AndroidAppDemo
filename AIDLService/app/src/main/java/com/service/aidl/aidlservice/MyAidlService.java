package com.service.aidl.aidlservice;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.service.aidl.aidlservice.IMyAidlInterface;
import com.service.aidl.aidlservice.Person;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static android.util.Log.d;

public class MyAidlService extends Service {
    private final String TAG = this.getClass().getSimpleName();

    private final Lock lock = new ReentrantLock();

    private Condition notFull = lock.newCondition();
    private Condition notEmpty = lock.newCondition() ;
    private Queue<Person> mPersons;

    /**
     * 创建生成的本地 Binder 对象，实现 AIDL 制定的方法
     */
    private IBinder mIBinder;

    {
        mIBinder = new IMyAidlInterface.Stub() {

            @Override
            public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

            }

            @Override
            public synchronized void addPerson(Person person) throws RemoteException {
                Log.d(TAG, "addPerson: pid:" + android.os.Process.myPid() + "tid:" + android.os.Process.myTid()
                        + "uid:" + android.os.Process.myUid());
                mPersons.offer(person);
                lock.unlock();
            }

            @Override
            public synchronized Person getPersonList() throws RemoteException {
                Log.d(TAG, "getPersonList: pid:" + android.os.Process.myPid() + "tid:" + android.os.Process.myTid()
                        + "uid:" + android.os.Process.myUid());
                if(mPersons.isEmpty())
                {
                    lock.lock();
                }
                return mPersons.poll();
            }
        };
    }

    /**
     * 客户端与服务端绑定时的回调，返回 mIBinder 后客户端就可以通过它远程调用服务端的方法，即实现了通讯
     * @param intent
     * @return
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mPersons = new LinkedList<>();
        System.out.println("MyAidlService onBind");
        return mIBinder;
    }
}