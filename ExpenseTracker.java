import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;

public class ExpenseTracker {
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/expense_tracker";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "root*1234";

    private JFrame frame;
    private JTextField descriptionField, amountField;
    private JTable expenseTable;
    private DefaultTableModel tableModel;

    // Variable to track the selected row for update
    private int selectedRowId = -1;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpenseTracker::new);
    }

    public ExpenseTracker() {
        frame = new JFrame("Expense Tracker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLayout(new BorderLayout());

        // UI Components
        JPanel inputPanel = new JPanel(new GridLayout(2, 5, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add/Update Expense"));
        descriptionField = new JTextField();
        amountField = new JTextField();
        JButton addButton = new JButton("Add Expense");
        JButton updateButton = new JButton("Update Expense");

        inputPanel.add(new JLabel("Description:"));
        inputPanel.add(descriptionField);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);
        inputPanel.add(addButton);
        inputPanel.add(new JLabel()); // Empty space
        inputPanel.add(new JLabel()); // Empty space
        inputPanel.add(updateButton);

        // Table for displaying expenses
        tableModel = new DefaultTableModel(new String[]{"ID", "Description", "Amount", "Date"}, 0);
        expenseTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(expenseTable);

        JButton deleteButton = new JButton("Delete Selected");
        JButton totalButton = new JButton("Show Total");
        JLabel totalLabel = new JLabel("Total: $0");

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(deleteButton);
        bottomPanel.add(totalButton);
        bottomPanel.add(totalLabel);

        frame.add(inputPanel, BorderLayout.NORTH);
        frame.add(tableScrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Event Listeners
        addButton.addActionListener(e -> addExpense());
        updateButton.addActionListener(e -> updateExpense());
        deleteButton.addActionListener(e -> deleteExpense());
        totalButton.addActionListener(e -> showTotal(totalLabel));

        // Add table row selection listener
        expenseTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFieldsForUpdate();
            }
        });

        // Load existing expenses
        loadExpenses();

        frame.setVisible(true);
    }

    private void addExpense() {
        String description = descriptionField.getText();
        String amountText = amountField.getText();

        if (description.isEmpty() || amountText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill out all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

            // Insert into database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
                String query = "INSERT INTO expenses (description, amount, date) VALUES (?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, description);
                stmt.setDouble(2, amount);
                stmt.setString(3, currentDate);
                stmt.executeUpdate();
            }

            // Refresh table
            loadExpenses();
            descriptionField.setText("");
            amountField.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Amount must be a valid number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error adding expense: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadExpenses() {
        tableModel.setRowCount(0); // Clear existing rows

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String query = "SELECT * FROM expenses";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("description"),
                        rs.getDouble("amount"),
                        rs.getDate("date")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error loading expenses: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteExpense() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String query = "DELETE FROM expenses WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, id);
            stmt.executeUpdate();

            // Refresh table
            loadExpenses();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error deleting expense: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateExpense() {
        if (selectedRowId == -1) {
            JOptionPane.showMessageDialog(frame, "Please select an expense to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String description = descriptionField.getText();
        String amountText = amountField.getText();

        if (description.isEmpty() || amountText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please fill out all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);

            // Update in database
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
                String query = "UPDATE expenses SET description = ?, amount = ? WHERE id = ?";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, description);
                stmt.setDouble(2, amount);
                stmt.setInt(3, selectedRowId);
                stmt.executeUpdate();
            }

            // Refresh table
            loadExpenses();
            descriptionField.setText("");
            amountField.setText("");
            selectedRowId = -1; // Reset selection
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Amount must be a valid number", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error updating expense: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTotal(JLabel totalLabel) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String query = "SELECT SUM(amount) AS total FROM expenses";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                double total = rs.getDouble("total");
                totalLabel.setText("Total: $" + total);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(frame, "Error calculating total: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void populateFieldsForUpdate() {
        int selectedRow = expenseTable.getSelectedRow();
        if (selectedRow != -1) {
            selectedRowId = (int) tableModel.getValueAt(selectedRow, 0);
            descriptionField.setText((String) tableModel.getValueAt(selectedRow, 1));
            amountField.setText(String.valueOf(tableModel.getValueAt(selectedRow, 2)));
        }
    }
}