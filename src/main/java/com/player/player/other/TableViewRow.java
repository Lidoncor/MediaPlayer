package com.player.player.other;

import com.player.player.common.EntityManagerFactory;
import com.player.player.dao.TranslationDao;
import com.player.player.dao.VideoDao;
import com.player.player.dao.WordDao;
import com.player.player.models.Translation;
import com.player.player.models.Video;
import com.player.player.models.Word;
import jakarta.persistence.EntityManager;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.util.Set;

public class TableViewRow extends Node {

    private TableView<TableViewRow> parent;
    private Label wordLabel;
    private VBox translationsShowCase;
    private TranslationDao translationDao;
    private WordDao wordDao;
    private VideoDao videoDao;

    public TableViewRow(Word word, Set<Translation> translations, Video selectedVideo) {
        EntityManager entityManager = EntityManagerFactory.getEntityManager();
        wordDao = new WordDao(entityManager);
        translationDao = new TranslationDao(entityManager);
        videoDao = new VideoDao(entityManager);

        wordLabel = new Label(word.getName());
        wordLabel.setWrapText(true);

        Button removeWordLabelBtn = new Button("X");
        wordLabel.setGraphic(removeWordLabelBtn);

        translationsShowCase = new VBox();
        translationsShowCase.setPrefHeight(0); // remove this later maybe

        removeWordLabelBtn.setOnAction(event -> {
            if (word.getVideos().size() < 2) {
                selectedVideo.getWords().remove(word);
                wordDao.delete(word);
            } else {
                word.getVideos().remove(selectedVideo);
                wordDao.merge(word);
                selectedVideo.getWords().remove(word);
                videoDao.merge(selectedVideo);
            }
            parent.getItems().remove(this);
        });

        for (Translation translation : translations) {
            Label translationLabel = translation.getLabel();
            translationLabel.setWrapText(true);

            Button removeTranslationLabelBtn = new Button("X");
            removeTranslationLabelBtn.setOnAction(event -> {
                translationDao.deleteByTranslationIdAndWordId(translation.getId(), word.getId());
                translationsShowCase.getChildren().remove(translationLabel);
                word.getTranslations().remove(translation);
            });
            translationLabel.setGraphic(removeTranslationLabelBtn);

            translationsShowCase.getChildren().add(translationLabel);
        }

    }

    public void setParent(TableView<TableViewRow> parent) {
        this.parent = parent;
    }

    public Label getWordLabel() {
        return wordLabel;
    }

    public VBox getTranslationsShowCase() {
        return translationsShowCase;
    }

}
