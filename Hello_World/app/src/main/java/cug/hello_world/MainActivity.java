package cug.hello_world;

import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.provider.Contacts;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.AndroidException;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.Console;
import java.security.Permission;
import java.util.List;
import java.util.jar.Manifest;


public class MainActivity extends AppCompatActivity {

    private TextView postionView;
    private Button locBtn;
    private LocationManager locationManager;
    private String locationProvider;

    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    Boolean mbCanGetPosition;


    protected void showText (){
        System.out.println("Test Hello World!");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //here is lat lon obtain code
        postionView = (TextView) findViewById(R.id.latLonView);
        locBtn = (Button) findViewById(R.id.locBtn);

        mbCanGetPosition = false;
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
        }
        else {
            mbCanGetPosition = true;
            testLocObtain();
        }

        if (mbCanGetPosition) {
            locBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testLocObtain();
                    Toast.makeText(getApplicationContext(), "Update location.", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    protected void testLocObtain(){
        try{
            //获取可用的位置管理器
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = locationManager.getProviders(true);


            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            locationProvider = locationManager.getBestProvider(criteria, true);
            if (locationProvider == null){
                Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
                return ;
            }


            //获取Location
            Location location = locationManager.getLastKnownLocation(locationProvider);
            if(location!=null){
                //不为空,显示地理位置经纬度
                showLocation(location);
            }


            //监视地理位置变化
            locationManager.requestLocationUpdates(locationProvider, 1000, 0.5f , locationListener);
        } catch (Exception ex){
            //Log.println(ex.getMessage());
            Toast.makeText(this, "location service error.", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                } else {
                    // permission denied
                }
                break;
            }

            case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted
                    mbCanGetPosition = true;
                    testLocObtain();

                } else {
                        // permission denied
                    Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
                }
                    break;
            }

        }
    }


    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */
      LocationListener locationListener =  new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            showLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };



    private void showLocation(Location location){
        String locationStr = "Lat: " + location.getLatitude() +"\n"
                        + "Lon: " + location.getLongitude();
        postionView.setText(locationStr);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
    }


}
