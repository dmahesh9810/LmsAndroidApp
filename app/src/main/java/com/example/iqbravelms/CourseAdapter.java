package com.example.iqbravelms;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    private List<Course> courseList;
    private Context context;
    private OnCourseActionListener listener;

    // Interface for click events
    public interface OnCourseActionListener {
        void onDeleteClick(int courseId, int position);
        void onEditClick(Course course); // Added for edit functionality
    }

    public CourseAdapter(List<Course> courseList, Context context, OnCourseActionListener listener) {
        this.courseList = courseList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.titleTextView.setText(course.getTitle());
        holder.descriptionTextView.setText(course.getDescription());

        // Edit Button Click Listener
        holder.buttonEditCourse.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(course);
            }
        });

        // Delete Button Click Listener
        holder.buttonDeleteCourse.setOnClickListener(v -> {
            if (listener != null) {
                new AlertDialog.Builder(context)
                        .setTitle("Delete Course")
                        .setMessage("Are you sure you want to delete '" + course.getTitle() + "'?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            listener.onDeleteClick(course.getId(), holder.getAdapterPosition()); // Use getAdapterPosition()
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView descriptionTextView;
        Button buttonEditCourse;   // Added Edit button
        Button buttonDeleteCourse;

        CourseViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.textViewCourseTitle); 
            descriptionTextView = view.findViewById(R.id.textViewCourseDescription);
            buttonEditCourse = view.findViewById(R.id.buttonEditCourse);     // Initialize Edit button
            buttonDeleteCourse = view.findViewById(R.id.buttonDeleteCourse);
        }
    }
}
