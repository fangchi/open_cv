package com.fc.opencv;

import org.bytedeco.javacv.CanvasFrame;

import java.awt.image.BufferedImage;

public class ShowImageEvent {

    private CanvasFrame canvasFrame;

    private BufferedImage image;

    public ShowImageEvent(CanvasFrame canvasFrame, BufferedImage image) {
        this.canvasFrame = canvasFrame;
        this.image = image;
    }

    public CanvasFrame getCanvasFrame() {
        return canvasFrame;
    }

    public void setCanvasFrame(CanvasFrame canvasFrame) {
        this.canvasFrame = canvasFrame;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
