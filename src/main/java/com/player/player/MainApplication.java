package com.player.player;

import com.player.player.controllers.DictionaryController;
import com.player.player.controllers.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.io.IOException;

public class MainApplication extends Application {


    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader;

        fxmlLoader = new FXMLLoader(MainApplication.class.getResource("views/MainView.fxml"));
        Scene mainScene = new Scene(fxmlLoader.load());
        MainController mainController = fxmlLoader.getController();

        fxmlLoader = new FXMLLoader(MainApplication.class.getResource("views/DictionaryView.fxml"));
        Scene dictionaryScene = new Scene(fxmlLoader.load());
        DictionaryController dictionaryController = fxmlLoader.getController();

        mainController.mainControllerSetUp(dictionaryScene, stage);
        dictionaryController.dictionaryControllerSetUp(mainScene, stage);

        stage.setScene(mainScene);
        stage.setTitle("Player");
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();

    }

    @Override
    public final void stop() {

    }

    public static void main(String[] args) {
        launch();
    }

    private static SessionFactory buildSessionFactory() {
        return new Configuration().configure().buildSessionFactory();
    }

}