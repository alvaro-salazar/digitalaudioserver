module com.denkitronik.digitalaudioserver {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;

    opens com.denkitronik.digitalaudioserver to javafx.fxml;
    exports com.denkitronik.digitalaudioserver;
}