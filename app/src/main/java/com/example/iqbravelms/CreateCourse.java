package com.example.iqbravelms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CreateCourse extends AppCompatActivity {

    private EditText editTextCourseTitle;
    private EditText editTextCourseDescription;
    private Button buttonSaveCourse;
    private ProgressBar progressBar;
    private RequestQueue requestQueue;

    private static final String CREATE_COURSE_API_URL = "http://localhost:8080/api/courses";
    private static final String TAG = "CreateCourseActivity";
    private String currentToken; // To store token for getHeaders

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_course);

        editTextCourseTitle = findViewById(R.id.editTextCourseTitle);
        editTextCourseDescription = findViewById(R.id.editTextCourseDescription);
        buttonSaveCourse = findViewById(R.id.buttonSaveCourse);
        // progressBar = findViewById(R.id.progressBarCreateCourse); // Add ProgressBar to XML if needed

        requestQueue = Volley.newRequestQueue(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        buttonSaveCourse.setOnClickListener(v -> saveCourse());
    }

    private void saveCourse() {
        final String title = editTextCourseTitle.getText().toString().trim();
        final String description = editTextCourseDescription.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editTextCourseTitle.setError("Title cannot be empty");
            editTextCourseTitle.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            editTextCourseDescription.setError("Description cannot be empty");
            editTextCourseDescription.requestFocus();
            return;
        }

        // if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        buttonSaveCourse.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        currentToken = prefs.getString(LoginActivity.AUTH_TOKEN_KEY, null);

        Log.d(TAG, "Retrieved token from SharedPreferences: " + currentToken);

        if (currentToken == null) {
            Toast.makeText(this, "Authentication token not found. Please login again.", Toast.LENGTH_LONG).show();
            // if (progressBar != null) progressBar.setVisibility(View.GONE);
            buttonSaveCourse.setEnabled(true);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("instructor_id", 1); // Still hardcoded for now
            requestBody.put("active", true);
            Log.d(TAG, "Request Body for API: " + requestBody.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception when creating request body: " + e.getMessage(), e);
            // if (progressBar != null) progressBar.setVisibility(View.GONE);
            buttonSaveCourse.setEnabled(true);
            Toast.makeText(this, "Error creating request for server.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                CREATE_COURSE_API_URL,
                requestBody,
                response -> {
                    // if (progressBar != null) progressBar.setVisibility(View.GONE);
                    buttonSaveCourse.setEnabled(true);
                    Log.d(TAG, "Create Course API Response: " + response.toString());
                    Toast.makeText(CreateCourse.this, "Course created successfully!", Toast.LENGTH_LONG).show();
                    finish();
                },
                error -> {
                    // if (progressBar != null) progressBar.setVisibility(View.GONE);
                    buttonSaveCourse.setEnabled(true);
                    Log.e(TAG, "Volley Error on create course: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Volley Error - Status Code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Volley Error - Response Body: " + responseBody);
                            JSONObject data = new JSONObject(responseBody);
                            String message = data.optString("message", "Failed to create course (default message).");
                            if (error.networkResponse.statusCode == 401) {
                                message = data.optString("error", message); // For 401, backend might send message in "error" field
                            }
                            Toast.makeText(CreateCourse.this, "Error: " + message + " (Status: " + error.networkResponse.statusCode + ")", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing Volley error response: " + e.getMessage());
                            Toast.makeText(CreateCourse.this, "Failed to create course. Status: " + error.networkResponse.statusCode + " (Error response parsing failed)", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(CreateCourse.this, "Failed to create course. Check network connection or server status.", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                if (currentToken != null) {
                    headers.put("Authorization", "Bearer " + currentToken);
                    Log.d(TAG, "Authorization Header set with token: Bearer " + (currentToken.length() > 10 ? currentToken.substring(0,10) : currentToken) + "...");
                } else {
                    Log.w(TAG, "Token is null, Authorization header NOT SET.");
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }
}
