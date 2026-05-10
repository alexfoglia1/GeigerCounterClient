package com.alex.geiger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MapFragment extends Fragment {

    private MapView map;
    private Button btnHome;
    private boolean centeredOnce = false;

    private static final GeoPoint CAMPI_BISENZIO =
            new GeoPoint(43.8245, 11.1306);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.map);
        btnHome = view.findViewById(R.id.btnHome);

        map.setMultiTouchControls(true);
        map.getController().setZoom(15.5);
        map.getController().setCenter(CAMPI_BISENZIO);

        btnHome.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showHome();
            }
        });

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

        if (map != null) {
            map.onResume();

            map.postDelayed(() -> {
                map.invalidate();
                map.getController().setCenter(map.getMapCenter());
            }, 300);
        }
    }

    @Override
    public void onPause() {
        if (map != null) {
            map.onPause();
        }

        super.onPause();
    }
}