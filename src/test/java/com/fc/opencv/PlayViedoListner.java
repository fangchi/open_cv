package com.fc.opencv;

import com.google.common.eventbus.Subscribe;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class PlayViedoListner {

    @Subscribe
    public void listen(VideoEvent event) {
        VideoStorage storage = event.getStorage();
        BufferedImage bi = event.getBi();
        ImageIcon ii = new ImageIcon(bi);
        //打水印
        Util.mark(bi, ii.getImage(), "Hello RTSP current :" + Util.getTimeString(event.getTime()), new Font("宋体", Font.PLAIN, 20), Color.WHITE, 20, 20);
        storage.produce(bi);
    }


    @Subscribe
    public void listen(ShowImageEvent event) {
        event.getCanvasFrame().showImage(event.getImage());
    }

}
