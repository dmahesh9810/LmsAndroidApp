package com.example.iqbravelms;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EditCourseActivity extends AppCompatActivity {

    private static final String TAG = "EditCourseActivity";
    private static final String COURSES_API_URL_BASE = "http://localhost:8080/api/courses"; // Base URL

    private EditText editTextTitle;
    private EditText editTextDescription;
    private Button buttonUpdateCourse;

    private RequestQueue requestQueue;
    private String authToken;

    private int courseIdToEdit;
    private int originalInstructorId; // To send back in PUT request
    private boolean originalIsActive; // To send back in PUT request

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_course);

        editTextTitle = findViewById(R.id.editTextEditCourseTitle);
        editTextDescription = findViewById(R.id.editTextEditCourseDescription);
        buttonUpdateCourse = findViewById(R.id.buttonUpdateCourse);

        requestQueue = Volley.newRequestQueue(this);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        authToken = prefs.getString(LoginActivity.AUTH_TOKEN_KEY, null);
        Log.d(TAG, "Auth Token in EditCourseActivity on Create: " + authToken);

        if (authToken == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Get data from Intent
        Intent intent = getIntent();
        courseIdToEdit = intent.getIntExtra("COURSE_ID", -1);
        String title = intent.getStringExtra("COURSE_TITLE");
        String description = intent.getStringExtra("COURSE_DESCRIPTION");
        originalInstructorId = intent.getIntExtra("COURSE_INSTRUCTOR_ID", -1); // Default to -1 to spot issues
        Log.d(TAG, "Original Instructor ID from Intent: " + originalInstructorId);
        originalIsActive = intent.getBooleanExtra("COURSE_IS_ACTIVE", true); 

        if (courseIdToEdit == -1) {
            Toast.makeText(this, "Error: Course ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (originalInstructorId == -1){
            Log.w(TAG, "Warning: Instructor ID was not passed correctly or is missing from the source intent (originalInstructorId is -1).");
        }

        // Populate EditTexts
        editTextTitle.setText(title);
        editTextDescription.setText(description);

        buttonUpdateCourse.setOnClickListener(v -> updateCourse());
    }

    private void updateCourse() {
        final String updatedTitle = editTextTitle.getText().toString().trim();
        final String updatedDescription = editTextDescription.getText().toString().trim();

        if (TextUtils.isEmpty(updatedTitle)) {
            editTextTitle.setError("Title cannot be empty");
            editTextTitle.requestFocus();
            return;
        }
        if (updatedTitle.length() < 3) { 
            editTextTitle.setError("Title must be at least 3 characters long");
            editTextTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(updatedDescription)) {
            editTextDescription.setError("Description cannot be empty");
            editTextDescription.requestFocus();
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("title", updatedTitle);
            requestBody.put("description", updatedDescription);
            
            // TEMPORARILY HARDCODE instructor_id to 1 for testing, as per Postman working example
            Log.d(TAG, "Using hardcoded instructor_id: 1 for this PUT request. Original was: " + originalInstructorId);
            requestBody.put("instructor_id", 1); 
            // requestBody.put("instructor_id", (originalInstructorId == -1 ? 1 : originalInstructorId) ); // Original logic commented out
            
            requestBody.put("active", originalIsActive);
            Log.d(TAG, "Update Request Body: " + requestBody.toString());
        } catch (JSONException e) {
            Log.e(TAG, "JSON Exception: " + e.getMessage(), e);
            Toast.makeText(this, "Error creating update request.", Toast.LENGTH_SHORT).show();
            return;
        }

        String updateUrl = COURSES_API_URL_BASE + "/" + courseIdToEdit;
        Log.d(TAG, "Attempting to update course at URL: " + updateUrl);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.PUT,
                updateUrl,
                requestBody,
                response -> {
                    Log.d(TAG, "Update Course Response: " + response.toString());
                    Toast.makeText(EditCourseActivity.this, "Course updated successfully!", Toast.LENGTH_LONG).show();
                    setResult(Activity.RESULT_OK); 
                    finish(); 
                },
                error -> {
                    Log.e(TAG, "Volley Error updating course: " + error.toString());
                    if (error.networkResponse != null) {
                        Log.e(TAG, "Error Response Code: " + error.networkResponse.statusCode);
                        try {
                            String responseBody = new String(error.networkResponse.data, "utf-8");
                            Log.e(TAG, "Error Response Body: " + responseBody);
                            JSONObject data = new JSONObject(responseBody);
                            String message = data.optString("message", "Failed to update course.");
                            if (error.networkResponse.statusCode == 400 && message.contains("Title must be between")){
                                 message = "Title must be between 3 and 100 characters.";
                             }
                            Toast.makeText(EditCourseActivity.this, "Error: " + message + " (Status: " + error.networkResponse.statusCode + ")", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(EditCourseActivity.this, "Failed to update course. Status: " + error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(EditCourseActivity.this, "Failed to update course. Check network connection.", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                Log.d(TAG, "Auth Token for PUT request header: " + authToken);
                if (authToken != null && !authToken.isEmpty()) {
                    headers.put("Authorization", "Bearer " + authToken);
                } else {
                    Log.w(TAG, "AuthToken is null or empty. Authorization header will not be set.");
                }
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(jsonObjectRequest);
    }
}
