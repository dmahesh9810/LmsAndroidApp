package com.example.iqbravelms;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstructorDashboardActivity extends AppCompatActivity implements CourseAdapter.OnCourseActionListener {

    private Button btnCreateCourse;
    private RecyclerView recyclerCourses;
    private CourseAdapter courseAdapter;
    private List<Course> courseList;
    private TabLayout tabLayout;
    private RequestQueue requestQueue;

    private static final String COURSES_API_URL = "http://localhost:8080/api/courses"; 
    private static final String TAG = "InstructorDashboard";
    private String authToken; 

    // ActivityResultLauncher for EditCourseActivity
    private ActivityResultLauncher<Intent> editCourseLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructor_dashboard);

        btnCreateCourse = findViewById(R.id.btnCreateCourse);
        recyclerCourses = findViewById(R.id.recyclerCourses);
        tabLayout = findViewById(R.id.tabLayout);

        courseList = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);

        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        authToken = prefs.getString(LoginActivity.AUTH_TOKEN_KEY, null);

        if (authToken == null) {
            Toast.makeText(this, "Authentication token not found. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return; 
        }

        courseAdapter = new CourseAdapter(courseList, this, this);
        recyclerCourses.setLayoutManager(new LinearLayoutManager(this));
        recyclerCourses.setAdapter(courseAdapter);

        tabLayout.addTab(tabLayout.newTab().setText("All Courses"));
        tabLayout.addTab(tabLayout.newTab().setText("My Courses"));

        // Initialize ActivityResultLauncher
        editCourseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Course was updated, reload the list from server
                        Log.d(TAG, "EditCourseActivity finished with RESULT_OK, reloading courses.");
                        loadCoursesFromServer();
                    } else {
                        Log.d(TAG, "EditCourseActivity finished, but no changes indicated or cancelled.");
                    }
                }
        );

        loadCoursesFromServer();

        btnCreateCourse.setOnClickListener(v -> {
            Intent intent = new Intent(InstructorDashboardActivity.this, CreateCourse.class); 
            startActivity(intent);
        });
    }

    private void loadCoursesFromServer() {
        if (authToken == null) { 
            Toast.makeText(this, "Auth token missing in loadCoursesFromServer. Please login.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }
        Log.d(TAG, "Attempting to load courses with token.");

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                COURSES_API_URL,
                null, 
                response -> {
                    Log.d(TAG, "Courses Response: " + response.toString().substring(0, Math.min(response.toString().length(), 500)) + "...");
                    courseList.clear(); 
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject courseObject = response.getJSONObject(i);
                            int id = courseObject.optInt("id", -1);
                            String title = courseObject.optString("title", "N/A");
                            String description = courseObject.optString("description", "No description");
                            JSONObject instructorObject = courseObject.optJSONObject("instructor");
                            int instructorId = 0; 
                            if (instructorObject != null) {
                                instructorId = instructorObject.optInt("id", 0);
                            }
                            boolean isActive = courseObject.optBoolean("active", false); 
                            if (id != -1) {
                                courseList.add(new Course(id, title, description, instructorId, isActive));
                            } else {
                                Log.w(TAG, "Course found with invalid ID, skipping: " + title);
                            }
                        }
                        courseAdapter.notifyDataSetChanged(); 
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Parsing error: " + e.getMessage(), e);
                        Toast.makeText(InstructorDashboardActivity.this, "Error parsing course data.", Toast.LENGTH_SHORT).show();
                    }
                },
                this::handleVolleyError
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                return headers;
            }
        };
        requestQueue.add(jsonArrayRequest);
    }

    @Override
    public void onDeleteClick(int courseId, int position) {
        if (authToken == null) {
            Toast.makeText(this, "Authentication error. Please login again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        String deleteUrl = COURSES_API_URL + "/" + courseId;
        Log.d(TAG, "Attempting to delete course with ID: " + courseId + " at URL: " + deleteUrl);

        StringRequest stringRequest = new StringRequest(Request.Method.DELETE, deleteUrl,
                response -> {
                    Log.d(TAG, "Delete Response: " + response);
                    Toast.makeText(InstructorDashboardActivity.this, "Course deleted successfully!", Toast.LENGTH_SHORT).show();
                    if (position >= 0 && position < courseList.size()) {
                        courseList.remove(position);
                        courseAdapter.notifyItemRemoved(position);
                        courseAdapter.notifyItemRangeChanged(position, courseList.size());
                    } else {
                        loadCoursesFromServer(); 
                    }
                },
                this::handleVolleyError 
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + authToken);
                return headers;
            }
        };
        requestQueue.add(stringRequest);
    }

    @Override
    public void onEditClick(Course course) {
        Log.d(TAG, "Edit clicked for course: " + course.getTitle() + " (ID: " + course.getId() + ")");
        Intent intent = new Intent(this, EditCourseActivity.class);
        // Pass course details to EditCourseActivity
        intent.putExtra("COURSE_ID", course.getId());
        intent.putExtra("COURSE_TITLE", course.getTitle());
        intent.putExtra("COURSE_DESCRIPTION", course.getDescription());
        intent.putExtra("COURSE_INSTRUCTOR_ID", course.getInstructor_id());
        intent.putExtra("COURSE_IS_ACTIVE", course.isActive());
        
        editCourseLauncher.launch(intent); // Use ActivityResultLauncher
    }

    private void handleVolleyError(VolleyError error) {
        Log.e(TAG, "Volley Error: " + error.toString());
        if (error.networkResponse != null) {
            Log.e(TAG, "Error Response Code: " + error.networkResponse.statusCode);
            if (error.networkResponse.statusCode == 401 || error.networkResponse.statusCode == 403) {
                Toast.makeText(InstructorDashboardActivity.this, "Unauthorized/Forbidden. Please login again.", Toast.LENGTH_LONG).show();
                redirectToLogin();
            } else {
                 try {
                    String responseBody = new String(error.networkResponse.data, "utf-8");
                    Log.e(TAG, "Error Response Body: " + responseBody);
                    JSONObject data = new JSONObject(responseBody);
                    String message = data.optString("message", "Failed to process request.");
                     if (error.networkResponse.statusCode == 400 && message.contains("Title must be between")){
                         message = "Title must be between 3 and 100 characters.";
                     }
                    Toast.makeText(InstructorDashboardActivity.this, "Error: " + message + " (Status: " + error.networkResponse.statusCode + ")", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Volley error response for status code "+ error.networkResponse.statusCode + ": " + e.getMessage());
                    Toast.makeText(InstructorDashboardActivity.this, "Server error (Status: " + error.networkResponse.statusCode + "). See logs.", Toast.LENGTH_LONG).show();
                }            }
        } else {
            Toast.makeText(InstructorDashboardActivity.this, "Network error or server unavailable. Check connection.", Toast.LENGTH_LONG).show();
        }
    }

    private void redirectToLogin(){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
