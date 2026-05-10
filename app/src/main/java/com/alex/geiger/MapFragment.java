package com.alex.geiger;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private MapView map;
    private Button btnHome;
    private Button btnClear;
    private RadiationOverlay radiationOverlay;

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

    private void clearPoints()
    {
        points.clear();

        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> redraw());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        map = view.findViewById(R.id.map);
        btnHome = view.findViewById(R.id.btnHome);
        btnClear = view.findViewById(R.id.btnClear);

        map.setMultiTouchControls(true);
        map.getController().setZoom(15.5);
        map.getController().setCenter(CAMPI_BISENZIO);

        radiationOverlay = new RadiationOverlay();
        map.getOverlays().add(radiationOverlay);

        map.postDelayed(() -> {
            if (map != null) {
                redraw();
                map.invalidate();
            }
        }, 300);

        btnHome.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showHome();
            }
        });

        btnClear.setOnClickListener(v -> {
            clearPoints();
        });

        redraw();

        return view;
    }

    public synchronized void addRadiationMarker(double lat, double lon, short cpm, double usv_h) {
        points.add(new RadiationPoint(lat, lon, cpm, usv_h));

        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> redraw());
    }

    private synchronized List<RadiationPoint> copyPoints() {
        return new ArrayList<>(points);
    }

    private void redraw() {
        if (map == null) return;

        List<RadiationPoint> copy = copyPoints();

        if (copy.size() > 0) {
            RadiationPoint last = copy.get(copy.size() - 1);
            map.getController().setCenter(new GeoPoint(last.lat, last.lon));
        } else {
            map.getController().setCenter(CAMPI_BISENZIO);
        }

        map.invalidate();
    }

    private class RadiationOverlay extends Overlay {

        private final Paint circlePaint = new Paint();
        private final Paint textPaint = new Paint();

        RadiationOverlay() {
            circlePaint.setAntiAlias(true);
            circlePaint.setStyle(Paint.Style.FILL);

            textPaint.setAntiAlias(true);
            textPaint.setTextSize(32.0f);
            textPaint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) {
            if (shadow) return;

            Projection projection = mapView.getProjection();
            Point screenPoint = new Point();

            List<RadiationPoint> copy = copyPoints();

            for (RadiationPoint p : copy) {
                GeoPoint geoPoint = new GeoPoint(p.lat, p.lon);
                projection.toPixels(geoPoint, screenPoint);

                circlePaint.setARGB(220, 255, 220, 0);
                canvas.drawCircle(screenPoint.x, screenPoint.y, 64.0f, circlePaint);

                textPaint.setARGB(255, 0, 0, 0);
                canvas.drawText("☢", screenPoint.x, screenPoint.y + 22.0f, textPaint);

                textPaint.setTextSize(48.0f);
                textPaint.setARGB(255, 0, 0, 0);
                canvas.drawText(
                        p.cpm + " CPM",
                        screenPoint.x,
                        screenPoint.y - 84.0f,
                        textPaint
                );
                textPaint.setTextSize(64.0f);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    
        if (map != null) {
            map.onResume();
    
            map.postDelayed(() -> {
                if (map != null) {
                    map.getController().setZoom(map.getZoomLevelDouble());
                    redraw();
                    map.invalidate();
                }
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

    public void forceRedraw() {
        if (map == null) return;
    
        map.postDelayed(() -> {
            if (map != null) {
                redraw();
                map.invalidate();
            }
        }, 100);
    }
}