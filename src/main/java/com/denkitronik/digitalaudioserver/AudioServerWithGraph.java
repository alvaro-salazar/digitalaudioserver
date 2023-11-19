/*
 MIT License

 Copyright (c) 2023 Alvaro Salazar <alvaro@denkitronik.com>

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package com.denkitronik.digitalaudioserver;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * AudioServerWithGraph
 * This Java application serves as a server for receiving audio data from clients and playing it back.
 * It includes a graphical user interface that displays a real-time waveform visualization of the received audio.
 * The visualization supports different styles such as showing the average, average and current values, or unique values.
 * Author: Alvaro Salazar <alvaro@denkitronik.com>
 * Date: 11-2023
 * License: MIT License
 * Usage:
 * - Run the AudioServerWithGraph application to start the server.
 * - Connect clients that send audio data to the server's IP address and port (default port: 12345).
 * - The server will play back the audio and display the waveform visualization.
 * Dependencies:
 * - JFreeChart library for graphical charting (https://sourceforge.net/projects/jfreechart/)
 * Note: Ensure that the JFreeChart library is included in the project's classpath.
 */
public class AudioServerWithGraph extends JFrame {

    private static String ip = "localhost";
    private static int port = 12345;
    // Constants to define the visualization styles
    private final int STYLE_AVG = 0;
    private final int STYLE_AVG_AND_CURRENT = 1;
    private final int STYLE_UNIQUE_VALUE = 2;

    // Audio-related parameters
    private final int SAMPLE_RATE = 44100;
    private static final int SERVER_PORT = 12345;

    // Data series for the visualizations
    private XYSeries series1;
    private XYSeries series2;

    // Dataset for the visualizations
    private XYSeriesCollection dataset = null;

    // Time interval for the visualization
    private final double DURATION = 0.07;
    private final int WIDTH = 150;

    // Visualization configuration
    private int maxAmplitude = 32768;
    private int minAmplitude = -32768;
    private boolean onePage;
    private double currentTime = 0;
    private int page = 0;
    private int indexAvg = 0;
    private int average = 0;
    private int style;

    /**
     * Method that creates the window and displays it.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        AudioDeviceSimulator audioDeviceSimulator;
        switch (args.length) {
            case 0:  // Default mode (server)
                System.out.println("Starting server mode default mode on port: " + SERVER_PORT);
                SwingUtilities.invokeLater(() -> {
                    new AudioServerWithGraph().setVisible(true);
                });
                break;
            case 1: // Client or server mode (server IP and port are not specified)
                String option = args[0];
                if (option.equals("client")) {
                    System.out.println("Starting client default mode. Connecting to server localhost on port: " + SERVER_PORT);
                    audioDeviceSimulator = new AudioDeviceSimulator();
                    audioDeviceSimulator.startClient(ip,port);
                } else if (option.equals("server")) {
                    System.out.println("Starting server default mode on port: " + SERVER_PORT);
                    // Create the window and display it
                    SwingUtilities.invokeLater(() -> {
                        new AudioServerWithGraph().setVisible(true);
                    });
                }
                break;
            case 3: // Client mode (server IP and port are specified)
                System.out.println("Starting client mode. Connecting to server "+ip+" on port: " + port);
                audioDeviceSimulator = new AudioDeviceSimulator();
                audioDeviceSimulator.startClient(args[1], Integer.parseInt(args[2]));
                break;
            case 4: // Client mode (server IP and port are specified)
                ip = args[1];
                port = Integer.parseInt(args[2]);
                String wavFile = args[3];
                System.out.println("Starting client mode. Connecting to server "+ip+" on port: " + port+ " with audio file: "+wavFile);
                audioDeviceSimulator = new AudioDeviceSimulator();
                audioDeviceSimulator.startClient(args[1], Integer.parseInt(args[2]), args[3]);
                break;
            default: // Invalid number of arguments
                System.out.println("Usage: java -jar AudioServerWithGraph.jar [client|server] [ip] [port]");
                System.exit(0);
        }
    }


    /**
     * Constructor of the class. Creates the window and configures it.
     * It also starts the audio server.
     *
     * @throws HeadlessException If the window cannot be created
     * @throws Exception         If the audio server cannot be started
     */
    public AudioServerWithGraph() {
        super("Audio Waveform Visualization and Player (Server) by Alvaro Salazar");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        onePage = DURATION >= 1.2;
        if (DURATION >= 0.5) {
            style = STYLE_UNIQUE_VALUE;
        } else if (DURATION >= 0.1) {
            style = STYLE_AVG_AND_CURRENT;
        } else if (DURATION >= 0.05) {
            style = STYLE_AVG;
        }
        series1 = new XYSeries("Amplitude (units)");
        series2 = new XYSeries("Amplitude (units)");
        dataset = new XYSeriesCollection(series1);
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 400));
        add(chartPanel);

        new Thread(this::startAudioStream).start();

        // Set a larger initial size for the window
        setSize(1000, 600);
    }

    /**
     * Method that creates the waveform visualization chart.
     *
     * @param dataset Dataset that will be used to create the chart
     * @return The created chart
     */
    private JFreeChart createChart(XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Audio Waveform Visualization",
                "Time (seconds)",
                "Amplitude",
                dataset
        );

        // Set the chart to look better and make it easier to interpret the data
        XYPlot plot = chart.getXYPlot();
        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        // Set the axis ranges to fit the data that will be displayed
        xAxis.setRange(0, DURATION);
        xAxis.setTickUnit(new NumberTickUnit((float) DURATION / 10));
        yAxis.setRange(minAmplitude, maxAmplitude);
        yAxis.setTickUnit(new NumberTickUnit((maxAmplitude - minAmplitude) / 10));

        // Set the spline renderer to make the visualization smoother
        XYSplineRenderer renderer = new XYSplineRenderer();

        // Set the renderer to not show the markers
        renderer.setSeriesShapesVisible(0, false); // Disable the markers for the series
        plot.setRenderer(renderer); // Set the renderer for the plot

        return chart;
    }


    /**
     * Method that starts the audio server and plays back the received audio.
     *
     * @throws Exception If the audio server cannot be started
     * @throws Exception If the audio cannot be played back
     * @throws Exception If the waveform visualization cannot be updated
     */
    private void startAudioStream() {
        while (true) {
            // Start the audio server and wait for a client to connect
            try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
                 Socket clientSocket = serverSocket.accept();
                 InputStream inputStream = clientSocket.getInputStream()) {

                // Create the audio format that will be used to play back the received audio
                AudioFormat audioFormat = getAudioFormat();
                // Create the object that will be used to play back the received audio
                SourceDataLine line = AudioSystem.getSourceDataLine(audioFormat);
                // Open the object so that it is ready to play back the audio
                line.open(audioFormat);
                // Start playing back the audio
                line.start();
                // Buffer to store the received audio data
                byte[] buffer = new byte[1024];
                // Number of bytes read from the buffer
                int bytesRead;

                // Read data from the buffer and play it back until the connection is closed
                while ((bytesRead = inputStream.read(buffer, 0, buffer.length)) != -1) {
                    updateWaveform(buffer, bytesRead); // Update the waveform visualization
                    line.write(buffer, 0, bytesRead);
                }

            } catch (Exception e) {
                System.out.println("Audio streaming finished.");
            }
        }
    }


    /**
     * Method that updates the waveform visualization.
     *
     * @param buffer    Buffer with the received audio data
     * @param bytesRead Number of bytes read from the buffer
     */
    private void updateWaveform(byte[] buffer, int bytesRead) {
        // Update the waveform visualization in the GUI thread
        SwingUtilities.invokeLater(() -> {

            // Sweep through the received data and update the visualization
            for (int i = 0; i < bytesRead; i += 2) {
                int scaledValue = ((buffer[i] & 0xFF) << 8) | (buffer[i + 1] & 0xFF);

                // Convert the value to a signed 16-bit value (short)
                if (scaledValue > Short.MAX_VALUE) {
                    // The value is in the range [32768, 65535]
                    scaledValue = (short) (scaledValue - 65536);
                }

                // Update the waveform visualization with the received value and the current time
                if (indexAvg < (SAMPLE_RATE * DURATION / WIDTH)) {  // If the end of the interval has not been reached
                    average += scaledValue;                         // Accumulate the values to calculate the average
                    indexAvg++;                                     // Increment the value counter
                } else {
                    average = (average / indexAvg);                 // Calculate the average
                    indexAvg = 0;                                   // Reset the value counter

                    // Update the waveform visualization with the received value and the current time
                    switch (style) { // Selected visualization style
                        case STYLE_AVG:     // Show only the average of the values received in the time interval
                            scaledValue = average;
                            break;
                        case STYLE_AVG_AND_CURRENT: // Show the average and current value of the values received in the time interval
                            scaledValue = (average + scaledValue) / 2;
                            break;
                        case STYLE_UNIQUE_VALUE: // Show only the current value of the values received in the time interval

                            break;
                    }
                    average = 0;
                    currentTime += DURATION / WIDTH; // Increment the current time in the time interval
                    //double amplitude = 20.0 * Math.log10(1.0 + scaledValue+32768); // Calculate the amplitude in dB
                    double amplitude = scaledValue; // Calculate the amplitude in units
                    if (page == 0) { // Show the values on the first page
                        series1.add(currentTime, amplitude); // Add the value to the visualization
                    } else {
                        series2.add(currentTime, amplitude); // Add the value to the visualization
                    }
                }
            }
            // If the end of the first page has been reached, go to the second page
            if (page == 0 && series1.getItemCount() >= WIDTH) { // If the end of the first page has been reached
                if (!onePage) {                     // If there is more than one page of waveform visualization
                    dataset.removeAllSeries();      // Remove the first page of the visualization
                    dataset.addSeries(series1);     // Add the second page of the visualization
                    series2.clear();                // Clear the second page of the visualization
                    page = 1;                       // Go to the second page
                } else {
                    series1.clear();                // Clear the first page of the visualization
                }
                currentTime = 0;                    // Reset the current time
            }
            // If the end of the second page has been reached, go to the first page
            if (!onePage && page == 1 && series2.getItemCount() >= WIDTH) { // If the end of the second page has been reached
                dataset.removeAllSeries();          // Remove the second page of the visualization
                dataset.addSeries(series2);         // Add the first page of the visualization
                series1.clear();                    // Clear the first page of the visualization
                page = 0;                           // Go to the first page
                currentTime = 0;                    // Reset the current time
            }
        });
    }

    /**
     * Method that returns the audio format that will be used to play back the received audio.
     *
     * @return The audio format
     */
    private AudioFormat getAudioFormat() {
        // Return the audio format that will be used to play back the received audio (16-bit, 44.1 kHz, mono, signed, little endian)
        return new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    }
}
