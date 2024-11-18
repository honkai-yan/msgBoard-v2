package com.peter.msgBoard.entity;

import java.io.Serializable;

public class DataWrapper implements Serializable {
    private String message;
    private byte[] data;

    public DataWrapper() {}

    public DataWrapper(String message, byte[] data) {
        this.message = message;
        this.data = data;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getData() {
        return this.data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
