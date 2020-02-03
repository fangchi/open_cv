package com.fc.opencv;

import com.google.common.eventbus.EventBus;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * 播放器
 */
public class PlayMedia {

    //https://www.jb51.net/article/131329.htm
    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
        String fileName = "/Users/fangchi/live/iPc.me-TheAuroraNorthernLights/我只在乎你.mp4";
        //String fileName = "/Users/fangchi/Music/网易云音乐/i2star - 湖光水色调.mp3";
        //String fileName = "rtsp://192.168.38.137:8554/ff";
        float vol = 1.2f;//音量
        new PlayMedia(fileName,vol);

    }

    /**
     * 播放
     * @param path 路径
     * @param vol  音量
     * @throws FrameGrabber.Exception
     * @throws InterruptedException
     */
    public PlayMedia(String path,float vol) throws FrameGrabber.Exception, InterruptedException {
        FFmpegFrameGrabber fg = new FFmpegFrameGrabber(path);
        //针对rtsp流
        fg.setOption("rtsp_transport", "udp");
//        fg.setImageWidth(fg.getImageWidth());
//        fg.setImageHeight(fg.getImageHeight());
        fg.setImageWidth(800);
        fg.setImageHeight(600);
        fg.start();
        Util.printMetaInfo(fg);
        //初始化幕布
        CanvasFrame canvasFrame = new CanvasFrame(path);
        canvasFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        canvasFrame.setAlwaysOnTop(true);
        //初始化视频帧存储
        VideoStorage storage = new VideoStorage();
        //初始化音频
        SourceDataLine sourceDataLine = initSourceDataLine(fg,vol);
        //获取视频帧率
        int frameRate = (int) fg.getFrameRate();
        EventBus eventBus = new EventBus("play");
        PlayViedoListner listener = new PlayViedoListner();
        eventBus.register(listener);

        new Thread(() -> {
            long i = 0;
            while (true) {
                i++;
                //af = fg.grabSamples(); //仅拉取音频帧
                try {
                    Frame vf = fg.grabFrame(true, true, true, false);
                    if (vf == null) {
                        fg.stop();
                        fg.release();
                        System.exit(0);
                    }
                    //处理音频
                    processAudio(fg, vf, sourceDataLine, i);
                    //处理视频
                    processVideo(eventBus,storage, fg, vf, i);
                } catch (FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        startRepresent(eventBus,frameRate,storage,canvasFrame);

        while (true) {
            Thread.sleep(1000);
        }
    }

    private void startRepresent(EventBus eventBus,int frameRate,VideoStorage storage,CanvasFrame canvasFrame){
        new Thread(() -> {
            int curentRate = frameRate;
            while (true) {
                if (storage.getRate() <= 0.1) { //库存积压 加速
                    curentRate= frameRate;// 库存不足 按照码率播放
                    try {
                        int sleep = 1000 / curentRate;
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                BufferedImage image = storage.consume();
                eventBus.post(new ShowImageEvent(canvasFrame,image));

            }
        }).start();
    }

    /**
     * 处理视频
     *
     * @param storage
     * @param fg
     * @param f          帧
     * @param frameIndex 帧index
     */
    private void processVideo(EventBus eventBus,VideoStorage storage, FFmpegFrameGrabber fg, Frame f, long frameIndex) {
        BufferedImage bi = (new Java2DFrameConverter()).getBufferedImage(f);
        if (bi != null) {
            eventBus.post(new VideoEvent(bi,storage,fg.getTimestamp()));
        } else {
            //System.out.println("%%%%第"+frameIndex+"帧为视频空帧");
        }
    }


    /**
     * 处理音频
     *
     * @param fg
     * @param f
     * @param sourceDataLine
     * @param frameindex
     */
    private void processAudio(FFmpegFrameGrabber fg, Frame f, SourceDataLine sourceDataLine, long frameindex) {
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
                    TLData = Util.floatToByteValue(leftData);
                    rightData = (FloatBuffer) buf[1];
                    TRData = Util.floatToByteValue(rightData);
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
                    TLData = Util.shortToByteValue(ILData);
                    tl = TLData.array();
                    sourceDataLine.write(tl, 0, tl.length);
                    break;
                case avutil.AV_SAMPLE_FMT_FLT://float非平面型
                    leftData = (FloatBuffer) buf[0];
                    TLData = Util.floatToByteValue(leftData);
                    tl = TLData.array();
                    sourceDataLine.write(tl, 0, tl.length);
                    break;
                case avutil.AV_SAMPLE_FMT_S16P://平面型左右声道分开
                    ILData = (ShortBuffer) buf[0];
                    IRData = (ShortBuffer) buf[1];
                    TLData = Util.shortToByteValue(ILData);
                    TRData = Util.shortToByteValue(IRData);
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
     * 初始化音频处理器
     * @param fg
     */
    private SourceDataLine initSourceDataLine(FFmpegFrameGrabber fg,float vol) {
        SourceDataLine sourceDataLine = null;
        AudioFormat af = null;
        af = getAudioFormat(fg, af);
        try {
            //java原生JDK进行 音频处理
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class,
                    af, AudioSystem.NOT_SPECIFIED);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(af);
            sourceDataLine.start();

            FloatControl fc = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            double value = vol;//音量是以分贝 (db) 为单位的量
            float dB =(float) (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
            fc.setValue(dB);

        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return sourceDataLine;
    }

    /**
     * 获取音频格式
     * @param fg
     * @param af
     * @return
     */
    static AudioFormat getAudioFormat(FFmpegFrameGrabber fg, AudioFormat af) {
        switch (fg.getSampleFormat()) {
            case avutil.AV_SAMPLE_FMT_U8://无符号short 8bit
                break;
            case avutil.AV_SAMPLE_FMT_S16://有符号short 16bit
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 16, fg.getAudioChannels(), fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_S32:
                break;
            case avutil.AV_SAMPLE_FMT_FLT:
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 16, fg.getAudioChannels(), fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_DBL:
                break;
            case avutil.AV_SAMPLE_FMT_U8P:
                break;
            case avutil.AV_SAMPLE_FMT_S16P://有符号short 16bit,平面型
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 16, fg.getAudioChannels(), fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_S32P://有符号short 32bit，平面型，但是32bit的话可能电脑声卡不支持，这种音乐也少见
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 32, fg.getAudioChannels(), fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_FLTP://float 平面型 需转为16bit short
                af = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fg.getSampleRate(), 16, fg.getAudioChannels(), fg.getAudioChannels() * 2, fg.getSampleRate(), true);
                break;
            case avutil.AV_SAMPLE_FMT_DBLP:
                break;
            case avutil.AV_SAMPLE_FMT_S64://有符号short 64bit 非平面型
                break;
            case avutil.AV_SAMPLE_FMT_S64P://有符号short 64bit平面型
                break;
            default:
                System.out.println("不支持的音乐格式");
                System.exit(0);
        }
        return af;
    }




}