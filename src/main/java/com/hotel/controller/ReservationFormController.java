package com.hotel.controller;

import com.hotel.dao.ReservationDAO;
import com.hotel.model.Reservation;
import com.hotel.model.User;
import com.hotel.session.Session;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ReservationFormController {

    @FXML private TextField guestNameField;
    @FXML private TextField phoneField;

    @FXML private ComboBox<String> roomTypeCombo;

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;

    @FXML private TextField totalPriceField;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private static final Logger LOGGER =
            Logger.getLogger(ReservationFormController.class.getName());

    private Integer editingReservationId = null;
    private boolean dirty = false;

    private static final Pattern PHONE_INTERNATIONAL =
            Pattern.compile("^\\+?\\d{8,15}$");

    private static final String STYLE_INVALID =
            "-fx-border-color: #e53935; -fx-border-width: 1;";
    private static final String STYLE_VALID = "";

    private static final double RATE_STANDARD = 3000.0;
    private static final double RATE_DELUXE = 5000.0;
    private static final double RATE_SUITE = 7500.0;

    @FXML
    public void initialize() {

        // Static room types (no availability)
        roomTypeCombo.getItems().setAll(
                "Standard",
                "Deluxe",
                "Suite"
        );

        totalPriceField.setEditable(false);

        // Date change handlers
        checkInPicker.setOnAction(e -> {
            computePrice();
            markDirty();
            validateForm();
        });

        checkOutPicker.setOnAction(e -> {
            computePrice();
            markDirty();
            validateForm();
        });

        // User input listeners
        roomTypeCombo.setOnAction(e -> {
            computePrice();
            markDirty();
            validateForm();
        });

        guestNameField.textProperty().addListener((obs, o, n) -> {
            markDirty();
            validateForm();
        });

        phoneField.textProperty().addListener((obs, o, n) -> {
            markDirty();
            validateForm();
        });

        saveButton.setOnAction(e -> onSaveClicked());
        cancelButton.setOnAction(e -> onCancelClicked());

        validateForm();
    }

    private void computePrice() {
        String roomType = roomTypeCombo.getValue();
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (roomType == null || in == null || out == null || !in.isBefore(out)) {
            totalPriceField.setText("");
            return;
        }

        long nights = ChronoUnit.DAYS.between(in, out);
        nights = Math.max(1, nights);

        double rate;
        switch (roomType) {
            case "Standard": rate = RATE_STANDARD; break;
            case "Deluxe": rate = RATE_DELUXE; break;
            case "Suite": rate = RATE_SUITE; break;
            default: rate = 0.0;
        }

        totalPriceField.setText(String.format("%.2f", rate * nights));
    }

    public void loadReservation(Reservation r) {
        if (r == null) return;

        editingReservationId = r.getId();
        guestNameField.setText(r.getGuestName());
        phoneField.setText(r.getPhoneNumber());

        if (r.getCheckIn() != null)
            checkInPicker.setValue(r.getCheckIn().toLocalDate());

        if (r.getCheckOut() != null)
            checkOutPicker.setValue(r.getCheckOut().toLocalDate());

        roomTypeCombo.setValue(r.getRoomType());

        computePrice();
        dirty = false;
        clearValidationUI();
        validateForm();
    }

    public void prepareForCreate() {
        editingReservationId = null;

        guestNameField.clear();
        phoneField.clear();
        roomTypeCombo.setValue(null);
        checkInPicker.setValue(null);
        checkOutPicker.setValue(null);
        totalPriceField.clear();

        dirty = false;
        clearValidationUI();
        validateForm();
    }


    private void validateForm() {
        clearValidationUI();

        if (guestNameField.getText().trim().isEmpty()) {
            markFieldInvalid(guestNameField);
            saveButton.setDisable(true);
            return;
        }

        String phone = phoneField.getText().trim();
        if (phone.isEmpty() || !PHONE_INTERNATIONAL.matcher(phone).matches()) {
            markFieldInvalid(phoneField);
            saveButton.setDisable(true);
            return;
        }

        if (roomTypeCombo.getValue() == null) {
            markFieldInvalid(roomTypeCombo);
            saveButton.setDisable(true);
            return;
        }

        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !in.isBefore(out)) {
            markFieldInvalid(checkInPicker);
            markFieldInvalid(checkOutPicker);
            saveButton.setDisable(true);
            return;
        }

        if (totalPriceField.getText().trim().isEmpty()) {
            markFieldInvalid(totalPriceField);
            saveButton.setDisable(true);
            return;
        }

        saveButton.setDisable(false);
    }

    private void saveReservation() {

        String guestName = guestNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String roomType = roomTypeCombo.getValue();

        LocalDate inLD = checkInPicker.getValue();
        LocalDate outLD = checkOutPicker.getValue();
        double price = Double.parseDouble(totalPriceField.getText());

        User user = Session.getCurrentUser();
        int userId = (user != null) ? user.getId() : -1;

        Date checkIn = Date.valueOf(inLD);
        Date checkOut = Date.valueOf(outLD);
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        saveButton.setDisable(true);

        Task<Boolean> saveTask = new Task<>() {
            private String errorMsg;

            @Override
            protected Boolean call() {
                try {
                    Optional<Integer> room =
                            reservationDAO.findAvailableRoom(roomType, checkIn, checkOut);

                    if (room.isEmpty()) {
                        errorMsg = "No available " + roomType + " rooms.";
                        return false;
                    }

                    Reservation res = new Reservation(
                            editingReservationId != null ? editingReservationId : 0,
                            userId,
                            guestName,
                            room.get(),
                            roomType,
                            checkIn,
                            checkOut,
                            phone,
                            "Pending",
                            price,
                            createdAt
                    );

                    boolean ok = (editingReservationId != null)
                            ? reservationDAO.updateReservation(res)
                            : reservationDAO.createReservation(res);

                    if (!ok) {
                        errorMsg = "Database error.";
                        return false;
                    }

                    return true;

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Save failed", ex);
                    errorMsg = "Internal error.";
                    return false;
                }
            }

            @Override
            protected void succeeded() {
                saveButton.setDisable(false);

                if (getValue()) {
                    dirty = false;
                    new Alert(Alert.AlertType.INFORMATION,
                            "Reservation saved.", ButtonType.OK)
                            .showAndWait();
                    closeWindow();
                } else {
                    new Alert(Alert.AlertType.ERROR,
                            errorMsg, ButtonType.OK)
                            .showAndWait();
                }
            }
        };

        new Thread(saveTask).start();
    }

    private void markFieldInvalid(Control c) {
        if (c != null) c.setStyle(STYLE_INVALID);
    }

    private void clearValidationUI() {
        guestNameField.setStyle(STYLE_VALID);
        phoneField.setStyle(STYLE_VALID);
        roomTypeCombo.setStyle(STYLE_VALID);
        checkInPicker.setStyle(STYLE_VALID);
        checkOutPicker.setStyle(STYLE_VALID);
        totalPriceField.setStyle(STYLE_VALID);
    }

    private void onSaveClicked() {
        saveReservation();
    }

    private void onCancelClicked() {
        closeWindow();
    }

    private void closeWindow() {
        Stage st = (Stage) saveButton.getScene().getWindow();
        st.close();
    }

    private void markDirty() {
        dirty = true;
    }
}
