package com.example.basicmivoapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.basicmivoapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private MainActivity activity = this;
//    private TextView text;
    private ListView testList;
    private String baseUrl = "https://api.mevo.co.nz/public";
    private String parkingEndpoint = "/parking/wellington";
    private String vehicleEndpoint = "/vehicles/wellington";
    private ArrayList<String> dataList;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map;
    private final double defaultLongitude = 174.77557;
    private final double defaultLatitude = -41.28664;
    private final double defaultZoom = 12.5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        requestPermissionsIfNecessary(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE});


        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(defaultZoom);
        GeoPoint startPoint = new GeoPoint(defaultLatitude, defaultLongitude);
        mapController.setCenter(startPoint);

//        testList = (ListView) findViewById(R.id.testList);
//        requestData();
    }

    @Override
    protected void onRestart() {
        super.onRestart();

//        requestData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        map.onResume();

//        requestData();
    }

    @Override
    protected void onPause() {
        super.onPause();

        map.onPause();
    }

    private void requestData() {
        // clear any old data before retrieving new data
        dataList = new ArrayList<String>();
        JsonObjectRequest vehicleRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + vehicleEndpoint, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray features = response.getJSONObject("data").getJSONArray("features");
                    Log.d("TEST", features.toString());

                    ArrayList<double[]> coordinates = getCoordinates(features);
//                    for (double[] coordinate : coordinates) {
//                        Log.d("TEST", coordinate[0] + ", " + coordinate[1]);
//                    }

                    dataList.addAll(coordinatesToString(coordinates));
                    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, R.layout.list_item, R.id.list_item, dataList);
                    testList.setAdapter(listAdapter);
                }
                catch (Exception exception) {
                    Log.d("TEST", response.toString());
                    Log.d("ERROR", "failed to get any vehicle data");
                    exception.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR", "An error occured");
                error.printStackTrace();
            }
        });

        JsonObjectRequest parkingRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + parkingEndpoint, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray polygons = response.getJSONObject("data").getJSONObject("geometry").getJSONArray("coordinates");
                    Log.d("TEST", polygons.toString());

                    ArrayList<ArrayList<double[]>> polygonList = generatePolygonList(polygons);
                    dataList.addAll(polygonToString(polygonList));

                    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, R.layout.list_item, R.id.list_item, dataList);
                    testList.setAdapter(listAdapter);
                }
                catch (Exception exception) {
                    Log.d("TEST", response.toString());
                    Log.d("ERROR", "failed to get any parking data");
                    exception.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR", "An error occured");
                error.printStackTrace();
            }
        });

        Volley.newRequestQueue(this).add(vehicleRequest);
        Volley.newRequestQueue(this).add(parkingRequest);
    }

    private ArrayList<double[]> getCoordinates(JSONArray features) throws JSONException {
        ArrayList<double[]> coordinates = new ArrayList<double[]>();

//        Log.d("TEST", String.valueOf(features.length()));
        for (int featuresNum = 0; featuresNum < features.length(); featuresNum++) {
            JSONObject feature = features.getJSONObject(featuresNum).getJSONObject("geometry");
            JSONArray coordinate = feature.getJSONArray("coordinates");
            coordinates.add(new double[]{coordinate.getDouble(0), coordinate.getDouble(1)});
        }

        return coordinates;
    }

    private ArrayList<String> coordinatesToString(ArrayList<double[]> coordinates) {
        ArrayList<String> coordinateStrings = new ArrayList<String>();

        for (double[] coordinate : coordinates) {
            String coordinateString = "(" + coordinate[0] + ", " + coordinate[1] + ")";
            coordinateStrings.add(coordinateString);
        }

        return coordinateStrings;
    }

    private ArrayList<ArrayList<double[]>> generatePolygonList(JSONArray polygons) throws JSONException {
        ArrayList<ArrayList<double[]>> polygonsList = new ArrayList<ArrayList<double[]>>();

        for (int polygonNum = 0; polygonNum < polygons.length(); polygonNum++) {
            JSONArray polygon = polygons.getJSONArray(polygonNum);
            ArrayList<double[]> polygonCoordList = new ArrayList<double[]>();

            for (int coordinateNum = 0; coordinateNum < polygon.length(); coordinateNum++) {
                JSONArray JSONcoordinate = polygon.getJSONArray(coordinateNum);
                double[] coordinate = new double[] {JSONcoordinate.getDouble(0), JSONcoordinate.getDouble(1)};
                polygonCoordList.add(coordinate);
            }

            polygonsList.add(polygonCoordList);
        }

        return polygonsList;
    }

    private ArrayList<String> polygonToString(ArrayList<ArrayList<double[]>> polygons) {
        ArrayList<String> polygonStrings = new ArrayList<String>();

        for (ArrayList<double[]> polygon: polygons) {
            ArrayList<String> polygonCoordStrings = coordinatesToString(polygon);

            String polygonString = "[";
            for (String coordinate : polygonCoordStrings) {
                polygonString += coordinate + ", ";
            }
            polygonString = polygonString.substring(0, polygonString.length()-2) + "]";

            polygonStrings.add(polygonString);
        }

        return polygonStrings;
    }

//    private boolean sameCoordinates(JSONArray coordinatePairingOne, JSONArray coordinatePairingTwo) throws JSONException {
//        return (coordinatePairingOne.getDouble(0) == coordinatePairingTwo.getDouble(0)) &&
//                (coordinatePairingOne.getDouble(1) == coordinatePairingTwo.getDouble(1));
//    }

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

