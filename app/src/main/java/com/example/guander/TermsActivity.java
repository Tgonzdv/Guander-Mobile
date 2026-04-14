package com.example.guander;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public class TermsActivity extends AppCompatActivity {

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
        settingsToolbar.setTitle("Términos y Condiciones");
        settingsToolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout content = findViewById(R.id.ll_content);
        addText(content, "Última actualización: enero 2025\n");

        addSection(content, "1. Aceptación de los Términos",
                "Al descargar, instalar o utilizar la aplicación Guander, usted acepta quedar " +
                "vinculado por estos Términos y Condiciones. Si no está de acuerdo con alguna " +
                "parte de estos términos, no podrá utilizar el servicio.");

        addSection(content, "2. Descripción del Servicio",
                "Guander es una plataforma que conecta a dueños de mascotas con establecimientos " +
                "pet-friendly. El servicio incluye la acumulación de PetPoints, canje de recompensas " +
                "y descubrimiento de lugares amigables para mascotas.");

        addSection(content, "3. Registro de Cuenta",
                "Para utilizar Guander debe crear una cuenta utilizando su cuenta de Google. " +
                "Es responsable de mantener la confidencialidad de su cuenta y de todas las " +
                "actividades realizadas bajo la misma.");

        addSection(content, "4. PetPoints y Recompensas",
                "Los PetPoints son un sistema de recompensas virtual sin valor monetario. " +
                "Guander se reserva el derecho de modificar, suspender o cancelar el programa " +
                "de puntos en cualquier momento. Los puntos no son transferibles ni canjeables " +
                "por dinero en efectivo.");

        addSection(content, "5. Contenido del Usuario",
                "Al subir fotos o información a Guander, usted otorga a Guander una licencia " +
                "no exclusiva para usar dicho contenido dentro de la plataforma. No debe subir " +
                "contenido que infrinja derechos de terceros.");

        addSection(content, "6. Limitación de Responsabilidad",
                "Guander no garantiza la exactitud, completitud o utilidad de la información " +
                "de los establecimientos. En ningún caso Guander será responsable de daños " +
                "directos o indirectos derivados del uso de la aplicación.");

        addSection(content, "7. Modificaciones",
                "Guander se reserva el derecho de modificar estos Términos en cualquier momento. " +
                "Las modificaciones entrarán en vigor una vez publicadas en la aplicación.");

        addSection(content, "8. Contacto",
                "Para cualquier consulta sobre estos Términos, contáctenos en: " +
                "legal@guander.app");
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
