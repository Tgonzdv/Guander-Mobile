package com.example.guander;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class PlaceDetailActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://guander-api.tomas-gonzalezz.workers.dev";

    private int placeId;
    private String placeName;
    private String placeCategory;
    private String userEmail = "";

    private TextView tvCommentsCount;
    private LinearLayout llComments;
    private ProgressBar pbCommentsLoading;
    private LinearLayout llLock;
    private LinearLayout llCommentForm;
    private RatingBar rbMyRating;
    private EditText etComment;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int[] AVATAR_COLORS = {
            0xFFE91E63, 0xFF9C27B0, 0xFF2196F3, 0xFF009688,
            0xFFFF5722, 0xFF607D8B, 0xFF795548, 0xFF3F51B5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_detail);

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

        placeId = getIntent().getIntExtra("PLACE_ID", 0);
        placeName = getIntent().getStringExtra("PLACE_NAME");
        if (placeName == null) placeName = "Lugar";
        placeCategory = getIntent().getStringExtra("PLACE_CATEGORY");
        if (placeCategory == null) placeCategory = "store";
        // placeType is "store" or "professional" — maps to real DB tables
        String intentType = getIntent().getStringExtra("PLACE_TYPE");
        if (intentType != null && !intentType.isEmpty()) placeCategory = intentType;

        ((TextView) findViewById(R.id.tv_place_header_name)).setText(placeName);

        tvCommentsCount = findViewById(R.id.tv_comments_count);
        llComments = findViewById(R.id.ll_comments);
        pbCommentsLoading = findViewById(R.id.pb_comments_loading);
        llLock = findViewById(R.id.ll_lock);
        llCommentForm = findViewById(R.id.ll_comment_form);
        rbMyRating = findViewById(R.id.rb_my_rating);
        etComment = findViewById(R.id.et_comment);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((MaterialButton) findViewById(R.id.btn_submit_comment)).setOnClickListener(v -> submitComment());

        loadComments();
    }

    private void loadComments() {
        pbCommentsLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                String enc = URLEncoder.encode(userEmail, "UTF-8");
                URL url = new URL(BASE_URL + "/comments?placeId=" + placeId + "&placeType=" + placeCategory + "&email=" + enc);
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
                    int total = json.optInt("totalComments", 0);
                    boolean canComment = json.optBoolean("canComment", false);
                    JSONArray comments = json.optJSONArray("comments");

                    mainHandler.post(() -> {
                        pbCommentsLoading.setVisibility(View.GONE);
                        tvCommentsCount.setText("Comentarios(" + total + ")");
                        renderComments(comments);
                        if (canComment) {
                            llCommentForm.setVisibility(View.VISIBLE);
                            llLock.setVisibility(View.GONE);
                        } else {
                            llLock.setVisibility(View.VISIBLE);
                            llCommentForm.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mainHandler.post(() -> pbCommentsLoading.setVisibility(View.GONE));
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    pbCommentsLoading.setVisibility(View.GONE);
                    Toast.makeText(this, "Error cargando comentarios", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void renderComments(JSONArray comments) {
        llComments.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        float density = getResources().getDisplayMetrics().density;

        if (comments == null || comments.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Sé el primero en comentar.");
            empty.setTextSize(13f);
            int p = (int) (8 * density);
            empty.setPadding(0, p, 0, p);
            empty.setTextColor(getColor(R.color.text_secondary));
            llComments.addView(empty);
            return;
        }

        for (int i = 0; i < comments.length(); i++) {
            try {
                JSONObject c = comments.getJSONObject(i);
                String author = c.optString("authorName", "?");
                String date = c.optString("date", "");
                float rating = (float) c.optDouble("rating", 5);
                String commentText = c.optString("comment", "");

                View row = inflater.inflate(R.layout.item_comment, llComments, false);

                // Avatar circle
                TextView tvAvatar = row.findViewById(R.id.tv_comment_avatar);
                String initial = author.length() > 0
                        ? String.valueOf(author.charAt(0)).toUpperCase() : "?";
                tvAvatar.setText(initial);
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.OVAL);
                gd.setColor(AVATAR_COLORS[i % AVATAR_COLORS.length]);
                tvAvatar.setBackground(gd);

                ((TextView) row.findViewById(R.id.tv_comment_author)).setText(author);
                ((TextView) row.findViewById(R.id.tv_comment_date)).setText(date);
                ((RatingBar) row.findViewById(R.id.rb_comment_rating)).setRating(rating);
                ((TextView) row.findViewById(R.id.tv_comment_text)).setText(commentText);

                llComments.addView(row);

                // Divider
                View divider = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor("#F0F0F0"));
                llComments.addView(divider);

            } catch (Exception ignored) {}
        }
    }

    private void submitComment() {
        float rating = rbMyRating.getRating();
        String comment = etComment.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Selecciona una calificación", Toast.LENGTH_SHORT).show();
            return;
        }
        if (comment.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show();
            return;
        }

        MaterialButton btnSubmit = findViewById(R.id.btn_submit_comment);
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "/review");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject body = new JSONObject();
                body.put("email", userEmail);
                body.put("placeId", placeId);
                body.put("placeType", placeCategory);
                body.put("rating", (int) rating);
                body.put("comment", comment);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    mainHandler.post(() -> {
                        btnSubmit.setEnabled(true);
                        etComment.setText("");
                        rbMyRating.setRating(0);
                        Toast.makeText(this, "¡Comentario publicado!", Toast.LENGTH_SHORT).show();
                        loadComments();
                    });
                } else {
                    String errMsg = "Error al publicar";
                    try {
                        java.io.InputStream errStream = conn.getErrorStream();
                        if (errStream != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(errStream));
                            StringBuilder sb = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) sb.append(line);
                            reader.close();
                            org.json.JSONObject errJson = new org.json.JSONObject(sb.toString());
                            errMsg = errJson.optString("error", errMsg);
                        }
                    } catch (Exception ignored) {}
                    final String finalMsg = errMsg;
                    mainHandler.post(() -> {
                        btnSubmit.setEnabled(true);
                        Toast.makeText(this, finalMsg, Toast.LENGTH_LONG).show();
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
