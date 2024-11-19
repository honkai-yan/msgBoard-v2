package com.peter.msgBoard.client;

import com.peter.msgBoard.entity.Request;
import com.peter.msgBoard.entity.Response;
import com.peter.msgBoard.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;

public class Client {
    // ********************************** Members ************************************* //

    // 当前登录的用户的用户名，如果没有用户登录，则该值为null
    private String curUserName;

    // 当前用户是否有管理员权限
    private boolean hasAuth;

    /**
     * 客户端套接字实例。
     * 在Client创建时被实例化。
     * 通过其{@code isConnected()}方法来判断是否建立了连接。
     * 通过其{@code isClosed()}方法来判断连接是否被关闭。
     * 通过其{@code connect()}方法来连接服务器。
     */
    private Socket socket;

    // 上次访问服务器是否成功
    private boolean lastRequestSuc;


    // ********************************** Functions ************************************* //

    protected Client() {
        this.curUserName = null;
        this.hasAuth = false;
        this.socket = null;
        this.lastRequestSuc = true;
    }

    protected boolean isNotLogin() {
        if (this.curUserName == null) {
            System.out.println("请先登录");
            Utils.pressEnter();
            return true;
        }
        return false;
    }

    protected boolean isNotAuthed() {
        if (!this.hasAuth) {
            System.out.println("您没有权限");
            Utils.pressEnter();
            return true;
        }
        return false;
    }

    /**
     * 判断客户端是否处于未连接服务器的状态。如果未连接会打印提示。
     * 当客户端套接字实例为{@code null}或{@code isConnected()}方法返回{@code false}或{@code isClose()}方法返回{@code true}时，
     * 该方法返回{@code true}，否则返回{@code false}。
     * @return 客户端连接状态
     */
    protected boolean isNotConnected() {
        if (this.socket == null || !this.socket.isConnected() || this.socket.isClosed()) {
            System.out.println("请先连接服务器");
            Utils.pressEnter();
            return true;
        }
        return false;
    }

    /**
     * 判断客户端是否处于未连接务器的状态。不打印提示。
     * @return 客户端连接状态
     */
    protected boolean _isNotConnected() {
        return this.socket == null || !this.socket.isConnected() || this.socket.isClosed();
    }

    /**
     * 判断某个请求是否失败
     * @param response 服务器的响应结果实例
     * @return 如果服务器响应不为null则返回false，否则返回true
     */
    protected boolean isRequestFailed(Response response) {
        if (response == null) {
            System.out.println("请求服务器失败...请重新连接服务器。");
            this.setLastRequestSuc(false);
            Utils.pressEnter();
            return true;
        }
        return false;
    }

    protected Socket getSocket() {
        return this.socket;
    }

    protected void setCurUserName(String name) {
        this.curUserName = name;
    }

    protected String getCurUserName() {
        return this.curUserName;
    }

    protected boolean isLastRequestSuc() {
        return this.lastRequestSuc;
    }

    protected void setHasAuth(boolean hasAuth) {
        this.hasAuth = hasAuth;
    }

    protected void setLastRequestSuc(boolean lastRequestSuc) {
        this.lastRequestSuc = lastRequestSuc;
    }

    protected String getServerAddr() {
        if (this._isNotConnected()) return null;
        return this.socket.getInetAddress().getHostAddress() + ":" + this.socket.getPort();
    }

    protected boolean closeConnection() {
        if (this.socket == null) return true;

        try {
            this.socket.close();
            this.socket = null;
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 连接到新服务器，连接之前会发起一次测试连接。
     * 如果测试失败，则返回false，不关闭之前的连接。
     * 如果测试成功，则会关闭之前的连接，然后替换客户端套接字实例为新实例，并返回true
     * @param address 服务器地址对象
     * @param timeout 超时时间
     * @return 连接是否成功的布尔值
     */
    protected boolean connectToServer(SocketAddress address, int timeout) {
        try {
            Socket testSocket = this.testConnection(address, timeout);
            if (!testSocket.isConnected()) return false;
            if (!this.closeConnection()) return false;
            this.socket = testSocket;
            this.lastRequestSuc = true;
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    /**
     * 向服务器发送请求
     * @param socket 客户端套接字实例
     * @param questName 请求名称，如login
     * @param data 要向服务器发送的字符串数据
     * @param binData 要向服务器发送的字节数组数据，即二进制数据
     * @return 服务器的响应结果实例。在请求名称为null、发生IO错误、序列化错误等情况时，返回null。如果请求失败，还会将{@code lastRequestSuc}设置为false。
     */
    protected Response sendRequest(Socket socket, String questName, String data, Object binData) {
        if (questName == null) {
            this.lastRequestSuc = false;
            return null;
        }

        try {
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            byte[] binDataBytes;
            if (binData == null) {
                binDataBytes = null;
            } else {
                binDataBytes = Utils.serializeObject(binData);
                if (binDataBytes == null) throw new RuntimeException();
            }

            Request request = new Request(data, binDataBytes, questName);
            byte[] requestBytes = Utils.serializeObject(request);
            if (requestBytes == null) throw new RuntimeException();

            dataOutputStream.writeInt(requestBytes.length);
            dataOutputStream.write(requestBytes);
            dataOutputStream.flush();


            // 获取并返回服务器请求
            int byteLength = dataInputStream.readInt();
            byte[] response = new byte[byteLength];
            dataInputStream.readFully(response);
            this.lastRequestSuc = true;

            // 反序列化响应体
            return Utils.deSerializeObject(response, Response.class);
        } catch (Exception ignore) {
            this.lastRequestSuc = false;
            return null;
        }
    }

    /**
     * 测试与服务器的连接
     * @param address 服务器地址
     * @param timeout 超时时间
     * @return 一个客户端套接字实例，如果测试失败，抛出IO错误
     */
    private Socket testConnection(SocketAddress address, int timeout) throws IOException {
        Socket socket1 = new Socket();
        socket1.connect(address, timeout);
        return socket1;
    }

    /**
     * 从指定客户端套接字实例中读取一个服务器响应。不能滥用，容易导致客户端阻塞。
     * @param clientSocket 指定的客户端套接字实例
     * @return 服务器响应对象
     */
    protected Response getResponseOfServer(Socket clientSocket) {
        try {
            DataInputStream inputStream = new DataInputStream(clientSocket.getInputStream());
            int resLength = inputStream.readInt();
            byte[] resBytes = new byte[resLength];
            inputStream.readFully(resBytes);
            return Utils.deSerializeObject(resBytes, Response.class);
        } catch (Exception ignore) {
            return null;
        }
    }
}
