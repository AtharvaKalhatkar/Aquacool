package com.aqua.controller;

import com.aqua.model.Bill;
import com.aqua.model.Customer;
import com.aqua.service.BillService;
import com.aqua.service.CustomerService;
import com.aqua.util.EmailService;
import com.aqua.util.AlertUtil;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class BlastView extends VBox {
    private BillService billService = new BillService();
    private CustomerService customerService = new CustomerService();
    private EmailService emailService = new EmailService();

    public static class BlastItem {
        private final Bill bill;
        private final SimpleBooleanProperty selected;

        public BlastItem(Bill bill) {
            this.bill = bill;
            this.selected = new SimpleBooleanProperty(false);
        }

        public Bill getBill() {
            return bill;
        }

        public SimpleBooleanProperty selectedProperty() {
            return selected;
        }

        public boolean isSelected() {
            return selected.get();
        }

        public void setSelected(boolean val) {
            selected.set(val);
        }
    }

    private ComboBox<String> monthCombo;
    private ComboBox<Integer> yearCombo;
    private TextField searchField;
    private CheckBox selectAllCheck;
    private TableView<BlastItem> table;
    private ObservableList<BlastItem> allItems = FXCollections.observableArrayList();
    private FilteredList<BlastItem> filteredItems;
    private Label countLabel;
    private Button sendBtn;

    public BlastView() {
        setPadding(new Insets(30));
        setSpacing(20);
        getStyleClass().add("content-area");
        buildHeader();
        buildControls();
        buildTable();
        loadData();
    }

    private void buildHeader() {
        Label title = new Label("WhatsApp Broadcaster");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Select multiple customers and instantly broadcast their bills & PDFs via WhatsApp Desktop.");
        subtitle.getStyleClass().add("page-subtitle");
        getChildren().add(new VBox(5, title, subtitle));
    }

    private void buildControls() {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("form-section");
        row.setPadding(new Insets(15, 20, 15, 20));

        monthCombo = new ComboBox<>();
        for (Month m : Month.values())
            monthCombo.getItems().add(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        monthCombo.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        monthCombo.setPrefWidth(140);
        monthCombo.setOnAction(e -> loadData());

        yearCombo = new ComboBox<>();
        int cy = LocalDate.now().getYear();
        for (int y = cy - 5; y <= cy + 1; y++)
            yearCombo.getItems().add(y);
        yearCombo.getSelectionModel().select(Integer.valueOf(cy));
        yearCombo.setPrefWidth(90);
        yearCombo.setOnAction(e -> loadData());

        searchField = new TextField();
        searchField.setPromptText("🔍 Search customer...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((obs, o, n) -> {
            if (filteredItems != null) {
                filteredItems.setPredicate(item -> {
                    if (n == null || n.isEmpty()) return true;
                    return item.getBill().getCustomerName().toLowerCase().contains(n.toLowerCase());
                });
                updateCount();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        sendBtn = new Button("🚀 Send Auto via WhatsApp");
        sendBtn.getStyleClass().add("btn-primary");
        sendBtn.setStyle("-fx-background-color: #25D366; -fx-font-weight: bold; -fx-font-size: 14px;");
        sendBtn.setOnAction(e -> initiateBlast());

        row.getChildren().addAll(
                new Label("Month:") {{ getStyleClass().add("form-label"); }}, monthCombo,
                new Label("Year:") {{ getStyleClass().add("form-label"); }}, yearCombo,
                searchField, spacer, sendBtn
        );
        getChildren().add(row);
    }

    private void buildTable() {
        VBox tableBox = new VBox(10);
        tableBox.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);");
        VBox.setVgrow(tableBox, Priority.ALWAYS);

        HBox toolsRow = new HBox(15);
        toolsRow.setAlignment(Pos.CENTER_LEFT);
        
        selectAllCheck = new CheckBox("Select All Visible");
        selectAllCheck.setStyle("-fx-font-weight: bold; -fx-text-fill: #0069b4;");
        selectAllCheck.setOnAction(e -> {
            boolean val = selectAllCheck.isSelected();
            for (BlastItem item : table.getItems()) {
                item.setSelected(val);
            }
        });

        countLabel = new Label("0 customers selected");
        countLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        
        toolsRow.getChildren().addAll(selectAllCheck, countLabel);

        table = new TableView<>();
        table.getStyleClass().add("data-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(45);
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<BlastItem, Boolean> checkCol = new TableColumn<>("Send");
        checkCol.setCellValueFactory(data -> data.getValue().selectedProperty());
        checkCol.setCellFactory(CheckBoxTableCell.forTableColumn(checkCol));
        checkCol.setPrefWidth(60);
        checkCol.setMaxWidth(60);
        checkCol.setMinWidth(60);

        TableColumn<BlastItem, String> custCol = new TableColumn<>("Customer");
        custCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBill().getCustomerName()));

        TableColumn<BlastItem, String> amountCol = new TableColumn<>("Total Amount (₹)");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(String.format("₹ %.0f", data.getValue().getBill().getGrandTotal())));

        TableColumn<BlastItem, String> statusCol = new TableColumn<>("Payment Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBill().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("PAID".equals(item) ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;" : "-fx-text-fill:#e67e22;-fx-font-weight:bold;");
                }
            }
        });

        table.getColumns().addAll(checkCol, custCol, amountCol, statusCol);
        table.setEditable(true); // Required for CheckBoxTableCell to work

        tableBox.getChildren().addAll(toolsRow, table);
        getChildren().add(tableBox);
    }

    private void loadData() {
        int m = monthCombo.getSelectionModel().getSelectedIndex() + 1;
        int y = yearCombo.getSelectionModel().getSelectedItem();
        
        List<Bill> bills = billService.getBillsByMonth(m, y);
        allItems.clear();
        for (Bill b : bills) {
            BlastItem item = new BlastItem(b);
            item.selectedProperty().addListener((obs, o, n) -> updateCount());
            allItems.add(item);
        }
        
        filteredItems = new FilteredList<>(allItems, p -> true);
        table.setItems(filteredItems);
        
        // Retrigger search filter if text exists
        String search = searchField.getText();
        if (search != null && !search.isEmpty()) {
            searchField.setText("");
            searchField.setText(search);
        } else {
            updateCount();
        }
        selectAllCheck.setSelected(false);
    }

    public void refreshData() {
        loadData();
    }

    private void updateCount() {
        long count = table.getItems().stream().filter(BlastItem::isSelected).count();
        countLabel.setText(count + " customers selected for broadcast");
        sendBtn.setText("🚀 Send to " + count + " Customers");
        sendBtn.setDisable(count == 0);
    }

    private void initiateBlast() {
        List<Bill> toSend = table.getItems().stream()
                .filter(BlastItem::isSelected)
                .map(BlastItem::getBill)
                .toList();

        if (toSend.isEmpty()) {
            AlertUtil.showWarning("No Selection", "Please select at least one customer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm WhatsApp Blast");
        confirm.setHeaderText("Ready to send " + toSend.size() + " WhatsApp messages?");
        confirm.setContentText("WARNING: This will open your WhatsApp Desktop App and take over your keyboard. Please DO NOT touch your mouse or keyboard until the process finishes!");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                executeBlast(toSend);
            }
        });
    }

    private void executeBlast(List<Bill> toSend) {
        Alert progress = new Alert(Alert.AlertType.INFORMATION);
        progress.setTitle("Auto WhatsApp Blast");
        progress.setHeaderText("Broadcasting...");
        progress.setContentText("DO NOT TOUCH MOUSE OR KEYBOARD!");
        progress.show();

        new Thread(() -> {
            try {
                java.awt.Robot robot = new java.awt.Robot();
                int count = 0;
                
                String upiId = emailService.getUpiId();
                if (upiId == null || upiId.isEmpty()) upiId = "kalhatkaratharva01@okhdfcbank";
                String senderName = emailService.getSenderName() != null ? emailService.getSenderName() : "Bhairavnath Cool Aqua";

                for (Bill bill : toSend) {
                    Customer cust = customerService.getCustomerById(bill.getCustomerId());
                    String mobile = (cust != null && cust.getMobile() != null) ? cust.getMobile().replaceAll("[^0-9]", "") : "";
                    
                    if (mobile.length() >= 10) {
                        if (mobile.length() == 10) mobile = "91" + mobile;
                        
                        String upiUri = String.format("upi://pay?pa=%s&pn=%s&am=%.0f&cu=INR",
                            upiId.replace(" ", ""), senderName.replace(" ", "%20"), bill.getGrandTotal());

                        String message = String.format("Dear %s,\n\nYour water bill for %s %d is ready.\n*Total Amount: Rs. %.0f*\n\nClick below to pay instantly via GPay/PhonePe/Paytm:\n%s\n\nThank you!\n- Bhairavnath Cool Aqua",
                            bill.getCustomerName(), bill.getMonthName(), bill.getBillYear(), bill.getGrandTotal(), upiUri);

                        // Locate PDF (generated by Bulk Billing)
                        String docsFolder = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "AquaBills" + File.separator + bill.getBillYear() + File.separator + bill.getMonthName();
                        String safeName = bill.getCustomerName().replaceAll("[^a-zA-Z0-9]", "_");
                        File dir = new File(docsFolder);
                        File pdfFile = null;
                        if (dir.exists()) {
                            File[] files = dir.listFiles((d, name) -> name.startsWith(safeName + "_") && name.endsWith(".pdf"));
                            if (files != null && files.length > 0) pdfFile = files[0];
                        }

                        // Open WhatsApp Desktop App natively
                        String waLink = "whatsapp://send?phone=" + mobile + "&text=" + java.net.URLEncoder.encode(message, "UTF-8").replace("+", "%20");
                        Runtime.getRuntime().exec("cmd /c start \"\" \"" + waLink + "\"");
                        
                        Thread.sleep(6000); // Wait for chat load
                        
                        if (pdfFile != null) {
                            // Copy PDF to clipboard using Native Java AWT API (100% Reliable for files)
                            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
                            final File finalPdf = pdfFile;
                            java.awt.datatransfer.Transferable transferable = new java.awt.datatransfer.Transferable() {
                                public java.awt.datatransfer.DataFlavor[] getTransferDataFlavors() {
                                    return new java.awt.datatransfer.DataFlavor[]{java.awt.datatransfer.DataFlavor.javaFileListFlavor};
                                }
                                public boolean isDataFlavorSupported(java.awt.datatransfer.DataFlavor flavor) {
                                    return java.awt.datatransfer.DataFlavor.javaFileListFlavor.equals(flavor);
                                }
                                public Object getTransferData(java.awt.datatransfer.DataFlavor flavor) throws java.awt.datatransfer.UnsupportedFlavorException {
                                    if (java.awt.datatransfer.DataFlavor.javaFileListFlavor.equals(flavor)) {
                                        return java.util.Arrays.asList(finalPdf);
                                    }
                                    throw new java.awt.datatransfer.UnsupportedFlavorException(flavor);
                                }
                            };
                            clipboard.setContents(transferable, null);
                            
                            Thread.sleep(2000); // Wait for clipboard to sync
                            
                            robot.keyPress(java.awt.event.KeyEvent.VK_CONTROL);
                            robot.keyPress(java.awt.event.KeyEvent.VK_V);
                            robot.keyRelease(java.awt.event.KeyEvent.VK_V);
                            robot.keyRelease(java.awt.event.KeyEvent.VK_CONTROL);
                            Thread.sleep(4000); // Crucial wait: WhatsApp needs 4 seconds to load the PDF attachment preview
                        }

                        robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
                        robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
                        
                        count++;
                        Thread.sleep(3000);
                    }
                }
                
                final int totalSent = count;
                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    AlertUtil.showSuccess("🎉 Blast Complete!\nSuccessfully sent " + totalSent + " messages.");
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    progress.close();
                    AlertUtil.showError("Blast Error", "Error: " + ex.getMessage());
                });
            }
        }).start();
    }
}
