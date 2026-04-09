package com.example.guander;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private static final String WORKER_URL = "https://guander-api.tomas-gonzalezz.workers.dev/register";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private MaterialButton btnGoogle;
    private MaterialButton btnGoogleRegister;
    private ProgressBar pbLoading;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    String idToken = account.getIdToken();
                    if (idToken == null) {
                        setLoading(false);
                        Toast.makeText(this, "Error: no se obtuvo token. Verificá el SHA-1 en Firebase.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    firebaseAuthWithGoogle(idToken);
                } catch (ApiException e) {
                    setLoading(false);
                    String msg;
                    switch (e.getStatusCode()) {
                        case 7:  msg = "Sin conexión a internet"; break;
                        case 10: msg = "Error de configuración (SHA-1)"; break;
                        case 12501: msg = "Inicio de sesión cancelado"; break;
                        default: msg = "Error Google: código " + e.getStatusCode();
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle = findViewById(R.id.btn_google);
        btnGoogleRegister = findViewById(R.id.btn_google_register);
        pbLoading = findViewById(R.id.pb_loading);

        btnGoogle.setOnClickListener(v -> { setLoading(true); signInWithGoogle(); });
        btnGoogleRegister.setOnClickListener(v -> { setLoading(true); signInWithGoogle(); });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToWelcome();
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGoogle.setEnabled(!loading);
        btnGoogleRegister.setEnabled(!loading);
        btnGoogle.setAlpha(loading ? 0.6f : 1f);
        btnGoogleRegister.setAlpha(loading ? 0.6f : 1f);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) saveUserToDatabase(user);
                    goToWelcome();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveUserToDatabase(FirebaseUser user) {
        String email = user.getEmail() != null ? user.getEmail() : "";
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
        String[] parts = displayName.split(" ", 2);
        String name = parts.length > 0 ? parts[0] : "";
        String lastName = parts.length > 1 ? parts[1] : "";

        new Thread(() -> {
            try {
                URL url = new URL(WORKER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                String safeEmail    = email.replace("\"", "\\\"");
                String safeName     = name.replace("\"", "\\\"");
                String safeLastName = lastName.replace("\"", "\\\"");
                String json = "{\"email\":\"" + safeEmail + "\","
                            + "\"name\":\"" + safeName + "\","
                            + "\"lastName\":\"" + safeLastName + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void goToWelcome() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}