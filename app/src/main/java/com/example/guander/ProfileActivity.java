package com.example.guander;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String WORKER = "https://guander-api.tomas-gonzalezz.workers.dev";
    private static final String PREFS = "guander_prefs";
    private static final String KEY_EMAIL_AUTH = "email_auth";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private String userEmail;
    private String currentPhotoUrl;

    private ShapeableImageView ivAvatar;
    private ProgressBar pbPhotoUpload;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageSelected(uri);
            });

    // EditProfile result
    private final ActivityResultLauncher<Intent> editProfileLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == RESULT_OK) loadProfileData();
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        } else {
            userEmail = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_EMAIL_AUTH, null);
        }
        if (userEmail == null) { finish(); return; }

        // Pre-fill email from Firebase immediately (visible before API responds)
        ((TextView) findViewById(R.id.tv_profile_email)).setText(userEmail);

        ivAvatar = findViewById(R.id.iv_avatar);
        pbPhotoUpload = findViewById(R.id.pb_photo_upload);

        // Back
        ((MaterialToolbar) findViewById(R.id.btn_back)).setNavigationOnClickListener(v -> finish());

        // Change photo
        findViewById(R.id.btn_change_photo).setOnClickListener(v ->
                imagePickerLauncher.launch("image/*"));

        // Edit profile
        findViewById(R.id.btn_edit).setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            intent.putExtra("email", userEmail);
            editProfileLauncher.launch(intent);
        });

        // Settings navigation
        navigate(R.id.item_notifications, NotificationsSettingsActivity.class);
        navigate(R.id.item_privacy, PrivacySecurityActivity.class);
        navigate(R.id.item_help, HelpCenterActivity.class);
        navigate(R.id.item_terms, TermsActivity.class);
        navigate(R.id.item_policy, PrivacyPolicyActivity.class);

        // Logout
        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());

        loadProfileData();
    }

    private void navigate(int viewId, Class<?> target) {
        findViewById(viewId).setOnClickListener(v -> startActivity(new Intent(this, target)));
    }

    private void loadProfileData() {
        new Thread(() -> {
            try {
                String encodedEmail = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(WORKER + "/profile?email=" + encodedEmail);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(sb.toString());
                    conn.disconnect();
                    mainHandler.post(() -> updateUI(json));
                } else {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(
                            conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()));
                    StringBuilder esb = new StringBuilder();
                    String el;
                    while ((el = errReader.readLine()) != null) esb.append(el);
                    errReader.close();
                    conn.disconnect();
                    final String errBody = esb.toString();
                    mainHandler.post(() -> Toast.makeText(this,
                            "Error cargando perfil (" + code + "): " + errBody,
                            Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(this,
                        "Error de conexion: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void updateUI(JSONObject json) {
        try {
            String name = json.optString("name", "") + " " + json.optString("lastName", "");
            String email = json.optString("email", "");
            String tel = json.optString("tel", "—");
            String address = json.optString("address", "—");
            String photoUrl = json.isNull("photoUrl") ? null : json.optString("photoUrl");
            String dateReg = json.optString("dateReg", "");
            int points = json.optInt("points", 0);
            int places = json.optInt("placesVisited", 0);
            int coupons = json.optInt("coupons", 0);

            currentPhotoUrl = photoUrl;

            ((TextView) findViewById(R.id.tv_profile_name)).setText(name.trim().isEmpty() ? email : name.trim());
            ((TextView) findViewById(R.id.tv_profile_email)).setText(email);

            // Member since
            if (!dateReg.isEmpty()) {
                try {
                    SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date date = inFmt.parse(dateReg);
                    if (date != null) {
                        SimpleDateFormat outFmt = new SimpleDateFormat("MMM yyyy", new Locale("es", "MX"));
                        ((TextView) findViewById(R.id.tv_member_since)).setText("Miembro desde " + outFmt.format(date));
                    }
                } catch (Exception ignored) {}
            }

            // Stats
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));
            ((TextView) findViewById(R.id.tv_places)).setText(String.valueOf(places));
            ((TextView) findViewById(R.id.tv_stat_points)).setText(nf.format(points));
            ((TextView) findViewById(R.id.tv_coupons)).setText(String.valueOf(coupons));

            // Info rows
            setRow(R.id.row_name, "👤", "Nombre completo", name.trim().isEmpty() ? "—" : name.trim());
            setRow(R.id.row_email, "✉️", "Correo electronico", email);
            setRow(R.id.row_tel, "📞", "Telefono", tel.isEmpty() ? "—" : tel);
            setRow(R.id.row_address, "📍", "Ubicacion", address.isEmpty() ? "—" : address);

            // Avatar
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Glide.with(this).load(photoUrl).centerCrop().into(ivAvatar);
            }

            // Save prefs for other screens
            SharedPreferences.Editor ed = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            ed.putString("profile_name", name.trim());
            ed.putString("profile_email", email);
            ed.putString("profile_photo", photoUrl != null ? photoUrl : "");
            ed.apply();

        } catch (Exception ignored) {}
    }

    private void setRow(int rowId, String icon, String label, String value) {
        LinearLayout row = findViewById(rowId);
        if (row == null) return;
        ((TextView) row.findViewById(R.id.tv_row_icon)).setText(icon);
        ((TextView) row.findViewById(R.id.tv_row_label)).setText(label);
        ((TextView) row.findViewById(R.id.tv_row_value)).setText(value);
    }

    // ── Cloudinary photo upload ─────────────────────────────────────────────
    private void handleImageSelected(Uri uri) {
        pbPhotoUpload.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                // 1. Get signature from worker
                URL signUrl = new URL(WORKER + "/sign-upload");
                HttpURLConnection sc = (HttpURLConnection) signUrl.openConnection();
                sc.setRequestMethod("POST");
                sc.setConnectTimeout(10000);
                sc.setReadTimeout(10000);
                sc.setDoOutput(true);
                sc.getOutputStream().write("{}".getBytes(StandardCharsets.UTF_8));

                BufferedReader sr = new BufferedReader(new InputStreamReader(sc.getInputStream()));
                StringBuilder ssb = new StringBuilder();
                String sl;
                while ((sl = sr.readLine()) != null) ssb.append(sl);
                sr.close();
                sc.disconnect();

                JSONObject sign = new JSONObject(ssb.toString());
                String timestamp = sign.getString("timestamp");
                String signature = sign.getString("signature");
                String apiKey = sign.getString("apiKey");
                String cloudName = sign.getString("cloudName");
                String folder = sign.getString("folder");

                // 2. Upload to Cloudinary
                InputStream imageStream = getContentResolver().openInputStream(uri);
                String secureUrl = uploadToCloudinary(cloudName, apiKey, timestamp, signature, folder, imageStream);
                if (imageStream != null) imageStream.close();

                if (secureUrl != null) {
                    // 3. Save to DB
                    savePhotoUrl(secureUrl);
                    mainHandler.post(() -> {
                        currentPhotoUrl = secureUrl;
                        Glide.with(this).load(secureUrl).centerCrop().into(ivAvatar);
                        pbPhotoUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> {
                        pbPhotoUpload.setVisibility(View.GONE);
                        Toast.makeText(this, "Error al subir la foto", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbPhotoUpload.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private String uploadToCloudinary(String cloudName, String apiKey, String timestamp,
                                       String signature, String folder, InputStream imageStream) throws Exception {
        String boundary = "----Boundary" + System.currentTimeMillis();
        URL url = new URL("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

        // File part
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n");
        dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        byte[] buf = new byte[4096];
        int n;
        while ((n = imageStream.read(buf)) != -1) dos.write(buf, 0, n);
        dos.writeBytes("\r\n");

        writeField(dos, boundary, "api_key", apiKey);
        writeField(dos, boundary, "timestamp", timestamp);
        writeField(dos, boundary, "signature", signature);
        writeField(dos, boundary, "folder", folder);

        dos.writeBytes("--" + boundary + "--\r\n");
        dos.flush();
        dos.close();

        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            conn.disconnect();
            return new JSONObject(sb.toString()).optString("secure_url", null);
        }
        conn.disconnect();
        return null;
    }

    private void writeField(DataOutputStream dos, String boundary, String name, String value) throws Exception {
        dos.writeBytes("--" + boundary + "\r\n");
        dos.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        dos.writeBytes(value);
        dos.writeBytes("\r\n");
    }

    private void savePhotoUrl(String photoUrl) {
        try {
            URL url = new URL(WORKER + "/profile");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String safe = userEmail.replace("\"", "\\\"");
            String safeUrl = photoUrl.replace("\"", "\\\"");
            String json = "{\"email\":\"" + safe + "\",\"photoUrl\":\"" + safeUrl + "\"}";
            conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            conn.getResponseCode();
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private void logout() {
        mAuth.signOut();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_EMAIL_AUTH).apply();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
