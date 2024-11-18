package com.peter.msgBoard.client;

import com.peter.msgBoard.entity.MessageEntity;
import com.peter.msgBoard.entity.Response;
import com.peter.msgBoard.entity.UserEntity;
import com.peter.msgBoard.utils.Utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class ClientOperations {
    private final Client client;
    private final App app;

    protected ClientOperations(Client client, App app) {
        this.client = client;
        this.app = app;
    }

    protected void userLogin() {
        if (this.client.isNotConnected()) return;
        String name, pwd;
        System.out.print("请输入用户名：");
        name = App.scanner.nextLine().trim();
        if (name.isEmpty()) return;

        System.out.print("请输入密码：");
        pwd = App.scanner.nextLine().trim();
        if (pwd.isEmpty()) return;
        pwd = Utils.md5(pwd);
        if (pwd == null) {
            System.out.println("密码加密失败。");
            return;
        }

        // 不能重复登录一个账户
        if (name.equals(this.client.getCurUserName())) {
            System.out.println(name + " 已登录，请勿重复登录。");
            return;
        }

        // 如果当前已登录，向服务器发起登出请求，然后发起新的登录请求
        if (this.client.getCurUserName() != null) {
            Response logoutRes = this.client.sendRequest(this.client.getSocket(), "logout", null, null);
            if (this.client.isRequestFailed(logoutRes)) return;

            System.out.println(logoutRes.getMessage());
            // 登出请求失败，则登录新用户也失败
            if (logoutRes.getStatusCode() != 200) {
                System.out.println("登录 " + name + " 失败");
                return;
            }
        }

        UserEntity user = new UserEntity(name, pwd);
        Response loginRes = this.client.sendRequest(this.client.getSocket(), "login", null, user);
        if (this.client.isRequestFailed(loginRes)) return;

        System.out.println(loginRes.getMessage());
        if (loginRes.getStatusCode() == 200) {
            this.client.setCurUserName(name);
            this.client.setHasAuth(name.equals("root"));
            Utils.sleep(1000);
            this.app.printNewMainMenu();
        }
    }

    protected void displayAllUsers() {
        if (this.client.isNotConnected()) return;

        Response response = this.client.sendRequest(this.client.getSocket(), "displayAllUsers", null, null);
        if (this.client.isRequestFailed(response)) return;

        if (response.getStatusCode() == 404) {
            System.out.println("暂无用户");
        } else {
            System.out.println("服务器所有用户的列表如下：");
            System.out.println(response.getMessage());
        }
    }

    protected void displayOnlineUsers() {
        if (this.client.isNotConnected()) return;

        Response response = this.client.sendRequest(this.client.getSocket(), "displayOnlineUsers", null, null);
        if (this.client.isRequestFailed(response)) return;

        if (response.getStatusCode() == 404) {
            System.out.println("暂无在线用户");
        } else {
            System.out.println("在线用户列表如下：");
            System.out.println(response.getMessage());
        }
    }

    protected void connect() {
        String addr;
        int port;
        SocketAddress address;

        System.out.print("请输入服务器地址(不包括端口和协议)：");
        addr = App.scanner.nextLine().trim().toLowerCase();
        if (addr.isEmpty()) return;

        System.out.print("请输入服务器端口号：");
        try {
            port = App.scanner.nextInt();
        } catch (Exception ignore) {
            System.out.println("请输入正确格式的端口号。");
            App.scanner.nextLine();
            return;
        }
        App.scanner.nextLine();

        if (port < 0 || port > 65535) {
            System.out.println("请输入正确范围内（0~65535）的端口号。");
            return;
        }

        try {
            address = new InetSocketAddress(addr, port);
        } catch (Exception ignore) {
            System.out.println("服务器地址参数有误...");
            return;
        }

        /*
          如果客户端已连接到服务器，判断新连接目标是否是同一个服务器。
          如果上次请求服务器失败，则跳过此步骤。
         */
        if (!this.client._isNotConnected() && this.client.isLastRequestSuc()) {
            InetAddress inetAddress = this.client.getSocket().getInetAddress();
            String hostAddress = inetAddress.getHostAddress();
            int _port = this.client.getSocket().getPort();
            if (hostAddress.equals(addr) && _port == port) {
                System.out.println("已连接到相同服务器，无需重复连接。");
                return;
            }
        }

        final int timeout = 10000;

        if (!this.client.connectToServer(address, timeout)) {
            System.out.println("连接失败，请检查本地网络和端口。服务器可能处于关闭或繁忙状态。");
            this.client.setLastRequestSuc(false);
            return;
        }

        // 获取服务器响应内容
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.client.getSocket().getInputStream()));
            String res = reader.readLine().trim();
            String[] resPair = res.split(",");
            System.out.println(resPair[0]);
            this.client.setCurUserName(null);
        } catch (Exception ignore) {
            System.out.println("连接服务器成功，但服务器没有任何响应。请检查服务器地址并重新连接。");
            this.client.setLastRequestSuc(false);
        }
    }

    protected void addUser() {
        if (this.client.isNotConnected()) return;
        if (this.client.isNotLogin()) return;
        if (this.client.isNotAuthed()) return;

        String userName, pwd, pwdAgain;
        System.out.print("请输入要添加的用户名称：");
        userName = App.scanner.nextLine().trim();
        if (userName.isEmpty()) return;
        if (userName.length() > 20) {
            System.out.println("请输入长度小于20位的名称");
            return;
        }

        System.out.print("请输入密码：");
        pwd = App.scanner.nextLine().trim();
        if (pwd.isEmpty()) return;
        if (pwd.length() > 15) {
            System.out.println("请输入长度小于15位的密码");
            return;
        }

        System.out.print("请重复输入密码：");
        pwdAgain = App.scanner.nextLine().trim();
        if (pwdAgain.isEmpty()) return;
        if (!pwd.equals(pwdAgain)) {
            System.out.println("两次输入的密码不相等");
            return;
        }

        pwd = Utils.md5(pwd);
        if (pwd == null) {
            System.out.println("密码加密失败，请重试。");
            return;
        }

        UserEntity user = new UserEntity(userName, pwd);
        Response response = this.client.sendRequest(this.client.getSocket(), "addUser", null, user);
        if (this.client.isRequestFailed(response)) return;

        System.out.println(response.getMessage());
    }

    protected void delUser() {
        if (this.client.isNotConnected()) return;
        if (this.client.isNotLogin()) return;
        if (this.client.isNotAuthed()) return;

        String name;
        System.out.print("请输入要删除的用户的名称：");
        name = App.scanner.nextLine().trim();
        if (name.isEmpty()) return;

        Response response = this.client.sendRequest(this.client.getSocket(), "delUser", name, null);
        if (this.client.isRequestFailed(response)) return;

        System.out.println(response.getMessage());
    }

    @SuppressWarnings("unchecked")
    protected void showAllMessages() {
        Response response = this.client.sendRequest(this.client.getSocket(), "displayAllMessages", this.client.getCurUserName(), null);
        if (this.client.isRequestFailed(response)) return;

        int code = response.getStatusCode();
        if (code == 200) {
            ArrayList<String> msgList = Utils.deSerializeObject(response.getData(), ArrayList.class);
            if (msgList == null) {
                System.out.println("解析数据失败");
                return;
            }
            System.out.println(this.client.getCurUserName() + " 的留言数据如下：");
            for (final String msg : msgList) {
                String[] pair = msg.split(",");
                if (pair.length < 2) {
                    System.out.println("解析数据失败");
                    return;
                }
                String date = pair[0], content = pair[1];
                System.out.println("时间：" + date);
                System.out.println("内容：" + content);
                System.out.println();
            }
        } else {
            System.out.println(response.getMessage());
        }
    }

    protected void writeNewMessage() {
        System.out.print("请输入你的留言(仅支持单行文本)：");
        String input = App.scanner.nextLine().trim();
        if (input.isEmpty()) return;
        if (input.length() > 400) {
            System.out.println("留言长度最长为400个字");
            return;
        }

        Socket clientSocket = this.client.getSocket();
        String requestTarget = "writeNewMessage";
        String curUserName = this.client.getCurUserName();
        MessageEntity message = new MessageEntity(input, LocalDateTime.now());
        Response response = this.client.sendRequest(clientSocket, requestTarget, curUserName, message);

        if (this.client.isRequestFailed(response)) return;

        System.out.println(response.getMessage());
    }
}