package com.denkitronik.digitalaudioserver;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Objects;

public class AudioDeviceSimulator {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    /**
     * Starts a client connection to the server on the specified ip and port.
     * If no ip is specified, the localhost is used.
     *
     * @param ip   IP address of the server
     * @param port TCP port of the server
     * @return 0 if the connection was successful, -1 otherwise
     */
    public int startClient(String ip, int port) {
        try (Socket socket = new Socket(ip == null ? "localhost" : null, port)) {
            System.out.println("Starting client connection to the server" + ip + " on port " + port + "...");
            int angulo = 270;
            double amp = Math.sin(angulo * Math.PI / 180);

            OutputStream outputStream = socket.getOutputStream();

            System.out.println("Initializing audio streaming (440Hz, sampling freq: 44100, 16 bit, mono, little endian, sin waveform)");
            // Simulation of a 440Hz sin wave
            int sampleRate = 44100;     // Sampling frequency in Hz
            double frequency = 440.0;   // Wave frequency in Hz
            double duration = 20.0;     // Duration in seconds

            for (int i = 0; i < sampleRate * duration; i++) {
                byte[] byteValue = getBytes(i, (double) sampleRate, frequency);
                outputStream.write(byteValue);
            }

            System.out.println("Sound streaming simulation finished.");
            return 0;
        } catch (IOException e) {
            System.out.println("Server connection finished.");
            return -1;
        }
    }

    private static byte[] getBytes(int i, double sampleRate, double frequency) {
        double t = i / sampleRate;             // Time in seconds
        double amplitude = Math.sin(2.0 * Math.PI * frequency * t);

        // Scale to maximum amplitude of a 16-bit integer
        int value = (int) (amplitude * 65535);

        // Convert the value to a signed 16-bit value (short) 2-complement
        short data = (short) (value - 32768);

        // Convert to 16-bit pcm sound array, little endian byte order
        return new byte[]{
                (byte) (data & 0xff),        // Low byte
                (byte) (data >>> 8)          // High byte
        };
    }

    /**
     * Starts a client connection to the server on the specified ip and port and sends a wav file stream.
     * If no ip is specified, the localhost is used.
     *
     * @param ip      IP address of the server
     * @param port    TCP port of the server
     * @param wavFile Path to the wav file
     */
    public void startClient(String ip, int port, String wavFile) {
        try (Socket socket = new Socket(ip==null?SERVER_HOST:ip, port==0?SERVER_PORT:port); // Use localhost if no ip is specified
             FileInputStream fileInputStream = new FileInputStream(wavFile); // Read the wav file
        ){
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream()); // Get the output stream to the server
            System.out.println("Server connection started.");

            // Read and send the WAV file header (first 44 bytes)
            byte[] header = new byte[44]; // Header size is 44 bytes
            if (fileInputStream.read(header) != -1) { // Read until EOF
                //outputStream.write(header); // Uncomment this line if you want to send the header to the server
            }

            System.out.println("Initializing audio streaming (sampling freq: 44100, 16 bit, mono, little endian, wave file: "+wavFile+")");
            // Read and send the WAV file stream
            byte[] buffer = new byte[1024]; // Buffer size can be optimized (e.g., to 4096 or 8192)
            int bytesRead; // Number of bytes read each time
            while ((bytesRead = fileInputStream.read(buffer)) != -1) { // Read until EOF
                outputStream.write(buffer, 0, bytesRead); // Write the audio stream to the server
            }
            System.out.println("Wave file stream sent to the server.");
        } catch (UnknownHostException e) {
            System.out.println("Unknown host. Check the server address and try again. ");
        } catch (FileNotFoundException e) {
            System.out.println("File not found. Check the file path and try again.");
        } catch (IOException e) {
            System.out.println("Server connection finished.");
        }
    }
}