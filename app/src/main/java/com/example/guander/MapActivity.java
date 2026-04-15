package com.example.guander;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://guander-api.tomas-gonzalezz.workers.dev";
    private static final int REQUEST_LOCATION = 101;

    private MapView mapView;
    private LinearLayout llPlaces;
    private ProgressBar pbMapLoading;
    private EditText etSearch;
    private LinearLayout llSubFilters;
    private TextView tvFilterChevron;

    private boolean filtersExpanded = false;
    private String currentFilter = "all";
    private String userEmail = "";
    private double userLat = -34.6037;
    private double userLng = -58.3816;
    private boolean locationAvailable = false;
    private final List<JSONObject> allPlaces = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // OSMDroid config MUST come before setContentView
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail() != null ? user.getEmail() : "";
        } else {
            userEmail = getSharedPreferences("guander_prefs", MODE_PRIVATE).getString("email_auth", null);
        }
        if (userEmail == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        mapView = findViewById(R.id.map_view);
        llPlaces = findViewById(R.id.ll_places);
        pbMapLoading = findViewById(R.id.pb_map_loading);
        etSearch = findViewById(R.id.et_search);
        llSubFilters = findViewById(R.id.ll_sub_filters);
        tvFilterChevron = findViewById(R.id.tv_filter_chevron);
        llSubFilters.setVisibility(View.GONE);
        tvFilterChevron.setText("\u25bc");

        setupMap();
        setupNavigation();
        setupFilters();
        setupSearch();
        requestLocationAndLoad();
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.5);
        mapView.getController().setCenter(new GeoPoint(userLat, userLng));
    }

    private void setupNavigation() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btn_my_location).setOnClickListener(v -> {
            mapView.getController().animateTo(new GeoPoint(userLat, userLng));
            mapView.getController().setZoom(15.0);
        });
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_mapa);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QrScanActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_puntos) {
                startActivity(new Intent(this, RewardsActivity.class));
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFilters() {
        findViewById(R.id.filter_todos).setOnClickListener(v -> {
            if (filtersExpanded) {
                llSubFilters.setVisibility(View.GONE);
                tvFilterChevron.setText("▼");
                filtersExpanded = false;
            } else {
                llSubFilters.setVisibility(View.VISIBLE);
                tvFilterChevron.setText("▲");
                filtersExpanded = true;
            }
            setActiveFilter("all");
        });
        findViewById(R.id.filter_locales).setOnClickListener(v -> setActiveFilter("store"));
        findViewById(R.id.filter_restaurantes).setOnClickListener(v -> setActiveFilter("restaurant"));
        findViewById(R.id.filter_profesionales).setOnClickListener(v -> setActiveFilter("professional"));
        findViewById(R.id.filter_servicios).setOnClickListener(v -> setActiveFilter("service"));
    }

    private void setActiveFilter(String category) {
        currentFilter = category;
        int normal = getColor(R.color.text_secondary);
        int active = getColor(R.color.green_primary);
        ((TextView) findViewById(R.id.tv_filter_locales)).setTextColor("store".equals(category) ? active : Color.parseColor("#212121"));
        ((TextView) findViewById(R.id.tv_filter_restaurantes)).setTextColor("restaurant".equals(category) ? active : Color.parseColor("#212121"));
        ((TextView) findViewById(R.id.tv_filter_profesionales)).setTextColor("professional".equals(category) ? active : Color.parseColor("#212121"));
        ((TextView) findViewById(R.id.tv_filter_servicios)).setTextColor("service".equals(category) ? active : Color.parseColor("#212121"));
        applyFilterAndSearch();
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilterAndSearch(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilterAndSearch() {
        String query = etSearch.getText().toString().toLowerCase(Locale.getDefault()).trim();
        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject place : allPlaces) {
            String cat = place.optString("category", "");
            String name = place.optString("name", "").toLowerCase(Locale.getDefault());
            String desc = place.optString("description", "").toLowerCase(Locale.getDefault());
            boolean catMatch = "all".equals(currentFilter) || cat.equals(currentFilter);
            boolean searchMatch = query.isEmpty() || name.contains(query) || desc.contains(query);
            if (catMatch && searchMatch) filtered.add(place);
        }
        renderPlaces(filtered);
        updateMapMarkers(filtered);
    }

    private void requestLocationAndLoad() {
        android.content.SharedPreferences prefs =
                getSharedPreferences("guander_prefs", MODE_PRIVATE);
        boolean locationAllowed = prefs.getBoolean("privacy_location", true);
        if (locationAllowed) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_LOCATION);
            }
        }
        loadPlaces();
    }

    private void getLastLocation() {
        try {
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = null;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (loc != null) {
                userLat = loc.getLatitude();
                userLng = loc.getLongitude();
                locationAvailable = true;
                mapView.getController().setCenter(new GeoPoint(userLat, userLng));
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
            updateMapMarkers(allPlaces);
        }
    }

    private void loadPlaces() {
        pbMapLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String enc = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(BASE_URL + "/places?email=" + enc
                        + "&lat=" + userLat + "&lng=" + userLng);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray places = json.optJSONArray("places");
                    allPlaces.clear();
                    if (places != null) {
                        for (int i = 0; i < places.length(); i++) {
                            allPlaces.add(places.getJSONObject(i));
                        }
                    }
                    mainHandler.post(() -> {
                        pbMapLoading.setVisibility(View.GONE);
                        applyFilterAndSearch();
                    });
                } else {
                    mainHandler.post(() -> pbMapLoading.setVisibility(View.GONE));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbMapLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error cargando lugares", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void renderPlaces(List<JSONObject> places) {
        llPlaces.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;

        if (places.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No se encontraron lugares.");
            empty.setTextSize(13f);
            int p = (int) (16 * density);
            empty.setPadding(p, p, p, p);
            empty.setTextColor(getColor(R.color.text_secondary));
            llPlaces.addView(empty);
            return;
        }

        for (JSONObject place : places) {
            View item = inflater.inflate(R.layout.item_place, llPlaces, false);

            String name = place.optString("name", "");
            String desc = place.optString("description", "");
            String category = place.optString("category", "store");
            int isOpen = place.optInt("is_open", 1);
            int pointsReward = place.optInt("points_reward", 50);
            double distKm = place.optDouble("distance_km", 0);
            int placeId = place.optInt("id_place", 0);

            ((TextView) item.findViewById(R.id.tv_place_name)).setText(name);

            // Stars
            double stars = place.optDouble("stars", 0);
            TextView tvStars = item.findViewById(R.id.tv_place_stars);
            if (stars > 0) {
                tvStars.setText(String.format(Locale.getDefault(), "★ %.1f", stars));
                tvStars.setVisibility(View.VISIBLE);
            } else {
                tvStars.setVisibility(View.GONE);
            }

            // Schedule
            String schedWeek = place.optString("sched_week", "");
            String schedWeekend = place.optString("sched_weekend", "");
            String schedSunday = place.optString("sched_sunday", "");
            TextView tvSchedule = item.findViewById(R.id.tv_place_schedule);
            if (!schedWeek.isEmpty()) {
                StringBuilder sched = new StringBuilder();
                sched.append("Lun–Vie: ").append(schedWeek);
                if (!schedWeekend.isEmpty()) sched.append("\nSáb: ").append(schedWeekend);
                if (!schedSunday.isEmpty()) sched.append("\nDom: ").append(schedSunday);
                tvSchedule.setText(sched.toString());
                tvSchedule.setVisibility(View.VISIBLE);
            } else {
                tvSchedule.setVisibility(View.GONE);
            }

            String openText = isOpen == 1 ? "Abierto ahora" : "Cerrado";
            ((TextView) item.findViewById(R.id.tv_place_info))
                    .setText(String.format(Locale.getDefault(), "%.1f km · %s", distKm, openText));

            String address = place.optString("address", "");
            TextView tvAddress = item.findViewById(R.id.tv_place_address);
            if (!address.isEmpty()) {
                tvAddress.setText(address);
                tvAddress.setVisibility(android.view.View.VISIBLE);
            } else {
                tvAddress.setVisibility(android.view.View.GONE);
            }

            // Category icon with circle background
            final String fPhoto = place.optString("photo_url", "");
            ImageView iconView = item.findViewById(R.id.tv_place_icon);
            applyPlaceIcon(iconView, category, density, fPhoto);

            MaterialButton btnDetail = item.findViewById(R.id.btn_detail);
            final int fId = placeId;
            final String fName = name;
            final String fCat = category;
            final String fType = place.optString("place_type", "store");
            final double fLat = place.optDouble("lat", 0);
            final double fLng = place.optDouble("lng", 0);
            final String fDesc = desc;
            final String fAddress = place.optString("address", "");
            final String fPhone = place.optString("phone", "");
            final int fIsOpen = isOpen;
            final double fDist = distKm;
            btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlaceDetailActivity.class);
                intent.putExtra("PLACE_ID", fId);
                intent.putExtra("PLACE_NAME", fName);
                intent.putExtra("PLACE_CATEGORY", fCat);
                intent.putExtra("PLACE_TYPE", fType);
                intent.putExtra("PLACE_LAT", fLat);
                intent.putExtra("PLACE_LNG", fLng);
                intent.putExtra("PLACE_DESC", fDesc);
                intent.putExtra("PLACE_ADDRESS", fAddress);
                intent.putExtra("PLACE_PHONE", fPhone);
                intent.putExtra("PLACE_IS_OPEN", fIsOpen);
                intent.putExtra("PLACE_DISTANCE", fDist);
                intent.putExtra("PLACE_PHOTO", fPhoto);
                startActivity(intent);
            });

            llPlaces.addView(item);
        }
    }

    private void applyPlaceIcon(ImageView iconView, String category, float density, String photoUrl) {
        int bgColor;
        String initial;
        switch (category) {
            case "store":        initial = "T"; bgColor = Color.parseColor("#2E7D32"); break;
            case "restaurant":   initial = "R"; bgColor = Color.parseColor("#FFC107"); break;
            case "professional": initial = "P"; bgColor = Color.parseColor("#1B5E20"); break;
            case "service":      initial = "S"; bgColor = Color.parseColor("#FF9800"); break;
            default:             initial = "L"; bgColor = Color.parseColor("#2E7D32"); break;
        }

        // Circular clip via shape resource applied as background
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(bgColor);
        iconView.setBackground(gd);

        if (photoUrl != null && !photoUrl.isEmpty()) {
            iconView.setImageDrawable(null);
            Glide.with(iconView.getContext())
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(gd)
                    .error(gd)
                    .into(iconView);
        } else {
            // Draw letter on colored circle as bitmap
            int size = (int) (40 * density);
            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setColor(bgColor);
            canvas.drawCircle(size / 2f, size / 2f, size / 2f, bg);
            Paint txt = new Paint(Paint.ANTI_ALIAS_FLAG);
            txt.setColor(Color.WHITE);
            txt.setTextSize(16 * density);
            txt.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = txt.getFontMetrics();
            float y = size / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(initial, size / 2f, y, txt);
            iconView.setImageBitmap(bm);
        }
    }

    private void updateMapMarkers(List<JSONObject> places) {
        mapView.getOverlays().clear();

        if (locationAvailable) {
            Marker userMarker = new Marker(mapView);
            userMarker.setPosition(new GeoPoint(userLat, userLng));
            userMarker.setTitle("Mi ubicación");
            userMarker.setIcon(createMarkerIcon("Yo", Color.parseColor("#2196F3")));
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            mapView.getOverlays().add(userMarker);
        }

        for (JSONObject place : places) {
            double lat = place.optDouble("lat", -34.6037);
            double lng = place.optDouble("lng", -58.3816);
            String name = place.optString("name", "");
            String category = place.optString("category", "store");
            int placeId = place.optInt("id_place", 0);

            int markerColor;
            String markerLabel;
            switch (category) {
                case "store":        markerColor = Color.parseColor("#2E7D32"); markerLabel = "L"; break;
                case "restaurant":   markerColor = Color.parseColor("#FFC107"); markerLabel = "R"; break;
                case "professional": markerColor = Color.parseColor("#1B5E20"); markerLabel = "P"; break;
                case "service":      markerColor = Color.parseColor("#FF9800");  markerLabel = "S"; break;
                default:             markerColor = Color.parseColor("#2E7D32"); markerLabel = "?"; break;
            }

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lng));
            marker.setTitle(name);
            marker.setIcon(createMarkerIcon(markerLabel, markerColor));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            final int fId = placeId;
            final String fName = name;
            final String fCat = category;
            final String fType = place.optString("place_type", "store");
            final JSONObject fPlace = place;
            marker.setOnMarkerClickListener((m, mv) -> {
                showPlaceBottomSheet(fPlace);
                return true;
            });
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    private void showPlaceBottomSheet(JSONObject place) {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_place, null);

        String name = place.optString("name", "");
        String category = place.optString("category", "store");
        String placeType = place.optString("place_type", "store");
        int placeId = place.optInt("id_place", 0);
        double lat = place.optDouble("lat", 0);
        double lng = place.optDouble("lng", 0);
        String desc = place.optString("description", "");
        String address = place.optString("address", "");
        String phone = place.optString("phone", "");
        double distKm = place.optDouble("distance_km", 0);
        int isOpen = place.optInt("is_open", 1);
        String photo = place.optString("photo_url", "");

        float density = getResources().getDisplayMetrics().density;
        ImageView tvIcon = sheetView.findViewById(R.id.tv_bs_icon);
        applyPlaceIcon(tvIcon, category, density, photo);

        ((TextView) sheetView.findViewById(R.id.tv_bs_name)).setText(name);

        String openText = isOpen == 1 ? "Abierto" : "Cerrado";
        String meta = String.format(Locale.getDefault(), "%.1f km · %s", distKm, openText);
        ((TextView) sheetView.findViewById(R.id.tv_bs_meta)).setText(meta);

        TextView tvDesc = sheetView.findViewById(R.id.tv_bs_desc);
        if (!desc.isEmpty()) {
            tvDesc.setText(desc);
            tvDesc.setVisibility(View.VISIBLE);
        }

        TextView tvAddress = sheetView.findViewById(R.id.tv_bs_address);
        if (!address.isEmpty()) {
            tvAddress.setText(address);
            tvAddress.setVisibility(View.VISIBLE);
        }

        sheetView.findViewById(R.id.btn_bs_directions).setOnClickListener(v -> {
            try {
                String geoUri = "geo:" + lat + "," + lng
                        + "?q=" + lat + "," + lng
                        + "(" + URLEncoder.encode(name, "UTF-8") + ")";
                startActivity(Intent.createChooser(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)), "Abrir con..."));
            } catch (Exception e) {
                Toast.makeText(this, "No se encontró una app de mapas", Toast.LENGTH_SHORT).show();
            }
            sheet.dismiss();
        });

        final String fPlaceType = placeType;
        final String fPhone = phone;
        sheetView.findViewById(R.id.btn_bs_detail).setOnClickListener(v -> {
            Intent intent = new Intent(this, PlaceDetailActivity.class);
            intent.putExtra("PLACE_ID", placeId);
            intent.putExtra("PLACE_NAME", name);
            intent.putExtra("PLACE_CATEGORY", category);
            intent.putExtra("PLACE_TYPE", fPlaceType);
            intent.putExtra("PLACE_LAT", lat);
            intent.putExtra("PLACE_LNG", lng);
            intent.putExtra("PLACE_DESC", desc);
            intent.putExtra("PLACE_ADDRESS", address);
            intent.putExtra("PLACE_PHONE", fPhone);
            intent.putExtra("PLACE_IS_OPEN", isOpen);
            intent.putExtra("PLACE_DISTANCE", distKm);
            intent.putExtra("PLACE_PHOTO", photo);
            startActivity(intent);
            sheet.dismiss();
        });

        sheet.setContentView(sheetView);
        sheet.show();
    }

    private BitmapDrawable createMarkerIcon(String label, int bgColor) {
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (36 * density);
        Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(bgColor);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, p);

        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f * density);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - p.getStrokeWidth(), p);

        p.setColor(Color.WHITE);
        p.setStyle(Paint.Style.FILL);
        p.setTextSize(12 * density);
        p.setTextAlign(Paint.Align.CENTER);
        float textY = size / 2f - (p.descent() + p.ascent()) / 2f;
        canvas.drawText(label, size / 2f, textY, p);

        return new BitmapDrawable(getResources(), bm);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
