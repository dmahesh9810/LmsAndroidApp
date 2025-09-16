package com.example.iqbravelms;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.TimeoutError;
import com.android.volley.NoConnectionError;
import com.android.volley.AuthFailureError;
import com.android.volley.ServerError;
import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

// Dashboard Activities
import com.example.iqbravelms.AdminDashboardActivity;
import com.example.iqbravelms.StudentDashboardActivity;
import com.example.iqbravelms.InstructorDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView textRegister;
    private RequestQueue requestQueue;

    // Change BASE_URL according to environment
    private static final String BASE_URL = "http://localhost:8080"; // Emulator
    // private static final String BASE_URL = "http://192.168.x.x:8080"; // Real Device
    private static final String LOGIN_URL = BASE_URL + "/api/users/login";

    private static final String TAG = "LoginActivity";
    public static final String PREFS_NAME = "IQBRAVE_PREFS";
    public static final String AUTH_TOKEN_KEY = "auth_token";
    public static final String USER_ROLE_KEY = "user_role";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        textRegister = findViewById(R.id.textRegister); // Register link

        requestQueue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(v -> loginUser());

        // Go to RegisterActivity
        textRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        final String email = editEmail.getText().toString().trim();
        final String password = editPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Log.d(TAG, "Attempting login with URL: " + LOGIN_URL);
            Log.d(TAG, "Request Body: " + body.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    LOGIN_URL,
                    body,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Log.d(TAG, "Login Response: " + response.toString());
                        handleLoginResponse(response);
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        handleVolleyError(error);
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            requestQueue.add(request);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSONException while creating request body: " + e.getMessage(), e);
            Toast.makeText(this, "JSON error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
        }
    }

    private void handleLoginResponse(JSONObject response) {
        try {
            String message = response.optString("message", "Login status unknown");
            Log.d(TAG, "Server Message: " + message);

            if (response.has("token") && response.has("role")) {
                String token = response.getString("token");
                String role = response.getString("role");

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(AUTH_TOKEN_KEY, token);
                editor.putString(USER_ROLE_KEY, role);
                editor.apply();

                Toast.makeText(this, "Login successful. Role: " + role, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Login successful. Token: " + token + ", Role: " + role);

                Intent intent = null;
                if (role.equals("ADMIN")) {
                    intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                } else if (role.equals("STUDENT")) {
                    intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
                } else if (role.equals("INSTRUCTOR")) {
                    intent = new Intent(LoginActivity.this, InstructorDashboardActivity.class);
                } else {
                    Log.w(TAG, "Unknown role: " + role);
                    Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_LONG).show();
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }

            } else {
                String missingData = "";
                if (!response.has("token")) missingData += "token ";
                if (!response.has("role")) missingData += "role ";
                Toast.makeText(this, "Login response missing: " + missingData.trim(), Toast.LENGTH_LONG).show();
                Log.w(TAG, "Login response missing: " + missingData.trim() + " in " + response.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "JSONException while parsing login response: " + e.getMessage(), e);
            Toast.makeText(this, "Response parsing error", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVolleyError(VolleyError error) {
        String msg = (error.getMessage() != null) ? error.getMessage() : "Unknown error";
        Log.e(TAG, "VolleyError: " + msg, error);

        if (error instanceof TimeoutError) {
            Toast.makeText(this, "Connection timed out. Please check server and network.", Toast.LENGTH_LONG).show();
        } else if (error instanceof NoConnectionError) {
            Toast.makeText(this, "Cannot connect to server. Please check network.", Toast.LENGTH_LONG).show();
        } else if (error instanceof AuthFailureError) {
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_LONG).show();
        } else if (error instanceof ServerError) {
            if (error.networkResponse != null) {
                Log.e(TAG, "ServerError Status Code: " + error.networkResponse.statusCode);
                try {
                    String responseBody = new String(error.networkResponse.data, "utf-8");
                    Log.e(TAG, "ServerError Response Body: " + responseBody);
                    Toast.makeText(this, "Server error (" + error.networkResponse.statusCode + ")", Toast.LENGTH_LONG).show();
                } catch (java.io.UnsupportedEncodingException e) {
                    Log.e(TAG, "Error parsing server error response", e);
                }
            } else {
                Toast.makeText(this, "Server error (no response).", Toast.LENGTH_LONG).show();
            }
        } else if (error instanceof NetworkError) {
            Toast.makeText(this, "Network error. Please check connection.", Toast.LENGTH_LONG).show();
        } else if (error instanceof ParseError) {
            Toast.makeText(this, "Error parsing server response.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
        }
    }
}
