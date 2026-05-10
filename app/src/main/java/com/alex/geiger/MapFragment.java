package com.alex.geiger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {

    private MapView map;
    private boolean centeredOnce = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.map);
        map.setMultiTouchControls(true);
        map.getController().setZoom(17.0);

        GeoPoint defaultPoint = new GeoPoint(43.7696, 11.2558);
        map.getController().setCenter(defaultPoint);

        return view;
    }

    public void addRadiationMarker(double lat, double lon, short cpm, double usv_h) {
        if (map == null || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            GeoPoint point = new GeoPoint(lat, lon);

            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle("☢ CPM: " + cpm + " | uSv/h: " + String.format("%.3f", usv_h));
            marker.setSnippet("Lat: " + lat + "\nLon: " + lon);

            map.getOverlays().add(marker);

            if (!centeredOnce) {
                map.getController().setCenter(point);
                centeredOnce = true;
            }

            map.invalidate();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }
}