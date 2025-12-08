package com.hotel.controller;

import com.hotel.dao.ReservationDAO;
import com.hotel.model.Reservation;
import com.hotel.model.User;
import com.hotel.security.RoleUtils;
import com.hotel.service.ReportService;
import com.hotel.session.Session;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DashboardController {

    @FXML private ImageView profileImage;
    @FXML private Label loggedInUserLabel;
    @FXML private Button switchAccountButton;
    @FXML private Button newReservationButton;

    @FXML private TextField searchGuestField;
    @FXML private TextField searchRoomField;
    @FXML private TextField searchDateField;
    @FXML private Button clearFiltersButton;

    @FXML private TableView<Reservation> reservationTable;

    @FXML private TableColumn<Reservation, Integer> colId;
    @FXML private TableColumn<Reservation, String> colGuest;
    @FXML private TableColumn<Reservation, Integer> colRoom;
    @FXML private TableColumn<Reservation, String> colType;
    @FXML private TableColumn<Reservation, java.sql.Date> colCheckIn;
    @FXML private TableColumn<Reservation, java.sql.Date> colCheckOut;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, Double> colPrice;

    @FXML private Label totalLabel;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private FilteredList<Reservation> filteredData;

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());


    @FXML
    public void initialize() {

        /* VALUE FACTORIES */
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colCheckIn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        colCheckOut.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));

        /* PREMIUM ALIGNMENT */
        alignCenter(colId);
        alignLeft(colGuest);
        alignCenter(colRoom);
        alignCenter(colType);
        alignCenter(colCheckIn);
        alignCenter(colCheckOut);
        alignCenter(colStatus);
        alignRight(colPrice);

        /* LOAD USER DETAILS (PROFILE + ROLE BADGE) */
        User user = Session.getCurrentUser();
        if (user != null) {
            loggedInUserLabel.setText("Logged in as: " + user.getUsername() + " (" + user.getRole() + ")");
            loggedInUserLabel.getStyleClass().add("role-" + user.getRole().toLowerCase());

            try {
                Image img = new Image(getClass().getResourceAsStream(
                        "/images/" + user.getRole().toLowerCase() + ".png"));
                profileImage.setImage(img);
            } catch (Exception ignored) {}
        }

        /* FILTERING */
        filteredData = new FilteredList<>(reservations, r -> true);
        reservationTable.setItems(filteredData);
        totalLabel.textProperty().bind(Bindings.size(filteredData).asString());

        searchGuestField.textProperty().addListener((a, b, c) -> applyFilters());
        searchRoomField.textProperty().addListener((a, b, c) -> applyFilters());
        searchDateField.textProperty().addListener((a, b, c) -> applyFilters());

        clearFiltersButton.setOnAction(e -> {
            searchGuestField.clear();
            searchRoomField.clear();
            searchDateField.clear();
        });

        /* STATUS PILL BADGES WITH ICONS */
        colStatus.setCellFactory(column -> new TableCell<>() {

            private final Label badge = new Label();

            {
                badge.setStyle("-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 12px;");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                String icon = "";

                switch (status.toLowerCase()) {
                    case "confirmed":
                        icon = "✔️ ";
                        badge.getStyleClass().setAll("badge-confirmed");
                        break;

                    case "pending":
                        icon = "⏳ ";
                        badge.getStyleClass().setAll("badge-pending");
                        break;

                    case "checked-in":
                        icon = "🟢 ";
                        badge.getStyleClass().setAll("badge-checkedin");
                        break;

                    case "checked-out":
                        icon = "🏁 ";
                        badge.getStyleClass().setAll("badge-checkedout");
                        break;

                    case "cancelled":
                        icon = "❌ ";
                        badge.getStyleClass().setAll("badge-cancelled");
                        break;

                    case "no-show":
                        icon = "🚫 ";
                        badge.getStyleClass().setAll("badge-noshow");
                        break;

                    default:
                        icon = "• ";
                        badge.getStyleClass().setAll("badge-default");
                }

                badge.setText(icon + status);
                setGraphic(badge);
                setText(null);
            }
        });

        /* ACTION BUTTONS COLUMN */
        addActionsColumn();

        loadReservations();

        /* NEW RESERVATION */
        newReservationButton.setOnAction(e -> openReservationForm());

        /* SWITCH ACCOUNT */
        switchAccountButton.setOnAction(e -> {
            Session.clear();
            try {
                FXMLLoader fx = new FXMLLoader(getClass().getResource("/fxml/LoginScreen.fxml"));
                Parent root = fx.load();

                Stage st = (Stage) switchAccountButton.getScene().getWindow();
                Scene sc = new Scene(root);
                sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

                st.setScene(sc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        applyRolePermissions();
    }


    /* ALIGNMENT HELPERS */
    private void alignCenter(TableColumn<?, ?> c){ c.setStyle("-fx-alignment: CENTER;"); }
    private void alignLeft(TableColumn<?, ?> c){ c.setStyle("-fx-alignment: CENTER-LEFT;"); }
    private void alignRight(TableColumn<?, ?> c){ c.setStyle("-fx-alignment: CENTER-RIGHT;"); }


    /* FILTER LOGIC */
    private void applyFilters() {
        String guest = safeLower(searchGuestField.getText());
        String room  = safe(searchRoomField.getText());
        String date  = safe(searchDateField.getText());

        filteredData.setPredicate(r -> {
            if (r == null) return false;

            boolean g = guest.isEmpty() || r.getGuestName().toLowerCase().contains(guest);
            boolean rn = room.isEmpty() || String.valueOf(r.getRoomNumber()).contains(room);
            boolean d = date.isEmpty()
                    || r.getCheckIn().toString().contains(date)
                    || r.getCheckOut().toString().contains(date);

            return g && rn && d;
        });
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
    private String safeLower(String s) { return s == null ? "" : s.trim().toLowerCase(); }


    /* ROLE-BASED PERMISSIONS */
    private void applyRolePermissions() {
        User u = Session.getCurrentUser();
        if (u == null) return;

        if (!RoleUtils.canCreateReservation(u.getRole()))
            newReservationButton.setVisible(false);

        if (RoleUtils.isUser(u.getRole())) {
            searchGuestField.setDisable(true);
            searchRoomField.setDisable(true);
            searchDateField.setDisable(true);
            clearFiltersButton.setDisable(true);
        }
    }


    /* ACTIONS COLUMN */
    private void addActionsColumn() {

        TableColumn<Reservation, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(160);
        alignCenter(actionsCol);

        actionsCol.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(10, editBtn, delBtn);

            {
                box.setStyle("-fx-alignment: CENTER;");

                editBtn.getStyleClass().add("table-edit-btn");
                delBtn.getStyleClass().add("table-delete-btn");

                editBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null && RoleUtils.canEditReservation(Session.getCurrentUser().getRole()))
                        openEditForm(r);
                });

                delBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null && RoleUtils.canDeleteReservation(Session.getCurrentUser().getRole()))
                        confirmAndDeleteReservation(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                User u = Session.getCurrentUser();

                editBtn.setVisible(RoleUtils.canEditReservation(u.getRole()));
                delBtn.setVisible(RoleUtils.canDeleteReservation(u.getRole()));

                setGraphic(box);
            }
        });

        reservationTable.getColumns().add(8, actionsCol);
    }


    /* LOAD RESERVATIONS */
    private void loadReservations() {
        Task<List<Reservation>> task = new Task<>() {
            @Override protected List<Reservation> call() {
                return reservationDAO.getAllReservations();
            }
        };

        task.setOnSucceeded(e -> reservations.setAll(task.getValue()));
        task.setOnFailed(e -> LOGGER.log(Level.SEVERE, "Load failed", task.getException()));

        new Thread(task).start();
    }


    /* DELETE RESERVATION */
    private void confirmAndDeleteReservation(Reservation r) {

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete reservation for " + r.getGuestName() + "?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setHeaderText("Confirm Deletion");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.YES)
            return;

        // Background deletion task
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return reservationDAO.deleteReservation(r.getId());
            }
        };

        task.setOnSucceeded(e -> {
            boolean deleted = task.getValue();

            if (deleted) {
                // SUCCESS POPUP (missing earlier)
                Alert ok = new Alert(
                        Alert.AlertType.INFORMATION,
                        "Reservation deleted successfully.",
                        ButtonType.OK
                );
                ok.setHeaderText(null);
                ok.showAndWait();

                loadReservations(); // refresh list

            } else {
                // FAILURE POPUP
                Alert err = new Alert(
                        Alert.AlertType.ERROR,
                        "Failed to delete reservation. Try again.",
                        ButtonType.OK
                );
                err.setHeaderText(null);
                err.showAndWait();
            }
        });

        task.setOnFailed(e -> {
            Alert err = new Alert(
                    Alert.AlertType.ERROR,
                    "Internal error while deleting reservation.",
                    ButtonType.OK
            );
            err.setHeaderText(null);
            err.showAndWait();
        });

        new Thread(task).start();
    }


    /* OPEN CREATE FORM */
    private void openReservationForm() {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/fxml/ReservationForm.fxml"));
            Parent root = fx.load();

            // Prepare controller for creation flow
            ReservationFormController ctrl = fx.getController();
            if (ctrl != null) ctrl.prepareForCreate();

            Stage st = new Stage();
            Scene sc = new Scene(root);
            sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            st.setScene(sc);
            st.initModality(Modality.APPLICATION_MODAL);
            st.setResizable(false);
            st.showAndWait();

            loadReservations();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to open form", ex);
        }
    }


    /* OPEN EDIT FORM */
    private void openEditForm(Reservation r) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/fxml/ReservationForm.fxml"));
            Parent root = fx.load();

            ReservationFormController ctrl = fx.getController();
            if (ctrl != null) ctrl.loadReservation(r);

            Stage st = new Stage();
            Scene sc = new Scene(root);
            sc.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            st.setScene(sc);
            st.initModality(Modality.APPLICATION_MODAL);
            st.setResizable(false);
            st.showAndWait();

            loadReservations();

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to open edit form", ex);
        }
    }

    @FXML
    private void handleCSVExportAll() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for All CSV export");

        File dir = chooser.showDialog(null);
        if (dir != null) {
            boolean ok = new ReportService().exportCSV(dir);
            showExportAlert(ok, "All CSV");
        }
    }

    @FXML
    private void handleCSVExportDaily() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for Daily CSV export");

        File dir = chooser.showDialog(null);
        if (dir != null) {
            boolean ok = new ReportService().exportDailyCSV(dir, LocalDate.now());
            showExportAlert(ok, "Daily CSV");
        }
    }

    @FXML
    private void handleCSVExportMonthly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for Monthly CSV export");

        File dir = chooser.showDialog(null);
        if (dir != null) {
            LocalDate now = LocalDate.now();
            boolean ok = new ReportService().exportMonthlyCSV(dir, now.getYear(), now.getMonthValue());
            showExportAlert(ok, "Monthly CSV");
        }
    }

    @FXML
    private void handleCSVExportYearly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose folder for Yearly CSV export");

        File dir = chooser.showDialog(null);
        if (dir != null) {
            int year = LocalDate.now().getYear();
            boolean ok = new ReportService().exportYearlyCSV(dir, year);
            showExportAlert(ok, "Yearly CSV");
        }
    }

    // ===================== EXCEL EXPORT =========================

    @FXML
    private void handleExcelExportAll() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Excel Export (All)");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            boolean ok = new ReportService().exportExcelAll(dir);
            showExportAlert(ok, "Excel (All)");
        }
    }

    @FXML
    private void handleExcelExportDaily() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Daily Excel Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            boolean ok = new ReportService().exportExcelDaily(dir, LocalDate.now());
            showExportAlert(ok, "Excel (Daily)");
        }
    }

    @FXML
    private void handleExcelExportMonthly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Monthly Excel Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            LocalDate now = LocalDate.now();
            boolean ok = new ReportService().exportExcelMonthly(dir, now.getYear(), now.getMonthValue());
            showExportAlert(ok, "Excel (Monthly)");
        }
    }

    @FXML
    private void handleExcelExportYearly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Yearly Excel Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            int year = LocalDate.now().getYear();
            boolean ok = new ReportService().exportExcelYearly(dir, year);
            showExportAlert(ok, "Excel (Yearly)");
        }
    }

    @FXML
    private void handlePDFExportAll() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for PDF Export (All)");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            boolean ok = new ReportService().exportPDFAll(dir);
            showExportAlert(ok, "PDF (All)");
        }
    }

    @FXML
    private void handlePDFExportDaily() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Daily PDF Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            boolean ok = new ReportService().exportPDFDaily(dir, LocalDate.now());
            showExportAlert(ok, "Daily PDF");
        }
    }

    @FXML
    private void handlePDFExportMonthly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Monthly PDF Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            LocalDate now = LocalDate.now();
            boolean ok = new ReportService().exportPDFMonthly(dir, now.getYear(), now.getMonthValue());
            showExportAlert(ok, "PDF (Monthly)");
        }
    }

    @FXML
    private void handlePDFExportYearly() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder for Yearly PDF Export");
        File dir = chooser.showDialog(null);

        if (dir != null) {
            int year = LocalDate.now().getYear();
            boolean ok = new ReportService().exportPDFYearly(dir, year);
            showExportAlert(ok, "PDF (Yearly)");
        }
    }

    private void showExportAlert(boolean success, String type) {
        Alert a = new Alert(success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
        a.setHeaderText(null);
        a.setContentText(success ? type + " exported successfully!" : "Failed to export " + type);
        a.showAndWait();
    }


}
