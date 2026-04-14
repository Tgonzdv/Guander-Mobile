package com.example.guander;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class NotificationsSettingsActivity extends AppCompatActivity {

    private static final String PREFS = "guander_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        MaterialToolbar settingsToolbar = findViewById(R.id.toolbar);
        settingsToolbar.setTitle("Notificaciones");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout content = findViewById(R.id.ll_content);

        boolean masterOn = prefs.getBoolean("notif_general", true);

        // Master switch
        View masterItem = getLayoutInflater().inflate(R.layout.item_switch_setting, content, false);
        ((TextView) masterItem.findViewById(R.id.tv_switch_title)).setText("Notificaciones generales");
        ((TextView) masterItem.findViewById(R.id.tv_switch_subtitle)).setText("Recibe novedades y actualizaciones de Guander");
        Switch masterSw = masterItem.findViewById(R.id.sw_toggle);
        masterSw.setChecked(masterOn);
        content.addView(masterItem);

        // Child switches
        List<View> childItems = new ArrayList<>();
        childItems.add(addSwitch(content, prefs, "notif_points", "Mis puntos",
                "Notificaciones cuando acumules o uses PetPoints", true, masterOn));
        childItems.add(addSwitch(content, prefs, "notif_coupons", "Cupones",
                "Alertas cuando tengas cupones por vencer", true, masterOn));
        childItems.add(addSwitch(content, prefs, "notif_places", "Nuevos lugares",
                "Entérate cuando haya lugares pet-friendly cerca de ti", false, masterOn));
        childItems.add(addSwitch(content, prefs, "notif_promo", "Promociones",
                "Descuentos especiales y ofertas de temporada", true, masterOn));

        masterSw.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("notif_general", checked).apply();
            for (View item : childItems) {
                Switch sw = item.findViewById(R.id.sw_toggle);
                sw.setEnabled(checked);
                item.setAlpha(checked ? 1f : 0.4f);
            }
        });
    }

    private View addSwitch(LinearLayout parent, SharedPreferences prefs,
                            String key, String title, String subtitle,
                            boolean defaultVal, boolean enabled) {
        View item = getLayoutInflater().inflate(R.layout.item_switch_setting, parent, false);
        ((TextView) item.findViewById(R.id.tv_switch_title)).setText(title);
        ((TextView) item.findViewById(R.id.tv_switch_subtitle)).setText(subtitle);
        Switch sw = item.findViewById(R.id.sw_toggle);
        sw.setChecked(prefs.getBoolean(key, defaultVal));
        sw.setEnabled(enabled);
        item.setAlpha(enabled ? 1f : 0.4f);
        sw.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(key, checked).apply());
        parent.addView(item);
        return item;
    }
}
