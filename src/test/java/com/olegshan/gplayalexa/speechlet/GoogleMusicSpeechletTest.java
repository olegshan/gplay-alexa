package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import org.junit.Before;
import org.junit.Test;

import static com.olegshan.gplayalexa.speechlet.SpeechletConstants.CHOOSE_THE_SONG_REQUEST;
import static com.olegshan.gplayalexa.speechlet.SpeechletConstants.WELCOME_TEXT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GoogleMusicSpeechletTest {

    private static final String TEST_SESSION_ID = "sessionId";

    private static GoogleMusicSpeechlet speechlet;
    private static Session              session;

    @Before
    public void setUp() {
        speechlet = new GoogleMusicSpeechlet();
        session = buildSession();
    }

    @Test
    public void onLaunch() {
        SpeechletResponse response = speechlet.onLaunch(buildLaunchRequestEnvelope());
        PlainTextOutputSpeech outputSpeech = (PlainTextOutputSpeech) response.getOutputSpeech();
        PlainTextOutputSpeech repromptSpeech = (PlainTextOutputSpeech) response.getReprompt().getOutputSpeech();

        assertNotNull(outputSpeech);
        assertNotNull(repromptSpeech);
        assertEquals(WELCOME_TEXT, outputSpeech.getText());
        assertEquals(CHOOSE_THE_SONG_REQUEST, repromptSpeech.getText());
    }

    @Test
    public void onIntent() {

    }

    private SpeechletRequestEnvelope<LaunchRequest> buildLaunchRequestEnvelope() {
        return SpeechletRequestEnvelope.<LaunchRequest>builder()
            .withRequest(buildLaunchRequest())
            .withSession(session)
            .build();
    }

    private Session buildSession() {
        return Session.builder().withSessionId(TEST_SESSION_ID).build();
    }

    private static LaunchRequest buildLaunchRequest() {
        return LaunchRequest.builder()
            .withRequestId("requestId")
            .build();
    }
}