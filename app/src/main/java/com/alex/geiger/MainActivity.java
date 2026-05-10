package com.alex.geiger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 100;

    private MapFragment mapFragment;
    private GeigerFragment geigerFragment;

    private LocationManager locationManager;
    private volatile Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		geigerFragment = new GeigerFragment();
        mapFragment = new MapFragment();

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this, geigerFragment, mapFragment));

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        requestLocationPermissionIfNeeded();
        startLocationUpdates();
    }

    private void requestLocationPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION
            );
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            Location gps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location net = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (gps != null) lastLocation = gps;
            else if (net != null) lastLocation = net;

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    1,
                    location -> lastLocation = location
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    1,
                    location -> lastLocation = location
            );

        } catch (Exception ignored) {
        }
    }

    public void onRadiationMeasurement(short cpm, double usv_h) {
        Location loc = lastLocation;

        if (loc == null || mapFragment == null) {
            return;
        }

        mapFragment.addRadiationMarker(
                loc.getLatitude(),
                loc.getLongitude(),
                cpm,
                usv_h
        );
    }

    @Override
    protected void onDestroy() {
        try {
            locationManager.removeUpdates(location -> {});
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }
}