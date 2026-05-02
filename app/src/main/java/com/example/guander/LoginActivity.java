package com.example.guander;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
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

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginActivity extends AppCompatActivity {

    private static final String WORKER_URL       = "https://guander-api.tomas-gonzalezz.workers.dev/register";
    private static final String WORKER_LOGIN_URL  = "https://guander-api.tomas-gonzalezz.workers.dev/login-email";
    private static final String WORKER_FORGOT_URL = "https://guander-api.tomas-gonzalezz.workers.dev/forgot-password";
    private static final String WORKER_RESET_URL  = "https://guander-api.tomas-gonzalezz.workers.dev/reset-password";
    private static final String PREFS             = "guander_prefs";
    private static final String KEY_EMAIL_AUTH    = "email_auth";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private MaterialButton btnGoogle;
    private MaterialButton btnEmailToggle;
    private MaterialButton btnEmailLogin;
    private MaterialButton btnEmailRegister;
    private LinearLayout emailFormContainer;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
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

        btnGoogle          = findViewById(R.id.btn_google);
        btnEmailToggle     = findViewById(R.id.btn_email_toggle);
        btnEmailLogin      = findViewById(R.id.btn_email_login);
        btnEmailRegister   = findViewById(R.id.btn_email_register);
        emailFormContainer = findViewById(R.id.email_form_container);
        etEmail            = findViewById(R.id.et_email);
        etPassword         = findViewById(R.id.et_password);
        pbLoading          = findViewById(R.id.pb_loading);

        btnGoogle.setOnClickListener(v -> { setLoading(true); signInWithGoogle(); });
        btnEmailToggle.setOnClickListener(v -> {
            boolean visible = emailFormContainer.getVisibility() == View.VISIBLE;
            emailFormContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        });
        btnEmailLogin.setOnClickListener(v -> handleEmailLogin());
        btnEmailRegister.setOnClickListener(v -> handleEmailRegister());
        findViewById(R.id.tv_forgot_password).setOnClickListener(v -> showForgotPasswordDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Google/Firebase session
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            goToWelcome();
            return;
        }
        // Email/password session stored in SharedPreferences
        String savedEmail = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_EMAIL_AUTH, null);
        if (savedEmail != null) {
            goToWelcome();
        }
    }

    // ── Google ───────────────────────────────────────────────────────────────

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "";
                        String[] parts = displayName.split(" ", 2);
                        String name     = parts.length > 0 ? parts[0] : "";
                        String lastName = parts.length > 1 ? parts[1] : "";
                        saveUserToWorker(user.getEmail() != null ? user.getEmail() : "", name, lastName, this::goToWelcome);
                    } else {
                        goToWelcome();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Error Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── Forgot password ───────────────────────────────────────────────────────

    private void showForgotPasswordDialog() {
        EditText etEmailInput = new EditText(this);
        etEmailInput.setHint("Correo electrónico");
        etEmailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etEmailInput.setPadding(pad, pad / 2, pad, pad / 2);

        // Pre-fill with current email field if visible
        if (etEmail.getText() != null && !etEmail.getText().toString().isEmpty()) {
            etEmailInput.setText(etEmail.getText().toString().trim());
        }

        new AlertDialog.Builder(this)
                .setTitle("Recuperar contraseña")
                .setMessage("Ingresá tu correo y te enviaremos un código de verificación.")
                .setView(etEmailInput)
                .setPositiveButton("Enviar código", (dialog, which) -> {
                    String email = etEmailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(this, "Ingresá tu correo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callForgotPassword(email);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void callForgotPassword(String email) {
        setLoading(true);
        new Thread(() -> {
            try {
                URL url = new URL(WORKER_FORGOT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                String safeEmail = email.replace("\"", "\\\"");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("{\"email\":\"" + safeEmail + "\"}").getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code < 400 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject resp = new JSONObject(sb.toString());
                if (resp.optBoolean("success", false)) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        showEnterCodeDialog(email);
                    });
                } else {
                    String msg = resp.optString("error", "Error al enviar el código");
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showEnterCodeDialog(String email) {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (16 * density);
        int gap = (int) (8 * density);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, gap, pad, 0);

        EditText etCode = new EditText(this);
        etCode.setHint("Código de 6 dígitos");
        etCode.setInputType(InputType.TYPE_CLASS_NUMBER);
        etCode.setMaxLines(1);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = gap;
        etCode.setLayoutParams(lp);
        layout.addView(etCode);

        EditText etNewPass = new EditText(this);
        etNewPass.setHint("Nueva contraseña (mín. 6 caracteres)");
        etNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etNewPass.setMaxLines(1);
        layout.addView(etNewPass);

        new AlertDialog.Builder(this)
                .setTitle("Restablecer contraseña")
                .setMessage("Revisá tu correo " + email + " e ingresá el código recibido.")
                .setView(layout)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String code       = etCode.getText().toString().trim();
                    String newPass    = etNewPass.getText().toString();
                    if (code.length() != 6) {
                        Toast.makeText(this, "El código debe tener 6 dígitos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    callResetPassword(email, code, newPass);
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Reenviar código", (dialog, which) -> callForgotPassword(email))
                .show();
    }

    private void callResetPassword(String email, String token, String newPassword) {
        setLoading(true);
        new Thread(() -> {
            try {
                URL url = new URL(WORKER_RESET_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("token", token);
                body.put("newPassword", newPassword);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code < 400 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject resp = new JSONObject(sb.toString());
                if (resp.optBoolean("success", false)) {
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, "✓ Contraseña actualizada. Ya podés iniciar sesión.", Toast.LENGTH_LONG).show();
                    });
                } else {
                    String msg = resp.optString("error", "Error al restablecer la contraseña");
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ── Email / password ─────────────────────────────────────────────────────

    private void handleEmailLogin() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresá tu correo");
            etEmail.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Ingresá tu contraseña");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);
        new Thread(() -> {
            try {
                URL url = new URL(WORKER_LOGIN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoOutput(true);

                String safeEmail    = email.replace("\"", "\\\"");
                String safePassword = password.replace("\"", "\\\"");
                String json = "{\"email\":\"" + safeEmail + "\",\"password\":\"" + safePassword + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject resp = new JSONObject(sb.toString());
                if (code == 200 && resp.optBoolean("success", false)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(KEY_EMAIL_AUTH, email).apply();
                    runOnUiThread(this::goToWelcome);
                } else {
                    String msg = resp.optString("error", "Error al iniciar sesión");
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void handleEmailRegister() {
        String email    = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Ingresá tu correo");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("La contraseña debe tener al menos 6 caracteres");
            etPassword.requestFocus();
            return;
        }

        // Collect name, last name and phone before registering
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_register_name, null);
        TextInputEditText etName     = dialogView.findViewById(R.id.et_dialog_name);
        TextInputEditText etLastName = dialogView.findViewById(R.id.et_dialog_last_name);
        TextInputEditText etTel      = dialogView.findViewById(R.id.et_dialog_tel);

        new AlertDialog.Builder(this)
                .setTitle("Completá tu perfil")
                .setView(dialogView)
                .setPositiveButton("Registrarse", (d, w) -> {
                    String name     = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
                    String tel      = etTel.getText() != null ? etTel.getText().toString().trim() : "";
                    registerWithCloudflare(email, password, name, lastName, tel);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void registerWithCloudflare(String email, String password, String name, String lastName, String tel) {
        setLoading(true);
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
                String safePassword = password.replace("\"", "\\\"");
                String safeName     = name.replace("\"", "\\\"");
                String safeLastName = lastName.replace("\"", "\\\"");
                String safeTel      = tel.replace("\"", "\\\"");
                String json = "{\"email\":\"" + safeEmail + "\","
                            + "\"password\":\"" + safePassword + "\","
                            + "\"name\":\"" + safeName + "\","
                            + "\"lastName\":\"" + safeLastName + "\","
                            + "\"tel\":\"" + safeTel + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        code < 400 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject resp = new JSONObject(sb.toString());
                if (code == 200 && resp.optBoolean("success", false)) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(KEY_EMAIL_AUTH, email).apply();
                    runOnUiThread(this::goToWelcome);
                } else {
                    String msg = resp.optString("error", "Error al registrarse");
                    runOnUiThread(() -> {
                        setLoading(false);
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private void saveUserToWorker(String email, String name, String lastName, Runnable onComplete) {
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
            } finally {
                if (onComplete != null) {
                    runOnUiThread(onComplete);
                }
            }
        }).start();
    }

    private String mapAuthError(String message) {
        if (message == null) return "Error desconocido";
        if (message.contains("email address is already in use")) return "Ese correo ya está registrado";
        if (message.contains("no user record"))                  return "No existe una cuenta con ese correo";
        if (message.contains("password is invalid") || message.contains("INVALID_LOGIN_CREDENTIALS")) return "Contraseña incorrecta";
        if (message.contains("badly formatted"))                 return "El correo no tiene un formato válido";
        if (message.contains("network"))                         return "Sin conexión a internet";
        return "Error: " + message;
    }

    private void setLoading(boolean loading) {
        pbLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnGoogle.setEnabled(!loading);
        btnEmailToggle.setEnabled(!loading);
        btnEmailLogin.setEnabled(!loading);
        btnEmailRegister.setEnabled(!loading);
        btnGoogle.setAlpha(loading ? 0.6f : 1f);
        btnEmailToggle.setAlpha(loading ? 0.6f : 1f);
        btnEmailLogin.setAlpha(loading ? 0.6f : 1f);
        btnEmailRegister.setAlpha(loading ? 0.6f : 1f);
    }

    private void goToWelcome() {
        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}