package com.example.iqbravelms;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.*;
import com.android.volley.toolbox.*;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    EditText editName, editEmail, editPassword;
    Button btnRegister;
    ProgressBar progressBar;
    TextView textLoginLink; // Declare the TextView for the login link

    private static final String REGISTER_URL = "http://localhost:8080/api/users/register";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        textLoginLink = findViewById(R.id.textLogin); // Initialize the TextView

        btnRegister.setOnClickListener(v -> registerUser());

        // Set OnClickListener for the textLoginLink TextView
        textLoginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            // Optional: Add flags to clear the back stack if needed, 
            // so pressing back from LoginActivity doesn't come back to RegisterActivity
            // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish RegisterActivity when navigating to Login
        });
    }

    private void registerUser() {
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false); // Disable button during API call

        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        // Basic client-side validation
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            return;
        }
        // Add more specific validation if needed (e.g., email format, password strength)

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("name", name);
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            jsonBody.put("role", "STUDENT"); // default set

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, REGISTER_URL, jsonBody,
                    response -> {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();
                        // Navigate to LoginActivity after successful registration
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); 
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                        String errorMessage = "Registration failed. ";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String responseBody = new String(error.networkResponse.data, "utf-8");
                                JSONObject data = new JSONObject(responseBody);
                                errorMessage += data.optString("message", error.getMessage());
                            } catch (Exception e) {
                                errorMessage += error.getMessage();
                            }
                        } else {
                            errorMessage += error.getMessage();
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    });

            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnRegister.setEnabled(true);
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
