module com.player.player {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires uk.co.caprica.vlcj;
    requires uk.co.caprica.vlcj.javafx;
    requires org.apache.logging.log4j;
    requires ffmpeg;
    requires com.google.gson;
    requires org.hibernate.orm.core;
    requires jakarta.persistence;
    requires java.naming;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires com.sun.jna;

    opens com.player.player to javafx.fxml, org.hibernate.orm.core;
    opens com.player.player.models to javafx.fxml, org.hibernate.orm.core;
    exports com.player.player;
    exports com.player.player.controllers;
    opens com.player.player.controllers to javafx.fxml, org.hibernate.orm.core;
    exports com.player.player.common;
    opens com.player.player.common to javafx.fxml, org.hibernate.orm.core;
    exports com.player.player.other;
    opens com.player.player.other to javafx.fxml, org.hibernate.orm.core;
}