package com.luoboduner.moo.tool.ui.component;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.EventObject;

public class TextAreaCellRendererEditor extends AbstractCellEditor implements TableCellRenderer, TableCellEditor {
    private JTextArea textArea;
    private JScrollPane scrollPane;

    public TextAreaCellRendererEditor() {
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane = new JScrollPane(textArea);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        if (isSelected) {
            textArea.setBackground(table.getSelectionBackground());
            textArea.setForeground(table.getSelectionForeground());
        } else {
            textArea.setBackground(table.getBackground());
            textArea.setForeground(table.getForeground());
        }
        return scrollPane;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        textArea.setText(value != null ? value.toString() : "");
        return scrollPane;
    }

    @Override
    public Object getCellEditorValue() {
        return textArea.getText();
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    @Override
    public boolean shouldSelectCell(EventObject e) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JTable with JTextArea");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            Object[][] data = {
                    {"Row1-Column1", "Row1-Column2"},
                    {"Row2-Column1", "Row2-Column2"}
            };
            String[] columnNames = {"Column 1", "Column 2"};

            JTable table = new JTable(data, columnNames);
            TableColumn column = table.getColumnModel().getColumn(1);
            TextAreaCellRendererEditor textAreaRendererEditor = new TextAreaCellRendererEditor();
            column.setCellRenderer(textAreaRendererEditor);
            column.setCellEditor(textAreaRendererEditor);

            frame.add(new JScrollPane(table));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}