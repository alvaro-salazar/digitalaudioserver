# AudioServerWithGraph

**AudioServerWithGraph** is a Java application that functions as a server for real-time audio streaming. It receives audio data from clients, plays it back, and dynamically visualizes the waveform on a graphical user interface. The application supports various visualization styles, allowing users to choose between displaying the average, average and current values, or unique values.

## Features

- **Real-time Audio Streaming:** Acts as a server for receiving and playing back audio data in real-time.
- **Waveform Visualization:** Displays the audio waveform dynamically with different visualization styles.
- **Customizable Parameters:** Users can customize parameters such as duration and width for the waveform visualization.
- **Graphical User Interface:** Provides an easy-to-use GUI for monitoring and controlling audio streaming.
- **Support digital audio formats:** Supports PCM audio formats (RIFF) @ 44100 Hz, 16-bit, stereo, little-endian, mono.

## Dependencies

- [JFreeChart Library](https://sourceforge.net/projects/jfreechart/): Used for graphical charting and visualization.
- [Java Sound API](https://docs.oracle.com/javase/tutorial/sound/index.html): Used for audio playback.

## Usage

1. Run the following command to build the project:

   ```bash
   ./gradlew build
   ```
2. Execute the server application using the following command:

   ```bash
   ./gradlew run
   ```
3. Connect clients that send audio data to the server's IP address and port (default port: 12345).
4. The server will play back the audio and display the waveform visualization on the GUI.

## Getting Started

Ensure that the JFreeChart library is included in the project's classpath. You can find the library [here](https://sourceforge.net/projects/jfreechart/).

## Building the JAR

To build a standalone JAR file, run the following command:
    
    ```bash
    ./gradlew jar
    ```

The JAR file will be located in the `build/libs/` directory.

## Running the JAR

To run the JAR file, execute the following command:

    ```bash
    java -jar build/libs/digitalaudioserver-1.0-SNAPSHOT-all.jar
    ```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Author

Alvaro Salazar <alvaro@denkitronik.com>

## Acknowledgments

- Special thanks to the contributors of the JFreeChart library.
- Thanks to the authors of the Java Sound API documentation.

## References

- [JFreeChart Library](https://sourceforge.net/projects/jfreechart/)
- [JFreeChart Developer Guide](https://www.jfree.org/jfreechart/api/guide.html)
- [JFreeChart API Reference](https://www.jfree.org/jfreechart/api/javadoc/index.html)
- [Java Sound API](https://docs.oracle.com/javase/tutorial/sound/index.html)
- [Java Sound API: Programmer's Guide](https://docs.oracle.com/javase/tutorial/sound/TOC.html)
- [Java Sound API: Reference](https://docs.oracle.com/javase/8/docs/api/javax/sound/sampled/package-summary.html)
