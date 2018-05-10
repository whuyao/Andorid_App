package hpscil.cug.urbansensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.math.BigDecimal;

/**
 * Created by astro on 2018/5/10.
 */

public class LightListener implements SensorEventListener {
    public double dLight = 0.0f;

    double getValue(){
        return dLight;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        dLight = event.values[0];
        //BigDecimal bd = new BigDecimal(temperatureValue);
        //dPressure = bd.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("Sensor", "onAccuracyChanged");
    }
}
