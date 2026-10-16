package com.smarttaskscheduler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main extends JFrame {
    private final TaskManager manager = new TaskManager();
    private final JsonStorage storage = new JsonStorage(Path.of("data", "tasks.json"));
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Task", "Priority", "Deadline", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Today", "High Priority", "Overdue", "Completed", "Pending"});
    private final JTextField search = new JTextField(18);
    private final JLabel stats = new JLabel();
    private final DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private final ReminderService reminders = new ReminderService(manager);

    public Main() {
        super("Smart Task Scheduler"); setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(940, 600); setMinimumSize(new Dimension(760, 480)); setLocationRelativeTo(null);
        buildUI(); loadTasks(); reminders.start(); refreshTable();
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { saveTasks(); reminders.close(); dispose(); } });
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10)); root.setBorder(new EmptyBorder(16, 16, 12, 16)); setContentPane(root);
        JLabel heading = new JLabel("SMART TASK SCHEDULER"); heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subtitle = new JLabel("Organize tasks by priority and deadline"); subtitle.setForeground(Color.GRAY);
        JPanel brand = new JPanel(new GridLayout(2, 1)); brand.add(heading); brand.add(subtitle);
        JButton add = new JButton("+ Add Task"), edit = new JButton("Edit"), delete = new JButton("Delete"), complete = new JButton("Toggle Complete");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.add(add); buttons.add(edit); buttons.add(delete); buttons.add(complete);
        JPanel top = new JPanel(new BorderLayout()); top.add(brand, BorderLayout.WEST); top.add(buttons, BorderLayout.EAST); root.add(top, BorderLayout.NORTH);
        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); tools.add(new JLabel("Filter:")); tools.add(filter); tools.add(new JLabel("Search:")); tools.add(search);
        tablePanel.add(tools, BorderLayout.NORTH); table.setRowHeight(29); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER); root.add(tablePanel, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout()); bottom.add(stats, BorderLayout.WEST); bottom.add(new JLabel("Order: priority, then earliest deadline"), BorderLayout.EAST); root.add(bottom, BorderLayout.SOUTH);
        add.addActionListener(e -> showTaskDialog(null)); edit.addActionListener(e -> { Task t=selectedTask(); if(t!=null) showTaskDialog(t); });
        delete.addActionListener(e -> { Task t=selectedTask(); if(t!=null && JOptionPane.showConfirmDialog(this,"Delete '"+t.getTitle()+"'?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){manager.remove(t);saveTasks();refreshTable();} });
        complete.addActionListener(e -> { Task t=selectedTask(); if(t!=null){t.setCompleted(!t.isCompleted());manager.rebuildQueue();saveTasks();refreshTable();} });
        filter.addActionListener(e -> refreshTable());
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){refreshTable();}public void removeUpdate(DocumentEvent e){refreshTable();}public void changedUpdate(DocumentEvent e){refreshTable();}});
    }

    private Task selectedTask() {
        int row=table.getSelectedRow(); if(row<0){JOptionPane.showMessageDialog(this,"Select a task first.");return null;}
        String title=(String)model.getValueAt(table.convertRowIndexToModel(row),0);
        return manager.filter((String)filter.getSelectedItem(), search.getText()).stream().filter(t->t.getTitle().equals(title)).findFirst().orElse(null);
    }

    private void showTaskDialog(Task task) {
        JTextField title = new JTextField(task==null?"":task.getTitle(), 22);
        JComboBox<Priority> priority = new JComboBox<>(Priority.values());
        JTextField deadline = new JTextField(task==null?LocalDateTime.now().plusHours(1).withSecond(0).withNano(0).format(inputFormat):task.getDeadline().format(inputFormat), 16);
        if(task!=null) priority.setSelectedItem(task.getPriority());
        JPanel form=new JPanel(new GridLayout(0,2,8,8)); form.add(new JLabel("Task title:"));form.add(title);form.add(new JLabel("Priority:"));form.add(priority);form.add(new JLabel("Deadline (yyyy-MM-dd HH:mm):"));form.add(deadline);
        while(true){int result=JOptionPane.showConfirmDialog(this,form,task==null?"Add Task":"Edit Task",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(result!=JOptionPane.OK_OPTION)return;
            try{String name=title.getText().trim();if(name.isEmpty())throw new IllegalArgumentException("Task title is required.");LocalDateTime due=LocalDateTime.parse(deadline.getText().trim(),inputFormat);
                if(task==null)manager.add(new Task(name,(Priority)priority.getSelectedItem(),due));else{task.setTitle(name);task.setPriority((Priority)priority.getSelectedItem());task.setDeadline(due);manager.rebuildQueue();}
                saveTasks();refreshTable();return;
            }catch(Exception ex){JOptionPane.showMessageDialog(this,"Please check the title and deadline.\nDeadline format: yyyy-MM-dd HH:mm","Invalid input",JOptionPane.ERROR_MESSAGE);return;}}
    }

    private void refreshTable() {
        model.setRowCount(0); for(Task t:manager.filter((String)filter.getSelectedItem(),search.getText())) model.addRow(new Object[]{t.getTitle(),t.getPriority(),t.getDeadline().format(displayFormat),t.isCompleted()?"Completed":"Pending"});
        stats.setText("Pending: "+manager.pendingCount()+"    Completed: "+manager.completedCount()+"    Total: "+manager.totalCount());
    }
    private void loadTasks(){try{for(Task t:storage.load())manager.add(t);}catch(Exception ex){JOptionPane.showMessageDialog(this,"Could not load tasks: "+ex.getMessage());}}
    private void saveTasks(){try{storage.save(manager.getAll());}catch(Exception ex){JOptionPane.showMessageDialog(this,"Could not save tasks: "+ex.getMessage());}}
    public static void main(String[] args){SwingUtilities.invokeLater(()->new Main().setVisible(true));}
}
