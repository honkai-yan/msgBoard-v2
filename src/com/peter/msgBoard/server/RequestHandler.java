package com.peter.msgBoard.server;

import com.peter.msgBoard.entity.MessageEntity;
import com.peter.msgBoard.entity.Request;
import com.peter.msgBoard.entity.Response;
import com.peter.msgBoard.entity.UserEntity;
import com.peter.msgBoard.utils.Utils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class RequestHandler implements Runnable {
    private final Server server;
    private final Socket clientSocket;
    private final UserManager userManager;
    private BufferedReader reader;
    private BufferedWriter writer;
    private InputStream inputStream;
    private OutputStream outputStream;
    private static final Map<String, BiConsumer<RequestHandler, Request>> optionMap = new HashMap<>();
    private String userName;

    static {
        // 操作-方法映射
        optionMap.put("login", RequestHandler::handleLogin);
        optionMap.put("quit", RequestHandler::handleClientQuit);
        optionMap.put("displayAllUsers", RequestHandler::handleDisplayAllUsers);
        optionMap.put("displayOnlineUsers", RequestHandler::handleDisplayOnlineUsers);
        optionMap.put("addUser", RequestHandler::handleAddUser);
        optionMap.put("delUser", RequestHandler::handleDelUser);
        optionMap.put("logout", RequestHandler::handleLogout);
        optionMap.put("displayAllMessages", RequestHandler::handleDisplayAllMessages);
        optionMap.put("writeNewMessage", RequestHandler::handleWriteNewMessage);
    }

    protected RequestHandler(Server server, Socket clientSocket, UserManager userManager) {
        this.userManager = userManager;
        this.reader = null;
        this.writer = null;
        this.inputStream = null;
        this.outputStream = null;
        this.userName = null;

        this.server = server;
        this.clientSocket = clientSocket;

        Utils.printLog(this.clientSocket, "连接成功");
    }

    @Override
    public void run() {
        try {
            this.reader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(this.clientSocket.getOutputStream()));
            this.inputStream = this.clientSocket.getInputStream();
            this.outputStream = this.clientSocket.getOutputStream();
            this.handleRequest();
        } catch (Exception ignore) {
            closeConnectionWithTip(this);
        }
    }

    private void handleRequest() {
        int requestBytesLength;
        DataInputStream inputStream = new DataInputStream(this.inputStream);
        while (true) {
            try {
                requestBytesLength = inputStream.readInt();
                byte[] clientRequestBytes = new byte[requestBytesLength];
                inputStream.readFully(clientRequestBytes);

                Request request = Utils.deSerializeObject(clientRequestBytes, Request.class);
                if (request == null) throw new RuntimeException();

                String requestTarget = request.getRequestTarget();
                if (optionMap.containsKey(requestTarget)) {
                    Utils.printLog(this.clientSocket, "收到请求：" + requestTarget);
                    BiConsumer<RequestHandler, Request> consumer = optionMap.get(requestTarget);
                    consumer.accept(this, request);
                } else {
                    Utils.printLog(this.clientSocket, "未知请求：" + requestTarget);
                }
            } catch (Exception ignore) {
                closeConnectionWithTip(this);
                return;
            }
        }
    }

    private static boolean logout(RequestHandler handler) {
        if (handler.userName != null) {
            handler.userManager.logout(handler.userName);
            Utils.printLog(handler.clientSocket, "用户 " + handler.userName + " 退出登录");
            return true;
        }
        return false;
    }

    private static void closeConnection(RequestHandler handler) {
        logout(handler);

        try {
            handler.server.removeConnection(handler.clientSocket);
            if (handler.reader != null) handler.reader.close();
            if (handler.writer != null) handler.writer.close();
            if (handler.inputStream != null) handler.inputStream.close();
            if (handler.outputStream != null) handler.outputStream.close();
            Utils.printLog(handler.clientSocket, "成功断开连接");
        } catch (Exception _ignore) {
            Utils.printLog(handler.clientSocket, "断开连接失败");
        } finally {
            handler.server.removeConnection(handler.clientSocket);
        }
    }

    private static void closeConnectionWithError(RequestHandler handler, Exception err) {
        Utils.printLog(handler.clientSocket, "发生内部错误，重置连接中");
        closeConnection(handler);
        System.out.println(err.getMessage());
    }

    private static void closeConnectionWithTip(RequestHandler handler) {
        Utils.printLog(handler.clientSocket, "客户端连接重置");
        closeConnection(handler);
    }

    /**
     * 向客户端发送响应
     * @param handler 请求处理实例
     * @param response 响应对象
     */
    private static void sendResponse(RequestHandler handler, Response response) throws IOException {
        byte[] bytes = Utils.serializeObject(response);
        if (bytes == null) throw new RuntimeException();

        DataOutputStream outputStream = new DataOutputStream(handler.outputStream);
        // 发送数据长度
        outputStream.writeInt(bytes.length);
        // 发送字节流数据
        outputStream.write(bytes);
        outputStream.flush();
    }

    private static void handleLogin(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            UserEntity user = Utils.deSerializeObject(request.getData(), UserEntity.class);
            if (user == null) throw new RuntimeException();
            String userName = user.getUserName(), pwd = user.getPassword_md5();

            if (!handler.userManager.hasUser(userName)) {
                response.setMessage("没有该用户");
                response.setStatusCode(404);
                sendResponse(handler, response);
                return;
            }

            int code = handler.userManager.login(userName, pwd);
            if (code == 1) {
                handler.userName = userName;
                Utils.printLog(handler.clientSocket, "用户 " + userName + " 登录成功");
                response.setMessage(userName + " 登录成功");
                response.setStatusCode(200);
            } else if (code == 2) {
                response.setMessage("密码错误，登录失败");
                response.setStatusCode(403);
            } else if (code == 4) {
                response.setMessage(userName + " 已登录，请不要重复登录");
                response.setStatusCode(403);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    private static void handleClientQuit(RequestHandler handler, Request ignore) {
        handler.server.removeConnection(handler.clientSocket);
        Utils.printLog(handler.clientSocket, "客户端断开连接");
    }

    private static void handleDisplayAllUsers(RequestHandler handler, Request ignore) {
        try {
            Response response = new Response();
            String userList = handler.userManager.getAllUsersList();

            if (userList == null) {
                response.setStatusCode(404);
            } else {
                response.setStatusCode(200);
                response.setMessage(userList);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleDisplayOnlineUsers(RequestHandler handler, Request ignore) {
        try {
            Response response = new Response();
            String onlineUserList = handler.userManager.getOnlineUsersList();

            if (onlineUserList == null) {
                response.setStatusCode(404);
            } else {
                response.setStatusCode(200);
                response.setMessage(onlineUserList);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleAddUser(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            UserEntity user = Utils.deSerializeObject(request.getData(), UserEntity.class);
            if (user == null) throw new RuntimeException();

            int code = handler.userManager.addUser(user);
            if (code == 1) {
                response.setMessage("该用户已存在");
                response.setStatusCode(400);
            } else if (code == 2) {
                response.setMessage("添加成功");
                response.setStatusCode(200);
            } else if (code == 3) {
                response.setMessage("添加失败，服务器发生内部错误");
                response.setStatusCode(500);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleDelUser(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            String userName = request.getMessage();
            int code = handler.userManager.delUser(userName);

            if (code == 1) {
                response.setMessage("没有该用户");
                response.setStatusCode(404);
            } else if (code == 2) {
                response.setMessage("删除失败，服务器内部错误");
                response.setStatusCode(500);
            } else if (code == 3) {
                response.setMessage("删除成功");
                response.setStatusCode(200);
            } else if (code ==4) {
                response.setMessage("无法删除在线用户");
                response.setStatusCode(403);
            } else if (code == 5) {
                response.setMessage("无法删除root账户");
                response.setStatusCode(403);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleLogout(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            if (logout(handler)) {
                response.setMessage(handler.userName + " 登出成功");
                response.setStatusCode(200);
            } else {
                response.setMessage(handler.userName + " 登出失败");
                response.setStatusCode(500);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleDisplayAllMessages(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            String name = request.getMessage();
            ArrayList<String> list = handler.userManager.getAllMessages(name);

            if (list == null) {
                response.setMessage(name + " 暂无留言记录");
                response.setStatusCode(404);
            } else {
                byte[] listBytes = Utils.serializeObject(list);
                if (listBytes == null) throw new RuntimeException();
                response.setData(listBytes);
                response.setStatusCode(200);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }

    protected static void handleWriteNewMessage(RequestHandler handler, Request request) {
        try {
            Response response = new Response();

            String userName = request.getMessage();
            MessageEntity message = Utils.deSerializeObject(request.getData(), MessageEntity.class);
            if (message == null) throw new RuntimeException();

            String content = message.getDate() + "," + message.getContent();

            if (handler.userManager.saveMessage(userName, content)) {
                response.setMessage("保存成功");
                response.setStatusCode(200);
            } else {
                response.setMessage("保存失败，相关问题请咨询管理员");
                response.setStatusCode(500);
            }

            sendResponse(handler, response);
        } catch (Exception e) {
            closeConnectionWithError(handler, e);
        }
    }
}
