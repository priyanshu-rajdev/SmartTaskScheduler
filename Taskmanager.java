package com.smarttaskscheduler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class TaskManager {
    private final List<Task> tasks = new ArrayList<>();
    private final Comparator<Task> ordering = Comparator
            .comparingInt((Task t) -> t.getPriority().getRank())
            .thenComparing(Task::getDeadline);
    private final PriorityQueue<Task> queue = new PriorityQueue<>(ordering);

    public synchronized void add(Task task) { tasks.add(task); rebuildQueue(); }
    public synchronized void remove(Task task) { tasks.remove(task); rebuildQueue(); }
    public synchronized List<Task> getAll() { return new ArrayList<>(tasks); }
    public synchronized void rebuildQueue() { queue.clear(); queue.addAll(tasks); }
    public synchronized List<Task> getPriorityOrdered() {
        PriorityQueue<Task> copy = new PriorityQueue<>(queue);
        List<Task> result = new ArrayList<>(); while (!copy.isEmpty()) result.add(copy.poll()); return result;
    }
    public synchronized List<Task> filter(String filter, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(); LocalDateTime now = LocalDateTime.now();
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (!t.getTitle().toLowerCase(Locale.ROOT).contains(q)) continue;
            boolean match = switch (filter) {
                case "Today" -> t.getDeadline().toLocalDate().equals(today);
                case "High Priority" -> t.getPriority() == Priority.CRITICAL || t.getPriority() == Priority.HIGH;
                case "Overdue" -> !t.isCompleted() && t.getDeadline().isBefore(now);
                case "Completed" -> t.isCompleted();
                case "Pending" -> !t.isCompleted();
                default -> true;
            };
            if (match) result.add(t);
        }
        result.sort(ordering); return result;
    }
    public synchronized int totalCount() { return tasks.size(); }
    public synchronized long completedCount() { return tasks.stream().filter(Task::isCompleted).count(); }
    public synchronized long pendingCount() { return tasks.stream().filter(t -> !t.isCompleted()).count(); }
}
