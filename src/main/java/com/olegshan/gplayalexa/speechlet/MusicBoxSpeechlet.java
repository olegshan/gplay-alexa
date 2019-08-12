package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.speechlet.interfaces.audioplayer.PlayBehavior;
import com.amazon.speech.speechlet.interfaces.audioplayer.Stream;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.enums.StreamQuality;
import com.github.felixgail.gplaymusic.util.TokenProvider;
import com.olegshan.gplayalexa.meta.MetaData;
import com.olegshan.gplayalexa.meta.Song;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svarzee.gps.gpsoauth.AuthToken;
import svarzee.gps.gpsoauth.Gpsoauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicBoxSpeechlet implements SpeechletV2 {

    private static final String WELCOME_TEXT            = "Welcome to Google Play Music skill.";
    private static final String CHOOSE_THE_SONG_REQUEST = "Say 'play' and then name the artist and the song.";
    private static final String WRONG_REQUEST           = "Sorry, I didn't get that.";
    private static final String ERROR                   = "Sorry, something went wrong. Please try again.";
    private static final String GOOGLE_MUSIC            = "GoogleMusic";
    private static final String SONG_SLOT               = "song";

    private SpeechletResponse launchResponse;

    private GPlayMusic api;

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted with requestId {} and sessionId {}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());
        launchResponse = newAskRequest(WELCOME_TEXT);

        AuthToken token = null;
        try {
            token = TokenProvider.provideToken(System.getenv("USER_NAME"), System.getenv("USER_PASSWORD"), System.getenv("IMEI"));
        } catch (IOException | Gpsoauth.TokenRequestFailed e) {
            e.printStackTrace();
        }

        api = new GPlayMusic.Builder()
            .setAuthToken(token)
            .build();
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch with requestId {} and sessionId {}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());
        return launchResponse;
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        Intent intent = request.getIntent();
        String name = intent.getName();
        log.info("onIntent with requestId {}, sessionId {} and intent {}", request.getRequestId(), requestEnvelope.getSession().getSessionId(), name);

        if (GOOGLE_MUSIC.equals(name)) {
            String song = intent.getSlot(SONG_SLOT).getValue();
            try {
                return playMusicResponse(song);
            } catch (Exception e) {
                log.error("Couldn't play {}", song, e);
                return newAskRequest(ERROR);
            }
        } else {
            return newAskRequest(WRONG_REQUEST);
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded with requestId {} and sessionId {}", requestEnvelope.getRequest().getRequestId(), requestEnvelope.getSession().getSessionId());
    }

    private SpeechletResponse playMusicResponse(String songRequest) throws Exception {
        List<Track> trackList = api.getTrackApi().search(songRequest, 1);
        if (trackList.isEmpty())
            return noTrackFoundResponse(songRequest);

        Song song = new Song();
        Track track = trackList.get(0);
        String trackTitle = track.getTitle();
        String artist = track.getArtist();

        MetaData metaData = new MetaData();
        metaData.setTitle(trackTitle);
        metaData.setSubTitle(artist);
        song.setMetaData(metaData);

        Stream stream = new Stream();
        stream.setUrl(track.getStreamURL(StreamQuality.HIGH).toString());
        stream.setOffsetInMilliseconds(0);
        song.setStream(stream);

        PlayDirective directive = new PlayDirective();
        directive.setAudioItem(song);
        directive.setPlayBehavior(PlayBehavior.REPLACE_ALL);

        List<Directive> directives = new ArrayList<>();
        directives.add(directive);

        SpeechletResponse response = new SpeechletResponse();
        response.setNullableShouldEndSession(null);
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Playing " + trackTitle + " by " + artist);
        response.setOutputSpeech(speech);
        response.setDirectives(directives);

        return null;
    }

    private SpeechletResponse noTrackFoundResponse(String songRequest) {
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

    private static final Logger log = LoggerFactory.getLogger(MusicBoxSpeechlet.class);
}
