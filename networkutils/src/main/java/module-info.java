module dev.hlq {
    requires transitive javafx.controls;
    requires javafx.fxml;

    opens dev.hlq to javafx.fxml;
    exports dev.hlq;
}
