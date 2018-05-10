package hpscil.cug.urbansensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.math.BigDecimal;

/**
 * Created by astro on 2018/5/10.
 */

public class TempListener implements SensorEventListener {

    public double dTemprature = 0.0f;

    double getValue(){
        return dTemprature;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        dTemprature = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("Sensor", "onAccuracyChanged");
    }
}
