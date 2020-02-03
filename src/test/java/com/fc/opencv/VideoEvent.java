package com.fc.opencv;

import java.awt.image.BufferedImage;

public class VideoEvent {

    private Long time;
    private BufferedImage bi;
    private VideoStorage storage;

    public VideoEvent(BufferedImage bi,VideoStorage storage,Long time){
        this.bi = bi;
        this.storage =storage;
        this.time = time;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public BufferedImage getBi() {
        return bi;
    }

    public void setBi(BufferedImage bi) {
        this.bi = bi;
    }

    public VideoStorage getStorage() {
        return storage;
    }

    public void setStorage(VideoStorage storage) {
        this.storage = storage;
    }
}
