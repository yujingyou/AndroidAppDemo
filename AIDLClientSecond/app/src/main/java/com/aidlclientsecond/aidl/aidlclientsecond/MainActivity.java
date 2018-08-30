package com.aidlclientsecond.aidl.aidlclientsecond;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.service.aidl.aidlservice.IMyAidlInterface;
import com.service.aidl.aidlservice.Person;

import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {
    private IMyAidlInterface mAidl;
    TextView mHellow;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //连接后拿到 Binder，转换成 AIDL，在不同进程会返回个代理

            mAidl = IMyAidlInterface.Stub.asInterface(service);
            System.out.println("onServiceConnected"+mAidl);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mAidl = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        System.out.println("onCreate"+mAidl);

        Intent intent = new Intent("com.service.aidl.aidlservice.IMyAidlInterface");

        //Android5.0后无法只通过隐式Intent绑定远程Service
        //需要通过setPackage()方法指定包名
        intent.setPackage("com.service.aidl.aidlservice");
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        Button button2 = (Button) findViewById(R.id.button);
        mHellow = (TextView)findViewById(R.id.txt1);
        System.out.println("onCreate"+mAidl);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPerson();

            }
        });
    }

    public void addPerson() {
        Random random = new Random();
        Person person = new Person("shixin" + random.nextInt(10));
        Log.d("client","getPersonList: pid:" +android.os.Process.myPid()+"tid:"+android.os.Process.myTid()
                +"uid:"+android.os.Process.myUid());
        try {
            mAidl.addPerson(person);
            List<Person> personList = mAidl.getPersonList();
            mHellow.setText(personList.toString());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}