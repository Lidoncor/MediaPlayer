package com.player.player.srtParser;

import com.player.player.common.Colors;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SRTParser {

    private final static Logger logger = LogManager.getLogger(SRTParser.class);

    private static final Pattern PATTERN_TIME = Pattern.compile("([\\d]{2}:[\\d]{2}:[\\d]{2},[\\d]{3}).*([\\d]{2}:[\\d]{2}:[\\d]{2},[\\d]{3})");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("(\\d+)");
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final String REGEX_REMOVE_TAGS = "<[^>]*>";

    private static final int PATTERN_TIME_REGEX_GROUP_START_TIME = 1;
    private static final int PATTERN_TIME_REGEX_GROUP_END_TIME = 2;

    public static Set<Subtitle> getSubtitlesFromFile (String path) {
        return getSubtitlesFromFile(path, false, false);
    }

    public static Set<Subtitle> getSubtitlesFromFile (String path, boolean keepNewlinesEscape, boolean usingNodes) {

        Set<Subtitle> subtitles = null;
        Subtitle subtitle;
        StringBuilder srt;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path), DEFAULT_CHARSET))) {

            subtitles = new LinkedHashSet<>();
            subtitle = new Subtitle();
            srt = new StringBuilder();

            String delim = " \n";
            StringTokenizer st;

            while (bufferedReader.ready()) {

                String line = bufferedReader.readLine();

                Matcher matcher = PATTERN_NUMBERS.matcher(line);

                if (matcher.find()) {
                    //subtitle.id = Integer.parseInt(matcher.group(1)); // index
                    line = bufferedReader.readLine();
                }

                matcher = PATTERN_TIME.matcher(line);

                if (matcher.find()) {
                    subtitle.startTime = matcher.group(PATTERN_TIME_REGEX_GROUP_START_TIME); // start time
                    subtitle.timeIn = SRTUtils.textTimeToMillis(subtitle.startTime);
                    subtitle.endTime = matcher.group(PATTERN_TIME_REGEX_GROUP_END_TIME); // end time
                    subtitle.timeOut = SRTUtils.textTimeToMillis(subtitle.endTime);
                }

                String aux;
                while ((aux = bufferedReader.readLine()) != null && !aux.isEmpty()) {
                    srt.append(aux);
                    if (keepNewlinesEscape)
                        srt.append("\n");
                    else {
                        if (!line.endsWith(" ")) // for any new lines '\n' removed from BufferedReader
                            srt.append(" ");
                    }
                }

                srt.delete(srt.length()-1, srt.length()); // remove '\n' or space from end string

                line = srt.toString();
                srt.setLength(0); // Clear buffer

                if (line != null && !line.isEmpty())
                    line = line.replaceAll(REGEX_REMOVE_TAGS, ""); // clear all tags

                subtitle.text = line;

                st = new StringTokenizer(line, delim);
                while (st.hasMoreTokens()) {
                    Text word = new Text(st.nextToken());
                    word.setFont(new Font("Arial", 27));
                    word.setFill(Colors.DEFAULT);
                    word.setOnMouseClicked(event -> {
                        ObservableList<Text> userDataUpdate = (ObservableList<Text>) word.getParent().getUserData();
                        if (userDataUpdate.size() == 0) {
                            word.setFill(Colors.SELECTED);
                            userDataUpdate.add(word);
                        } else if (userDataUpdate.contains(word)) {
                            word.setFill(Colors.DEFAULT);
                            userDataUpdate.remove(word);
                        } else if (userDataUpdate.get(0).getParent() == word.getParent()) {
                            word.setFill(Colors.SELECTED);
                            userDataUpdate.add(word);
                        }
                    });
                    word.setOnMouseMoved(event -> word.setCursor(Cursor.HAND));
                    subtitle.wordGroup.add(word);
                }

                subtitles.add(subtitle);

                if (usingNodes) {
                    subtitle.nextSubtitle = new Subtitle();
                    subtitle = subtitle.nextSubtitle;
                } else {
                    subtitle = new Subtitle();
                }
            }
        } catch (Exception e) {
            logger.error("error parsing srt file", e);
        }
        return subtitles;
    }
}
