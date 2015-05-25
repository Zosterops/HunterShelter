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
    private boolean mRunning = false;
    private static JSONObject ouverture = new JSONObject();
    private DataOutputStream mDataOutputStream;
    private Socket socket;
    private int[] degValues = {0,0,0};
    private boolean changed = false;
    private final Object MUTEX = new Object();

    static {
        try {
            ouverture.put("type", "stream_state");
            ouverture.put("status", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRegValue(float[] regValue){
        int[] tmpDegValues = new int[3];
        if(regValue.length < 3){
            throw new IllegalArgumentException("Illegal Argument");
        }
        tmpDegValues[0] = (int) Math.round(Math.toDegrees(regValue[0]));
        tmpDegValues[1] = (int) Math.round(Math.toDegrees(regValue[1]));
        tmpDegValues[2] = (int) Math.round(Math.toDegrees(regValue[2]));

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
        mRunning = true;
        Log.d("MSG NETWORK","Launch Thread");

        while(mRunning){
            if(socket == null){
                try {
                    Log.d("MSG", ouverture.toString());

                    socket = new Socket("10.0.1.3                                                                                                                                                                   ", 1993);
                    mDataOutputStream = new DataOutputStream(socket.getOutputStream());
                    mDataOutputStream.writeUTF(ouverture.toString());
                    mDataOutputStream.flush();
                    Log.d("MSG NETWORK", ouverture.toString());
                } catch (IOException e) {
                    Log.e("MSG NETWORK","Failed to conenct socket");
                    socket = null;
                }
            }

            if(socket != null){
                JSONObject msg = new JSONObject();
                synchronized (MUTEX){
                    if(changed){
                        try {
                            msg.put("type", "movement");
                            msg.put("x", degValues[0]);
                            msg.put("y", degValues[1]);
                            msg.put("z", degValues[2]);
                            mDataOutputStream.writeUTF(msg.toString());
                            mDataOutputStream.flush();
                            Log.d("MSG NETWORK", msg.toString());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            socket = null;
                        }
                    }
                }
                SystemClock.sleep(100);
            }
            else {
                SystemClock.sleep(500);
            }
        }
        Log.d("MSG NETWORK","Closed");

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
    }

    public void close() {
        mRunning = false;
    }
}
