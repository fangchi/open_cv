package com.fc.opencv;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
public class Util {
    /**
     * 打印音频缓冲区情况
     */
    public static boolean DEBUG1 = false;
    /**
     * 打印生产消费情况
     */
    public static boolean DEBUG2 = false;
    /**
     * 打印音视频同步情况
     */
    public static boolean DEBUG3 = false;
    /**
     * 打印帧捕捉情况
     */
    public static boolean DEBUG4 = false;
    /**
     * 打印其他信息
     */
    public static boolean DEBUG5 = false;
    public static ByteBuffer shortToByteValue(ShortBuffer arr,float vol) {
        int len  = arr.capacity();
        ByteBuffer bb = ByteBuffer.allocate(len * 2);
        for(int i = 0;i<len;i++){
            bb.putShort(i*2,(short)((float)arr.get(i)*vol));
        }
        return bb; // 默认转为大端序
    }
    public static ByteBuffer floatToByteForm(FloatBuffer arr) {
        //这个函数仅仅将float数据转为了float的字节代表形式，不代表float的值。
        ByteBuffer bb = ByteBuffer.allocate(arr.capacity() * 4);
        bb.asFloatBuffer().put(arr);
        return bb; //
    }
    public static ByteBuffer floatToByteValue(FloatBuffer arr,float vol){
        int len = arr.capacity();
        float f;
        float v;
        ByteBuffer res = ByteBuffer.allocate(len*2);
        v = 32768.0f*vol;
        for(int i=0;i<len;i++){
            f = arr.get(i)*v;//Ref：https://stackoverflow.com/questions/15087668/how-to-convert-pcm-samples-in-byte-array-as-floating-point-numbers-in-the-range
            if(f>v) f = v;
            if(f<-v) f = v;
            //默认转为大端序
            res.putShort(i*2,(short)f);//注意乘以2，因为一次写入两个字节。
        }
        return res;
    }

    /**
     * Ref:http://blog.csdn.net/eguid_1/article/details/52973508
     * add watermark to picture.
     */
    public static void mark(BufferedImage bufImg, Image img, String text, Font font, Color color, int x, int y) {
        Graphics2D g = bufImg.createGraphics();
        /* 消除java.awt.Font字体的锯齿 */
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(img, 0, 0, bufImg.getWidth(), bufImg.getHeight(), null);
        g.setColor(color);
        g.setFont(font);
        g.drawString(text, x, y);
        g.dispose();
    }
    public static String getTimeString(long timeUS){
        long Sec,Hour,Min;
        String H;
        String M;
        String S;
        Sec = timeUS/1000000;
        Hour = Sec/3600;
        H = timeConvert(Hour);
        Min = Sec/60-Hour*60;
        M = timeConvert(Min);
        S = timeConvert(Sec%60);
        return H+":"+M+":"+S;
    }
    private static String timeConvert(long time){
        String str = String.valueOf(time);
        if(str.length()==1){
            str = "0"+str;
        }
        return str;
    }
}