package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.StopDirective;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.enums.StreamQuality;
import com.github.felixgail.gplaymusic.util.TokenProvider;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svarzee.gps.gpsoauth.AuthToken;
import svarzee.gps.gpsoauth.Gpsoauth;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;

public class MusicBoxSpeechlet implements SpeechletV2 {

    private static final String WELCOME_TEXT            = "Welcome to Google Music skill.";
    private static final String CHOOSE_THE_SONG_REQUEST = "Say 'play' and then name the artist and the song.";
    private static final String WRONG_REQUEST           = "Sorry, I didn't get that.";
    private static final String ERROR                   = "Sorry, something went wrong. Please try again.";

    private static final String GOOGLE_MUSIC_INTENT = "GoogleMusic";
    private static final String SONG_SLOT           = "song";

    private GPlayMusic api;

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        prepareLogger();
        logMethodStart("onSessionStarted", requestEnvelope);
        prepareApi();
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        logMethodStart("onLaunch", requestEnvelope);
        return newAskRequest(WELCOME_TEXT);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        logMethodStart("onIntent", requestEnvelope);
        IntentRequest request = requestEnvelope.getRequest();
        Intent intent = request.getIntent();
        String name = intent.getName();
        log.info("Requested intent: {}", name);

        switch (name) {
            case GOOGLE_MUSIC_INTENT:
                String song = intent.getSlot(SONG_SLOT).getValue();
                try {
                    return playMusicResponse(song);
                } catch (Exception e) {
                    log.error("Couldn't play {}", song, e);
                    return newAskRequest(ERROR);
                }
            case "AMAZON.StopIntent":
            case "AMAZON.CancelIntent":
                return goodbye();
            default:
                log.error("Unexpected intent: " + name);
                return newAskRequest(WRONG_REQUEST);
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        logMethodStart("onSessionEnded", requestEnvelope);
    }

    private SpeechletResponse playMusicResponse(String songRequest) throws Exception {
        List<Track> trackList = api.getTrackApi().search(songRequest, 1);
        if (trackList.isEmpty())
            return noTrackFoundResponse(songRequest);

        Track track = trackList.get(0);

        Stream stream = new Stream();
        stream.setUrl(track.getStreamURL(StreamQuality.HIGH).toString());
        stream.setOffsetInMilliseconds(0);
        stream.setExpectedPreviousToken(null);
        stream.setToken("0");

        AudioItem song = new AudioItem();
        song.setStream(stream);

        PlayDirective directive = new PlayDirective();
        directive.setAudioItem(song);
        directive.setPlayBehavior(PlayBehavior.REPLACE_ALL);

        SpeechletResponse response = new SpeechletResponse();
        response.setDirectives(singletonList(directive));
        response.setNullableShouldEndSession(null);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Playing " + track.getTitle() + " by " + track.getArtist());

        response.setOutputSpeech(speech);

        return response;
    }

    private SpeechletResponse noTrackFoundResponse(String songRequest) {
        log.info("No track found for request [{}]", songRequest);

        SpeechletResponse response = new SpeechletResponse();
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Sorry, I couldn't find a song by request " + songRequest);
        response.setOutputSpeech(speech);

        return response;
    }

    private SpeechletResponse newAskRequest(String text) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText(CHOOSE_THE_SONG_REQUEST);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    private SpeechletResponse goodbye() {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Goodbye");
        SpeechletResponse response = SpeechletResponse.newTellResponse(speech);
        response.setDirectives(singletonList(new StopDirective()));

        return response;
    }

    private void logMethodStart(String methodName, SpeechletRequestEnvelope<? extends SpeechletRequest> request) {
        log.info("{} with requestId {} and sessionId {}", methodName, request.getRequest().getRequestId(), request.getSession().getSessionId());
    }

    private void prepareApi() {
        AuthToken token = null;
        try {
            token = TokenProvider.provideToken(System.getenv("USER_NAME"), System.getenv("USER_PASSWORD"), System.getenv("IMEI"));
        } catch (IOException | Gpsoauth.TokenRequestFailed e) {
            log.error("Error while auth token generating", e);
        }

        api = new GPlayMusic.Builder()
            .setAuthToken(token)
            .build();

        log.info("Successfully logged into Google Music.");
    }

    private void prepareLogger() {
        String log4jConfPath = "log4j.properties";
        PropertyConfigurator.configure(log4jConfPath);
    }

    private static final Logger log = LoggerFactory.getLogger(MusicBoxSpeechlet.class);
}
