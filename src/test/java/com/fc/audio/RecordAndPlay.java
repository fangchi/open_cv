package com.fc.audio;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.TargetDataLine;

/**
 * 使用扬声器播放麦克风的声音
 */
public class RecordAndPlay {

    public static void main(String[] args) {
        Play();
    }

    //播放音频文件
    public static void Play() {
        try {
            AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100F, 16, 2, 4,
                            44100F, true);
            //麦克风
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            TargetDataLine targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            //扬声器
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine sourceDataLine = (SourceDataLine) AudioSystem.getLine(info);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();
            //用于设置音量
            FloatControl fc = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            double value = 1.3;//音量是以分贝 (db) 为单位的量
            //
            float dB =(float) (Math.log(value==0.0?0.0001:value)/Math.log(10.0)*20.0);
            System.out.println("db:"+dB);
            fc.setValue(dB);
            int nByte = 0;
            final int bufSize = 4100;
            byte[] buffer = new byte[bufSize];
            while (nByte != -1) {
                //System.in.read();
                nByte = targetDataLine.read(buffer, 0, bufSize);
                sourceDataLine.write(buffer, 0, nByte);
            }
            sourceDataLine.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
