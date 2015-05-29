package com.zosterops.huntershelter;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zosterops.huntershelter.NetworkThread.NetworkConnectedListener;


public class ConfigActivty extends Activity implements NetworkConnectedListener {

    private TextView step, msg;
    private FragmentManager fm;
    private Fragment f;
    private RelativeLayout load;
    private RetainedFragment dataFragment;

    public class RetainedFragment extends Fragment {

        // data object we want to retain
        private NetworkThread networkThread;

        // this method is only called once for this fragment
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // retain this fragment
            setRetainInstance(true);
        }

        public void setData(NetworkThread data) {
            this.networkThread = data;
        }

        public NetworkThread getData() {
            return networkThread;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_activty);
        step = (TextView) findViewById(R.id.step);
        msg = (TextView) findViewById(R.id.msg);
        fm = getFragmentManager();
        f = fm.findFragmentById(R.id.fragment);
        load = (RelativeLayout) findViewById(R.id.load);
        FragmentManager fm = getFragmentManager();
        dataFragment = (RetainedFragment) fm.findFragmentByTag("data");

        if (dataFragment == null) {
            // add the fragment
            dataFragment = new RetainedFragment();
            fm.beginTransaction().add(dataFragment,"data").commit();
        }
    }

    public void processIP(String ip1, String ip2, String ip3, String ip4){
        String addrIp = ip1 + '.' + ip2 + '.' + ip3 + '.' + ip4;

        printConnecting();
        NetworkThread networkThread = new NetworkThread(addrIp);
        networkThread.setListener(this);
        networkThread.start();
        dataFragment.setData(networkThread);

    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkThread networkThread = dataFragment.getData();
        if(networkThread != null) {
            networkThread.pause();
            networkThread.setListener(null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkThread networkThread = dataFragment.getData();
        if(networkThread != null){
            networkThread.wakeup();
            networkThread.setListener(this);

            switch (networkThread.getNetworkState()){
                case NetworkThread.STATE_NOT_CONNECTED :
                    printIpFragment();
                    break;
                case NetworkThread.STATE_CONNECTING :
                    printConnecting();
                    break;
                case NetworkThread.STATE_CONNECTED :
                    printConnected();
                    break;
            }
        }
    }

    private void printConnected() {
        step.setText("Step 2");
        msg.setText("connection established !");
        fm.beginTransaction().hide(f).commit();
        load.setVisibility(View.VISIBLE);
    }

    private void printConnecting() {
        step.setText("Step 2");
        msg.setText("establishing connection ...");
        fm.beginTransaction().hide(f).commit();
        load.setVisibility(View.VISIBLE);
    }

    private void printIpFragment() {
        step.setText("Step 1");
        msg.setText("connection failed !");
        fm.beginTransaction().show(f).commit();
        load.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        NetworkThread networkThread = dataFragment.getData();
        if(networkThread != null && isFinishing()) {
            networkThread.close();
        }
    }

    @Override
    public void onConnected(boolean isConnected) {
        NetworkThread networkThread = dataFragment.getData();
        if(isConnected){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printConnected();
                }
            });

        }
        else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    printIpFragment();
                }
            });
            networkThread.close();
        }
    }
}
