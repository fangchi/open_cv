package com.fc.opencv;

import com.google.gson.Gson;
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
 * 播放器同步处理仅仅处理视频帧
 */
public class PlayMediaOnlyVideo {

    public static void main(String[] args) throws FrameGrabber.Exception, InterruptedException {
        String fileName = "/Users/fangchi/live/iPc.me-TheAuroraNorthernLights/我只在乎你.mp4";
        //String fileName = "/Users/fangchi/Music/网易云音乐/i2star - 湖光水色调.mp3";
        //String fileName = "rtsp://192.168.38.137:8554/ff";
        float vol = 1f;//音量
        new PlayMediaOnlyVideo(fileName,vol);

    }

    /**
     * 播放
     * @param path 路径
     * @param vol  音量
     * @throws FrameGrabber.Exception
     * @throws InterruptedException
     */
    public PlayMediaOnlyVideo(String path, float vol) throws FrameGrabber.Exception, InterruptedException {
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
        System.out.println("getSampleFormat:" + fg.getSampleFormat());//有符号short 16bit,平面型
        System.out.println("SampleRate:" + fg.getSampleRate());
        System.out.println("getVideoCodecName:" + fg.getVideoCodecName());
        System.out.println("getAudioCodecName:" + fg.getAudioCodecName());
        System.out.println("length:" + fg.getLengthInTime() / 1000000L);
        System.out.println("getImageHeight:" + fg.getImageHeight());
        System.out.println("getImageWidth:" + fg.getImageWidth());
        System.out.println("Format:" + fg.getFormat());
        System.out.println("getMetadata:" + new Gson().toJson(fg.getMetadata()));
        System.out.println("音频通道数:" + fg.getAudioChannels());
        System.out.println("视频总帧数:" + fg.getLengthInVideoFrames());
        System.out.println("音频总帧数:" + fg.getLengthInAudioFrames());
        System.out.println("视频帧数率:" + fg.getVideoFrameRate());
        System.out.println("音频帧数率:" + fg.getAudioFrameRate());
    }

}