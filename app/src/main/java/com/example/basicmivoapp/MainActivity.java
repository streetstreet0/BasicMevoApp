package com.example.basicmivoapp;

import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.basicmivoapp.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

//    private TextView text;
    private ListView testList;
    private String baseUrl = "https://api.mevo.co.nz/public";
    private String parkingEndpoint = "/parking/wellington";
    private String vehicleEndpoint = "/vehicles/wellington";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        text = findViewById(R.id.testData);
        testList = (ListView) findViewById(R.id.testList);


        String url = baseUrl + parkingEndpoint;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // Note that the data might not be a featureCollection, which has the list of coordinates
                    // if it is just a Feature, need another way to deal with it
                    JSONArray features = response.getJSONObject("data").getJSONArray("features");
                    Log.d("TEST", features.toString());

                    ArrayList<double[]> coordinates = getCoordinates(features);
                    Log.d("TEST", coordinates.toString());

                }
                catch (Exception exception) {
                    Log.d("TEST", response.toString());
                    Log.d("ERROR", "failed to get any data");
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
        Volley.newRequestQueue(this).add(request);
    }

    private ArrayList<double[]> getCoordinates(JSONArray features) throws JSONException {
        ArrayList<double[]> coordinates = new ArrayList<double[]>();

        Log.d("TEST", String.valueOf(features.length()));
        for (int featuresNum = 0; featuresNum < features.length(); featuresNum++) {
            JSONObject feature = features.getJSONObject(featuresNum).getJSONObject("geometry");
            String type = feature.getString("type");
            if (type.equals("Point")) {
                JSONArray coordinate = feature.getJSONArray("coordinates");
                coordinates.add(new double[]{coordinate.getDouble(0), coordinate.getDouble(1)});
            }
            else {
                Log.d("TEST", "type = " + type);
            }
        }

        return coordinates;
    }

//    private boolean sameCoordinates(JSONArray coordinatePairingOne, JSONArray coordinatePairingTwo) throws JSONException {
//        return (coordinatePairingOne.getDouble(0) == coordinatePairingTwo.getDouble(0)) &&
//                (coordinatePairingOne.getDouble(1) == coordinatePairingTwo.getDouble(1));
//    }
}

