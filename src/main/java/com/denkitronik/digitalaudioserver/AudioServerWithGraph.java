package com.denkitronik.digitalaudioserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AudioServerWithGraph extends Application {

    private static final int SERVER_PORT = 12345; // Puerto que el servidor utilizará para la transmisión
    private LineChart<Number, Number> waveformChart;
    private XYChart.Series<Number, Number> series;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        waveformChart = createChart();
        series = new XYChart.Series<>();
        waveformChart.getData().add(series);

        primaryStage.setTitle("Audio Waveform Visualization (Server)");
        primaryStage.setScene(new Scene(waveformChart, 800, 400));
        primaryStage.show();

        new Thread(this::startAudioStream).start();
    }

    private LineChart<Number, Number> createChart() {
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Amplitude");

        return new LineChart<>(xAxis, yAxis);
    }

    private void startAudioStream() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
             Socket clientSocket = serverSocket.accept();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            AudioFormat audioFormat = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();

            byte[] buffer = new byte[512];

            while (true) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                outputStream.write(buffer, 0, bytesRead);
                updateWaveform(buffer, bytesRead);
            }

        } catch (IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void updateWaveform(byte[] buffer, int bytesRead) {
        for (int i = 0; i < bytesRead; i++) {
            series.getData().add(new XYChart.Data<>(i, buffer[i]));
        }

        if (series.getData().size() > 800) {
            series.getData().remove(0, series.getData().size() - 800);
        }
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(44100, 16, 1, true, false);
    }
}