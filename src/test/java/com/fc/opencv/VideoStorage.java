package com.fc.opencv;


import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * 视频存储缓冲区
 */
public class VideoStorage {
    private final int MAX = 200;
    private ArrayList<BufferedImage> list = new ArrayList<>();


    public void produce(BufferedImage f) {
        synchronized (list) {
            while (list.size() + 1 > MAX) {
                try {
                    System.out.println("视频缓冲区已满，需要等待消费");
                    list.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            list.add(f);
            list.notifyAll();
        }
    }

    public BufferedImage consume() {
        BufferedImage f;
        synchronized (list) {
            while (list.size() < 1) {
                try {
                    System.out.println("视频缓冲区为空，需要等待生产");
                    list.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("***视频缓冲区当前数量" + list.size());
            f = list.remove(0);
            list.notifyAll();
            return f;
        }
    }


    public int getCur() {
        synchronized (list) {
            return list.size();
        }
    }

    public double getRate() {
        return (double) getCur() / MAX;
    }
}