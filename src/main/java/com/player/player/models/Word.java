package com.player.player.models;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Word {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true)
    private String name;

    @OneToMany(mappedBy = "word", cascade = CascadeType.ALL)
    private Set<Translation> translations;

    @ManyToMany(mappedBy = "words")
    private Set<Video> videos;

    public Word() {
    }

    public Word(String name) {
        this.name = name;

        this.translations = new HashSet<>();
    }

    public Set<Video> getVideos() {
        return videos;
    }

    public void setVideos(Set<Video> videos) {
        this.videos = videos;
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

    public void setTranslations(Set<Translation> translations) {
        this.translations = translations;
    }

    public Set<Translation> getTranslations() {
        return translations;
    }

    public void addTranslation(Translation translation) {
        this.translations.add(translation);
    }

}