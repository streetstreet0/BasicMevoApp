package com.example.basicmevoapp;

import android.Manifest;
import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
//import android.graphics.drawable.BitmapDrawable;
//import android.media.Image;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
//import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

//import android.preference.PreferenceManager;
import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;

//import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import androidx.core.view.WindowCompat;
//import androidx.navigation.NavController;
//import androidx.navigation.Navigation;
//import androidx.navigation.ui.AppBarConfiguration;
//import androidx.navigation.ui.NavigationUI;

//import com.example.basicmivoapp.databinding.ActivityMainBinding;

//import android.view.Menu;
//import android.view.MenuItem;
//import android.widget.ArrayAdapter;
//import android.widget.ListView;
//import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.views.overlay.Marker;
//import org.osmdroid.views.overlay.OverlayItem;

//import java.io.IOException;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

//    private MainActivity activity = this;
//    private TextView text;
//    private ListView testList;
//    private ArrayList<String> dataList;

    private String baseUrl = "https://api.mevo.co.nz/public";
    private String parkingEndpoint = "/parking/wellington";
    private String vehicleEndpoint = "/vehicles/wellington";

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map;
    private final double defaultLongitude = 174.77557;
    private final double defaultLatitude = -41.28664;
    private final double defaultZoom = 13.5;


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

//        testList = (ListView) findViewById(R.id.testList);
        requestData();
    }


    /**
     * This method restarts the app if it gets opened after going to the background
     */
    @Override
    protected void onRestart() {
        super.onRestart();

//        requestDataTest();
    }


    /**
     * This method restarts the app if it gets opened after another app enters the foreground (but this app doesn't go to the background)
     */
    @Override
    protected void onResume() {
        super.onResume();

        map.onResume();

//        requestDataTest();
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
        // The request for vehicle data
        JsonObjectRequest vehicleRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + vehicleEndpoint, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray features = response.getJSONObject("data").getJSONArray("features");
                    Log.d("TEST, vehicles:", features.toString());

                    generateVehicleMarkers(features);
                }
                catch (Exception exception) {
                    Log.d("TEST, vehicles", response.toString());
                    Log.d("ERROR, vehicles", "failed to parse vehicle data");
                    exception.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR, vehicles", "An error occured with the vehicle data");
                error.printStackTrace();
            }
        });

        // The request for parking data
        JsonObjectRequest parkingRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + parkingEndpoint, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray polygons = response.getJSONObject("data").getJSONObject("geometry").getJSONArray("coordinates");
                    JSONObject properties = response.getJSONObject("data").getJSONObject("properties");
                    Log.d("TEST, parking", polygons.toString());
                    Log.d("TEST, parking", properties.toString());

                    generateParkingPolygons(polygons, properties);
                }
                catch (Exception exception) {
                    Log.d("TEST, parking", response.toString());
                    Log.d("ERROR, parking", "failed to parse parking data");
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

        // Send the requests
        Volley.newRequestQueue(this).add(vehicleRequest);
        Volley.newRequestQueue(this).add(parkingRequest);
    }


    /**
     * Generates the vehicle markers on the map from the given vehicle data.
     *
     * @param features JSONArray of features given by the Mevo API. Each feature contains an icon url and coordinates.
     * @throws JSONException
     */
    private void generateVehicleMarkers(JSONArray features) throws JSONException {
        // for each vehicle
        for (int featureNum = 0; featureNum < features.length(); featureNum++) {
            // each feature is a vehicle
            JSONObject vehicle = features.getJSONObject(featureNum);
            JSONArray vehicleCoordinates = vehicle.getJSONObject("geometry").getJSONArray("coordinates");
            double longitude = vehicleCoordinates.getDouble(0);
            double latitude = vehicleCoordinates.getDouble(1);

            GeoPoint vehicleLocation = new GeoPoint(latitude, longitude);
            String vehicleImageUrl = vehicle.getJSONObject("properties").getString("iconUrl");


            // add a marker to the map for the vehicle
            Marker vehicleMarker = new Marker(map);
            vehicleMarker.setPosition(vehicleLocation);
            vehicleMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            map.getOverlays().add(vehicleMarker);
            // For now, the text for a vehicle is just the url for the Mevo marker image
            vehicleMarker.setTitle(vehicleImageUrl);
        }
    }


    /**
     * Generates the parking polygons on the map from the given polygon data.
     *
     * @param polygons JSONArray of polygons given by the Mevo API. Each polygon is a list of coordinates
     * @throws JSONException
     */
    private void generateParkingPolygons(JSONArray polygons, JSONObject properties) throws  JSONException {
        // for each parking location (polygon)
        for (int polygonNum = 0; polygonNum < polygons.length(); polygonNum++) {
            JSONArray JSONpolygon = polygons.getJSONArray(polygonNum);
            // convert the JSONArray of coordinates to a ArrayList of GeoPoints
            ArrayList<GeoPoint> polygonGeoPoints = new ArrayList<GeoPoint>();

            // For each coordinate in the JSONArray
            for (int coordinateNum = 0; coordinateNum < JSONpolygon.length(); coordinateNum++) {
                JSONArray JSONCoordinate = JSONpolygon.getJSONArray(coordinateNum);
                double longitude = JSONCoordinate.getDouble(0);
                double latitude = JSONCoordinate.getDouble(1);

                // add an equivalent GeoPoint to the ArrayList of GeoPoints
                polygonGeoPoints.add(new GeoPoint(latitude, longitude));
            }
            // polygons needs to be closed
            // i.e the first and last point need to be the same point
            if (polygonGeoPoints.get(0) != polygonGeoPoints.get(polygonGeoPoints.size()-1)) {
                polygonGeoPoints.add(polygonGeoPoints.get(0));
            }

            // create the map polygon
            Polygon polygon = new Polygon();
            polygon.setPoints(polygonGeoPoints);

            // customise the polygon
            Paint polygonPaint = polygon.getFillPaint();
            polygonPaint.setColor(Color.parseColor(properties.getString("fill")));
            polygonPaint.setAlpha((int) (255 * properties.getDouble("fill-opacity")));
            polygonPaint.setStrokeWidth((float) properties.getDouble("stroke-width"));
            polygon.setStrokeColor(Color.parseColor(properties.getString("stroke")));


//            Log.d("TEST", "generated polygon " + polygonNum + " with starting location: latitude = " + polygonGeoPoints.get(0).getLatitude() + ", longitude = " + polygonGeoPoints.get(0).getLongitude());
            // add the polygon to the map
            // adding it at index 0 should result in the polygons always being underneath the vehicle pins
            map.getOverlayManager().add(0, polygon);
        }
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


    /***
     * The following methods were used when developing the app, They serve no purpose anymore.
     */


//    /**
//     * Requests vehicle and parking data from the Mevo API's, then displays them as a list.
//     *
//     * Only works if the testList object is uncommented in activity_main.xml
//     */
//    private void requestDataTest() {
//        // clear any old data before retrieving new data
//        dataList = new ArrayList<String>();
//        // The request for vehicle data
//        JsonObjectRequest vehicleRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + vehicleEndpoint, null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                try {
//                    JSONArray features = response.getJSONObject("data").getJSONArray("features");
//                    Log.d("TEST", features.toString());
//
//                    ArrayList<double[]> coordinates = getCoordinates(features);
////                    for (double[] coordinate : coordinates) {
////                        Log.d("TEST", coordinate[0] + ", " + coordinate[1]);
////                    }
//
//                    // add the vehicles to the datalist as coordinates
//                    dataList.addAll(coordinatesToString(coordinates));
//
//                    // display the updated datalist
//                    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, R.layout.list_item, R.id.list_item, dataList);
//                    testList.setAdapter(listAdapter);
//                }
//                catch (Exception exception) {
//                    Log.d("TEST", response.toString());
//                    Log.d("ERROR", "failed to get any vehicle data");
//                    exception.printStackTrace();
//                }
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("ERROR", "An error occured");
//                error.printStackTrace();
//            }
//        });
//
//        // the request for parking data
//        JsonObjectRequest parkingRequest = new JsonObjectRequest(Request.Method.GET, baseUrl + parkingEndpoint, null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                try {
//                    JSONArray polygons = response.getJSONObject("data").getJSONObject("geometry").getJSONArray("coordinates");
//                    Log.d("TEST", polygons.toString());
//
//                    // add the polygons to the datalist as strings
//                    ArrayList<ArrayList<double[]>> polygonList = generatePolygonList(polygons);
//                    dataList.addAll(polygonToString(polygonList));
//
//                    // display the updated datalist
//                    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, R.layout.list_item, R.id.list_item, dataList);
//                    testList.setAdapter(listAdapter);
//                }
//                catch (Exception exception) {
//                    Log.d("TEST", response.toString());
//                    Log.d("ERROR", "failed to get any parking data");
//                    exception.printStackTrace();
//                }
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Log.d("ERROR", "An error occured");
//                error.printStackTrace();
//            }
//        });
//
//        // Send the requests
//        Volley.newRequestQueue(this).add(vehicleRequest);
//        Volley.newRequestQueue(this).add(parkingRequest);
//    }
//
//
//    /**
//     * This method takes the list of features, and converts them into an arraylist of coordinates.
//     *
//     * @param features A JSONArray of features. Each feature contains a JSONObject called "geometry" which contains a JSONArray called "coordinates" which contains the coordinates.
//     * @return An array list of coordinates, each coordinate is an array of doubles.
//     * @throws JSONException
//     */
//    private ArrayList<double[]> getCoordinates(JSONArray features) throws JSONException {
//        ArrayList<double[]> coordinates = new ArrayList<double[]>();
//
////        Log.d("TEST", String.valueOf(features.length()));
//        // for each feature
//        for (int featuresNum = 0; featuresNum < features.length(); featuresNum++) {
//            JSONObject feature = features.getJSONObject(featuresNum).getJSONObject("geometry");
//            JSONArray coordinate = feature.getJSONArray("coordinates");
//            coordinates.add(new double[]{coordinate.getDouble(0), coordinate.getDouble(1)});
//        }
//
//        return coordinates;
//    }
//
//
//    /**
//     * This method takes a list of coordinates and converts them into a list of strings
//     *
//     * @param coordinates An array list of coordinates, each coordinate is an array of doubles.
//     * @return An array list of strings that represent the coordinates
//     */
//    private ArrayList<String> coordinatesToString(ArrayList<double[]> coordinates) {
//        ArrayList<String> coordinateStrings = new ArrayList<String>();
//
//        for (double[] coordinate : coordinates) {
//            String coordinateString = "(" + coordinate[0] + ", " + coordinate[1] + ")";
//            coordinateStrings.add(coordinateString);
//        }
//
//        return coordinateStrings;
//    }
//
//
//    /**
//     * This method takes the list of JSONpolygons, and converts them into an arraylist of polygons represented by a list of coordinates.
//     *
//     * Each polygon is a list of coordinates. Each coordinate is represented by an array of numbers.
//     *
//     * @param polygons A JSONArray of polygons. Each polygon contains a list of coordinates. Each coordinate is a list of doubles.
//     * @return An ArrayList<ArrayList<double[]>> that represents a list of polygons. Each polygon is an ArrayList of coordinates, which are themselves an array of doubles.
//     * @throws JSONException
//     */
//    private ArrayList<ArrayList<double[]>> generatePolygonList(JSONArray polygons) throws JSONException {
//        ArrayList<ArrayList<double[]>> polygonsList = new ArrayList<ArrayList<double[]>>();
//
//        for (int polygonNum = 0; polygonNum < polygons.length(); polygonNum++) {
//            JSONArray polygon = polygons.getJSONArray(polygonNum);
//            ArrayList<double[]> polygonCoordList = new ArrayList<double[]>();
//
//            for (int coordinateNum = 0; coordinateNum < polygon.length(); coordinateNum++) {
//                JSONArray JSONcoordinate = polygon.getJSONArray(coordinateNum);
//                double[] coordinate = new double[] {JSONcoordinate.getDouble(0), JSONcoordinate.getDouble(1)};
//                polygonCoordList.add(coordinate);
//            }
//
//            polygonsList.add(polygonCoordList);
//        }
//
//        return polygonsList;
//    }
//
//
//    /**
//     * This method converts a list of polygons into a list of strings that represent those polygons
//     *
//     * @param polygons An ArrayList<ArrayList<double[]>> that represents a list of polygons. Each polygon is an ArrayList of coordinates, which are themselves an array of doubles.
//     * @return An ArrayLlist<String> that represents polygons
//     */
//    private ArrayList<String> polygonToString(ArrayList<ArrayList<double[]>> polygons) {
//        ArrayList<String> polygonStrings = new ArrayList<String>();
//
//        for (ArrayList<double[]> polygon: polygons) {
//            ArrayList<String> polygonCoordStrings = coordinatesToString(polygon);
//
//            String polygonString = "[";
//            for (String coordinate : polygonCoordStrings) {
//                polygonString += coordinate + ", ";
//            }
//            // need to remove the final ", " and add a closing ]
//            polygonString = polygonString.substring(0, polygonString.length()-2) + "]";
//
//            polygonStrings.add(polygonString);
//        }
//
//        return polygonStrings;
//    }
}

