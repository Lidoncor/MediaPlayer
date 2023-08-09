package com.player.player.controllers;

import com.player.player.common.EntityManagerFactory;
import com.player.player.dao.VideoDao;
import com.player.player.dao.WordDao;
import com.player.player.other.TableViewRow;
import com.player.player.models.Video;
import com.player.player.models.Word;
import jakarta.persistence.EntityManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DictionaryController implements Initializable {
    @FXML
    private ListView<Video> mediaListView;
    @FXML
    public Button backToPlayerBtn;
    @FXML
    private Label mediaName;
    @FXML
    private Label mediaPath;
    @FXML
    private TableView<TableViewRow> wordsTableView;
    @FXML
    private TableColumn<TableViewRow, Label> wordColumn;
    @FXML
    private TableColumn<TableViewRow, VBox> translationsColumn;
    private Stage stage;
    private Scene mainScene;
    private VideoDao videoDao;
    private WordDao wordDao;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        EntityManager entityManager = EntityManagerFactory.getEntityManager();
        videoDao = new VideoDao(entityManager);
        wordDao = new WordDao(entityManager);

        wordColumn.setCellValueFactory(new PropertyValueFactory<>("wordLabel"));
        translationsColumn.setCellValueFactory(new PropertyValueFactory<>("translationsShowCase"));

        mediaListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Video> call(ListView<Video> param) {
                return new ListCell<>() {
                    @Override
                    public void updateItem(Video video, boolean empty) {
                        super.updateItem(video, empty);
                        if (empty || video == null) {
                            setGraphic(null);
                            setText(null);
                        } else {
                            prefWidthProperty().bind(param.widthProperty());
                            setWrapText(true);
                            setText(video.getName());
                            Button removeMediaBtn = new Button("X");
                            removeMediaBtn.setOnAction(event -> {
                                for (Word w : video.getWords()) {
                                    if(w.getVideos().size() > 1) {
                                        w.getVideos().remove(video);
                                        wordDao.merge(w);
                                        video.getWords().remove(w);
                                        videoDao.merge(video);
                                    } else {
                                        video.getWords().remove(w);
                                        wordDao.delete(w);
                                    }
                                }
                                videoDao.delete(video);
                                updateDictionary();
                            });
                            setGraphic(removeMediaBtn);
                        }
                    }
                };
            }
        });

        mediaListView.setOnMouseClicked(event -> {
            wordsTableView.getItems().clear();

            Video selectedVideo = mediaListView.getSelectionModel().getSelectedItem();

            if (selectedVideo != null) {
                mediaName.setText(selectedVideo.getName());
                mediaPath.setText(selectedVideo.getPath());

                for (Word w : selectedVideo.getWords()) {
                    TableViewRow tableViewRow = new TableViewRow(w, w.getTranslations(), selectedVideo);
                    tableViewRow.setParent(wordsTableView);
                    wordsTableView.getItems().add(tableViewRow);
                }
            }
        });

    }

    private void updateDictionary() {
        mediaListView.getItems().clear();
        wordsTableView.getItems().clear();

        mediaName.setText("Media Name");
        mediaPath.setText("Media Path");

        List<Video> videos = videoDao.selectAll();

        for (Video v : videos) {
            mediaListView.getItems().add(v);
        }
    }

    @FXML
    private void backToPlayerBtnAction(ActionEvent event) {
        double w = stage.getWidth();
        double h = stage.getHeight();
        stage.setScene(mainScene);
        stage.setWidth(w);
        stage.setHeight(h);
    }

    public void dictionaryControllerSetUp(Scene sceneFromMain, Stage stageFromMain){
        stage = stageFromMain;

        mainScene = sceneFromMain;

        stage.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != mainScene) updateDictionary();
        });
    }

}
