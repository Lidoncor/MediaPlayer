package com.player.player.srtParser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SRTUtils {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private final static long MILLIS_IN_SECOND = 1000;
    private final static long MILLIS_IN_MINUTE = MILLIS_IN_SECOND * 60; // 60000
    private final static long MILLIS_IN_HOUR = MILLIS_IN_MINUTE * 60; // 3600000

    private final static Pattern PATTERN_TIME = Pattern.compile("([\\d]{2}):([\\d]{2}):([\\d]{2}),([\\d]{3})");

    private final static int PATTER_TIME_GROUP_HOURS = 1;
    private final static int PATTER_TIME_GROUP_MINUTES = 2;
    private final static int PATTER_TIME_GROUP_SECONDS = 3;
    private final static int PATTER_TIME_GROUP_MILLISECONDS = 4;

    private final static String SCAPE_TIME_TO_TIME = " --> ";

    private final static Logger logger = LogManager.getLogger(SRTUtils.class);

    public static long textTimeToMillis (final String time) throws Exception {

        if (time == null) throw new NullPointerException("Time should not be null");

        Matcher matcher = PATTERN_TIME.matcher(time);
        if (time.isEmpty() || !matcher.find()) throw new Exception("incorrect time format...");

        long msTime = 0;
        short hours = Short.parseShort(matcher.group(PATTER_TIME_GROUP_HOURS));
        byte min = Byte.parseByte(matcher.group(PATTER_TIME_GROUP_MINUTES));
        byte sec = Byte.parseByte(matcher.group(PATTER_TIME_GROUP_SECONDS));
        short millis = Short.parseShort(matcher.group(PATTER_TIME_GROUP_MILLISECONDS));

        if (hours > 0) msTime += hours * MILLIS_IN_HOUR;
        if (min > 0) msTime += min * MILLIS_IN_MINUTE;
        if (sec > 0) msTime += sec * MILLIS_IN_SECOND;

        return msTime + millis;
    }

    public static Subtitle findSubtitle (Set<Subtitle> subtitles, long timeMillis) {
        if (subtitles == null || subtitles.isEmpty()) return null;

        for (Subtitle sub : subtitles) {
            if (inTime(sub, timeMillis)) return sub;
        }

        return null;
    }

    private static boolean inTime(final Subtitle subtitle, long timeMillis) {
        return timeMillis >= subtitle.timeIn && timeMillis <= subtitle.timeOut;
    }

}
