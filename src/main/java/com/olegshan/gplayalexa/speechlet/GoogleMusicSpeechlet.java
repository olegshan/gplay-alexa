package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.speechlet.interfaces.audioplayer.*;
import com.amazon.speech.speechlet.interfaces.audioplayer.Error;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.speechlet.interfaces.audioplayer.request.*;
import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.model.Album;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.enums.ResultType;
import com.github.felixgail.gplaymusic.model.enums.StreamQuality;
import com.github.felixgail.gplaymusic.model.requests.SearchTypes;
import com.github.felixgail.gplaymusic.util.TokenProvider;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import svarzee.gps.gpsoauth.AuthToken;
import svarzee.gps.gpsoauth.Gpsoauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.olegshan.gplayalexa.speechlet.SpeechletConstants.*;
import static com.olegshan.gplayalexa.speechlet.SpeechletResponses.*;
import static java.util.Collections.singletonList;

public class GoogleMusicSpeechlet implements SpeechletV2, AudioPlayer {

    private GPlayMusic  api;
    private List<Track> tracks = new ArrayList<>();
    private int         currentTrack;

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        prepareApi();
        logMethodStart("onSessionStarted", requestEnvelope);
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        logMethodStart("onLaunch", requestEnvelope);
        return newAskResponse(WELCOME_TEXT);
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        logMethodStart("onIntent", requestEnvelope);
        Intent intent = requestEnvelope.getRequest().getIntent();
        String name = intent.getName();
        log.info("Requested intent: {}", name);

        switch (name) {
            case SINGLE_SONG:
                String song = intent.getSlot(SONG_SLOT).getValue();
                try {
                    return playSingleSong(song);
                } catch (Exception e) {
                    log.error("Couldn't play {}", song, e);
                    return newAskResponse(ERROR);
                }
            case ALBUM:
                String album = intent.getSlot(ALBUM_SLOT).getValue();
                try {
                    return playAlbum(album);
                } catch (Exception e) {
                    log.error("Couldn't play album {}", album, e);
                    return newAskResponse(ERROR);
                }
            case "AMAZON.StopIntent":
            case "AMAZON.CancelIntent":
                return stopResponse();
            case "AMAZON.NextIntent":
                return playNextSong(++currentTrack);
            case "AMAZON.PreviousIntent":
                return playPreviousSong(--currentTrack);
            default:
                log.error("Unexpected intent: " + name);
                return newAskResponse(WRONG_REQUEST);
        }
    }

    @Override
    public SpeechletResponse onPlaybackNearlyFinished(SpeechletRequestEnvelope<PlaybackNearlyFinishedRequest> requestEnvelope) {
        logMethodStart("onPlaybackNearlyFinished", requestEnvelope);

        if (tracks.size() <= currentTrack + 1) {
            log.info("No next track will be played. Tracks list size: {}, current track: {}", tracks.size(), currentTrack);
            return null;
        }

        Track track = tracks.get(currentTrack + 1);
        String previousToken = tracks.get(currentTrack).getID();
        Directive directive = preparePlayDirective(track, PlayBehavior.ENQUEUE, previousToken);

        SpeechletResponse response = new SpeechletResponse();
        response.setDirectives(singletonList(directive));

        currentTrack++;
        log.info("Next song to play: {}", track.getTitle());
        return response;
    }

    @Override
    public SpeechletResponse onPlaybackStarted(SpeechletRequestEnvelope<PlaybackStartedRequest> requestEnvelope) {
        logMethodStart("onPlaybackStarted", requestEnvelope);
        return null;
    }

    @Override
    public SpeechletResponse onPlaybackStopped(SpeechletRequestEnvelope<PlaybackStoppedRequest> requestEnvelope) {
        logMethodStart("onPlaybackStopped", requestEnvelope);
        return null;
    }

    @Override
    public SpeechletResponse onPlaybackFailed(SpeechletRequestEnvelope<PlaybackFailedRequest> requestEnvelope) {
        logMethodStart("onPlaybackFailed", requestEnvelope);
        Error error = requestEnvelope.getRequest().getError();
        log.error("ERROR: {}, {}", error.getType(), error.getMessage());
        return null;
    }

    @Override
    public SpeechletResponse onPlaybackFinished(SpeechletRequestEnvelope<PlaybackFinishedRequest> requestEnvelope) {
        logMethodStart("onPlaybackFinished", requestEnvelope);
        return null;
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        logMethodStart("onSessionEnded", requestEnvelope);
    }

    private SpeechletResponse playSingleSong(String songRequest) throws Exception {
        log.info("Single song request: {}", songRequest);
        tracks.clear();
        currentTrack = 0;

        List<Track> trackList = api.getTrackApi().search(songRequest, 1);
        if (trackList.isEmpty())
            return songNotFoundResponse(songRequest);

        Track track = trackList.get(0);
        Directive directive = preparePlayDirective(track);
        return playResponse(track.getTitle(), track.getArtist(), directive);
    }

    private SpeechletResponse playAlbum(String albumRequest) throws Exception {
        log.info("Album request: {}", albumRequest);

        List<Album> albums = api.search(albumRequest, 1, new SearchTypes(ResultType.ALBUM)).getAlbums();
        if (albums.isEmpty())
            return albumNotFoundResponse(albumRequest);

        Album album = api.getAlbum(albums.get(0).getAlbumId(), true);

        tracks = album.getTracks().orElseThrow(() -> new RuntimeException("No tracks in album " + album.getName()));
        currentTrack = 0;

        Directive directive = preparePlayDirective(tracks.get(currentTrack));
        return playResponse("album " + album.getName(), album.getAlbumArtist(), directive);
    }

    private SpeechletResponse playNextSong(int trackNumber) {
        if (tracks.isEmpty())
            return emptyListResponse("next");

        Track track;
        if (tracks.size() <= trackNumber) {
            currentTrack = 0;
            track = tracks.get(currentTrack);
        } else {
            track = tracks.get(trackNumber);
        }

        Directive directive = preparePlayDirective(track);
        return playResponse(track.getTitle(), track.getArtist(), directive);
    }

    private SpeechletResponse playPreviousSong(int trackNumber) {
        if (tracks.isEmpty())
            return emptyListResponse("previous");

        Track track;
        if (trackNumber < 0) {
            currentTrack = tracks.size() - 1;
            track = tracks.get(currentTrack);
        } else {
            track = tracks.get(trackNumber);
        }

        Directive directive = preparePlayDirective(track);
        return playResponse(track.getTitle(), track.getArtist(), directive);
    }

    private String getStreamUrl(Track track) {
        try {
            return track.getStreamURL(StreamQuality.HIGH).toString();
        } catch (IOException e) {
            log.error("Error getting stream url for track {} ", track.getTitle());
            throw new RuntimeException(e);
        }
    }

    private Directive preparePlayDirective(Track track) {
        return preparePlayDirective(track, PlayBehavior.REPLACE_ALL, null);
    }

    private Directive preparePlayDirective(Track track, PlayBehavior playBehavior, String previousToken) {
        Stream stream = new Stream();
        stream.setUrl(getStreamUrl(track));
        stream.setExpectedPreviousToken(previousToken);
        stream.setToken(track.getID());

        AudioItem song = new AudioItem();
        song.setStream(stream);

        PlayDirective directive = new PlayDirective();
        directive.setAudioItem(song);
        directive.setPlayBehavior(playBehavior);
        return directive;
    }

    private void prepareApi() {
        if (api != null)
            return;

        prepareLogger();

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

    private void logMethodStart(String methodName, SpeechletRequestEnvelope<? extends SpeechletRequest> request) {
        Session session = request.getSession();
        log.info("METHOD START: {} with requestId {} and sessionId {}", methodName, request.getRequest().getRequestId(),
            session != null ? session.getSessionId() : null);
    }

    private static final Logger log = LoggerFactory.getLogger(GoogleMusicSpeechlet.class);
}
