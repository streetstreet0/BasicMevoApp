package com.example.basicmevoapp;

import android.graphics.Color;
import android.graphics.Paint;

import org.osmdroid.views.MapView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import android.util.Log;

public class ParkingDataController {
    private final String parkingUrl;
    private final MapView map;

    /**
     * The standard constructor for the parking data controller
     *
     * @param parkingUrl The url for the API to request
     * @param map The MapView object to display the vehicle data
     */
    public ParkingDataController(String parkingUrl, MapView map) {
        this.parkingUrl = parkingUrl;
        this.map = map;
    }

    /**
     * Generates a JsonObjectRequest for the parking data
     *
     * @return The JsonObjectRequest
     */
    public JsonObjectRequest generateDataRequest() {
        return new JsonObjectRequest(Request.Method.GET, parkingUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d("TEST, parking:", response.toString());
                    parseParkingData(response);
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
                Log.d("ERROR, parking", "An error occurred with the parking data");
                error.printStackTrace();
            }
        });
    }

    /**
     * Parse the parking data received from the Mevo API
     *
     * @param parkingData JSONObject of parking data received from the Mevo API
     */
    private void parseParkingData(JSONObject parkingData) throws JSONException {
        JSONArray polygons = parkingData.getJSONObject("data").getJSONObject("geometry").getJSONArray("coordinates");
        JSONObject properties = parkingData.getJSONObject("data").getJSONObject("properties");

        Log.d("TEST, parking", polygons.toString());
        Log.d("TEST, parking", properties.toString());

        generateParkingPolygons(polygons, properties);
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


            map.getOverlayManager().add(0, polygon);
        }
    }
}
