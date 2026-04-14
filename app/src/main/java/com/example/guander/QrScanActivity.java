package com.example.guander;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class QrScanActivity extends AppCompatActivity {

    private static final String WORKER = "https://guander-api.tomas-gonzalezz.workers.dev";
    private static final int REQUEST_CAMERA = 101;

    private BarcodeView barcodeView;
    private View llPlaceholder;
    private View scanLine;
    private boolean isScanning = false;
    private boolean resultHandled = false;
    private String userEmail;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        } else {
            userEmail = getSharedPreferences("guander_prefs", MODE_PRIVATE).getString("email_auth", null);
        }
        if (userEmail == null) { finish(); return; }

        barcodeView = findViewById(R.id.barcode_view);
        llPlaceholder = findViewById(R.id.ll_camera_placeholder);
        scanLine = findViewById(R.id.scan_line);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.btn_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        findViewById(R.id.btn_activate_camera).setOnClickListener(v -> requestCameraAndStart());

        // Bottom nav
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_qr);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardActivity.class)); finish();
                return true;
            } else if (id == R.id.nav_mapa) {
                startActivity(new Intent(this, MapActivity.class)); finish();
                return true;
            } else if (id == R.id.nav_puntos) {
                startActivity(new Intent(this, RewardsActivity.class)); finish();
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, ProfileActivity.class)); finish();
                return true;
            }
            return false;
        });
    }

    private void requestCameraAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    private void startScanning() {
        if (isScanning) return;
        isScanning = true;
        resultHandled = false;
        llPlaceholder.setVisibility(View.GONE);
        scanLine.setVisibility(View.VISIBLE);
        barcodeView.setVisibility(View.VISIBLE);
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (resultHandled) return;
                resultHandled = true;
                barcodeView.stopDecoding();
                String text = result.getText();
                processQrCode(text);
            }

            @Override
            public void possibleResultPoints(List points) {}
        });
        barcodeView.resume();
    }

    private void processQrCode(String raw) {
        try {
            JSONObject qr = new JSONObject(raw);
            String secret = qr.optString("secret", "");
            if (!"guander2026".equals(secret)) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "QR inválido", Toast.LENGTH_LONG).show();
                    resetScanner();
                });
                return;
            }

            new Thread(() -> {
                try {
                    URL url = new URL(WORKER + "/validate-qr");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    JSONObject body = new JSONObject();
                    body.put("email", userEmail);
                    body.put("qrData", raw);

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body.toString().getBytes("UTF-8"));
                    }

                    int code = conn.getResponseCode();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    conn.disconnect();

                    JSONObject resp = new JSONObject(sb.toString());

                    if (code == 200) {
                        mainHandler.post(() -> showSuccessDialog(resp));
                    } else {
                        String errMsg = resp.optString("error", "Error al validar QR");
                        mainHandler.post(() -> {
                            Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                            resetScanner();
                        });
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error de conexión: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        resetScanner();
                    });
                }
            }).start();

        } catch (Exception e) {
            Toast.makeText(this, "QR no reconocido", Toast.LENGTH_LONG).show();
            resetScanner();
        }
    }

    private void showSuccessDialog(JSONObject resp) {
        String storeName = resp.optString("storeName", "");
        String item = resp.optString("item", "");
        int amount = resp.optInt("amount", 0);
        int pointsEarned = resp.optInt("pointsEarned", 0);
        int newBalance = resp.optInt("newBalance", 0);

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_qr_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);

        ((TextView) dialog.findViewById(R.id.tv_points_earned)).setText("+" + pointsEarned);
        ((TextView) dialog.findViewById(R.id.tv_store_name)).setText(storeName);
        ((TextView) dialog.findViewById(R.id.tv_item_desc)).setText(item);

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        ((TextView) dialog.findViewById(R.id.tv_amount))
                .setText("Monto $" + nf.format(amount));
        ((TextView) dialog.findViewById(R.id.tv_new_balance))
                .setText("Nuevo balance: " + nf.format(newBalance) + " PetPoints");

        dialog.findViewById(R.id.btn_great).setOnClickListener(v -> {
            dialog.dismiss();
            resetScanner();
        });

        dialog.show();
    }

    private void resetScanner() {
        isScanning = false;
        resultHandled = false;
        barcodeView.setVisibility(View.GONE);
        scanLine.setVisibility(View.GONE);
        llPlaceholder.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScanning) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
