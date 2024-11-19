package com.peter.msgBoard.entity;

import java.io.Serializable;

public class Response extends DataWrapper implements Serializable {
    private int statusCode;

    public Response() {
        super();
    }

    public Response(String msg, byte[] data, int statusCode) {
        super(msg, data);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
