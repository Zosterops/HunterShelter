package com.zosterops.huntershelter;

import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.DecimalFormat;

/**
 * Created by david on 19/05/15.
 */
public class NetworkThread extends Thread {
    public final static int STATE_NOT_CONNECTED = -1;
    public final static int STATE_CONNECTING = 0;
    public final static int STATE_CONNECTED = 1;

    private boolean mRunning = true;
    private int mState = STATE_NOT_CONNECTED;

    private DataOutputStream mDataOutputStream;
    private String ip;
    private Socket socket;
    private int[] degValues = {0,0,0};
    private boolean changed = false;
    private final Object MUTEX = new Object();
    private final Object MUTEX_RUNNING = new Object();
    private final Object MUTEX_STATE = new Object();
    private NetworkConnectedListener listener;
    private static NetworkThread instance = null;

    public static NetworkThread getInstance(String addr) {
        if(instance == null){
            instance = new NetworkThread(addr);
        }
        return instance;
    }

    public static NetworkThread getInstance() {
        return instance;
    }

    private NetworkThread(String addr){
        ip = addr;
    }

    public void setRegValue(float[] regValue){
        int[] tmpDegValues = new int[3];
        if(regValue.length < 3){
            throw new IllegalArgumentException("Illegal Argument");
        }
        tmpDegValues[0] = (int) Math.round((regValue[0]));
        tmpDegValues[1] = (int) Math.round((regValue[1]));
        tmpDegValues[2] = (int) Math.round((regValue[2]));

        synchronized (MUTEX){
            changed = false;

            if(Math.abs(tmpDegValues[0] - degValues[0])>0){
                degValues[0] = tmpDegValues[0];
                changed = true;
            }
            if(Math.abs(tmpDegValues[1] - degValues[1])>0){
                degValues[1] = tmpDegValues[1];
                changed = true;
            }
            if(Math.abs(tmpDegValues[2] - degValues[2])>0){
                degValues[2] = tmpDegValues[2];
                changed = true;
            }
        }
    }

    @Override
    public void run() {
        super.run();
        boolean running;
        boolean first = true;

        synchronized (MUTEX_RUNNING) {
            running = true;
        }
        Log.d("MSG NETWORK", "Launch Thread");
        try {
            setState(STATE_CONNECTING);

            socket = new Socket(ip, 1993);
            mDataOutputStream = new DataOutputStream(socket.getOutputStream());

            onConnected(true);
            setState(STATE_CONNECTED);
            Log.d("MSG NETWORK", "Connected");

        } catch (IOException e) {
            e.printStackTrace();
            onConnected(false);
        }

        while(running && mDataOutputStream !=null) {
            Log.d("MSG NETWORK","Update mouvement");

            synchronized (MUTEX_RUNNING) {
                running = mRunning;
            }

            synchronized (MUTEX){
                if(changed){
                    JSONObject msg = new JSONObject();
                    JSONObject ouverture = new JSONObject();
                    try {

                        if(first){
                          ouverture.put("type", "stream_state");
                          ouverture.put("status", true);
                          Log.d("MSG", ouverture.toString());

                          mDataOutputStream.writeUTF(ouverture.toString());
                          mDataOutputStream.flush();
                          Log.d("MSG NETWORK", ouverture.toString());
                          first = false;
                        }

                        msg.put("type", "movement");
                        msg.put("x", degValues[0]);
                        msg.put("y", degValues[1]);
                        msg.put("z", degValues[2]);
                        mDataOutputStream.writeUTF(msg.toString());
                        mDataOutputStream.flush();
                        Log.d("MSG NETWORK", msg.toString());

                    } catch (JSONException e) {
                    } catch (IOException e) {
                        e.printStackTrace();
                        onConnected(false);
                        mRunning=false;
                    }
                }
            }

            SystemClock.sleep(100);
        }


        Log.d("MSG NETWORK", "Closed");
        onConnected(false);

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (mDataOutputStream != null){
            try {
                mDataOutputStream.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        instance = null;
    }

    private void onConnected(boolean b) {
        if(!b){
            setState(STATE_NOT_CONNECTED);
        }
        if(listener != null){
            listener.onConnected(b);
        }
    }

    private void setState(int i){
        synchronized (MUTEX_STATE){
            mState = i;
        }
    }

    public int getNetworkState(){
        int retour;
        synchronized (MUTEX_STATE){
            retour = mState;
        }
        return retour;
    }
    public void close() {
        synchronized (MUTEX_RUNNING){
            mRunning = false;
        }
    }

    public void setListener(NetworkConnectedListener listener) {
        this.listener = listener;
    }

    interface NetworkConnectedListener {
         void onConnected(boolean isConnected);
    }
}
