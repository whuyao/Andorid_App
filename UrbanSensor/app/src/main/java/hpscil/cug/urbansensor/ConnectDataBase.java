package hpscil.cug.urbansensor;

/**
 * Created by astro on 2018/5/9.
 */

import android.util.Log;
import android.webkit.JavascriptInterface;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ConnectDataBase {
    //private Connection connection = null;
   // private Statement smst;
    private String sSQL;

    ConnectDataBase (){
        try {
            Class.forName("com.mysql.jdbc.Driver");
            //connection = DriverManager.getConnection("jdbc:mysql://gz-cdb-c0wxxh0c.sql.tencentcdb.com:62712/UrbanSensor_DB", "yy", "YY98lpzx");
            //smst = connection.createStatement();
                        /*
                    } catch (SQLException ex){
                        Log.e("database: ", ex.toString());
                        Log.e("SQLState: " , ex.getSQLState());
                        Log.e("VendorError: " , String.valueOf(ex.getErrorCode()));
                        */
        } catch (Exception ex){
            Log.e("database register: ", ex.toString());
        }

    }


    public Boolean writeIntoDataBase(String usr_mac, String usr_bssid, String ip_addr,
                                     String datetime, String gps_provider,
                                     double lat, double lon, double altitude, double accu, double speed, double bearing, double dBs,
                                     int weather_code, int motion_level, int confort_level, int population_level,
                                     int car_stream_level, double dTemp, double dHuminity, double dlight, String remarks, Boolean isTrack){

        sSQL = "Insert into UserUploadData_tbl (usr_mac, usr_bssid, ip_addr, dt, gps_provider, " +
                "lat, lon, altitude, accuracy, speed, bearing, " +
                "weather_code, motion_level, confort_level, dBfs, population_level, car_stream_level, temperature, huminity, light, remark, isTrack)" +
                " values (" +
                "'" + usr_mac + "'" + ", " + "'" + usr_bssid + "'"+ ", " +"'" + ip_addr + "'"+ ", " +"'" + datetime + "'"+ ", " + "'" + gps_provider + "'"+ ", " +
                String.format("%.10f", lat)+ ", " + String.format("%.10f", lon) + ", " + String.format("%.10f", altitude)+ ", " + String.format("%.10f", accu)+ ", " + String.format("%.10f", speed)+ ", " + String.format("%.10f", bearing)+ ", " +
                weather_code+ ", " + motion_level + ", " + confort_level + ", " + String.format("%.10f", dBs) + ", " + population_level +
                ", " + car_stream_level + ", " + String.format("%.10f", dTemp)+ ", " + String.format("%.10f", dHuminity) + ", " + String.format("%.10f", dlight) + ", " + "'"+ remarks+ "'"+ ", " + isTrack.toString()
                + ");";

        Log.v("SQL: ", sSQL);

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 反复尝试连接，直到连接成功后退出循环
                while (!Thread.interrupted()) {
                    try {
                        Thread.sleep(100);  // 每隔0.1秒尝试连接
                    } catch (InterruptedException e) {
                        Log.e("Database connection: ", e.toString());
                    }

                    try {
                        String sInSQL = sSQL;
                        //Class.forName("com.mysql.jdbc.Driver");
                        Connection connection = DriverManager.getConnection("jdbc:mysql://119.29.196.240:3306/UrbanSensor_DB?characterEncoding=GBK&useUnicode=true", "guest", "guest");
                        Statement smst = connection.createStatement();
                        smst.execute(sInSQL);
                        smst.close();
                        connection.close();
                        return;
                    } catch (SQLException ex){
                        Log.e("database: ", ex.toString());
                        Log.e("SQLState: " , ex.getSQLState());
                        Log.e("VendorError: " , String.valueOf(ex.getErrorCode()));
                        return;
                    } catch (Exception ex){
                        Log.e("database: ", ex.toString());
                        return;
                    }
                }
            }
        });
        thread.start();

        /*
        try {

            String inserSQL = "Insert into UserUploadData_tbl (usr_mac, usr_bssid, ip_addr, dt, gps_provider, " +
                    "lat, lon, altitude, accuracy, speed, bearing, " +
                    "weather_code, motion_level, confort_level, dBfs, population_level, car_stream_level, remark, isTrack)" +
                    " values (" +
                    "'" + usr_mac + "'" + ", " + "'" + usr_bssid + "'"+ ", " +"'" + ip_addr + "'"+ ", " +"'" + datetime + "'"+ ", " + "'" + gps_provider + "'"+ ", " +
                    lat+ ", " + lon+ ", " + altitude+ ", " + accu+ ", " + speed+ ", " + bearing+ ", " +
                    weather_code+ ", " + motion_level+ ", " + confort_level+ ", " + dBs+ ", " + population_level+ ", " + car_stream_level+ ", " + "'"+ remarks+ "'"+ ", " + isTrack.toString()
                    + ");";

            smst.executeQuery(inserSQL);
            return true;

        } catch (SQLException ex) {
            Log.e("database: ", ex.toString());
            return false;
        }*/


        return true;
    }





}
