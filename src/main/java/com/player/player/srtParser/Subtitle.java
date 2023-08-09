package com.player.player.srtParser;

import javafx.scene.Group;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Objects;

public class Subtitle {
    //public int id;
    public String startTime;
    public String endTime;
    public String text;
    public long timeIn;
    public long timeOut;
    public Subtitle nextSubtitle;

    public ArrayList<Text> wordGroup = new ArrayList<>();

    @Override
    public boolean equals(Object object) {

        Subtitle subtitle = (Subtitle) object;

        if(Objects.equals(subtitle.text, text) && Objects.equals(subtitle.startTime, startTime) && Objects.equals(subtitle.endTime, endTime))
            return true;
        else
            return false;
    }

    @Override
    public int hashCode() {
        return text.hashCode() + startTime.hashCode() + endTime.hashCode();

    }
}
