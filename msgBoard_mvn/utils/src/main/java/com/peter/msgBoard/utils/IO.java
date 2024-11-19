package com.peter.msgBoard.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class IO {
    private IO() {}

    private static final String APP_DIR_NAME = "msg_board_v2";
    private static final String USER_DIR = System.getProperty("user.home") + File.separator;

    public static final String BASE_DIR = USER_DIR + APP_DIR_NAME + File.separator;
    public static final String FILE_SUFFIX = ".dat";
    public static final String USER_DATA_PATH = BASE_DIR + "user" + File.separator + "users" + FILE_SUFFIX;
    public static final String MSG_DATA_DIR = BASE_DIR + "data" + File.separator;


    /**
     * 检查某个文件是否存在。
     * 如果目标存在且类型为文件，则返回。
     * 如果目标的父目录不存在，则尝试创建，创建失败抛错。
     * 如果目标的父目录存在，则尝试创建文件，创建失败抛错。
     * @param path 目标文件的绝对路径。
     * @throws IOException 创建目录或文件失败时抛出。
     */
    private static void _check(String path) throws IOException {
        File file = new File(path);
        if (file.exists() && file.isFile()) return;
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists())
            if (!parentDir.mkdirs()) throw new RuntimeException();
        if (!file.createNewFile()) throw new RuntimeException();
    }

    public static void _checkFileWithError(String path) throws IOException {
        _check(path);
    }

    public static void _checkFileSilently(String path) {
        try {
            _check(path);
        } catch (Exception ignore) {}
    }

    public static <T> T readFileBin(String path, Class<T> clazz) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(path))) {
            Object obj = inputStream.readObject();
            if (clazz.isInstance(obj)) {
                return clazz.cast(obj);
            } else return null;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static boolean writeFileBin(String path, Object data) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(path))) {
            outputStream.writeObject(data);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    public static ArrayList<String> readFileByLine(String path) {
        ArrayList<String> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
            return list;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static boolean writeFile(String path, String content, boolean isAppend) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path, isAppend), StandardCharsets.UTF_8))) {
            writer.write(content);
            writer.newLine();
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }
}
