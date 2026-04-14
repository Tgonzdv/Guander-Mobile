package com.example.guander;

import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import androidx.appcompat.app.AppCompatDelegate;

public class AppearanceActivity extends AppCompatActivity {

    private static final String PREFS = "guander_prefs";
    private static final String KEY_THEME = "appearance_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        MaterialToolbar settingsToolbar = findViewById(R.id.toolbar);
        settingsToolbar.setTitle("Apariencia");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int savedMode = prefs.getInt(KEY_THEME, 0); // 0=system, 1=light, 2=dark

        LinearLayout content = findViewById(R.id.ll_content);

        // Resolve theme-aware colors
        TypedArray ta = obtainStyledAttributes(new int[]{
                com.google.android.material.R.attr.colorSurface,
                com.google.android.material.R.attr.colorOnSurface,
                com.google.android.material.R.attr.colorOnSurfaceVariant});
        int colorSurface         = ta.getColor(0, 0xFFFFFFFF);
        int colorOnSurface       = ta.getColor(1, 0xFF212121);
        int colorOnSurfaceVariant = ta.getColor(2, 0xFF757575);
        ta.recycle();

        TextView label = new TextView(this);
        label.setText("Tema de la aplicación");
        label.setTextSize(13f);
        label.setTextColor(colorOnSurfaceVariant);
        label.setPadding(0, 0, 0, dp(8));
        content.addView(label);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(colorSurface);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(card);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);

        String[] labels = {"🌐  Sistema (predeterminado)", "☀️  Claro", "🌙  Oscuro"};
        int[] values = {0, 1, 2};
        int[] ids = {R.id.rbtn_system, R.id.rbtn_light, R.id.rbtn_dark};

        RadioButton[] buttons = new RadioButton[3];
        for (int i = 0; i < 3; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(ids[i]);
            rb.setText(labels[i]);
            rb.setTextSize(15f);
            rb.setTextColor(colorOnSurface);
            rb.setPadding(dp(4), dp(12), dp(4), dp(12));
            rg.addView(rb);
            buttons[i] = rb;
        }
        buttons[savedMode].setChecked(true);
        card.addView(rg);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.rbtn_light) {
                mode = 1;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.rbtn_dark) {
                mode = 2;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                mode = 0;
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
            prefs.edit().putInt(KEY_THEME, mode).apply();
        });
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
