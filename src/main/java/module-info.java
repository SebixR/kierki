module com.example.kierki {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.almasb.fxgl.all;

    opens com.example.kierki to javafx.fxml;
    exports com.example.kierki;
}