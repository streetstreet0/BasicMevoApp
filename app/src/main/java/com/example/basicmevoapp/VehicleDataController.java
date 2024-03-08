package com.example.basicmevoapp;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.MapView;

import android.util.Log;

public class VehicleDataController {
    private final String vehicleUrl;
    private JSONObject vehicleData;
    private final MapView map;

    /**
     * The standard constructor for the vehicle data controller
     *
     * @param vehicleUrl The url for the API to request
     * @param map The MapView object to display the vehicle data
     */
    public VehicleDataController(String vehicleUrl, MapView map) {
        this.vehicleUrl = vehicleUrl;
        this.map = map;
    }

    /**
     * Generates a JsonObjectRequest for the vehicle data
     *
     * @return The JsonObjectRequest
     */
    public JsonObjectRequest generateDataRequest() {
        return new JsonObjectRequest(Request.Method.GET, vehicleUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    vehicleData = response;
                    Log.d("TEST, vehicles:", vehicleData.toString());

                    parseVehicleData(response);
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
                Log.d("ERROR, vehicles", "An error occurred with the vehicle data");
                error.printStackTrace();
            }
        });
    }


    /**
     * Parse the vehicle data received from the Mevo API
     *
     * @param vehicleData JSONObject of vehicle data received from the Mevo API
     */
    private void parseVehicleData(JSONObject vehicleData) throws JSONException {
        JSONArray features = vehicleData.getJSONObject("data").getJSONArray("features");
        Log.d("TEST, vehicles:", features.toString());

        generateVehicleMarkers(features);
    }


    /**
     * Generates the vehicle markers on the map from the given vehicle data.
     *
     * @param features JSONArray of features given by the Mevo API. Each feature contains an icon url and coordinates.
     * @throws JSONException
     */
    public void generateVehicleMarkers(JSONArray features) throws JSONException {
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
}
