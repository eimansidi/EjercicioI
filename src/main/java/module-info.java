module com.example.eji {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;


    opens com.example.eji to javafx.fxml;
    exports com.example.eji;
    exports com.example.eji.model;
    opens com.example.eji.model to javafx.fxml;
}