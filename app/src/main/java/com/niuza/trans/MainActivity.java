package com.niuza.trans;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.niuza.trans.p2p.WiFiDirectBroadcastReceiver;
import com.niuza.trans.ui.DeviceDetailFragment;
import com.niuza.trans.ui.DeviceListFragment;

import java.lang.reflect.Method;
public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener,DeviceListFragment.DeviceActionListener{

    public static final String TAG = "debug_info";
    //P2P网络状态，频道刷新
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;

    //manager和频道，还有广播
    private WifiP2pManager manager;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver = null;//创建receiver和intentFilter
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //首先在MainActivity中初始化intentFilter来监听P2P网络的状态
        initIntentFilter();



//        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
//        //创建本线程到WifiP2p框架的通道A，在操作之前要初始化
//        channel = manager.initialize(this, getMainLooper(), null);

        try {
            manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            channel = manager.initialize(this, getMainLooper(), new WifiP2pManager.ChannelListener() {
                @Override
                public void onChannelDisconnected() {
                    manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
                }
            });
        }catch (Exception e)
        {}



    }
    public void editdevname(String devName)
    {
        try {
            Class[] paramTypes = new Class[3];
            paramTypes[0] = WifiP2pManager.Channel.class;
            paramTypes[1] = String.class;
            paramTypes[2] = WifiP2pManager.ActionListener.class;
            Method setDeviceName = manager.getClass().getMethod("setDeviceName", paramTypes);
            setDeviceName.setAccessible(true);
            Object arglist[] = new Object[3];
            arglist[0] = channel;
            arglist[1] = devName;
            arglist[2] = new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.d("setDeviceName succeeded", "true");
                }

                @Override
                public void onFailure(int reason) {
                    Log.d("setDeviceName failed", "true");
                }
            };
            setDeviceName.invoke(manager, arglist);

        }
        catch (Exception e){
            e.printStackTrace();

        }




    }











    public void editname(View v)
    {
        AlertDialog.Builder customDia=new AlertDialog.Builder(this);
        final View viewDia= LayoutInflater.from(MainActivity.this).inflate(R.layout.edit_name_dialog, null);
        customDia.setTitle("自定义对话框");
        customDia.setView(viewDia);

        final EditText editText=(EditText)findViewById(R.id.name_edit);
        customDia.setPositiveButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

                editdevname("丢类");
            }
        });
        customDia.create().show();





    }
















    //在Activity的onResume()中挂入接收器A，在onPause()中取消接收器A
    //~~~~~~ 准备工作结束，下面启动扫描 ~~~~~~
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        //把intent过滤器A注册到接收器A中
        registerReceiver(receiver, intentFilter);
        //接收WiFiDirect广播
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * 当状态变化的时候重置所有数据
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }

    private void initIntentFilter(){
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);//Idicates whether Wi-Fi P2P is enabled
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);//Indicates that the available peer list has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);//Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);//Indicates this device's configuration details have changed.

    }



    /**
     * 打开wifi设置界面
     * @param v
     */
    public void startWifi(View v){

        if (manager != null && channel != null) {


            //打开系统默认的WIFI设置，没有返回值
           // startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));

            //使用WifiManger的服务来进行Wifi的开启
            WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                if (wifiManager.setWifiEnabled(true))
                    Toast.makeText(this,R.string.wifi_success,Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this,R.string.wifi_fail,Toast.LENGTH_LONG).show();


            } else {
                Toast.makeText(this,R.string.wifi_success,Toast.LENGTH_LONG).show();
            }





        } else {
            Log.e(TAG, "没有channel或者manager没有被初始化");
        }



    }
    /**
     * 启动扫描
     * @param v
     */
    public void startScan(View v){

        if (!isWifiP2pEnabled) {
            Toast.makeText(getApplicationContext(), R.string.p2p_fail,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_list);

        fragment.onInitiateDiscovery();
        //启动对等发现

        //初始化P2P搜索
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), R.string.scan_success,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {
                Toast.makeText(getApplicationContext(), R.string.scan_fail + reasonCode,
                        Toast.LENGTH_SHORT).show();
            }
        });

    }



    //活动实现了Channel监听器
    /**
     * 以下是ChannelListener 要实现的方法
     */
    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, R.string.disconnect_success, Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    R.string.disconnect_fail,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     *DeviceActionListener需要实现的方法，当用户点击连接的时候，走的连接方法
     */
    @Override
    public void connect(WifiP2pConfig config) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(),R.string.connect_fail,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    /**
     * 取消连接调用的方法
     */
    @Override
    public void cancelDisconnect() {

        /*
         * A cancel abort request by user. Disconnect i.e. removeGroup if
         * already connected. Else, request WifiP2pManager to abort the ongoing
         * request
         */
        if (manager != null) {
            final DeviceListFragment fragment = (DeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.frag_list);
            if (fragment.getDevice() == null
                    || fragment.getDevice().status == WifiP2pDevice.CONNECTED) {
                disconnect();
            } else if (fragment.getDevice().status == WifiP2pDevice.AVAILABLE
                    || fragment.getDevice().status == WifiP2pDevice.INVITED) {

                manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),R.string.abort_success,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(getApplicationContext(),
                                R.string.abort_fail + reasonCode,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }


    /**以上是ChannelListener 要实现的方法*/
}