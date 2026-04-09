package com.example.guander;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class GuanderApp extends Application {

    private static final String PREFS = "guander_prefs";

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyTheme(prefs);
        applyLocale(prefs);
    }

    private void applyTheme(SharedPreferences prefs) {
        int mode = prefs.getInt("appearance_mode", 0);
        switch (mode) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void applyLocale(SharedPreferences prefs) {
        String lang = prefs.getString("language", "es");
        LocaleListCompat localeList = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(localeList);
    }
}
