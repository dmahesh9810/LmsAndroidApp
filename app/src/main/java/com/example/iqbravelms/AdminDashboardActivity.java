package com.example.iqbravelms;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView; // TextView එකක් දානවා නම්

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // උදාහරණයක්: Dashboard එකේ නම පෙන්වන්න TextView එකක්
        TextView dashboardTitle = findViewById(R.id.textViewAdminDashboardTitle);
        if (dashboardTitle != null) {
            dashboardTitle.setText("Admin Dashboard");
        }

        // මෙතන තමයි Admin Dashboard එකට අදාළ අනිත් දේවල් (buttons, lists etc.)
        // initialize කරලා, ඒවායේ ක්‍රියාකාරීත්වය ලියන්න ඕන.
    }
}
