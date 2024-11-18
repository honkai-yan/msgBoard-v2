package com.peter.msgBoard.utils;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.security.MessageDigest;

public class Utils {
    private Utils() {}

    public static String getSysCharset() {
        return System.getProperty("native.encoding");
    }

    public static String md5(String str) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] hashBytes = md.digest(str.getBytes());
            final StringBuilder builder = new StringBuilder();
            for (final var b : hashBytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    public static String getSocketIP(Socket socket) {
        InetAddress address = socket.getInetAddress();
        if (address == null) return "null";
        return address.getHostAddress() + ":" + socket.getPort();
    }

    public static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception ignore) {}
    }

    public static void printLog(Socket socket, String msg) {
        System.out.println(getSocketIP(socket) + "：" + msg + "。");
    }

    // 将任意对象进行序列化，返回一个字节序列，序列化失败返回null。
    public static byte[] serializeObject(Object object) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(object);
            return outputStream.toByteArray();
        } catch (Exception ignore) {
            return null;
        }
    }

    // 对字节数组进行反序列化，返回一个指定类型的对象，反序列化失败返回null。
    public static <T> T deSerializeObject(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(inputStream)) {
            Object obj = ois.readObject();
            if (clazz.isInstance(obj))
                return clazz.cast(obj);
            else return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static void openLinkWithDefaultBrowser(String link) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    URI uri = new URI(link);
                    desktop.browse(uri);
                } else {
                    throw new RuntimeException();
                }
            } else {
                throw new RuntimeException();
            }
        } catch (Exception ignore) {
            System.out.println("居然打开失败了...");
        }
    }

    public static void toggleEasterEgg() {
        System.out.println("bingo，你找到了彩蛋！现在去看Clannad吧！");
        openLinkWithDefaultBrowser("https://www.bilibili.com/bangumi/play/ep34489/?share_source=copy_web");
    }
}
