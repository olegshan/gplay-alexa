package com.olegshan.gplayalexa.meta;

import com.amazon.speech.speechlet.interfaces.audioplayer.AudioItem;

public class Song extends AudioItem {
    private MetaData metaData;

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }
}
