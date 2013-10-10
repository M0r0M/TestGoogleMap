package com.moro.matth.test.application.testgooglemap;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends Activity implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener, 
LocationListener {
	
	/*
     * Define a request code to send to Google Play services
     * This code is returned in Activity.onActivityResult
     */
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	
	// Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    private static final int FASTEST_INTERVAL = 1000;
    public static final int UPDATE_INTERVAL = 5000;
    LocationClient mLocationClient;
    boolean mUpdatesRequested;
    
    // Global variable to hold the current location
    Location mCurrentLocation;
    
    // preferences
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
	
    // the location object
    Location mLocation;
    
	private GoogleMap gMap;
	
	private Button PathButton;
	private boolean makingPath = false;
	
	private Marker marker;
	private Polyline polyline;
	private boolean newPolyline;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        
        // Open the shared preferences
        mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        // Get a SharedPreferences editor
        mEditor = mPrefs.edit();
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
        // Start with updates turned off
        mUpdatesRequested = false;
		
        // on récupere la map
        if (gMap == null) {
        	gMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        	initMap();
        }
		
		PathButton = (Button)findViewById(R.id.Path);
	}

	@Override
    protected void onPause() {
        // Save the current setting for updates
        mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
        mEditor.commit();
        super.onPause();
    }
	
	@Override
    protected void onStart() {
		super.onStart();
        mLocationClient.connect();
    }
	
	@Override
    protected void onResume() {
        /*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         */
		super.onResume();
        if (mPrefs.contains("KEY_UPDATES_ON")) {
            mUpdatesRequested =
                    mPrefs.getBoolean("KEY_UPDATES_ON", false);

        // Otherwise, turn off location updates
        } else {
            mEditor.putBoolean("KEY_UPDATES_ON", false);
            mEditor.commit();
        }
    }
	
	/*
     * Called when the Activity is no longer visible at all.
     * Stop updates and disconnect.
     */
    @Override
    protected void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            //removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();
        super.onStop();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	/*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        // If already requested, start periodic updates
        if (mUpdatesRequested) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }
    }
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }
    
    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            //showErrorDialog(connectionResult.getErrorCode());
        }
    }
    
    // Define the callback method that receives location updates
    @Override
    public void onLocationChanged(Location location) {
        // Report to the UI that the location was updated
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        mLocation = location;
    }
    public void initMap() {
		gMap.setOnMapClickListener(new OnMapClickListener() {
			@Override
			public void onMapClick(LatLng point) {
				double lat = point.latitude;
				double lng = point.longitude;
				String msg = "Clicked on :" + Double.toString(lat) + ", " + Double.toString(lng);
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
			}
		});
		
		// get the map location aware
		//gMap.setMyLocationEnabled(true);
		// change the presentation
		//UiSettings settings = gMap.getUiSettings();
		//settings.setMyLocationButtonEnabled(true);
	}
    
    public void showToastLocation(double Lat, double Long) {
    	String msg = "Updated Location: " +
                Double.toString(Lat) + "," +
                Double.toString(Long);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    
    public void GoToLocation(double lat, double lng, int Zoom) {
    	LatLng CurrentLatLong = new LatLng(lat, lng);
    	CameraUpdate camUpdate = CameraUpdateFactory.newLatLngZoom(CurrentLatLong,Zoom);
    	gMap.animateCamera(camUpdate);
    }
    
    public void addMarker(double lat, double lng) {
    	LatLng CurrentLatLong = new LatLng(lat, lng);
    	marker = gMap.addMarker(new MarkerOptions().title("You are here").position(CurrentLatLong));
    }
    
    public void Locate(View v) {
    	mLocation = mLocationClient.getLastLocation();
    	double Lat = mLocation.getLatitude();
    	double Long = mLocation.getLongitude();
    	showToastLocation(Lat,Long);
    	addMarker(Lat,Long);
    	GoToLocation(Lat,Long,12);
    }
    
    public void Reset(View v) {
    	if (marker != null) {
    		marker.remove();
    	}
    	if (polyline != null) {
    		polyline.remove();
    	}
    	GoToLocation(0,0,1);
    }
    
    public void createPath(View v) {
    	UiSettings settings = gMap.getUiSettings();
    	makingPath = !makingPath;
    	if (makingPath) {
    		PathButton.setText("Finish");
    		
    		settings.setScrollGesturesEnabled(false);
    		settings.setZoomGesturesEnabled(false);
    		settings.setZoomControlsEnabled(false);
    		
    		newPolyline = true;
    		if (polyline != null) {
	    		polyline.remove();
	    	}
    		
    		gMap.setOnMapClickListener(new OnMapClickListener() {
    			@Override
    			public void onMapClick(LatLng point) {
    				if (newPolyline) {
    					newPolyline = false;
    					
    					PolylineOptions polyOpts = new PolylineOptions().add(point);
    					List<LatLng> latLngList = new ArrayList<LatLng>();
    					latLngList.add(point);
    					polyline = gMap.addPolyline(polyOpts);
    					
    				} else {
    	    			List<LatLng> latLngList = polyline.getPoints();
    	    			latLngList.add(point);
    	    			polyline.setPoints(latLngList);
    	    		}
    			}
    		});
    	} else {
    		PathButton.setText("Create Path");
    		
    		if (polyline != null) {
    			List<LatLng> latLngList = new ArrayList<LatLng>();
    			latLngList = polyline.getPoints();
    			// close the loop
    			latLngList.add(latLngList.get(0));
    			polyline.setPoints(latLngList);
    		}
    		
    		settings.setScrollGesturesEnabled(true);
    		settings.setZoomGesturesEnabled(true);
    		settings.setZoomControlsEnabled(true);
    	}
    }

}
