package com.alex.geiger;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 100;

    private MapFragment mapFragment;
    private GeigerFragment geigerFragment;

    private LocationManager locationManager;
    private volatile Location lastLocation;

    private final LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private final LocationListener networkListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            lastLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        geigerFragment = new GeigerFragment();
        mapFragment = new MapFragment();

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setUserInputEnabled(false);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this, geigerFragment, mapFragment));

        viewPager.setOffscreenPageLimit(2);
        
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
                    2000L,
                    1.0f,
                    gpsListener
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000L,
                    1.0f,
                    networkListener
            );

        } catch (Exception ignored) {
        }
    }

    public void onRadiationMeasurement(short cpm, double usv_h) {
        try {
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
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(gpsListener);
                locationManager.removeUpdates(networkListener);
            }
        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    public void showMap() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setCurrentItem(1, true);
    
        viewPager.postDelayed(() -> {
            if (mapFragment != null) {
                mapFragment.forceRedraw();
            }
        }, 300);
    }
    
    public void showHome() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setCurrentItem(0, true);
    }
}