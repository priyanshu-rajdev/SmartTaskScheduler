package com.smarttaskscheduler;

import java.time.LocalDateTime;

public class Task {
    private String title;
    private Priority priority;
    private LocalDateTime deadline;
    private boolean completed;

    public Task(String title, Priority priority, LocalDateTime deadline) {
        this(title, priority, deadline, false);
    }
    public Task(String title, Priority priority, LocalDateTime deadline, boolean completed) {
        this.title = title; this.priority = priority; this.deadline = deadline; this.completed = completed;
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
