package com.peter.msgBoard.entity;

import java.io.Serializable;

public class UserEntity implements Serializable {
    private String userName;
    private String password_md5;

    public UserEntity(String userName, String password_md5) {
        this.userName = userName;
        this.password_md5 = password_md5;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword_md5() {
        return this.password_md5;
    }

    public void setPassword_md5(String password_md5) {
        this.password_md5 = password_md5;
    }
}
