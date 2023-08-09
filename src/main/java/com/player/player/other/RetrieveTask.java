package com.player.player.other;

import com.player.player.srtParser.SRTParser;
import com.player.player.srtParser.Subtitle;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import java.io.File;
import java.util.Set;

public class RetrieveTask extends Task {
    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private Set<Subtitle> subtitles;
    private String path;
    private Long duration;
    private String subId;
    private ObjectProperty<Integer> timeUpdate;
    private File tempFile;
    private int time = 0;
    private boolean timeChanged = false;
    private boolean stop = false;
    private boolean callDone = false;
    private static final int TIME_INTERVAL = 10;

    public RetrieveTask(Set<Subtitle> subtitles,
                        String path,
                        Long duration,
                        String subId,
                        ObjectProperty<Integer> timeUpdate) {
        this.subtitles = subtitles;
        this.path = path;
        this.duration = duration;
        this.subId = subId;
        this.timeUpdate = timeUpdate;

        try {
            tempFile = File.createTempFile("sub", ".srt");
            tempFile.deleteOnExit();

            ffmpeg = new FFmpeg("src/main/resources/com/player/player/ffmpeg/ffmpeg.exe");
            ffprobe = new FFprobe("src/main/resources/com/player/player/ffmpeg/ffprobe.exe");

        } catch (Exception ignored) { }

    }

    public void stop() {
        stop = true;
        if (callDone) {
            resumeThread();
        }
    }

    private synchronized void pauseThread() {
        while (callDone) {
            try {
                wait();
            } catch (InterruptedException ignored) { }
        }
    }

    private synchronized void resumeThread() {
        callDone = false;
        notify();
    }

    @Override
    protected Object call() {
        time = timeUpdate.getValue();

        timeUpdate.addListener((observable, oldValue, newValue) -> {
            if (callDone) {
                time = newValue;
                resumeThread();
            } else timeChanged = true;
        });

        while (!stop) {

            while (time <= (duration / 1000)) {
                if (stop) break;

                if (timeChanged) {
                    time = timeUpdate.getValue();
                    timeChanged = false;
                    continue;
                }

                FFmpegBuilder builder = new FFmpegBuilder();
                builder.addExtraArgs("-ss", "" + time)
                        .addInput(path)
                        .addExtraArgs("-to", "" + (time + TIME_INTERVAL))
                        .addOutput(tempFile.getAbsolutePath())
                        .addExtraArgs("-c", "copy")
                        .addExtraArgs("-map", "0:s:" + subId)
                        .addExtraArgs("-copyts")
                        .done();
                FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
                executor.createJob(builder).run();

                subtitles.addAll(SRTParser.getSubtitlesFromFile(tempFile.getAbsolutePath()));

                time += TIME_INTERVAL;
            }

            if (!stop) {
                callDone = true;
                pauseThread();
            }
        }

        return null;
    }
}
