package com.prakriti.taxiapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.w3c.dom.Text;

import java.util.List;

// works better on physical device - with internet/wifi services on
public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, LocationListener {
// access permission for coarse location -> dangerous permission
// add metadata for google play services in manifests file
// here, we use FusedLocationProviderApi interface for user location -> deprecated
// added functionality - auto updates if location has changed

    public static final String TAG = "LocationTag";
    private static final int LOC_REQ_CODE = 3;
    private static final int CONN_REQ_CODE = 99;

    private GoogleApiClient googleApiClient;
    //    private Location location; // holds lat & long
    private TextView txtLocation;

    // UI Components
    private EditText edtDestination, edtSpeed, edtMetresPerMile;
    private TextView txtDistanceValue, txtTimeRem;
    private Button btnGetData;

    private String destLocationAddress = "";
    private TaxiManager taxiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLocation = findViewById(R.id.txtLocation);

        // register this class to receive conn messages from this api client
        googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addConnectionCallbacks(MainActivity.this)
                .addOnConnectionFailedListener(MainActivity.this)
                .addApi(LocationServices.API)
                .build();

        // init UI
        edtDestination = findViewById(R.id.edtDestination);
        edtMetresPerMile = findViewById(R.id.edtMetresPerMile);
        edtSpeed = findViewById(R.id.edtSpeed);
        txtDistanceValue = findViewById(R.id.txtDistanceValue);
        txtTimeRem = findViewById(R.id.txtTimeRem);
        findViewById(R.id.btnGetData).setOnClickListener(this);

        taxiManager = new TaxiManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // since we dont want loc updates when app is in background & avoid battery draining, onPause is called when app is in bg
        FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
        fusedLocationProviderApi.removeLocationUpdates(googleApiClient, MainActivity.this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnGetData: // null check -
                String destination = edtDestination.getText().toString();
                boolean isGeoCoding = true; // geo-coding is process of converting lat & long to human readable text
                if (!destination.equals(destLocationAddress)) {
                    destLocationAddress = destination; // assign edt value to String
                    // use GeoCoder
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        List<Address> myAddresses = geocoder.getFromLocationName(destLocationAddress, 4); // no of most accurate results
                        if (myAddresses != null) {
                            double latitude = myAddresses.get(0).getLatitude();
                            double longitude = myAddresses.get(0).getLongitude();
                            Location locationAddress = new Location("MyDestination");
                            locationAddress.setLatitude(latitude);
                            locationAddress.setLongitude(longitude);
                            // use TaxiManager obj
                            taxiManager.setDestinationLocation(locationAddress);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        isGeoCoding = false; // if goes wrong, app will not geo-code, no battery consumption
                    }
                }
                // user permission
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // use main entry point for location
                    FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
                    Location currLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);

                    if (currLocation != null && isGeoCoding) { // if set to true
                        // get miles per hour & metres per mile
                        int metres = Integer.parseInt(edtMetresPerMile.getText().toString()); // validation checks for edt values
                        float miles = Float.parseFloat(edtSpeed.getText().toString());
                        // set distance bw curr & dest loc
                        String txtDistanceValueOld = txtDistanceValue.getText().toString();
                        txtDistanceValue.setText(txtDistanceValueOld + taxiManager.returnMilesBetweenCurrentAndDestination(currLocation, metres));
                        // set time remaining
                        String txtTimeRemOld = txtTimeRem.getText().toString();
                        txtTimeRem.setText(txtTimeRemOld + taxiManager.returnTimeLeftToReachDestination(currLocation, miles, metres));
                    }
                } else {
                    txtLocation.setText(R.string.permission_denied);
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOC_REQ_CODE);
                }
                break;
        }
    }

    // from ConnCallbacks
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected to user location!");
//        showUserLocation();
        // code for auto location update if changed, update the textviews
        FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;

        @SuppressLint("RestrictedApi") LocationRequest locationRequest = new LocationRequest(); // check dis ------
        locationRequest.setInterval(10000); // request loc updates in 10 sec
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY); // default, affects battery drainage
        locationRequest.setSmallestDisplacement(5); // every 5m displacement, get loc request
        if (googleApiClient.isConnected()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, MainActivity.this);
            }
        }
        else {
            googleApiClient.connect(); // try to connect again if not connected
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection is suspended");
    }

    // from OnConnFailedListener
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Connected Failed");
        // need access to google play services to use this app features -> ConnectionResult class provides error codes for ConnFailed listener
        if (connectionResult.hasResolution()) {
            // returns true if foll method will start intents requiring user interaction
            try {
                connectionResult.startResolutionForResult(MainActivity.this, CONN_REQ_CODE);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, "SEND_INTENT_ERROR", e);
                e.printStackTrace();
            } // once user has interacted with an intent, override onActivityResult() to connect to play services again
        } else { // cannot access google play services, exit app
            Toast.makeText(MainActivity.this, R.string.play_services_error, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        onClick(findViewById(R.id.btnGetData)); // get loc automatically w/o clicking button
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CONN_REQ_CODE && resultCode == RESULT_OK) {
            googleApiClient.connect(); // if successful, will call the onConnected() method
        }
    }

/*
    private void showUserLocation() {
        // access runtime permission
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // use interface, cannot instantiate
            FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
            location = fusedLocationProviderApi.getLastLocation(googleApiClient);
            // replace above api with location provider client, work with Task class
//            FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
//            location = client.getLastLocation();

            // Defensive programming, check for conditions, ex. null values
            if(location != null) {
                double latitute = location.getLatitude();
                double longitude = location.getLongitude();
                txtLocation.setText(latitute + ", " + longitude);
            }
            else { // location is null
                txtLocation.setText(R.string.access_error);
            }
        }
        else { // not granted
            txtLocation.setText(R.string.permission_denied);
            // ask user to allow permission
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, LOC_REQ_CODE);
        }
    }

 */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOC_REQ_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission granted
//            showUserLocation();
            FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location currLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);
            }
        }
    }

}