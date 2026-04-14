package com.example.guander;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class PrivacySecurityActivity extends AppCompatActivity {

    private static final String PREFS = "guander_prefs";
    private int colorSurface;
    private int colorOnSurface;
    private int colorOnSurfaceVariant;
    private int colorOutline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        int[] attrs = {
            com.google.android.material.R.attr.colorSurface,
            com.google.android.material.R.attr.colorOnSurface,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            com.google.android.material.R.attr.colorOutline
        };
        TypedArray ta = obtainStyledAttributes(attrs);
        colorSurface = ta.getColor(0, 0xFFFFFFFF);
        colorOnSurface = ta.getColor(1, 0xFF212121);
        colorOnSurfaceVariant = ta.getColor(2, 0xFF757575);
        colorOutline = ta.getColor(3, 0xFFEEEEEE);
        ta.recycle();

        MaterialToolbar settingsToolbar = findViewById(R.id.toolbar);
        settingsToolbar.setTitle("Privacidad y Seguridad");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        LinearLayout content = findViewById(R.id.ll_content);

        addSwitch(content, prefs, "privacy_location", "Compartir ubicación",
                "Permite mostrar tu ubicación para encontrar lugares cercanos", true);
        addSwitch(content, prefs, "privacy_analytics", "Datos de uso",
                "Ayuda a mejorar Guander compartiendo datos de uso anónimos", true);
        addSwitch(content, prefs, "privacy_personalized", "Contenido personalizado",
                "Muestra recomendaciones basadas en tus visitas y preferencias", true);

        addInfoCard(content, "🔐", "Cuenta vinculada con Google",
                "Tu acceso está protegido por Google Sign-In. No se almacena ninguna contraseña en nuestros servidores.");
        addInfoCard(content, "🛡️", "Protección de datos",
                "Tu información personal está cifrada y nunca se comparte con terceros sin tu consentimiento.");
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
        addDivider(parent);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(colorOutline);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        parent.addView(divider, params);
    }

    private void addInfoCard(LinearLayout parent, String icon, String title, String body) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(colorSurface);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(params);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(icon + "  " + title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(colorOnSurface);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextSize(12f);
        tvBody.setTextColor(colorOnSurfaceVariant);
        tvBody.setPadding(0, dp(4), 0, 0);
        card.addView(tvBody);

        parent.addView(card);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
