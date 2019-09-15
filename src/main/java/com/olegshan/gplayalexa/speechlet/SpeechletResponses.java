package com.olegshan.gplayalexa.speechlet;

import com.amazon.speech.speechlet.Directive;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.interfaces.audioplayer.directive.StopDirective;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.olegshan.gplayalexa.speechlet.SpeechletConstants.CHOOSE_THE_MUSIC_REQUEST;
import static java.util.Collections.singletonList;

class SpeechletResponses {

    static SpeechletResponse playResponse(String item, String artist, Directive directive) {
        SpeechletResponse response = new SpeechletResponse();
        response.setDirectives(singletonList(directive));
        response.setNullableShouldEndSession(true);

        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText("Playing " + item + " by " + artist);
        response.setOutputSpeech(speech);

        return response;
    }

    static SpeechletResponse newAskResponse(String text) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);
        PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
        repromptSpeech.setText(CHOOSE_THE_MUSIC_REQUEST);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptSpeech);

        return SpeechletResponse.newAskResponse(speech, reprompt);
    }

    static SpeechletResponse songNotFoundResponse(String request) {
        return notFoundResponse("Sorry, I couldn't find a song by request " + request);
    }

    static SpeechletResponse albumNotFoundResponse(String request) {
        return notFoundResponse("Sorry, I couldn't find an album by request " + request);
    }

    static SpeechletResponse emptyListResponse(String type) {
        return notFoundResponse("Sorry, I couldn't play " + type + " song because the track list is empty");
    }

    private static SpeechletResponse notFoundResponse(String text) {
        log.error(text);
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(text);
        return SpeechletResponse.newTellResponse(speech);
    }

    static SpeechletResponse stopResponse() {
        SpeechletResponse response = new SpeechletResponse();
        response.setDirectives(singletonList(new StopDirective()));

        return response;
    }

    private static final Logger log = LoggerFactory.getLogger(SpeechletResponses.class);
}
