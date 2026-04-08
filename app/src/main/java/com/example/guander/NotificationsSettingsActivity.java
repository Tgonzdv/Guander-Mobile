package com.example.guander;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationsSettingsActivity extends AppCompatActivity {

    private static final String PREFS = "guander_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        ((TextView) findViewById(R.id.tv_settings_title)).setText("Notificaciones");
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout content = findViewById(R.id.ll_content);

        addSwitch(content, prefs, "notif_general", "Notificaciones generales",
                "Recibe novedades y actualizaciones de Guander", true);
        addSwitch(content, prefs, "notif_points", "Mis puntos",
                "Notificaciones cuando acumules o uses PetPoints", true);
        addSwitch(content, prefs, "notif_coupons", "Cupones",
                "Alertas cuando tengas cupones por vencer", true);
        addSwitch(content, prefs, "notif_places", "Nuevos lugares",
                "Enterate cuando haya lugares pet-friendly cerca de ti", false);
        addSwitch(content, prefs, "notif_promo", "Promociones",
                "Descuentos especiales y ofertas de temporada", true);
    }

    private void addSwitch(LinearLayout parent, SharedPreferences prefs,
                            String key, String title, String subtitle, boolean defaultVal) {
        View item = getLayoutInflater().inflate(R.layout.item_switch_setting, parent, false);
        ((TextView) item.findViewById(R.id.tv_switch_title)).setText(title);
        ((TextView) item.findViewById(R.id.tv_switch_subtitle)).setText(subtitle);
        Switch sw = item.findViewById(R.id.sw_toggle);
        sw.setChecked(prefs.getBoolean(key, defaultVal));
        sw.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(key, checked).apply());
        parent.addView(item);
    }
}
