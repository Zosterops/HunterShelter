package com.zosterops.huntershelter;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;


import org.freedesktop.gstreamer.GStreamer;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private native void nativeInit();     // Initialize native code, build pipeline, etc
    private native void nativeFinalize(); // Destroy pipeline and shutdown native code
    private native void nativePlay();     // Set pipeline to PLAYING
    private native void nativePause();    // Set pipeline to PAUSED
    private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
    private native void nativeSurfaceInit(Object surface);
    private native void nativeSurfaceFinalize();
    private long native_custom_data;      // Native code will use this to keep private data

    private boolean is_playing_desired;   // Whether the user asked to go to PLAYING

    private NetworkThread networkThread;
    private float[] mHeadView;
    private SensorManager mSensorManager;
    private Sensor accelero;
    private Sensor magneto;
    private float[] mGravity;
    private float[] mMagnetic;
    private boolean mPause = false;

    // Called when the activity is first created.
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelero = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneto = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (accelero != null && magneto != null) {
            mSensorManager.registerListener(mySensorEventListener, accelero,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(mySensorEventListener, magneto,
                    SensorManager.SENSOR_DELAY_NORMAL);

        }else{
            finish();
        }


        // Initialize GStreamer and warn if it fails
        try {
            GStreamer.init(this);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
        SurfaceHolder sh = sv.getHolder();
        sh.addCallback(this);

        is_playing_desired = true;
        nativePlay();

        mHeadView = new float[16];

        nativeInit();

        networkThread = NetworkThread.getInstance();
        if(networkThread == null){
          finish();
        }
    }

    private float[] getDirection()
    {

        float[] temp = new float[9];
        float[] R = new float[9];
        //Load rotation matrix into R
        SensorManager.getRotationMatrix(temp, null,
                mGravity, mMagnetic);

        //Remap to camera's point-of-view
        SensorManager.remapCoordinateSystem(temp,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z, R);

        //Return the orientation values
        float[] values = new float[3];
        SensorManager.getOrientation(R, values);

        //Convert to degrees
        for (int i=0; i < values.length; i++) {
            Double degrees = (values[i] * 180) / Math.PI;
            values[i] = degrees.floatValue();
        }

        return values;

    }

    private SensorEventListener mySensorEventListener = new SensorEventListener() {

       @Override
       public void onAccuracyChanged(Sensor sensor, int accuracy) {
       }

       @Override
       public void onSensorChanged(SensorEvent event) {
           if(networkThread != null){
             switch(event.sensor.getType()) {

                case Sensor.TYPE_ACCELEROMETER:
                  mGravity = event.values.clone();
                  break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                  mMagnetic = event.values.clone();
                  break;
                default:
                  return;
        }
        if(mGravity != null && mMagnetic != null && mPause) {
          networkThread.setRegValue(getDirection());
        }
             //networkThread.setRegValue(event.values);
           }
       }
   };


   @Override
  public void onPause() {
      super.onPause();  // Always call the superclass method first

      mPause = false;
  }

     @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        mPause = true;
    }

    protected void onSaveInstanceState (Bundle outState) {
        Log.d ("GStreamer", "Saving state, playing:" + is_playing_desired);
    }

    @Override
    protected void onDestroy() {
        nativeFinalize();
        super.onDestroy();
    }

    // Called from native code. This sets the content of the TextView from the UI thread.
    private void setMessage(final String message) {
    }

    // Called from native code. Native code calls this once it has created its pipeline and
    // the main loop is running, so it is ready to accept commands.
    private void onGStreamerInitialized () {
        Log.i ("GStreamer", "Gst initialized. Restoring state, playing:" + is_playing_desired);
        // Restore previous playing state
        nativePlay();
    }

    static {
        System.loadLibrary("gstreamer_android");
        System.loadLibrary("tutorial-3");
        nativeClassInit();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d("GStreamer", "Surface changed to format " + format + " width "
                + width + " height " + height);
        nativeSurfaceInit (holder.getSurface());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("GStreamer", "Surface created: " + holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("GStreamer", "Surface destroyed");
        nativeSurfaceFinalize ();
    }

}
