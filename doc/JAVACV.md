# JAVA-CV

## 基本概念
视频数据本身由视频文件(或流媒体)承载，视频数据由数据帧（frame）组成，一般来说视频帧按照文件类型划分可以分为<span style="color:red">**视频帧**</span>和<span style="color:red">**音频帧**</span>，视频帧本质上一副静态图片(image),音频帧本质上是一段声音，他们共同构成了视频文件的基础文件结构。如下图：

![视频文件基本格式](images/%E8%A7%86%E9%A2%91%E6%96%87%E4%BB%B6%E5%9F%BA%E6%9C%AC%E6%A0%BC%E5%BC%8F.PNG)

为了节约存储的空间，往往视频帧又会分为**关键帧**(I帧，一般采用帧内压缩算法)和非关键帧(基于I帧的增量算法，帧间压缩算法)

**视频/音频帧率：**单位时间(1秒)内的视频/音频帧数，如上图视频码率为4，音频码率为2 ，显然帧率影响文件播放的流畅度，帧率越高，流畅度越高

**编码格式**：一种压缩视频文件的方法，如（H264编码），而目前主流的播放器和浏览器都能够内置对于H264编码解码的支持从而能够进行视频文件的播放（专业的播放器支持的编码格式更多，因为安装自带了更多的编解码器）


## 视频播放原理
本质上即在一块幕布上进行视频帧的顺序快速展示以及调用声卡进行音频帧的顺序播放

![视频播放原理](images/%E8%A7%86%E9%A2%91%E6%92%AD%E6%94%BE%E5%8E%9F%E7%90%86.PNG)



> 注：视频帧和音频帧在播放的时候是有区别的，音频帧播放是连续发音（是<span style="color:red">时间段</span>逻辑），而视频帧则是通过连续播放的幻灯片是<span style="color:red">时间点</span>逻辑，这时如果完全按照顺序播放每一帧，则会带来一个问题：在播放音频帧时，视频帧没有更新，画面会带来卡顿感。这样从本质上就决定了**音频和视频不能完全放在一个线程中顺序处理**



## JAVA-CV platform

简介：　JavaCV使用来自计算机视觉领域(OpenCV, FFmpeg, libdc1394, PGR FlyCapture, OpenKinect, librealsense, CL PS3 Eye Driver, videoInput, ARToolKitPlus, flandmark, Leptonica, and Tesseract)领域的研究人员常用库的JavaCPP预设的封装。提供实用程序类，使其功能更易于在Java平台上使用

``` xml
 <dependency>
     <groupId>org.bytedeco</groupId>
     <artifactId>javacv-platform</artifactId>
     <version>1.4.1</version><!--其他版本 1.5.1-->
</dependency>
```

使用platform的好处是，此包会自动引入各大平台的依赖jar（内含dll），实现跨平台免安装软件好处，缺点是所有平台的jar以及dll添加总jar包打包超过500MB，该组件库提供了一下核心功能
1. 读能力： 通过数据源(本地文件、网络数据源)获取视频流中数据的能力 
2. 改能力：提供将图像，声音的编辑能力 
3. 写能力：提供将图片和声音写入视频的能力 

## 音频播放

``` java
/**
     * 处理音频
     * @param fg 记录器
     * @param f 数据帧
     * @param vol 音量
     * @param sourceDataLine 音频
     * @param frameindex
     */
    private void processAudio(FFmpegFrameGrabber fg, Frame f, float vol, SourceDataLine sourceDataLine, long frameindex) {
        int k;
       	//从数据帧获取音频数据
        Buffer[] buf = f.samples;
        if (buf != null) {
            FloatBuffer leftData, rightData;
            ShortBuffer ILData, IRData;
            ByteBuffer TLData, TRData;
            byte[] tl, tr;
            byte[] combine;
            //获取音频编码
            int sampleFormat = fg.getSampleFormat();
            switch (sampleFormat) {
                case avutil.AV_SAMPLE_FMT_FLTP://平面型左右声道分开。
                    //左声道数据
                    leftData = (FloatBuffer) buf[0];
                    TLData = Util.floatToByteValue(leftData, vol);
                    //右声道数据
                    rightData = (FloatBuffer) buf[1];
                    TRData = Util.floatToByteValue(rightData, vol);
                    tl = TLData.array();
                    tr = TRData.array();
                    combine = new byte[tl.length + tr.length];
                    k = 0;
                    //混合两个声道
                    for (int i = 0; i < tl.length; i = i + 2) {。
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
 * 工具类
 */
public class Util {
    
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
    
    //写入缓存区
    public static ByteBuffer floatToByteValue(FloatBuffer arr,float vol){
        int len = arr.capacity();
        float f;
        float v;
        ByteBuffer res = ByteBuffer.allocate(len*2);
        /**
        * 编码时需要将音频数据压制到16位 16 bit integers is -32768 to 32767 
        * float f;
        * int16 i = ...;
        * f = ((float) i) / (float) 32768
        * if( f > 1 ) f = 1;
        * if( f < -1 ) f = -1;
        * 故反向还原时如下逻辑
        */
        v = 32768.0f*vol;
        for(int i=0;i<len;i++){
            f = arr.get(i)*v;//参考：https://stackoverflow.com/questions/15087668/how-to-convert-pcm-samples-in-byte-array-as-floating-point-numbers-in-the-range
            if(f>v) f = v;
            if(f<-v) f = v;
            //默认转为大端序，注意乘以2，因为一次写入两个字节
            res.putShort(i*2,(short)f);
        }
        return res;
    }

    /**
     * 
     * 打水印.
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
```

音频播放本质上是通过声卡播放，由于音频表现为时间段逻辑而非时间片逻辑，故音频可直接播放，不存在时间问题

图例：左右声道混合逻辑

![左右声道混合](images/%E5%B7%A6%E5%8F%B3%E5%A3%B0%E9%81%93%E6%B7%B7%E5%90%88.PNG)

## 视频播放

不断循环渲染画布

``` java	
   /**
     * 处理视频
     * @param storage
     * @param fg
     * @param f 帧
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

   public someFunc(){
       while(true){
           //循环播放
           processVideo()
       }
   }
```

这段代码在执行时，如果是数据提供方是完整数据（非实时RTSP），这时播放速度会明显<span style="color:red">**快于**</span>正常时间，因为画布的渲染和时间无关，这时需要在播放侧对视频的播放进行<span style="color:red">**限速**</span>

## 音频&视频联合播放

为了对视频进行限速，故需要对视频帧进行播放存储处理

``` java
class VideoStorage {
    private final int MAX = 200;
    private ArrayList<BufferedImage> list = new ArrayList<>();
    
    //添加进入播放池队列
    public void produce(BufferedImage f) {
        synchronized (list) {
            while (list.size() + 1 > MAX) {
                try {
                    //System.out.println("视频缓冲区已满，需要等待消费");
                    list.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            list.add(f);
            list.notifyAll();
        }
    }
    
    //播放
    public BufferedImage consume() {
        BufferedImage f;
        synchronized (list) {
            while (list.size() < 1) {
                try {
                    //System.out.println("视频缓冲区为空，需要等待生产");
                    list.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("******************视频缓冲区当前数量" + list.size());
            f = list.remove(0);
            list.notifyAll();
            return f;
        }
    }
}

class SomeClass {
    //视频码率
    int curentRate = frameRate;
    while (true) {
        if (storage.getRate() > 0.1) { //库存积压 加速
            curentRate++;
        } else {
            curentRate--;// 库存不足 减速
            if (curentRate < frameRate) {
                curentRate = frameRate;
            }
        }

        try {
            Thread.sleep(1000 / curentRate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        BufferedImage image = storage.consume();
        canvasFrame.showImage(image);
    }
}


/**
 * 视频资源获取执行器
 **/
public FFmpegFrameGrabber(String filename) {
        this.filename = filename;
  			//AV_PIX_FMT_NONE 默认像素格式 其他如RGB，YUV420P
  			//FFmpeg视频解码后，一般存储为AV_PIX_FMT_YUV420P 的format，而解码后的数据存储在结构体 AVFrame 中。YUV420P在内存中的排布
        this.pixelFormat = AV_PIX_FMT_NONE;
  			//声音格式
        this.sampleFormat = AV_SAMPLE_FMT_NONE;
}
```



## 项目代码

> https://github.com/fangchi/open_cv


## 参考网址

https://blog.csdn.net/ffffffff8/article/details/78663814/

https://blog.csdn.net/qq_16234613/article/details/8228

https://stackoverflow.com/questions/15087668/how-to-convert-pcm-samples-in-byte-array-as-floating-point-numbers-in-the-range