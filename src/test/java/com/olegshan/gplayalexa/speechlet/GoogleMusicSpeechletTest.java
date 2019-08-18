package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.PlayDirective;
import com.amazon.speech.ui.OutputSpeech;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.api.TrackApi;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.enums.StreamQuality;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URL;

import static com.olegshan.gplayalexa.speechlet.SpeechletConstants.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GoogleMusicSpeechletTest {
    private static final String TEST_SESSION_ID = "sessionId";
    private static final String TEST_REQUEST_ID = "requestId";

    @Mock
    private GPlayMusic googleApiMock;
    @Mock
    private TrackApi   trackApiMock;
    @Mock
    private Track      trackMock;

    private static Session session;

    @InjectMocks
    private final GoogleMusicSpeechlet speechlet = new GoogleMusicSpeechlet();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        session = buildSession();

        when(googleApiMock.getTrackApi()).thenReturn(trackApiMock);
    }

    @Test
    public void onLaunch() {
        SpeechletResponse response = speechlet.onLaunch(buildLaunchRequestEnvelope());
        checkOutputSpeech(response.getOutputSpeech(), WELCOME_TEXT);
        checkOutputSpeech(response.getReprompt().getOutputSpeech(), CHOOSE_THE_SONG_REQUEST);
    }

    @Test
    public void onIntentWithCorrectRequest() throws Exception {
        String songRequest = "Metallica The Unforgiven";
        String streamUrl = "https://stream_url.com";

        when(trackApiMock.search(songRequest, 1))
            .thenReturn(singletonList(trackMock));

        when(trackMock.getTitle())
            .thenReturn("The Unforgiven");

        when(trackMock.getArtist())
            .thenReturn("Metallica");

        when(trackMock.getStreamURL(any()))
            .thenReturn(new URL(streamUrl));

        SpeechletResponse response = speechlet.onIntent(
            buildIntentRequestEnvelope(
                GOOGLE_MUSIC_INTENT,
                SONG_SLOT,
                songRequest
            )
        );

        verify(trackApiMock).search(songRequest, 1);
        verify(trackMock).getTitle();
        verify(trackMock).getArtist();
        verify(trackMock).getStreamURL(StreamQuality.HIGH);

        checkOutputSpeech(response.getOutputSpeech(), "Playing The Unforgiven by Metallica");

        assertNotNull(response.getDirectives());
        assertEquals(1, response.getDirectives().size());
        assertTrue(response.getDirectives().get(0) instanceof PlayDirective);

        PlayDirective directive = (PlayDirective) response.getDirectives().get(0);
        assertNotNull(directive.getAudioItem());
        assertEquals(streamUrl, directive.getAudioItem().getStream().getUrl());
    }

    @Test
    public void onIntentWithNotCorrectRequest() throws Exception {
        String songRequest = "Some song that can't be found";

        when(trackApiMock.search(songRequest, 1))
            .thenReturn(emptyList());

        SpeechletResponse response = speechlet.onIntent(
            buildIntentRequestEnvelope(
                GOOGLE_MUSIC_INTENT,
                SONG_SLOT,
                songRequest
            )
        );

        verify(trackApiMock).search(songRequest, 1);
        verifyZeroInteractions(trackMock);

        checkOutputSpeech(response.getOutputSpeech(), "Sorry, I couldn't find a song by request " + songRequest);
        assertNull(response.getDirectives());
    }

    private void checkOutputSpeech(OutputSpeech outputSpeech, String speechText) {
        PlainTextOutputSpeech plainTextOutputSpeech = (PlainTextOutputSpeech) outputSpeech;

        assertNotNull(outputSpeech);
        assertEquals(speechText, plainTextOutputSpeech.getText());
    }

    private SpeechletRequestEnvelope<LaunchRequest> buildLaunchRequestEnvelope() {
        return SpeechletRequestEnvelope.<LaunchRequest>builder()
            .withRequest(buildLaunchRequest())
            .withSession(session)
            .build();
    }

    private SpeechletRequestEnvelope<IntentRequest> buildIntentRequestEnvelope(
        String intentName,
        String slotName,
        String slotValue
    ) {
        return SpeechletRequestEnvelope.<IntentRequest>builder()
            .withRequest(buildIntentRequest(intentName, slotName, slotValue))
            .withSession(session)
            .build();
    }

    private static LaunchRequest buildLaunchRequest() {
        return LaunchRequest.builder()
            .withRequestId(TEST_REQUEST_ID)
            .build();
    }

    private static IntentRequest buildIntentRequest(String intentName, String slotName, String slotValue) {
        Slot slot = Slot.builder()
            .withName(slotName)
            .withValue(slotValue)
            .build();

        Intent intent = Intent.builder()
            .withName(intentName)
            .withSlot(slot)
            .build();

        return IntentRequest.builder()
            .withRequestId(TEST_REQUEST_ID)
            .withIntent(intent)
            .build();
    }

    private Session buildSession() {
        return Session.builder().withSessionId(TEST_SESSION_ID).build();
    }
}