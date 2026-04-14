package com.example.guander;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private static final String WORKER_DASHBOARD_URL =
            "https://guander-api.tomas-gonzalezz.workers.dev/dashboard";
    private static final String PREFS          = "guander_prefs";
    private static final String KEY_EMAIL_AUTH = "email_auth";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private TextView tvPoints;
    private LinearLayout llNotifications;
    private ProgressBar pbLoading;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        tvPoints = findViewById(R.id.tv_points);
        llNotifications = findViewById(R.id.ll_notifications);
        pbLoading = findViewById(R.id.pb_loading);

        FirebaseUser user = mAuth.getCurrentUser();
        // Determine active email: Google/Firebase user first, then email/password session
        String activeEmail = null;
        String displayName = null;
        if (user != null) {
            activeEmail = user.getEmail();
            displayName = user.getDisplayName();
        } else {
            activeEmail = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_EMAIL_AUTH, null);
        }

        if (activeEmail == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Populate greeting
        String firstName;
        if (displayName != null && !displayName.isEmpty()) {
            firstName = displayName.split(" ")[0];
        } else {
            firstName = activeEmail.split("@")[0];
        }

        final String emailForFetch = activeEmail;

        TextView tvGreeting = findViewById(R.id.tv_greeting);
        TextView tvAvatar = findViewById(R.id.tv_avatar);
        tvGreeting.setText("¡Hola, " + firstName + "!");
        tvAvatar.setText(String.valueOf(firstName.charAt(0)).toUpperCase());

        // Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_inicio);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_mapa) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QrScanActivity.class));
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
        findViewById(R.id.card_lugares).setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.card_recompensas).setOnClickListener(v ->
                startActivity(new Intent(this, RewardsActivity.class)));
        findViewById(R.id.btn_canjear).setOnClickListener(v ->
                startActivity(new Intent(this, RewardsActivity.class)));

        // Fetch dashboard data
        fetchDashboardData(emailForFetch);
    }

    private void fetchDashboardData(String email) {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String encodedEmail = URLEncoder.encode(email, "UTF-8");
                URL url = new URL(WORKER_DASHBOARD_URL + "?email=" + encodedEmail);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    int points = json.optInt("points", 0);
                    JSONArray notifArray = json.optJSONArray("notifications");

                    mainHandler.post(() -> {
                        updatePoints(points);
                        updateNotifications(notifArray);
                        pbLoading.setVisibility(View.GONE);
                    });
                } else {
                    mainHandler.post(() -> pbLoading.setVisibility(View.GONE));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> pbLoading.setVisibility(View.GONE));
            }
        }).start();
    }

    private void updatePoints(int points) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));
        tvPoints.setText(nf.format(points));
    }

    private void updateNotifications(JSONArray notifArray) {
        llNotifications.removeAllViews();
        String[] icons = {"🛍️", "⭐", "😊", "🔔", "🎁"};

        if (notifArray == null || notifArray.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Sin notificaciones por el momento.");
            empty.setTextSize(13f);
            empty.setPadding(4, 16, 4, 16);
            llNotifications.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < notifArray.length(); i++) {
            try {
                JSONObject notif = notifArray.getJSONObject(i);
                String title = notif.optString("name", "");
                String desc = notif.optString("description", "");

                View item = inflater.inflate(R.layout.item_notification, llNotifications, false);
                ((TextView) item.findViewById(R.id.tv_notif_icon)).setText(icons[i % icons.length]);
                ((TextView) item.findViewById(R.id.tv_notif_title)).setText(title);
                ((TextView) item.findViewById(R.id.tv_notif_desc)).setText(desc);
                llNotifications.addView(item);
            } catch (Exception ignored) {
            }
        }
    }

    private void logout() {
        mAuth.signOut();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_EMAIL_AUTH).apply();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
