package com.smarttaskscheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** Dependency-free JSON persistence for this application's simple task schema. */
public class JsonStorage {
    private final Path path;
    public JsonStorage(Path path) { this.path = path; }

    public void save(List<Task> tasks) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            json.append("  {\"title\":\"").append(escape(t.getTitle())).append("\",")
                .append("\"priority\":\"").append(t.getPriority().name()).append("\",")
                .append("\"deadline\":\"").append(t.getDeadline()).append("\",")
                .append("\"completed\":").append(t.isCompleted()).append("}")
                .append(i + 1 < tasks.size() ? ",\n" : "\n");
        }
        json.append("]\n");
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    public List<Task> load() throws IOException {
        if (!Files.exists(path)) return new ArrayList<>();
        String s = Files.readString(path, StandardCharsets.UTF_8);
        List<Task> result = new ArrayList<>();
        for (String obj : objects(s)) {
            String title = stringField(obj, "title");
            Priority priority = Priority.valueOf(stringField(obj, "priority"));
            LocalDateTime deadline = LocalDateTime.parse(stringField(obj, "deadline"));
            boolean completed = Boolean.parseBoolean(rawField(obj, "completed"));
            result.add(new Task(title, priority, deadline, completed));
        }
        return result;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
    private static String unescape(String s) {
        StringBuilder b = new StringBuilder(); boolean esc = false;
        for (int i=0; i<s.length(); i++) { char c=s.charAt(i);
            if (esc) { switch(c) { case 'n' -> b.append('\n'); case 'r' -> b.append('\r'); case 't' -> b.append('\t'); default -> b.append(c); } esc=false; }
            else if(c=='\\') esc=true; else b.append(c);
        }
        if (esc) b.append('\\'); return b.toString();
    }
    private static String rawField(String obj, String key) {
        String marker = "\"" + key + "\":"; int p = obj.indexOf(marker);
        if (p < 0) throw new IllegalArgumentException("Missing field " + key);
        p += marker.length(); while (p < obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        int end=p; while(end<obj.length() && obj.charAt(end)!=',' && obj.charAt(end)!='}') end++;
        return obj.substring(p,end).trim();
    }
    private static String stringField(String obj, String key) {
        String marker = "\"" + key + "\":"; int p=obj.indexOf(marker);
        if(p<0) throw new IllegalArgumentException("Missing field " + key);
        p += marker.length(); while(p<obj.length() && Character.isWhitespace(obj.charAt(p))) p++;
        if(p>=obj.length() || obj.charAt(p)!='\"') throw new IllegalArgumentException("Expected string: " + key);
        p++; StringBuilder b=new StringBuilder(); boolean esc=false;
        for(;p<obj.length();p++){char c=obj.charAt(p);if(esc){b.append('\\').append(c);esc=false;}else if(c=='\\')esc=true;else if(c=='\"')return unescape(b.toString());else b.append(c);}
        throw new IllegalArgumentException("Unterminated string: " + key);
    }
    private static List<String> objects(String s) {
        List<String> out=new ArrayList<>(); int depth=0,start=-1; boolean in=false,esc=false;
        for(int i=0;i<s.length();i++){char c=s.charAt(i);
            if(in){if(esc)esc=false;else if(c=='\\')esc=true;else if(c=='\"')in=false;continue;}
            if(c=='\"'){in=true;continue;} if(c=='{'){if(depth++==0)start=i;} else if(c=='}' && --depth==0 && start>=0){out.add(s.substring(start,i+1));start=-1;}
        } return out;
    }
}
