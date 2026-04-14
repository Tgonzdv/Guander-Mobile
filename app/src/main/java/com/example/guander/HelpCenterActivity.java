package com.example.guander;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class HelpCenterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        MaterialToolbar settingsToolbar = findViewById(R.id.toolbar);
        settingsToolbar.setTitle("Centro de Ayuda");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout content = findViewById(R.id.ll_content);

        addFaq(content, "¿Cómo acumulo PetPoints?",
                "Acumulas PetPoints cada vez que visitas un lugar pet-friendly " +
                "registrado en Guander. Los puntos se agregan automáticamente al escanear el código QR del establecimiento.");

        addFaq(content, "¿Cómo canjeo mis PetPoints?",
                "Puedes canjear tus PetPoints en la sección 'Mis Recompensas' del menú principal. " +
                "Cada recompensa indica cuántos puntos necesitas.");

        addFaq(content, "¿Cómo actualizo mi foto de perfil?",
                "Ve a tu perfil y toca el ícono de cámara sobre tu foto. " +
                "Selecciona una imagen de tu galería y se actualizará automáticamente.");

        addFaq(content, "¿Puedo cambiar mi información personal?",
                "Sí. En tu perfil, toca el botón 'Editar' en la sección de Información Personal. " +
                "Puedes cambiar tu nombre, teléfono y ubicación.");

        addFaq(content, "¿Cómo activo las notificaciones?",
                "Ve a Configuración → Notificaciones en tu perfil y activa o desactiva " +
                "los tipos de notificaciones que desees recibir.");

        addFaq(content, "¿Mi cuenta está segura?",
                "Sí. Guander usa Google Sign-In, por lo que tu contraseña es gestionada " +
                "por Google. Nosotros nunca almacenamos contraseñas.");

        addFaq(content, "¿Cómo elimino mi cuenta?",
                "Para eliminar tu cuenta, envíanos un correo a soporte@guander.app con tu " +
                "solicitud y lo procesaremos en un plazo de 5 días hábiles.");
    }

    private void addFaq(LinearLayout parent, String question, String answer) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(params);

        TextView tvQ = new TextView(this);
        tvQ.setText("❓  " + question);
        tvQ.setTextSize(14f);
        tvQ.setTextColor(0xFF1B5E20);
        tvQ.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvQ);

        TextView tvA = new TextView(this);
        tvA.setText(answer);
        tvA.setTextSize(13f);
        tvA.setTextColor(0xFF424242);
        tvA.setPadding(0, dp(6), 0, 0);
        card.addView(tvA);

        parent.addView(card);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
