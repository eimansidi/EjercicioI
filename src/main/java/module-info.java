module com.example.eji {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.example.eji to javafx.fxml;
    exports com.example.eji;
}