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

    private DataOutputStream mDataOutputStream;
    private Socket socket;
    private int[] degValues = {0,0,0};
    private boolean changed = false;
    private final Object MUTEX = new Object();

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
        try {

            JSONObject ouverture = new JSONObject();
            ouverture.put("type", "stream_state");
            ouverture.put("status", true);
            Log.d("MSG", ouverture.toString());

            socket = new Socket("10.0.1.5", 1993);
            mDataOutputStream = new DataOutputStream(socket.getOutputStream());
            mDataOutputStream.writeUTF(ouverture.toString());
            mDataOutputStream.flush();
            Log.d("MSG NETWORK", ouverture.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(mRunning && mDataOutputStream !=null){
            Log.d("MSG NETWORK","Update mouvement");

            synchronized (MUTEX){
                if(changed){
                    JSONObject msg = new JSONObject();
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
                        e.printStackTrace();
                        mRunning=false;
                    }
                }
            }
            SystemClock.sleep(100);
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
