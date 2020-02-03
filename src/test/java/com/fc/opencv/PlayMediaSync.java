package com.fc.opencv;

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 播放器同步处理
 * 造成视频帧卡顿
 */
public class PlayMediaSync {

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
        String fileName = "/Users/fangchi/live/iPc.me-TheAuroraNorthernLights/我只在乎你.mp4";
        //String fileName = "/Users/fangchi/Music/网易云音乐/i2star - 湖光水色调.mp3";
        //String fileName = "rtsp://192.168.38.137:8554/ff";
        float vol = 1f;//音量
        new PlayMediaSync(fileName,vol);

    }

    /**
     * 播放
     * @param path 路径
     * @param vol  音量
     * @throws FrameGrabber.Exception
     * @throws InterruptedException
     */
    public PlayMediaSync(String path, float vol) throws FrameGrabber.Exception, InterruptedException {
        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(path);
        fg.setOption("rtsp_transport", "udp");
        fg.setImageWidth(800);
        fg.setImageHeight(600);

        fg.start();
        printMetaInfo(fg);
        //初始化幕布
        CanvasFrame canvasFrame = new CanvasFrame(path);
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvasFrame.setAlwaysOnTop(true);
        //初始化视频帧存储
        VideoStorage storage = new VideoStorage();
        //初始化音频
        SourceDataLine sourceDataLine = initSourceDataLine(fg);
        //获取视频帧率
        int frameRate = (int) fg.getFrameRate();
        new Thread(() -> {
            long i = 0;
            while (true) {
                i++;
                //af = fg.grabSamples(); //仅拉取音频帧
                try {
                    Frame vf = fg.grabFrame(true, true, true, false);
                    if (vf == null) {
                        fg.stop();
                        System.exit(0);
                    }
                    //处理音频
                    processAudio(fg, vf, vol, sourceDataLine, i);
                    //处理视频
                    processVideo(storage, fg, vf, i,canvasFrame);
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        while (true) {
            Thread.sleep(1000);
        }
    }

    /**
     * 处理视频
     *
     * @param storage
     * @param fg
     * @param f          帧
     * @param frameIndex 帧index
     */
    private void processVideo(VideoStorage storage, FFmpegFrameGrabber fg, Frame f, long frameIndex,CanvasFrame canvasFrame) {
        BufferedImage bi = (new Java2DFrameConverter()).getBufferedImage(f);
        if (bi != null) {
            ImageIcon ii = new ImageIcon(bi);
            //打水印
            Util.mark(bi, ii.getImage(), "Hello RTSP current :" + Util.getTimeString(fg.getTimestamp()), new Font("宋体", Font.PLAIN, 20), Color.WHITE, 20, 20);
            canvasFrame.showImage(bi);
        } else {
            //System.out.println("%%%%第"+frameIndex+"帧为视频空帧");
        }
    }


    /**
     * 处理音频
     *
     * @param fg
     * @param f
     * @param vol
     * @param sourceDataLine
     * @param frameindex
     */
    private void processAudio(FFmpegFrameGrabber fg, Frame f, float vol, SourceDataLine sourceDataLine, long frameindex) {
        int k;
        Buffer[] buf = f.samples;
        if (buf != null) {
            FloatBuffer leftData, rightData;
            ShortBuffer ILData, IRData;
            ByteBuffer TLData, TRData;
            byte[] tl, tr;
            byte[] combine;
            int sampleFormat = fg.getSampleFormat();
            switch (sampleFormat) {
                case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开。
                    leftData = (FloatBuffer) buf[0];
                    TLData = Util.floatToByteValue(leftData, vol);
                    rightData = (FloatBuffer) buf[1];
                    TRData = Util.floatToByteValue(rightData, vol);
                    tl = TLData.array();
                    tr = TRData.array();
                    combine = new byte[tl.length + tr.length];
                    k = 0;
                    for (int i = 0; i < tl.length; i = i + 2) {//混合两个声道。
                        for (int j = 0; j < 2; j++) {
                            combine[j + 4 * k] = tl[i + j];
                            combine[j + 2 + 4 * k] = tr[i + j];
                        }
                        k++;
                    }
                    sourceDataLine.write(combine, 0, combine.length);
                    break;
                case avutil.AV_SAMPLE_FMT_S16://非平面型左右声道在一个buffer中。
                    ILData = (ShortBuffer) buf[0];
                    TLData = Util.shortToByteValue(ILData, vol);
                    tl = TLData.array();
                    sourceDataLine.write(tl, 0, tl.length);
                    break;
                case avutil.AV_SAMPLE_FMT_FLT://float非平面型
                    leftData = (FloatBuffer) buf[0];
                    TLData = Util.floatToByteValue(leftData, vol);
                    tl = TLData.array();
                    sourceDataLine.write(tl, 0, tl.length);
                    break;
                case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
                    ILData = (ShortBuffer) buf[0];
                    IRData = (ShortBuffer) buf[1];
                    TLData = Util.shortToByteValue(ILData, vol);
                    TRData = Util.shortToByteValue(IRData, vol);
                    tl = TLData.array();
                    tr = TRData.array();
                    combine = new byte[tl.length + tr.length];
                    k = 0;
                    for (int i = 0; i < tl.length; i = i + 2) {
                        for (int j = 0; j < 2; j++) {
                            combine[j + 4 * k] = tl[i + j];
                            combine[j + 2 + 4 * k] = tr[i + j];
                        }
                        k++;
                    }
                    sourceDataLine.write(combine, 0, combine.length);
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "unsupport audio format", "unsupport audio format", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                    break;
            }
        } else {
            //System.out.println("￥￥￥第"+frameindex+"帧为音频空帧");
        }
    }

    /**
     * 初始话
     *
     * @param fg
     */
    private SourceDataLine initSourceDataLine(FFmpegFrameGrabber fg) {
        SourceDataLine sourceDataLine = null;
        AudioFormat af = null;
        af = PlayMedia.getAudioFormat(fg, af);
        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class,
                af, AudioSystem.NOT_SPECIFIED);
        try {
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(af);
            sourceDataLine.start();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return sourceDataLine;
    }


    private void printMetaInfo(FFmpegFrameGrabber fg) {
        Util.printMetaInfo(fg);
    }

}