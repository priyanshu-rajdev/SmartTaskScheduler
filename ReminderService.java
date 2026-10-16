package com.smarttaskscheduler;

import javax.swing.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ReminderService implements AutoCloseable {
    private final TaskManager manager;
    private final Set<Task> notified = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Timer timer = new Timer("task-reminder", true);
    public ReminderService(TaskManager manager) { this.manager = manager; }
    public void start() { timer.scheduleAtFixedRate(new TimerTask() { @Override public void run() { checkDeadlines(); } }, 1000, 30000); }
    private void checkDeadlines() {
        LocalDateTime now = LocalDateTime.now();
        for (Task task : manager.getAll()) {
            if (!task.isCompleted() && !notified.contains(task) && !task.getDeadline().isAfter(now)) {
                notified.add(task);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "This task is due or overdue:\n\n" + task.getTitle() + "\nDeadline: " + task.getDeadline(),
                    "Task Reminder", JOptionPane.WARNING_MESSAGE));
            }
        }
    }
    @Override public void close() { timer.cancel(); }
}
