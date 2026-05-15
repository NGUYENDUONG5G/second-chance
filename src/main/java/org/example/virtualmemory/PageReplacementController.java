package org.example.virtualmemory;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class PageReplacementController {

    @FXML
    private TextField txtPages, txtFrames, txtRefBits;

    @FXML
    private HBox boxVisualization;
    @FXML
    private Label lblSummary;

    @FXML
    private void handleRunSimulation() {
        try {
            String frameText = txtFrames.getText().trim();
            if (frameText.isEmpty())
                throw new Exception("Số khung trống");
            int capacity = Integer.parseInt(frameText);

            String pagesText = txtPages.getText().trim();
            if (pagesText.isEmpty())
                throw new Exception("Chuỗi trang trống");

            int[] pages = Arrays.stream(pagesText.split("[,\\s]+"))
                    .mapToInt(Integer::parseInt).toArray();

            String refBitsText = txtRefBits.getText().trim();
            boolean[] initialRefBits = new boolean[capacity];
            if (!refBitsText.isEmpty()) {
                String[] bits = refBitsText.split("[,\\s]+");
                for (int i = 0; i < Math.min(capacity, bits.length); i++) {
                    initialRefBits[i] = bits[i].equals("1");
                }
            }

            SimulationResult result = runLogic(pages, capacity, initialRefBits);

            renderGraphics(pages, result);
            lblSummary.setText("Thuật toán: Second-Chance | Tổng số Page Faults: " + result.totalFaults);

        } catch (Exception e) {
            lblSummary.setText("Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderGraphics(int[] pages, SimulationResult result) {
        boxVisualization.getChildren().clear();
        int capacity = result.history[0].length;

        for (int j = 0; j < pages.length; j++) {

            Label lblRef = new Label(String.valueOf(pages[j]));
            lblRef.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 10 0 10;");
            VBox refBox = new VBox(lblRef);
            refBox.setAlignment(Pos.TOP_CENTER);
            boxVisualization.getChildren().add(refBox);


            VBox framesBox = new VBox(0);
            framesBox.setAlignment(Pos.CENTER);

            if (result.isFault[j]) {
                for (int i = 0; i < capacity; i++) {
                    String val = result.history[j][i];
                    boolean bit = result.refBitsHistory[j][i];
                    String displayVal = val.equals("-1") ? "" : val + " (" + (bit ? "1" : "0") + ")";
                    Label lblFrame = new Label(displayVal);
                    lblFrame.setStyle("-fx-background-color: #d1e9ff; " +
                            "-fx-border-color: #555555; " +
                            "-fx-border-width: 0.5; " +
                            "-fx-min-width: 50; " +
                            "-fx-min-height: 30; " +
                            "-fx-alignment: center; " +
                            "-fx-font-weight: bold;");
                    framesBox.getChildren().add(lblFrame);
                }
            } else {

                Region spacer = new Region();
                spacer.setMinWidth(30);
                framesBox.getChildren().add(spacer);
            }

            VBox columnWrapper = new VBox(15);
            columnWrapper.getChildren().add(new Region()); // Spacer ảo
            columnWrapper.getChildren().add(framesBox);
            boxVisualization.getChildren().add(columnWrapper);
        }
    }

    private static class SimulationResult {
        String[][] history;
        boolean[][] refBitsHistory;
        boolean[] isFault;
        int totalFaults;
    }

    private SimulationResult runLogic(int[] pages, int capacity, boolean[] refBits) {
        SimulationResult res = new SimulationResult();
        int n = pages.length;
        res.history = new String[n][capacity];
        res.refBitsHistory = new boolean[n][capacity];
        res.isFault = new boolean[n];

        for (int i = 0; i < n; i++)
            Arrays.fill(res.history[i], "-1");

        int[] frames = new int[capacity];
        Arrays.fill(frames, -1);
        int pointer = 0;

        for (int j = 0; j < n; j++) {
            int x = pages[j];
            boolean hit = false;
            for (int i = 0; i < capacity; i++) {
                if (frames[i] == x) {
                    refBits[i] = true;
                    hit = true;
                    break;
                }
            }

            if (!hit) {
                res.isFault[j] = true;
                res.totalFaults++;

                while (true) {
                    if (!refBits[pointer]) {
                        frames[pointer] = x;
                        pointer = (pointer + 1) % capacity;
                        break;
                    }
                    refBits[pointer] = false;
                    pointer = (pointer + 1) % capacity;
                }
            }

            for (int i = 0; i < capacity; i++) {
                res.history[j][i] = String.valueOf(frames[i]);
                res.refBitsHistory[j][i] = refBits[i];
            }
        }

        return res;
    }

    @FXML
    private void handleImportFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file dữ liệu");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showOpenDialog(txtPages.getScene().getWindow());
        if (file != null) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                if (!lines.isEmpty()) {
                    txtPages.setText(lines.get(0).trim());
                    if (lines.size() > 1) {
                        txtFrames.setText(lines.get(1).trim());
                    }
                    if (lines.size() > 2) {
                        txtRefBits.setText(lines.get(2).trim());
                    }
                }
            } catch (Exception e) {
                lblSummary.setText("Lỗi đọc file!");
            }
        }
    }
}