package com.example.guander;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class EditProfileActivity extends AppCompatActivity {

    private static final String WORKER = "https://guander-api.tomas-gonzalezz.workers.dev";

    private String userEmail;
    private String currentPhotoUrl;

    private ShapeableImageView ivAvatar;
    private ProgressBar pbUpload;
    private ProgressBar pbSaving;
    private TextInputEditText etName, etLastname, etEmail, etTel, etAddress;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) handleImageSelected(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userEmail = getIntent().getStringExtra("email");
        if (userEmail == null) { finish(); return; }

        ivAvatar = findViewById(R.id.iv_edit_avatar);
        pbUpload = findViewById(R.id.pb_edit_upload);
        pbSaving = findViewById(R.id.pb_saving);
        etName = findViewById(R.id.et_name);
        etLastname = findViewById(R.id.et_lastname);
        etEmail = findViewById(R.id.et_email);
        etTel = findViewById(R.id.et_tel);
        etAddress = findViewById(R.id.et_address);

        etEmail.setText(userEmail);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_edit_photo).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());

        loadCurrentData();
    }

    private void loadCurrentData() {
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(WORKER + "/profile?email=" + encoded);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    JSONObject json = new JSONObject(sb.toString());
                    conn.disconnect();
                    mainHandler.post(() -> {
                        etName.setText(json.optString("name", ""));
                        etLastname.setText(json.optString("lastName", ""));
                        etTel.setText(json.optString("tel", ""));
                        etAddress.setText(json.optString("address", ""));
                        currentPhotoUrl = json.isNull("photoUrl") ? null : json.optString("photoUrl");
                        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
                            Glide.with(this).load(currentPhotoUrl).centerCrop().into(ivAvatar);
                        }
                    });
                } else {
                    conn.disconnect();
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void saveProfile() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String lastName = etLastname.getText() != null ? etLastname.getText().toString().trim() : "";
        String tel = etTel.getText() != null ? etTel.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";

        pbSaving.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL(WORKER + "/profile");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                String safeEmail = userEmail.replace("\"", "\\\"");
                String safeName = name.replace("\"", "\\\"");
                String safeLastName = lastName.replace("\"", "\\\"");
                String safeTel = tel.replace("\"", "\\\"");
                String safeAddress = address.replace("\"", "\\\"");
                String photoJson = (currentPhotoUrl != null) ? "\"" + currentPhotoUrl.replace("\"", "\\\"") + "\"" : "null";

                String json = "{\"email\":\"" + safeEmail + "\","
                        + "\"name\":\"" + safeName + "\","
                        + "\"lastName\":\"" + safeLastName + "\","
                        + "\"tel\":\"" + safeTel + "\","
                        + "\"address\":\"" + safeAddress + "\","
                        + "\"photoUrl\":" + photoJson + "}";

                conn.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
                int code = conn.getResponseCode();
                conn.disconnect();
                mainHandler.post(() -> {
                    pbSaving.setVisibility(View.GONE);
                    if (code == 200) {
                        Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbSaving.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void handleImageSelected(Uri uri) {
        pbUpload.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                // Get signature
                URL signUrl = new URL(WORKER + "/sign-upload");
                HttpURLConnection sc = (HttpURLConnection) signUrl.openConnection();
                sc.setRequestMethod("POST");
                sc.setDoOutput(true);
                sc.setConnectTimeout(10000);
                sc.setReadTimeout(10000);
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

                InputStream imageStream = getContentResolver().openInputStream(uri);
                String secureUrl = uploadToCloudinary(cloudName, apiKey, timestamp, signature, folder, imageStream);
                if (imageStream != null) imageStream.close();

                if (secureUrl != null) {
                    currentPhotoUrl = secureUrl;
                    mainHandler.post(() -> {
                        pbUpload.setVisibility(View.GONE);
                        Glide.with(this).load(secureUrl).centerCrop().into(ivAvatar);
                        Toast.makeText(this, "Foto lista", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    mainHandler.post(() -> { pbUpload.setVisibility(View.GONE); Toast.makeText(this, "Error al subir", Toast.LENGTH_SHORT).show(); });
                }
            } catch (Exception e) {
                mainHandler.post(() -> { pbUpload.setVisibility(View.GONE); Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show(); });
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
}
