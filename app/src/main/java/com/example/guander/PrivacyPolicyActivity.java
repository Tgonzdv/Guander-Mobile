package com.example.guander;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private int colorOnSurface;
    private int colorOnSurfaceVariant;
    private int colorPrimary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_base);

        int[] attrs = {
            com.google.android.material.R.attr.colorOnSurface,
            com.google.android.material.R.attr.colorOnSurfaceVariant
        };
        TypedArray ta = obtainStyledAttributes(attrs);
        colorOnSurface = ta.getColor(0, 0xFF212121);
        colorOnSurfaceVariant = ta.getColor(1, 0xFF757575);
        ta.recycle();
        colorPrimary = getColor(R.color.green_primary);

        MaterialToolbar settingsToolbar = findViewById(R.id.toolbar);
        settingsToolbar.setTitle("Políticas de Privacidad");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout content = findViewById(R.id.ll_content);
        addText(content, "Última actualización: enero 2025\n");

        addSection(content, "1. Información que Recopilamos",
                "• Información de cuenta: nombre, correo electrónico (vía Google Sign-In)\n" +
                "• Información de perfil: número de teléfono, ubicación (opcional), foto de perfil\n" +
                "• Datos de uso: lugares visitados, puntos acumulados, cupones canjeados\n" +
                "• Datos técnicos: versión del sistema operativo, identificador del dispositivo");

        addSection(content, "2. Cómo Usamos tu Información",
                "Usamos tu información personal para:\n" +
                "• Proveer y mejorar el servicio Guander\n" +
                "• Gestionar tu cuenta y PetPoints\n" +
                "• Enviarte notificaciones relevantes (si las activas)\n" +
                "• Personalizar tu experiencia en la aplicación\n" +
                "• Cumplir con obligaciones legales");

        addSection(content, "3. Almacenamiento de Imágenes",
                "Las fotos de perfil se almacenan de forma segura en Cloudinary, un servicio " +
                "de gestión de medios en la nube. Las imágenes están protegidas y solo son " +
                "accesibles para tu cuenta.");

        addSection(content, "4. Compartición de Datos",
                "No vendemos, alquilamos ni compartimos tu información personal con terceros " +
                "sin tu consentimiento, excepto:\n" +
                "• Con proveedores de servicios que nos ayudan a operar la plataforma\n" +
                "• Cuando sea requerido por la ley");

        addSection(content, "5. Seguridad",
                "Implementamos medidas de seguridad industry-standard para proteger tu información. " +
                "Sin embargo, ningún sistema es completamente seguro. Te recomendamos usar " +
                "contraseñas fuertes en tu cuenta de Google.");

        addSection(content, "6. Tus Derechos",
                "Tienes derecho a:\n" +
                "• Acceder a tu información personal\n" +
                "• Corregir datos incorrectos (desde tu perfil)\n" +
                "• Solicitar la eliminación de tu cuenta\n" +
                "• Desactivar notificaciones en cualquier momento");

        addSection(content, "7. Retención de Datos",
                "Conservamos tu información mentre tu cuenta esté activa. Puedes solicitar la " +
                "eliminación de tus datos en cualquier momento escribiendo a privacidad@guander.app");

        addSection(content, "8. Contacto",
                "Si tienes preguntas sobre esta política, contáctanos:\n" +
                "📧 privacidad@guander.app");
    }

    private void addSection(LinearLayout parent, String title, String body) {
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(colorPrimary);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, dp(12), 0, dp(4));
        parent.addView(tvTitle);

        TextView tvBody = new TextView(this);
        tvBody.setText(body);
        tvBody.setTextSize(13f);
        tvBody.setTextColor(colorOnSurface);
        tvBody.setLineSpacing(0, 1.4f);
        parent.addView(tvBody);
    }

    private void addText(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(colorOnSurfaceVariant);
        parent.addView(tv);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
