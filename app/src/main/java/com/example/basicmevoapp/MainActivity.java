package com.example.basicmevoapp;

import android.Manifest;

import android.os.Bundle;

import android.preference.PreferenceManager;

import android.content.Context;
import android.content.pm.PackageManager;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private final String baseUrl = "https://api.mevo.co.nz/public";
    private final String parkingEndpoint = "/parking/wellington";
    private final String vehicleEndpoint = "/vehicles/wellington";

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map;
    private final double defaultLongitude = 174.77557;
    private final double defaultLatitude = -41.28664;
    private final double defaultZoom = 13.5;
    private VehicleDataController vehicleController;
    private ParkingDataController parkingController;


    /**
     * The method starts the application, creates the map, and adds all of the icons onto the map.
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        requestPermissionsIfNecessary(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE});

        // set up the settings for the map
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(defaultZoom);
        GeoPoint startPoint = new GeoPoint(defaultLatitude, defaultLongitude);
        mapController.setCenter(startPoint);

        vehicleController = new VehicleDataController(baseUrl + vehicleEndpoint, map);
        parkingController = new ParkingDataController(baseUrl + parkingEndpoint, map);

        requestData();
    }


    /**
     * This method restarts the app if it gets opened after going to the background
     */
    @Override
    protected void onRestart() {
        super.onRestart();
    }


    /**
     * This method restarts the app if it gets opened after another app enters the foreground (but this app doesn't go to the background)
     */
    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }


    /**
     * This method pauses the app if it gets opened after another app enters the foreground (but this app doesn't go to the background)
     */
    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }


    /**
     * This method requests data from the public Mevo API's and places the vehicle points, and parking polygons on the map.
     */
    private void requestData() {
        JsonObjectRequest vehicleRequest = vehicleController.generateDataRequest();
        JsonObjectRequest parkingRequest = parkingController.generateDataRequest();

        Volley.newRequestQueue(this).add(vehicleRequest);
        Volley.newRequestQueue(this).add(parkingRequest);
    }


    /**
     * This method requests the necessary permissions for the app to function.
     *
     * If a request has already been granted, it does not check again.
     *
     * @param permissions A String[] of the permissions that we are requesting
     */
    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<String>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


}

