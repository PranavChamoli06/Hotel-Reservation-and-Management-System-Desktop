package com.hotel.controller;

import com.hotel.dao.ReservationDAO;
import com.hotel.model.Reservation;
import com.hotel.model.User;
import com.hotel.session.Session;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.ListCell;
import javafx.stage.Stage;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ReservationFormController {

    @FXML private TextField guestNameField;
    @FXML private TextField phoneField;

    @FXML private ComboBox<String> roomTypeCombo;        // now shows "Standard (N available)" etc.

    @FXML private DatePicker checkInPicker;
    @FXML private DatePicker checkOutPicker;

    @FXML private TextField totalPriceField;

    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    // convenience: title label exists in FXML (optional visual cue)
    @FXML private Label titleLabel;

    private final ReservationDAO reservationDAO = new ReservationDAO();
    private static final Logger LOGGER = Logger.getLogger(ReservationFormController.class.getName());

    private Integer editingReservationId = null;
    private boolean dirty = false;

    private static final Map<String, int[]> ROOM_RANGES = Map.of(
            "Standard", new int[]{100, 150},
            "Deluxe", new int[]{200, 250},
            "Suite", new int[]{300, 350}
    );

    private final Map<String, Double> RATE_PER_NIGHT = Map.of(
            "Standard", 3000.0,
            "Deluxe", 5000.0,
            "Suite", 7500.0
    );

    private static final Pattern PHONE_INTERNATIONAL = Pattern.compile("^\\+?\\d{8,15}$");

    private static final String STYLE_INVALID = "-fx-border-color: #e53935; -fx-border-width: 1;";
    private static final String STYLE_VALID = "";

    @FXML
    public void initialize() {

        // initial population (will be refreshed immediately)
        refreshRoomTypeAvailabilityLabels();

        // colorize dropdown cells & button (based on availability)
        roomTypeCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                int avail = extractAvailability(item);
                if (avail >= 10) setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");   // green
                else if (avail >= 5) setStyle("-fx-text-fill: #f9a825; -fx-font-weight: bold;"); // yellow
                else setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");              // red
            }
        });

        roomTypeCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                int avail = extractAvailability(item);
                if (avail >= 10) setStyle("-fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                else if (avail >= 5) setStyle("-fx-text-fill: #f9a825; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #c62828; -fx-font-weight: bold;");
            }
        });

        // refresh availability when dates change
        checkInPicker.setOnAction(e -> {
            computePrice();
            if (checkOutPicker.getValue() != null)
                refreshRoomTypeAvailabilityLabelsPreservingSelection();
            markDirty();
            validateForm();
        });

        checkOutPicker.setOnAction(e -> {
            computePrice();
            if (checkInPicker.getValue() != null)
                refreshRoomTypeAvailabilityLabelsPreservingSelection();
            markDirty();
            validateForm();
        });


        // user interactions
        roomTypeCombo.setOnAction(e -> { computePrice(); markDirty(); validateForm(); });

        guestNameField.textProperty().addListener((obs, o, n) -> { markDirty(); validateForm(); });
        phoneField.textProperty().addListener((obs, o, n) -> { markDirty(); validateForm(); });

        totalPriceField.setEditable(false);

        saveButton.setOnAction(evt -> onSaveClicked());
        cancelButton.setOnAction(evt -> onCancelClicked());

        validateForm();
    }

    /** Extract the integer availability from a label like "Standard (12 available)" */
    private int extractAvailability(String label) {
        if (label == null) return 0;
        try {
            int start = label.indexOf('(');
            int end = label.indexOf(' ', start);
            if (start >= 0 && end > start) {
                String num = label.substring(start + 1, end);
                return Integer.parseInt(num);
            }
            // fallback: try between '(' and ')'
            int close = label.indexOf(')', start);
            if (start >= 0 && close > start) {
                return Integer.parseInt(label.substring(start + 1, close));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /** Build the display label for a room type + available count */
    private String makeTypeLabel(String type, int available) {
        return String.format("%s (%d available)", type, available);
    }

    /** Recompute and refresh the room type items with availability counts */
    private void refreshRoomTypeAvailabilityLabels() {
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null || !in.isBefore(out)) {
            roomTypeCombo.getItems().clear();
            roomTypeCombo.getItems().add("Select room type (auto availability)");
            roomTypeCombo.setValue("Select room type (auto availability)");
            return;
        }

        refreshRoomTypeAvailabilityLabelsPreservingSelection();
    }

    private void refreshRoomTypeAvailabilityLabelsPreservingSelection() {
        String previous = roomTypeCombo.getValue();
        String previousType = extractPlainType(previous);

        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (in == null || out == null || !in.isBefore(out)) return;

        Date checkInDate = Date.valueOf(in);
        Date checkOutDate = Date.valueOf(out);

        int std = reservationDAO.countAvailableRooms("Standard", checkInDate, checkOutDate);
        int dlx = reservationDAO.countAvailableRooms("Deluxe", checkInDate, checkOutDate);
        int ste = reservationDAO.countAvailableRooms("Suite", checkInDate, checkOutDate);

        roomTypeCombo.getItems().clear();
        roomTypeCombo.getItems().add(makeTypeLabel("Standard", std));
        roomTypeCombo.getItems().add(makeTypeLabel("Deluxe", dlx));
        roomTypeCombo.getItems().add(makeTypeLabel("Suite", ste));

        if (previousType != null) {
            for (String item : roomTypeCombo.getItems()) {
                if (item.startsWith(previousType)) {
                    roomTypeCombo.setValue(item);
                    break;
                }
            }
        }
    }

    private void computePrice() {
        String typeLabel = roomTypeCombo.getValue();
        String selectedType = extractPlainType(typeLabel);
        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();

        if (selectedType == null || in == null || out == null || !in.isBefore(out)) {
            totalPriceField.setText("");
            return;
        }

        long nights = ChronoUnit.DAYS.between(in, out);
        double rate = RATE_PER_NIGHT.getOrDefault(selectedType, 0.0);
        double total = rate * Math.max(1, nights);

        totalPriceField.setText(String.format("%.2f", total));
    }

    /** Converts "Standard (12 available)" -> "Standard" */
    private String extractPlainType(String label) {
        if (label == null) return null;
        int sp = label.indexOf(' ');
        if (sp == -1) return label;
        return label.substring(0, sp);
    }

    public void loadReservation(Reservation r) {
        if (r == null) return;

        editingReservationId = r.getId();
        guestNameField.setText(r.getGuestName());
        phoneField.setText(r.getPhoneNumber());

        if (r.getCheckIn() != null) checkInPicker.setValue(r.getCheckIn().toLocalDate());
        if (r.getCheckOut() != null) checkOutPicker.setValue(r.getCheckOut().toLocalDate());

        computePrice();
        refreshRoomTypeAvailabilityLabels();

        // select the matching room type label (Standard/Deluxe/Suite)
        if (r.getRoomType() != null) {
            for (String item : roomTypeCombo.getItems()) {
                if (item.startsWith(r.getRoomType())) {
                    roomTypeCombo.setValue(item);
                    break;
                }
            }
        }

        // -----------------------
        // ENFORCE readonly for EDIT MODE (lock down fields that must not be edited)
        // -----------------------
        guestNameField.setEditable(false);
        phoneField.setEditable(false);
        roomTypeCombo.setDisable(true);
        totalPriceField.setEditable(false);

        // Optional visual cue
        if (titleLabel != null) titleLabel.setText("Edit Reservation — only dates editable");

        dirty = false;
        clearValidationUI();
        validateForm();
    }

    /** Call this when opening the form for a new reservation */
    public void prepareForCreate() {
        editingReservationId = null;
        guestNameField.setText("");
        phoneField.setText("");
        roomTypeCombo.setValue(null);
        checkInPicker.setValue(null);
        checkOutPicker.setValue(null);
        totalPriceField.setText("");

        // re-enable inputs for creation
        guestNameField.setEditable(true);
        phoneField.setEditable(true);
        roomTypeCombo.setDisable(false);
        totalPriceField.setEditable(false);

        if (titleLabel != null) titleLabel.setText("Reservation Form");

        dirty = false;
        clearValidationUI();
        validateForm();
    }

    private boolean isEditMode() {
        return editingReservationId != null;
    }

    private void validateForm() {
        clearValidationUI();

        // In edit mode we only require valid dates. In create mode run full validation.
        if (isEditMode()) {
            LocalDate in = checkInPicker.getValue();
            LocalDate out = checkOutPicker.getValue();
            if (in == null || out == null || !in.isBefore(out)) {
                markFieldInvalid(checkInPicker, "Invalid dates");
                markFieldInvalid(checkOutPicker, "Invalid dates");
                saveButton.setDisable(true);
                return;
            }
            saveButton.setDisable(false);
            return;
        }

        // CREATE mode validation (unchanged behavior)
        if (guestNameField.getText().trim().isEmpty()) {
            markFieldInvalid(guestNameField, "Guest required");
            saveButton.setDisable(true);
            return;
        }

        String phone = phoneField.getText().trim();
        if (phone.isEmpty() || !PHONE_INTERNATIONAL.matcher(phone).matches()) {
            markFieldInvalid(phoneField, "Invalid phone");
            saveButton.setDisable(true);
            return;
        }

        String typeLabel = roomTypeCombo.getValue();
        String roomType = extractPlainType(typeLabel);
        if (roomType == null || roomType.isEmpty()) {
            markFieldInvalid(roomTypeCombo, "Select room type");
            saveButton.setDisable(true);
            return;
        }

        LocalDate in = checkInPicker.getValue();
        LocalDate out = checkOutPicker.getValue();
        if (in == null || out == null || !in.isBefore(out)) {
            markFieldInvalid(checkInPicker, "Invalid dates");
            markFieldInvalid(checkOutPicker, "Invalid dates");
            saveButton.setDisable(true);
            return;
        }

        // availability quick check
        Date checkInDate = Date.valueOf(in);
        Date checkOutDate = Date.valueOf(out);
        int available = reservationDAO.countAvailableRooms(roomType, checkInDate, checkOutDate);
        if (available <= 0) {
            markFieldInvalid(roomTypeCombo, "No rooms available for selected type/dates");
            saveButton.setDisable(true);
            return;
        }

        if (totalPriceField.getText().trim().isEmpty()) {
            markFieldInvalid(totalPriceField, "Price not calculated");
            saveButton.setDisable(true);
            return;
        }

        saveButton.setDisable(false);
    }

    private void markFieldInvalid(Control n, String hint) {
        if (n != null) n.setStyle(STYLE_INVALID);
    }

    private void clearValidationUI() {
        guestNameField.setStyle(STYLE_VALID);
        phoneField.setStyle(STYLE_VALID);
        roomTypeCombo.setStyle(STYLE_VALID);
        checkInPicker.setStyle(STYLE_VALID);
        checkOutPicker.setStyle(STYLE_VALID);
        totalPriceField.setStyle(STYLE_VALID);
    }

    private void saveReservation() {
        LocalDate inLD = checkInPicker.getValue();
        LocalDate outLD = checkOutPicker.getValue();
        if (inLD == null || outLD == null) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Please select valid check-in and check-out dates.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            return;
        }

        final Date checkIn = Date.valueOf(inLD);
        final Date checkOut = Date.valueOf(outLD);

        saveButton.setDisable(true);

        Task<Boolean> saveTask = new Task<>() {
            private String errorMsg = "Unknown error";

            @Override
            protected Boolean call() {
                try {
                    if (isEditMode()) {
                        // EDIT MODE: only update check_in and check_out in DB
                        Reservation res = new Reservation();
                        res.setId(editingReservationId);
                        res.setCheckIn(checkIn);
                        res.setCheckOut(checkOut);

                        boolean ok = reservationDAO.updateCheckInOut(res);
                        if (!ok) errorMsg = "Failed to update reservation dates.";
                        return ok;
                    } else {
                        // CREATE MODE: original flow
                        String guestName = guestNameField.getText().trim();
                        String phone = phoneField.getText().trim();
                        String typeLabel = roomTypeCombo.getValue();
                        String roomType = extractPlainType(typeLabel);

                        double price = 0.0;
                        try { price = Double.parseDouble(totalPriceField.getText()); } catch (Exception ex) {}

                        Optional<Integer> maybe = reservationDAO.findAvailableRoom(roomType, checkIn, checkOut);
                        if (maybe.isEmpty()) {
                            errorMsg = "No available " + roomType + " rooms.";
                            return false;
                        }
                        int assignedRoom = maybe.get();

                        User user = Session.getCurrentUser();
                        int userId = user != null ? user.getId() : -1;
                        final java.sql.Timestamp createdAt = new java.sql.Timestamp(System.currentTimeMillis());

                        Reservation res = new Reservation(
                                0,
                                userId,
                                guestName,
                                assignedRoom,
                                roomType,
                                checkIn,
                                checkOut,
                                phone,
                                "Pending",
                                price,
                                createdAt
                        );

                        boolean ok = reservationDAO.createReservation(res);
                        if (!ok) errorMsg = "Database error while creating reservation.";
                        return ok;
                    }
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
                    Alert a = new Alert(Alert.AlertType.INFORMATION, "Reservation saved.", ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                    closeWindow();
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, errorMsg, ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                }
            }
        };

        new Thread(saveTask).start();
    }

    private void onSaveClicked() { saveReservation(); }
    private void onCancelClicked() { closeWindow(); }

    private void closeWindow() {
        Stage st = (Stage) saveButton.getScene().getWindow();
        st.close();
    }

    private void markDirty() { dirty = true; }
}
