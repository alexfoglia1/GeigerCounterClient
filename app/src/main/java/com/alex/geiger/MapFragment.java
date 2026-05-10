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

import java.util.ArrayList;

public class MapFragment extends Fragment {

    private MapView map;
    private Button btnHome;

    private static final GeoPoint CAMPI_BISENZIO =
            new GeoPoint(43.8245, 11.1306);

    private final ArrayList<RadiationPoint> points = new ArrayList<>();

    private static class RadiationPoint {
        double lat;
        double lon;
        short cpm;
        double usv_h;

        RadiationPoint(double lat, double lon, short cpm, double usv_h) {
            this.lat = lat;
            this.lon = lon;
            this.cpm = cpm;
            this.usv_h = usv_h;
        }
    }

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

        redrawMarkers();

        return view;
    }

    public synchronized void addRadiationMarker(double lat, double lon, short cpm, double usv_h) {
        points.add(new RadiationPoint(lat, lon, cpm, usv_h));

        if (getActivity() == null) {
            return;
        }

        getActivity().runOnUiThread(() -> {
            try {
                redrawMarkers();
            } catch (Exception ignored) {
            }
        });
    }

    private synchronized void redrawMarkers() {
        if (map == null) {
            return;
        }

        map.getOverlays().clear();

        for (RadiationPoint p : points) {
            GeoPoint point = new GeoPoint(p.lat, p.lon);

            Marker marker = new Marker(map);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            marker.setTitle("CPM: " + p.cpm + " | uSv/h: " + String.format("%.3f", p.usv_h));
            marker.setSnippet("Lat: " + p.lat + "\nLon: " + p.lon);

            map.getOverlays().add(marker);
        }

        if (points.size() == 0) {
            map.getController().setCenter(CAMPI_BISENZIO);
        } else {
            RadiationPoint last = points.get(points.size() - 1);
            map.getController().setCenter(new GeoPoint(last.lat, last.lon));
        }

        map.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (map != null) {
            map.onResume();
            redrawMarkers();
            map.invalidate();
        }
    }

    @Override
    public void onPause() {
        if (map != null) {
            map.onPause();
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        map = null;
        btnHome = null;
        super.onDestroyView();
    }
}