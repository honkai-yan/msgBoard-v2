package com.peter.msgBoard.server;

import com.peter.msgBoard.utils.Utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 2580;
    private static final int MAX_CONNECTION_NUM = 30;
    private int onlineUserCnt;
    private final ServerSocket serverSocket;
    private final UserManager userManager;
    private final Map<String, Socket> socketMap;
    private Exception err;

    protected Server(UserManager userManager) throws IOException {
        this.err = null;
        this.onlineUserCnt = 0;
        this.serverSocket = new ServerSocket(PORT);
        this.userManager = userManager;
        this.socketMap = new HashMap<>();
        System.out.println("服务器正在运行，端口：" + PORT);
        this.start();
    }

    private void start() {
        while (true) {
            try {
                Socket clientSocket = this.serverSocket.accept();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
                if (this.onlineUserCnt + 1 > MAX_CONNECTION_NUM) {
                    Utils.printLog(clientSocket, "连接失败，服务器连接数已达上限");
                    writer.write("服务器连接数已满,403");
                    writer.newLine();
                    writer.flush();
                    clientSocket.close();
                    continue;
                }

                writer.write("连接成功,200");
                writer.newLine();
                writer.flush();
                this.onlineUserCnt += 1;
                this.addConnection(clientSocket);
                new Thread(new RequestHandler(this, clientSocket, this.userManager)).start();
            } catch (Exception e) {
                System.err.println("服务器遇到内部错误，即将终止运行...");
                this.err = e;
                break;
            }
        }

        throw new RuntimeException(this.err);
    }

    protected void reduceOnlineUsers() {
        this.onlineUserCnt -= 1;
    }

    /**
     * 添加一个连接，将客户端IP地址与客户端套接字实例作映射
     * @param clientSocket 客户端套接字实例
     */
    protected void addConnection(Socket clientSocket) {
        this.socketMap.put(Utils.getSocketIP(clientSocket), clientSocket);
    }

    /**
     * 删除一个连接，将指定客户端套接字实例的连接从映射表中删除
     * 在客户端主动断开连接、服务器出现内部错误、客户端出现错误等必须要断开连接时调用
     * @param clientSocket 指定的客户端套接字实例
     */
    protected void removeConnection(Socket clientSocket) {
        try {
            Socket s = this.socketMap.remove(Utils.getSocketIP(clientSocket));
            if (s != null && !s.isClosed()) s.close();
            this.reduceOnlineUsers();
        } catch (Exception ignore) {
            Utils.printLog(clientSocket, "断开与客户端的连接失败");
        }
    }
}
