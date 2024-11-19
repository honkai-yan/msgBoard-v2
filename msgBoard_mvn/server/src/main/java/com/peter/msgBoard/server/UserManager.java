package com.peter.msgBoard.server;

import com.peter.msgBoard.entity.UserEntity;
import com.peter.msgBoard.utils.IO;
import com.peter.msgBoard.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class UserManager {
    private final Map<String, UserEntity> userList;
    private final LinkedList<String> onlineUserList;
    private static final String ROOT_NAME= "root";
    private static final String ROOT_PWD = "123456";

    protected UserManager() {
        this.userList = new HashMap<>();
        this.onlineUserList = new LinkedList<>();

        this.init();
    }

    @SuppressWarnings("unchecked")
    private void init() {
        String userDataFilePath = IO.USER_DATA_PATH;
        IO._checkFileSilently(userDataFilePath);
        ArrayList<UserEntity> list = IO.readFileBin(userDataFilePath, ArrayList.class);
        if (list == null)
            System.out.println("读取用户文件失败");
        else {
            for (final UserEntity user : list) {
                String userName = user.getUserName();
                String pwd = user.getPassword_md5();
                this.userList.put(userName, new UserEntity(userName, pwd));
            }
        }

        if (!this.userList.containsKey(ROOT_NAME)) {
            this.userList.put(ROOT_NAME, new UserEntity(ROOT_NAME, Utils.md5(ROOT_PWD)));
            IO._checkFileSilently(IO.MSG_DATA_DIR + ROOT_NAME + IO.FILE_SUFFIX);
            if (!this.storeUsers()) System.exit(-1);
        }
    }

    private boolean storeUsers() {
        ArrayList<UserEntity> list = new ArrayList<>();
        for (final Map.Entry<String, UserEntity> entry : this.userList.entrySet()) {
            list.add(entry.getValue());
        }
        if (!IO.writeFileBin(IO.USER_DATA_PATH, list)) {
            System.out.println("保存用户信息失败");
            return false;
        }

        return true;
    }

    protected boolean hasUser(String name) {
        return this.userList.containsKey(name);
    }

    /**
     * 进行用户登录
     * @param name 用户的账户名
     * @param pwd 用户密码
     * @return 1-登录成功 2-密码错误 3-没有该用户 4-用户已登录
     */
    protected int login(String name, String pwd) {
        if (!this.hasUser(name)) return 3;
        if (this.onlineUserList.contains(name)) return 4;
        UserEntity user = this.userList.get(name);
        if (pwd.equals(user.getPassword_md5())) {
            this.onlineUserList.add(name);
            return 1;
        } else return 2;
    }

    protected void logout(String name) {
        this.onlineUserList.remove(name);
    }

    protected String getAllUsersList() {
        if (this.userList.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        int i  = 0;
        for (final Map.Entry<String, UserEntity> userEntityEntry : this.userList.entrySet()) {
            i += 1;
            String name = userEntityEntry.getKey();
            sb.append(i).append(". ").append(name).append("\n");
        }

        return sb.toString();
    }

    protected String getOnlineUsersList() {
        if (this.onlineUserList.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (final String userName : this.onlineUserList) {
            i += 1;
            sb.append(i).append(". ").append(userName).append("\n");
        }

        return sb.toString();
    }

    /**
     * 添加用户
     * @param user 用户实例类
     * @return 1-用户已存在 2-添加成功 3-添加失败，服务器内部错误
     */
    protected int addUser(UserEntity user) {
        String name = user.getUserName();
        if (this.userList.containsKey(name)) return 1;
        this.userList.put(name, user);
        if (!this.storeUsers()) return 3;
        IO._checkFileSilently(IO.MSG_DATA_DIR + name + IO.FILE_SUFFIX);
        return 2;
    }

    /**
     * 删除用户
     * @param name 用户名称
     * @return 1-没有该用户 2-删除失败，服务器内部错误 3-删除成功 4-用户在线，无法删除 5-不能删除root账户
     */
    protected int delUser(String name) {
        if (name.equals("root")) return 5;
        if (!this.userList.containsKey(name)) return 1;
        if (this.onlineUserList.contains(name)) return 4;
        this.userList.remove(name);
        if (!this.storeUsers()) return 2;
        return 3;
    }

    protected ArrayList<String> getAllMessages(String userName) {
        String dataFilePath = IO.MSG_DATA_DIR + userName + IO.FILE_SUFFIX;
        ArrayList<String> list = IO.readFileByLine(dataFilePath);
        if (list == null || list.isEmpty()) return null;
        return list;
    }

    protected boolean saveMessage(String userName, String content) {
        String dataFilePath = IO.MSG_DATA_DIR + userName + IO.FILE_SUFFIX;
        return IO.writeFile(dataFilePath, content, true);
    }
}
