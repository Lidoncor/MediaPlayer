package com.player.player.models;

import jakarta.persistence.*;
import javafx.scene.control.Label;

@Entity
public class Translation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String name;

    @ManyToOne()
    @JoinColumn(name = "word_id")
    private Word word;

    public Translation() {
    }

    public Translation(String name, Word word) {
        this.name = name;
        this.word = word;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Word getWord() {
        return word;
    }

    public void setWord(Word word) {
        this.word = word;
    }

    public Label getLabel() {
        Label lbl = new Label(name);
        lbl.setWrapText(true);
        return lbl;
    }
}
