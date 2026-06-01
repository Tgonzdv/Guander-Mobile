package com.example.guander;

import android.app.Dialog;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.imageview.ShapeableImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RewardsActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://guander-api.tomas-gonzalezz.workers.dev";

    private int currentPoints = 0;
    private String userEmail = "";
    private final List<JSONObject> allRewards = new ArrayList<>();
    private int currentFilterStore = -1;

    private TextView tvPoints;
    private TextView tvFilterLabel;
    private LinearLayout llRewards;
    private LinearLayout llHistory;
    private LinearLayout contentCanjear;
    private ScrollView contentHistorial;
    private ProgressBar pbLoading;
    private EditText etSearch;
    private View indicatorCanjear;
    private View indicatorHistorial;
    private TextView tvTabCanjear;
    private TextView tvTabHistorial;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rewards);

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

        tvPoints = findViewById(R.id.tv_rewards_points);
        llRewards = findViewById(R.id.ll_rewards);
        llHistory = findViewById(R.id.ll_history);
        contentCanjear = findViewById(R.id.content_canjear);
        contentHistorial = findViewById(R.id.content_historial);
        pbLoading = findViewById(R.id.pb_rewards_loading);
        etSearch = findViewById(R.id.et_search);
        tvFilterLabel = findViewById(R.id.tv_filter_label);
        indicatorCanjear = findViewById(R.id.indicator_canjear);
        indicatorHistorial = findViewById(R.id.indicator_historial);
        tvTabCanjear = findViewById(R.id.tv_tab_canjear);
        tvTabHistorial = findViewById(R.id.tv_tab_historial);

        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.tab_canjear).setOnClickListener(v -> switchTab(true));
        findViewById(R.id.tab_historial).setOnClickListener(v -> switchTab(false));
        findViewById(R.id.ll_filter_todos).setOnClickListener(v -> showFilterDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_puntos);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_mapa) {
                startActivity(new Intent(this, MapActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_qr) {
                startActivity(new Intent(this, QrScanActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_perfil) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        loadRewards();
    }

    private void switchTab(boolean canjear) {
        if (canjear) {
            contentCanjear.setVisibility(View.VISIBLE);
            contentHistorial.setVisibility(View.GONE);
            tvTabCanjear.setTextColor(getColor(R.color.green_primary));
            tvTabCanjear.setTypeface(null, Typeface.BOLD);
            tvTabHistorial.setTextColor(getColor(R.color.text_secondary));
            tvTabHistorial.setTypeface(null, Typeface.NORMAL);
            indicatorCanjear.setBackgroundColor(getColor(R.color.green_primary));
            indicatorHistorial.setBackgroundColor(Color.TRANSPARENT);
        } else {
            contentCanjear.setVisibility(View.GONE);
            contentHistorial.setVisibility(View.VISIBLE);
            tvTabHistorial.setTextColor(getColor(R.color.green_primary));
            tvTabHistorial.setTypeface(null, Typeface.BOLD);
            tvTabCanjear.setTextColor(getColor(R.color.text_secondary));
            tvTabCanjear.setTypeface(null, Typeface.NORMAL);
            indicatorHistorial.setBackgroundColor(getColor(R.color.green_primary));
            indicatorCanjear.setBackgroundColor(Color.TRANSPARENT);
            loadHistory();
        }
    }

    private void loadRewards() {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String enc = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(BASE_URL + "/rewards?email=" + enc);
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
                    int pts = json.optInt("points", 0);
                    JSONArray rewardsArr = json.optJSONArray("rewards");

                    mainHandler.post(() -> {
                        currentPoints = pts;
                        updatePointsDisplay(pts);
                        allRewards.clear();
                        if (rewardsArr != null) {
                            for (int i = 0; i < rewardsArr.length(); i++) {
                                try { allRewards.add(rewardsArr.getJSONObject(i)); }
                                catch (Exception ignored) {}
                            }
                        }
                        renderRewards(allRewards);
                        pbLoading.setVisibility(View.GONE);
                    });
                } else {
                    mainHandler.post(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, "Error al cargar recompensas", Toast.LENGTH_SHORT).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void loadHistory() {
        llHistory.removeAllViews();
        new Thread(() -> {
            try {
                String enc = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(BASE_URL + "/redeem-history?email=" + enc);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray history = json.optJSONArray("history");
                    mainHandler.post(() -> renderHistory(history));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Sin conexión", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updatePointsDisplay(int points) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));
        tvPoints.setText(nf.format(points));
    }

    private void showFilterDialog() {
        java.util.List<String> labels = new ArrayList<>();
        java.util.List<Integer> stores = new ArrayList<>();
        labels.add("Todos");
        stores.add(-1);

        String[] storeNames = {"", "Tienda de mascotas", "Veterinaria",
                "Cafetería", "Peluquería", "Hotel",
                "Supermercado", "Restaurante", "Paquetería"};
        java.util.LinkedHashSet<Integer> seen = new java.util.LinkedHashSet<>();
        for (JSONObject r : allRewards) seen.add(r.optInt("fk_store", 0));
        for (int fk : seen) {
            if (fk >= 1 && fk < storeNames.length) {
                labels.add(storeNames[fk]);
                stores.add(fk);
            } else if (fk == 0) {
                labels.add("General");
                stores.add(0);
            }
        }

        String[] items = labels.toArray(new String[0]);
        int checkedItem = stores.indexOf(currentFilterStore);
        if (checkedItem < 0) checkedItem = 0;
        final int[] selected = {checkedItem};

        new AlertDialog.Builder(this)
                .setTitle("Filtrar por categoría")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    currentFilterStore = stores.get(selected[0]);
                    tvFilterLabel.setText(" " + labels.get(selected[0]));
                    applyFilters();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase(Locale.getDefault());
        List<JSONObject> filtered = new ArrayList<>();
        for (JSONObject r : allRewards) {
            if (currentFilterStore != -1 && r.optInt("fk_store", 0) != currentFilterStore) continue;
            if (!query.isEmpty()) {
                String name = r.optString("name", "").toLowerCase(Locale.getDefault());
                String desc = r.optString("description", "").toLowerCase(Locale.getDefault());
                if (!name.contains(query) && !desc.contains(query)) continue;
            }
            filtered.add(r);
        }
        renderRewards(filtered);
    }

    private void renderRewards(List<JSONObject> rewards) {
        llRewards.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));

        if (rewards.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No hay recompensas disponibles.");
            empty.setTextSize(13f);
            int p = (int) (16 * density);
            empty.setPadding(p, p * 2, p, p);
            empty.setTextColor(getColor(R.color.text_secondary));
            llRewards.addView(empty);
            return;
        }

        for (JSONObject reward : rewards) {
            View item = inflater.inflate(R.layout.item_reward, llRewards, false);

            String name = reward.optString("name", "");
            String desc = reward.optString("description", "");
            int pointsCost = reward.optInt("point_req", 0);
            int fkStore = reward.optInt("fk_store", 0);
            int idCoupon = reward.optInt("id", 0);
            String couponType = reward.optString("type", "store");

            ((TextView) item.findViewById(R.id.tv_reward_name)).setText(name);
            ((TextView) item.findViewById(R.id.tv_reward_desc)).setText(desc);
            ((TextView) item.findViewById(R.id.tv_reward_points))
                    .setText(nf.format(pointsCost) + " pts");

            TextView iconView = item.findViewById(R.id.tv_reward_icon);
            ShapeableImageView photoView = item.findViewById(R.id.iv_reward_photo);
            String photoUrl = reward.optString("photo_url", "");

            if (!photoUrl.isEmpty()) {
                iconView.setVisibility(View.GONE);
                photoView.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(photoUrl)
                        .transform(new CircleCrop())
                        .placeholder(android.R.color.transparent)
                        .error(android.R.color.darker_gray)
                        .into(photoView);
            } else {
                applyRewardIcon(iconView, fkStore, density);
            }

            MaterialButton btnCanjear = item.findViewById(R.id.btn_reward_canjear);
            boolean canAfford = currentPoints >= pointsCost;
            btnCanjear.setEnabled(canAfford);
            btnCanjear.setAlpha(canAfford ? 1f : 0.5f);

            final int fId = idCoupon;
            final String fName = name;
            final int fCost = pointsCost;
            final String fType = couponType;
            final String fPhoto = photoUrl;
            btnCanjear.setOnClickListener(v -> showConfirmDialog(fId, fName, fCost, fType, fPhoto));

            llRewards.addView(item);
        }
    }

    private void applyRewardIcon(TextView iconView, int fkStore, float density) {
        String emoji;
        int bgColor;
        switch (fkStore) {
            case 1:  emoji = "🐾"; bgColor = Color.parseColor("#795548"); break;
            case 2:  emoji = "🏥"; bgColor = Color.parseColor("#2196F3"); break;
            case 3:  emoji = "☕"; bgColor = Color.parseColor("#FF9800"); break;
            case 4:  emoji = "✂️"; bgColor = Color.parseColor("#9C27B0"); break;
            case 5:  emoji = "🏨"; bgColor = Color.parseColor("#009688"); break;
            case 6:  emoji = "🛒"; bgColor = Color.parseColor("#E91E63"); break;
            case 7:  emoji = "🍽️"; bgColor = Color.parseColor("#FF5722"); break;
            case 8:  emoji = "📦"; bgColor = Color.parseColor("#607D8B"); break;
            default: emoji = "🎁"; bgColor = getColor(R.color.md_theme_primary);  break;
        }
        iconView.setText(emoji);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(bgColor);
        iconView.setBackground(gd);
    }

    private void renderHistory(JSONArray history) {
        llHistory.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));

        if (history == null || history.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Sin historial de canjes aún.");
            empty.setTextSize(13f);
            int p = (int) (16 * density);
            empty.setPadding(p, p * 2, p, p);
            empty.setTextColor(getColor(R.color.text_secondary));
            llHistory.addView(empty);
            return;
        }

        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject entry = history.getJSONObject(i);
                String name = entry.optString("name", "");
                String date = entry.optString("date", "");
                int pointsChange = entry.optInt("points_change", 0);
                String redemptionCode = entry.optString("redemption_code", "");

                View row = inflater.inflate(R.layout.item_reward_history, llHistory, false);
                ((TextView) row.findViewById(R.id.tv_history_name)).setText(name);
                ((TextView) row.findViewById(R.id.tv_history_date)).setText(date);

                TextView tvIcon = row.findViewById(R.id.tv_history_icon);
                TextView tvPts = row.findViewById(R.id.tv_history_points);

                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);

                if (pointsChange >= 0) {
                    tvIcon.setText("+");
                    gd.setColor(getColor(R.color.green_light));
                    tvPts.setText("+" + nf.format(pointsChange) + " pts");
                    tvPts.setTextColor(getColor(R.color.green_light));
                } else {
                    tvIcon.setText("−");
                    gd.setColor(getColor(R.color.color_error));
                    tvPts.setText("−" + nf.format(Math.abs(pointsChange)) + " pts");
                    tvPts.setTextColor(getColor(R.color.color_error));
                }
                tvIcon.setBackground(gd);

                if (!redemptionCode.isEmpty()) {
                    final String finalCode = redemptionCode;
                    row.setBackgroundResource(android.R.drawable.list_selector_background);
                    row.setClickable(true);
                    row.setOnClickListener(v -> showSuccessDialog(finalCode));
                }

                llHistory.addView(row);
            } catch (Exception ignored) {}
        }
    }

    private void showConfirmDialog(int couponId, String rewardName, int pointsCost, String couponType, String photoUrl) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_confirm_redeem);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "MX"));
        ((TextView) dialog.findViewById(R.id.tv_confirm_reward_name)).setText(rewardName);
        ((TextView) dialog.findViewById(R.id.tv_confirm_cost))
                .setText("🪙 " + nf.format(pointsCost) + " pts");
        ((TextView) dialog.findViewById(R.id.tv_confirm_remaining))
                .setText("🪙 " + nf.format(currentPoints - pointsCost) + " pts");

        // Load store photo if available
        if (photoUrl != null && !photoUrl.isEmpty()) {
            TextView tvIcon = dialog.findViewById(R.id.tv_confirm_icon);
            ShapeableImageView ivPhoto = dialog.findViewById(R.id.iv_confirm_photo);
            tvIcon.setVisibility(View.GONE);
            ivPhoto.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(photoUrl)
                    .transform(new CircleCrop())
                    .placeholder(android.R.color.transparent)
                    .error(android.R.color.darker_gray)
                    .into(ivPhoto);
        }

        dialog.findViewById(R.id.btn_cancel_redeem).setOnClickListener(v -> dialog.dismiss());
        dialog.findViewById(R.id.btn_confirm_redeem).setOnClickListener(v -> {
            dialog.dismiss();
            performRedeem(couponId, pointsCost, couponType);
        });

        dialog.show();
    }

    private void performRedeem(int couponId, int pointsCost, String couponType) {
        pbLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/redeem");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                JSONObject body = new JSONObject();
                body.put("email", userEmail);
                body.put("couponId", couponId);
                body.put("couponType", couponType);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject resp = new JSONObject(sb.toString());
                    String redemptionCode = resp.optString("code", "GUAN-0000");
                    int remaining = resp.optInt("remainingPoints", currentPoints - pointsCost);

                    mainHandler.post(() -> {
                        pbLoading.setVisibility(View.GONE);
                        currentPoints = remaining;
                        updatePointsDisplay(remaining);
                        loadRewards();
                        showSuccessDialog(redemptionCode);
                    });
                } else {
                    String errMsg = "Error al canjear";
                    try {
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(conn.getErrorStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        errMsg = new JSONObject(sb.toString()).optString("error", errMsg);
                    } catch (Exception ignored) {}
                    final String msg = errMsg;
                    mainHandler.post(() -> {
                        pbLoading.setVisibility(View.GONE);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showSuccessDialog(String code) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_redeem_success);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        ((TextView) dialog.findViewById(R.id.tv_redemption_code)).setText(code);
        dialog.findViewById(R.id.btn_understood).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
