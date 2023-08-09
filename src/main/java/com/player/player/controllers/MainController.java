package com.player.player.controllers;

import com.google.gson.stream.JsonReader;
import com.player.player.dao.TranslationDao;
import com.player.player.dao.VideoDao;
import com.player.player.dao.WordDao;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import com.google.gson.*;
import com.player.player.common.Colors;
import com.player.player.common.EntityManagerFactory;
import com.player.player.other.RetrieveTask;
import com.player.player.models.Translation;
import com.player.player.models.Video;
import com.player.player.models.Word;
import com.player.player.srtParser.SRTParser;
import com.player.player.srtParser.SRTUtils;
import com.player.player.srtParser.Subtitle;
import jakarta.persistence.EntityManager;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.media.*;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.TrackDescription;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class MainController implements Initializable {
    @FXML
    private BorderPane mainPane;
    @FXML
    private HBox buttonBar;
    @FXML
    private AnchorPane controlsPane;
    @FXML
    private StackPane mediaPane;
    @FXML
    private VBox subtitlesBox;
    @FXML
    private Slider timelineSlider;
    @FXML
    private ImageView videoImageView;
    @FXML
    private Menu subTracks;
    @FXML
    private Menu audioTracks;
    @FXML
    private Text currentTimeLbl;
    @FXML
    private Text endTimeLbl;
    @FXML
    private Button playbackBtn;
    @FXML
    private Button muteBtn;
    @FXML
    private Slider volumeSlider;
    @FXML
    private VBox translationPane;
    @FXML
    private VBox translations;
    @FXML
    private Text selectedWord;
    @FXML
    private MenuButton languagesMenu;

    private ToggleGroup languagesToggleGroup;
    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer embeddedMediaPlayer;
    private Map<FlowPane, Set<Subtitle>> subPanels;
    private ObjectProperty<Integer> timeUpdate;
    private Map<String, RetrieveTask> RetrieveTasks;
    private String mediaPath;
    private String mediaName;
    private Video currentVideoMedia;
    private ObservableList<Text> userDataUpdate;
    private boolean collecting = false;
    private boolean collectingToPanel = false;
    private Stage stage;
    private Scene dictionaryScene;
    private VideoDao videoDao;
    private WordDao wordDao;
    private TranslationDao translationDao;
    private int selectedSubTracks;
    private static final int SUBTITLES_ALLOWED = 2;
    FileChooser.ExtensionFilter extFilterMedia;
    FileChooser.ExtensionFilter extFilterSubtitles;
    private FileChooser fileChooserMedia;
    private FileChooser fileChooserSubtitles;
    private Label translationErrorLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        EntityManager entityManager = EntityManagerFactory.getEntityManager();
        videoDao = new VideoDao(entityManager);
        wordDao = new WordDao(entityManager);
        translationDao= new TranslationDao(entityManager);

        videoImageView.fitWidthProperty().bind(mediaPane.widthProperty());
        videoImageView.fitHeightProperty().bind(mediaPane.heightProperty());

        mediaPlayerFactory = new MediaPlayerFactory();
        embeddedMediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        embeddedMediaPlayer.videoSurface().set(new ImageViewVideoSurface(this.videoImageView));

        timeUpdate = new SimpleObjectProperty<>();
        timeUpdate.setValue(0);

        timelineSlider.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> timelineSlider.setValueChanging(true));
        timelineSlider.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> timelineSlider.setValueChanging(false));
        timelineSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (timelineSlider.isValueChanging()) {
                embeddedMediaPlayer.controls().setPosition(newValue.intValue() / 1000.0f);
                currentTimeLbl.setText(formatTime(embeddedMediaPlayer.status().time()));
                timeUpdate.setValue((int) (embeddedMediaPlayer.status().time() / 1000));
            }
        });

        volumeSlider.setValue(embeddedMediaPlayer.audio().volume());
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (volumeSlider.isValueChanging()) {
                embeddedMediaPlayer.audio().setVolume(newValue.intValue());
            }
        });

        ArrayList<Text> transferWords = new ArrayList<>();
        userDataUpdate = FXCollections.observableArrayList(transferWords);
        userDataUpdate.addListener((ListChangeListener<Text>) c -> {
            if (!collecting && !collectingToPanel) {
                Text t = c.getList().get(0);
                showTranslations(t.getText());
                t.setFill(Colors.DEFAULT);
                c.getList().clear();
            }
        });

        RetrieveTasks = new HashMap<>();
        subPanels = new HashMap<>();
        selectedSubTracks = 0;

        mainPane.setRight(null);

        translationPane.prefWidthProperty().bind(mainPane.widthProperty().divide(2.5));

        selectedWord.wrappingWidthProperty().bind(translationPane.prefWidthProperty());

        embeddedMediaPlayer.events().addMediaEventListener(new MediaEventAdapter(){
            @Override
            public void mediaParsedChanged(Media media, MediaParsedStatus newStatus) {
                embeddedMediaPlayer.subpictures().setTrack(-1);

                List<TrackDescription> subDesc = embeddedMediaPlayer.subpictures().trackDescriptions();
                List<TextTrackInfo> textTrackInfo = embeddedMediaPlayer.media().info().textTracks();
                for (int i = 0; i < textTrackInfo.size(); i++) {
                    if (!textTrackInfo.get(i).codecName().equals("subt")) continue;
                    CheckMenuItem item = new CheckMenuItem(subDesc.get(i + 1).description());
                    item.setId(String.valueOf(i));
                    item.setUserData(createSubtitlePane());
                    item.setOnAction(event -> {
                        String subId = item.getId();
                        FlowPane subPane = (FlowPane) item.getUserData();
                        if (item.isSelected()) {
                            if (selectedSubTracks < SUBTITLES_ALLOWED) {
                                selectedSubTracks++;
                            } else {
                                item.setSelected(false);
                                return;
                            }

                            long duration = media.info().duration();

                            Set<Subtitle> subtitles = new LinkedHashSet<>();
                            subtitlesBox.getChildren().add(0, subPane);
                            subPanels.put(subPane, subtitles);

                            timeUpdate.setValue((int) (embeddedMediaPlayer.status().time() / 1000));
                            RetrieveTask RetrieveTask = new RetrieveTask(subtitles, mediaPath, duration, subId, timeUpdate);
                            Thread thread = new Thread(RetrieveTask);
                            thread.start();
                            RetrieveTasks.put(subId, RetrieveTask);

                        } else {
                            selectedSubTracks--;

                            RetrieveTasks.get(subId).stop();
                            RetrieveTasks.remove(subId);

                            subPane.getChildren().clear();
                            subtitlesBox.getChildren().remove(subPane);
                            subPanels.remove(subPane);
                        }
                    });

                    subTracks.getItems().add(item);
                }

                List<TrackDescription> audioDesc = embeddedMediaPlayer.audio().trackDescriptions();
                ToggleGroup audioToggleGroup = new ToggleGroup();
                for (int i = 1; i < audioDesc.size(); i++) {
                    RadioMenuItem item = new RadioMenuItem(audioDesc.get(i).description());
                    item.setId(String.valueOf(i));
                    item.setOnAction(event -> embeddedMediaPlayer.audio().setTrack(Integer.parseInt(item.getId())));
                    item.setToggleGroup(audioToggleGroup);

                    audioTracks.getItems().add(item);
                }

                audioToggleGroup.selectToggle(audioToggleGroup.getToggles().get(0));
                embeddedMediaPlayer.audio().setTrack(1);

                endTimeLbl.setText(formatTime(embeddedMediaPlayer.status().length()));

                if (embeddedMediaPlayer.audio().isMute()) {
                    embeddedMediaPlayer.audio().setMute(false);
                }
                embeddedMediaPlayer.audio().setVolume(100);
            }
        });

        //need to fix
        embeddedMediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (!timelineSlider.isValueChanging()) {
                    currentTimeLbl.setText(formatTime(newTime));
                    timelineSlider.setValue((int) (embeddedMediaPlayer.status().position() * 1000.0f));
                }

                if (!subPanels.isEmpty()) {
                    for (var entry : subPanels.entrySet()) {
                        showSubtitle(newTime, entry);
                    }
                }
            }
        });

        extFilterMedia =
                new FileChooser.ExtensionFilter("Media Files (*.mkv, *.avi, *.mp4)", "*.mkv", "*.avi", "*.mp4");
        extFilterSubtitles =
                new FileChooser.ExtensionFilter("Subtitles (*.srt)", "*.srt");

        fileChooserMedia = new FileChooser();
        fileChooserSubtitles = new FileChooser();

        fileChooserMedia.getExtensionFilters().add(extFilterMedia);
        fileChooserSubtitles.getExtensionFilters().add(extFilterSubtitles);

        translationErrorLabel = new Label("Translation request failed");
        translationErrorLabel.setTextFill(Colors.ERROR);


        //initYandexLanguages();
        initGoogleLanguages();
    }

    private void showSubtitle(long time, Map.Entry<FlowPane, Set<Subtitle>> entry) {
        Platform.runLater(() -> {
            Subtitle temp = SRTUtils.findSubtitle(entry.getValue(), time);

            entry.getKey().getChildren().clear();

            if (temp != null) {
                entry.getKey().getChildren().addAll(temp.wordGroup);
            }
        });
    }

    private String formatTime(long value) {
        value /= 1000;
        int hours = (int) value / 3600;
        int remainder = (int) value - hours * 3600;
        int minutes = remainder / 60;
        remainder = remainder - minutes * 60;
        int seconds = remainder;
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    private FlowPane createSubtitlePane() {
        FlowPane subtitlePane = new FlowPane();
        subtitlePane.setAlignment(Pos.BOTTOM_CENTER);
        subtitlePane.setHgap(5);
        subtitlePane.setUserData(userDataUpdate);
        return subtitlePane;
    }

    private Button createActiveTranslation(String input) {
        Button activeTranslation = new Button(input);
        activeTranslation.setWrapText(true);
        activeTranslation.setFont(new Font("Arial", 20));

        Long exist = translationDao.isExistByTranslationNameAndWordName(input, selectedWord.getText());
        if (exist == 0) {
            activeTranslation.setUserData(false);
            activeTranslation.setBackground(Background.fill(Colors.NEW));
        } else {
            activeTranslation.setUserData(true);
            activeTranslation.setBackground(Background.fill(Colors.ADDED));
        }

        activeTranslation.setOnMouseClicked(event -> {
            //need to fix
            Word findedWord;
            try {
                findedWord = wordDao.findByName(selectedWord.getText());
            } catch (Exception ex) { findedWord = new Word(selectedWord.getText()); }
            //need to fix
            try {
                currentVideoMedia = videoDao.findByNameAndPath(mediaPath, mediaName);
            } catch (Exception ex) {
                currentVideoMedia = new Video(mediaPath, mediaName);
                videoDao.persist(currentVideoMedia);
            }

            //false - new, true - added
            if (activeTranslation.getUserData().equals(false)) {
                findedWord.addTranslation(new Translation(activeTranslation.getText(), findedWord));

                currentVideoMedia.addWord(findedWord);
                videoDao.merge(currentVideoMedia);

                activeTranslation.setBackground(Background.fill(Colors.ADDED));
                activeTranslation.setUserData(true);
            } else {
                translationDao.deleteByTranslationNameAndWordId(activeTranslation.getText(), findedWord.getId());

                activeTranslation.setBackground(Background.fill(Colors.NEW));
                activeTranslation.setUserData(false);
            }
        });
        activeTranslation.setOnMouseMoved(event -> activeTranslation.setCursor(Cursor.HAND));

        return activeTranslation;
    }

    private void initGoogleLanguages() {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader("src/main/resources/com/player/player/languages/languagesGoogle.json"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(reader);

        JsonObject rootObj = root.getAsJsonObject();
        JsonArray array = rootObj.getAsJsonArray("text");

        languagesToggleGroup = new ToggleGroup();
        for (JsonElement element : array) {
            String code = element.getAsJsonObject().get("code").toString();
            String language = element.getAsJsonObject().get("language").toString();

            code = code.substring(1, code.length() - 1);
            language = language.substring(1, language.length() - 1);

            RadioMenuItem languageItem = new RadioMenuItem(language);
            languageItem.setId(code);
            languageItem.setToggleGroup(languagesToggleGroup);
            languageItem.setOnAction(event -> {
                languagesMenu.setText(languageItem.getText());
                translations.getChildren().clear();
                translations.getChildren().addAll(googleTranslation(selectedWord.getText()));
            });

            if (code.equals("ru")) {
                languageItem.setSelected(true);
                languagesMenu.setText(languageItem.getText());
            };

            languagesMenu.getItems().add(languageItem);
        }
    }

    private void initYandexLanguages() {
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader("src/main/resources/com/player/player/languages/languagesYandex.json"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(reader);

        JsonObject rootObj = root.getAsJsonObject();
        JsonArray array = rootObj.getAsJsonArray("languages");

        languagesToggleGroup = new ToggleGroup();
        for (JsonElement element : array) {
            String code = element.getAsJsonObject().get("code").toString();
            String name = element.getAsJsonObject().get("name").toString();

            code = code.substring(1, code.length() - 1);
            name = name.substring(1, name.length() - 1);

            RadioMenuItem languageItem = new RadioMenuItem(name);
            languageItem.setId(code);
            languageItem.setToggleGroup(languagesToggleGroup);
            languageItem.setOnAction(event -> {
                languagesMenu.setText(languageItem.getText());
                translations.getChildren().clear();
                translations.getChildren().addAll(yandexTranslation(selectedWord.getText()));
            });

            if (code.equals("ru")) {
                languageItem.setSelected(true);
                languagesMenu.setText(languageItem.getText());
            };

            languagesMenu.getItems().add(languageItem);
        }
    }

    private Set<Button> googleTranslation(String text) {
        RadioMenuItem selected = (RadioMenuItem) languagesToggleGroup.getSelectedToggle();

        String sUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="+selected.getId()+"&dt=at&dt=bd&q="+text+"&ie=UTF-8&oe=UTF-8";
        URL url;
        URLConnection request;

        JsonElement root = null;
        JsonParser parser = new JsonParser();
        JsonArray translationsArray;

        try {
            url = new URL(sUrl.replace(" ", "%20"));
            request = url.openConnection();
            request.connect();
            root = parser.parse(new InputStreamReader((InputStream) request.getContent()));

            if (translationPane.getChildren().get(2) instanceof Label)
                translationPane.getChildren().remove(2);
        } catch (Exception ex) {
            if (!(translationPane.getChildren().get(2) instanceof Label))
                translationPane.getChildren().add(2, translationErrorLabel);
            return null;
        };

        Set<Button> activeTranslations = new HashSet<>();

        try {
            translationsArray = root.getAsJsonArray().get(1).getAsJsonArray();
            for (JsonElement e : translationsArray) {
                JsonArray words = e.getAsJsonArray().get(1).getAsJsonArray();
                for (JsonElement w : words) {
                    String input = w.getAsJsonPrimitive().toString();
                    input = input.substring(1, input.length() - 1);
                    Button activeTranslation = createActiveTranslation(input);
                    activeTranslations.add(activeTranslation);
                }
            }
        } catch (Exception ignored) { }

        try {
            translationsArray = root.getAsJsonArray().get(5).getAsJsonArray().get(0).getAsJsonArray().get(2).getAsJsonArray();
            for (JsonElement e : translationsArray) {
                String input = e.getAsJsonArray().get(0).getAsJsonPrimitive().toString();
                input = input.substring(1, input.length() - 1);
                Button activeTranslation = createActiveTranslation(input);
                activeTranslations.add(activeTranslation);
            }
        } catch (Exception ignored) { };

        return activeTranslations;
    }

    private Button yandexTranslation(String text) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("https://translate.api.cloud.yandex.net/translate/v2/translate");
        request.addHeader("method", "post");
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Authorization", "Api-key " + "key");

        RadioMenuItem selected = (RadioMenuItem) languagesToggleGroup.getSelectedToggle();

        StringEntity entity = null;
        HttpResponse response = null;
        try {
            entity = new StringEntity("{\"folderId\":\"id\"," +
                                                    "\"texts\":[\""+ text +"\"]," +
                                                    "\"targetLanguageCode\":\""+ selected.getId() +"\"}");
            request.setEntity(entity);
            response = httpClient.execute(request);
            if (translationPane.getChildren().get(2) instanceof Label)
                translationPane.getChildren().remove(2);
        } catch (IOException e) {
            if (!(translationPane.getChildren().get(2) instanceof Label))
                translationPane.getChildren().add(2, translationErrorLabel);
            return null;
        }

        BufferedReader rd = null;
        StringBuffer result = new StringBuffer();
        String line = "";
        try {
            rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JsonParser parser = new JsonParser();
        JsonObject object = parser.parse(result.toString()).getAsJsonObject();
        JsonPrimitive translation = object.getAsJsonArray("translations")
                .get(0).getAsJsonObject().getAsJsonPrimitive("text");

        return createActiveTranslation(translation.getAsString());
    }

    private void showTranslations(String string) {
        selectedWord.setText(string);
        Platform.runLater(() -> {
            translations.getChildren().clear();
            translations.getChildren().addAll(Objects.requireNonNull(googleTranslation(string)));
            //translations.getChildren().addAll(yandexTranslation(string));
        });
        if (mainPane.getRight() == null) {
            mainPane.setRight(translationPane);
        }
    }

    public void mainControllerSetUp(Scene sceneFromMain, Stage stageFromMain) {
        stage = stageFromMain;

        stageFromMain.setOnCloseRequest(event -> {
            embeddedMediaPlayer.controls().stop();
            embeddedMediaPlayer.release();
            mediaPlayerFactory.release();

            if (!RetrieveTasks.isEmpty()) {
                for (var entry : RetrieveTasks.entrySet()) {
                    entry.getValue().stop();
                }
                RetrieveTasks.clear();
            }


        });

        dictionaryScene = sceneFromMain;
    }

    @FXML
    void playbackBtnAction() {
        if (!embeddedMediaPlayer.status().isPlayable()) return;

        if (embeddedMediaPlayer.status().isPlaying()) {
            embeddedMediaPlayer.controls().pause();
            playbackBtn.setText("Play");
        } else {
            embeddedMediaPlayer.controls().play();
            playbackBtn.setText("Pause");
        }
    }

    @FXML
    void openMediaBtnAction() {
        File selectedFile = fileChooserMedia.showOpenDialog(null);
        if (selectedFile != null) {
            if (embeddedMediaPlayer.status().isPlaying()) {
                embeddedMediaPlayer.controls().pause();
            }

            mediaPath = selectedFile.getAbsolutePath();
            mediaName = selectedFile.getName();

            if (mainPane.getRight() != null) {
                mainPane.setRight(null);
            }

            subTracks.getItems().clear();
            audioTracks.getItems().clear();
            selectedSubTracks = 0;

            if (!RetrieveTasks.isEmpty()) {
                for (var entry : RetrieveTasks.entrySet()) {
                    entry.getValue().stop();
                }
                RetrieveTasks.clear();
            }

            if (!subPanels.isEmpty()) {
                for (var entry : subPanels.entrySet()) {
                    entry.getKey().getChildren().clear();
                }
                subPanels.clear();
            }

            embeddedMediaPlayer.media().prepare(selectedFile.getAbsolutePath());
            embeddedMediaPlayer.media().parsing().parse();
            embeddedMediaPlayer.media().play(selectedFile.getAbsolutePath());

            playbackBtn.setText("Pause");

            try {
                currentVideoMedia = videoDao.findByNameAndPath(mediaPath, mediaName);
            } catch (Exception ex) {
                currentVideoMedia = new Video(mediaPath, mediaName);
                videoDao.persist(currentVideoMedia);
            }

        }
    }

    @FXML
    void addSubBtnAction() {
        File selectedFile = fileChooserSubtitles.showOpenDialog(null);
        if (selectedFile != null) {
            Set<Subtitle> subtitles = SRTParser.getSubtitlesFromFile(selectedFile.getAbsolutePath());
            CheckMenuItem item = new CheckMenuItem(selectedFile.getName().substring(0, selectedFile.getName().lastIndexOf(".")));
            item.setUserData(createSubtitlePane());
            item.setOnAction(event -> {
                FlowPane subtitlePane = (FlowPane) item.getUserData();
                if (item.isSelected()) {
                    if (selectedSubTracks < SUBTITLES_ALLOWED) {
                        selectedSubTracks++;
                    } else {
                        item.setSelected(false);
                        return;
                    }
                    subtitlesBox.getChildren().add(0, subtitlePane);
                    subPanels.put(subtitlePane, subtitles);
                } else {
                    selectedSubTracks--;
                    subtitlePane.getChildren().clear();
                    subtitlesBox.getChildren().remove(subtitlePane);
                    subPanels.remove(subtitlePane);
                }
            });

            subTracks.getItems().add(item);
        }
    }

    @FXML
    void dictionaryBtnAction() {
        double w = stage.getWidth();
        double h = stage.getHeight();
        mainPane.setRight(null);
        stage.setScene(dictionaryScene);
        stage.setWidth(w);
        stage.setHeight(h);
        stage.show();
    }

    @FXML
    void addAllBtnAction() {
        Word findedWord;
        try {
            findedWord = wordDao.findByName(selectedWord.getText());
        } catch (Exception ex) { findedWord = new Word(selectedWord.getText()); }

        for (Node node : translations.getChildren()) {
            Button activeTranslation = (Button) node;

            Long exist = translationDao.isExistByTranslationNameAndWordName(activeTranslation.getText(), selectedWord.getText());
            if (exist == 0) {
                activeTranslation.setBackground(Background.fill(Colors.ADDED));
                activeTranslation.setUserData(true);
                findedWord.addTranslation(new Translation(activeTranslation.getText(), findedWord));
            }
        }

        currentVideoMedia.addWord(findedWord);
        videoDao.merge(currentVideoMedia);
    }

    @FXML
    void mediaPaneKeyPressed(KeyEvent event) {
        if (event.getCode().isWhitespaceKey()) {
            if (embeddedMediaPlayer.status().isPlaying()) {
                embeddedMediaPlayer.controls().pause();
                playbackBtn.setText("Play");
            } else {
                embeddedMediaPlayer.controls().play();
                playbackBtn.setText("Pause");
            }
        }

        if (event.getCode() == KeyCode.LEFT) {
            embeddedMediaPlayer.controls().setPosition(embeddedMediaPlayer.status().position() - 0.01f);
            currentTimeLbl.setText(formatTime(embeddedMediaPlayer.status().time()));
            timelineSlider.setValue((int) (embeddedMediaPlayer.status().position() * 1000.0f));
        } else if (event.getCode() == KeyCode.RIGHT) {
            embeddedMediaPlayer.controls().setPosition(embeddedMediaPlayer.status().position() + 0.01f);
            currentTimeLbl.setText(formatTime(embeddedMediaPlayer.status().time()));
            timelineSlider.setValue((int) (embeddedMediaPlayer.status().position() * 1000.0f));
        }
    }

    @FXML
    void mediaPaneMouseClicked(MouseEvent event) {
        if (event.getButton().equals(MouseButton.PRIMARY) && !(event.getTarget() instanceof Text)) {
            if (event.getClickCount() == 2) {
                if (stage.isFullScreen()) {
                    mainPane.setTop(buttonBar);
                    mainPane.setBottom(controlsPane);
                    stage.setFullScreen(false);
                } else {
                    mainPane.setTop(null);
                    mainPane.setBottom(null);
                    stage.setFullScreen(true);
                }
            }
            mainPane.setRight(null);
            translations.getChildren().clear();

            mediaPane.requestFocus();
        }
    }

    @FXML
    void muteBtnAction() {
        if (embeddedMediaPlayer.audio().isMute()) {
            muteBtn.setText("Mute");
            embeddedMediaPlayer.audio().setMute(false);
        } else {
            muteBtn.setText("UnMute");
            embeddedMediaPlayer.audio().setMute(true);
        }
    }

    @FXML
    void mainPaneKeyReleased() {
        if (collectingToPanel && userDataUpdate.size() != 0) {
            String string = "";
            for (Text t : userDataUpdate) {
                string += t.getText() + " ";
                t.setFill(Colors.DEFAULT);
            }
            translations.getChildren().add(createActiveTranslation(string));
            userDataUpdate.clear();
            collectingToPanel = false;
            return;
        }

        if (collecting && userDataUpdate.size() != 0) {
            String string = "";
            for (Text t : userDataUpdate) {
                string += t.getText() + " ";
                t.setFill(Colors.DEFAULT);
            }
            showTranslations(string);
            userDataUpdate.clear();
            collecting = false;
        }

    }

    @FXML
    void mainPaneKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            collecting = true;
            return;
        }

        if (event.isAltDown()) {
            collectingToPanel = true;
        }
    }

}
