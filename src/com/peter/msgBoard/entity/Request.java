package com.peter.msgBoard.entity;

import java.io.Serializable;

public class Request extends DataWrapper implements Serializable {
    private String requestTarget;

    public Request() {
        super();
    }

    public Request(String msg, byte[] data, String requestTarget) {
        super(msg, data);
        this.requestTarget = requestTarget;
    }

    public String getRequestTarget() {
        return requestTarget;
    }

    public void setRequestTarget(String requestTarget) {
        this.requestTarget = requestTarget;
    }
}
