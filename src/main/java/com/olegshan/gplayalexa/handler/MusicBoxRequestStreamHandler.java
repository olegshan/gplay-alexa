package com.olegshan.gplayalexa.handler;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;
import com.olegshan.gplayalexa.speechlet.MusicBoxSpeechlet;

import java.util.HashSet;
import java.util.Set;

public class MusicBoxRequestStreamHandler extends SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds = new HashSet<>();

    static {
        supportedApplicationIds.add("amzn1.ask.skill.da7a7858-5bf8-46be-a12a-30f85a7b3283");
    }

    public MusicBoxRequestStreamHandler() {
        super(new MusicBoxSpeechlet(), supportedApplicationIds);
    }
}
