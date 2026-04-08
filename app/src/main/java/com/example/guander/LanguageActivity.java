package com.example.guander;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LanguageActivity extends AppCompatActivity {

    private static final String PREFS = "guander_prefs";
    private static final String KEY_LANG = "language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        ((TextView) findViewById(R.id.tv_settings_title)).setText("Idioma");
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedLang = prefs.getString(KEY_LANG, "es");

        LinearLayout content = findViewById(R.id.ll_content);

        TextView label = new TextView(this);
        label.setText("Selecciona el idioma de la aplicación");
        label.setTextSize(13f);
        label.setTextColor(0xFF757575);
        label.setPadding(0, 0, 0, dp(8));
        content.addView(label);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(dp(16), dp(8), dp(16), dp(8));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        content.addView(card);

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);

        RadioButton rbEs = new RadioButton(this);
        rbEs.setId(R.id.rbtn_spanish);
        rbEs.setText("🇲🇽  Español");
        rbEs.setTextSize(15f);
        rbEs.setTextColor(0xFF212121);
        rbEs.setPadding(dp(4), dp(12), dp(4), dp(12));
        rbEs.setChecked("es".equals(savedLang));
        rg.addView(rbEs);

        RadioButton rbEn = new RadioButton(this);
        rbEn.setId(R.id.rbtn_english);
        rbEn.setText("🇺🇸  English");
        rbEn.setTextSize(15f);
        rbEn.setTextColor(0xFF212121);
        rbEn.setPadding(dp(4), dp(12), dp(4), dp(12));
        rbEn.setChecked("en".equals(savedLang));
        card.addView(rg);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = (checkedId == R.id.rbtn_english) ? "en" : "es";
            prefs.edit().putString(KEY_LANG, lang).apply();
            Toast.makeText(this,
                    "en".equals(lang) ? "Language saved. Restart the app to apply." :
                            "Idioma guardado. Reinicia la app para aplicar.",
                    Toast.LENGTH_LONG).show();
        });
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
