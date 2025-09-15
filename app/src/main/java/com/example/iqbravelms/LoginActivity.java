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

// අදාළ Dashboard Activities import කරගන්න
import com.example.iqbravelms.AdminDashboardActivity;
import com.example.iqbravelms.StudentDashboardActivity;
import com.example.iqbravelms.InstructorDashboardActivity;
// MainActivity ද අවශ්‍ය නම් import කරගන්න (default fallback සඳහා)
// import com.example.iqbravelms.MainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    private static final String BASE_URL = "http://localhost:8080"; // USB Debugging නිසා localhost
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

        requestQueue = Volley.newRequestQueue(this);

        btnLogin.setOnClickListener(v -> loginUser());
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
                        String msg = (error.getMessage() != null) ? error.getMessage() : "Login failed with unknown error";
                        Log.e(TAG, "Login VolleyError: " + msg, error);

                        if (error instanceof TimeoutError) {
                            Log.e(TAG, "Detailed Error: TimeoutError");
                            Toast.makeText(LoginActivity.this, "Login failed: Connection timed out. Please check server and network.", Toast.LENGTH_LONG).show();
                        } else if (error instanceof NoConnectionError) {
                            Log.e(TAG, "Detailed Error: NoConnectionError");
                            Toast.makeText(LoginActivity.this, "Login failed: Cannot connect to server. Please check network.", Toast.LENGTH_LONG).show();
                        } else if (error instanceof AuthFailureError) {
                            Log.e(TAG, "Detailed Error: AuthFailureError");
                            Toast.makeText(LoginActivity.this, "Login failed: Authentication error.", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ServerError) {
                            Log.e(TAG, "Detailed Error: ServerError");
                            if (error.networkResponse != null) {
                                Log.e(TAG, "ServerError Status Code: " + error.networkResponse.statusCode);
                                try {
                                    String responseBody = new String(error.networkResponse.data, "utf-8");
                                    Log.e(TAG, "ServerError Response Body: " + responseBody);
                                    Toast.makeText(LoginActivity.this, "Login failed: Server error (" + error.networkResponse.statusCode + "). See logs.", Toast.LENGTH_LONG).show();
                                } catch (java.io.UnsupportedEncodingException e) {
                                    Log.e(TAG, "Error parsing server error response", e);
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Login failed: Server error (no response data).", Toast.LENGTH_LONG).show();
                            }
                        } else if (error instanceof NetworkError) {
                            Log.e(TAG, "Detailed Error: NetworkError");
                            Toast.makeText(LoginActivity.this, "Login failed: Network error. Please check connection.", Toast.LENGTH_LONG).show();
                        } else if (error instanceof ParseError) {
                            Log.e(TAG, "Detailed Error: ParseError");
                            Toast.makeText(LoginActivity.this, "Login failed: Error parsing server response.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // initial timeout_ms
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // maxNumRetries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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
                    Log.w(TAG, "Unknown role: " + role + ". Redirecting to default activity.");
                    // ඔබ default activity එකක් ලෙස MainActivity පාවිච්චි කරනවා නම්, එය import කර තිබිය යුතුයි.
                    // intent = new Intent(LoginActivity.this, MainActivity.class); 
                    // Default fallback එකක් නැත්නම්, දෝෂයක් පෙන්වීම හෝ කිසිවක් නොකර සිටීම කළ හැක.
                    // For now, let's just log and not start any activity if role is unknown and MainActivity is not defined as fallback
                    Toast.makeText(this, "Unknown role: " + role, Toast.LENGTH_LONG).show();
                }
                
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish(); // LoginActivity එක අවසන් කිරීම
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
}
