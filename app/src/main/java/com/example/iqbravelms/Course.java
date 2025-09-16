package com.example.iqbravelms;

public class Course {
    private int id;
    private String title;
    private String description;
    private int instructor_id;
    private boolean active;

    public Course(int id, String title, String description, int instructor_id, boolean active) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.instructor_id = instructor_id;
        this.active = active;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getInstructor_id() { return instructor_id; }
    public boolean isActive() { return active; }
}
