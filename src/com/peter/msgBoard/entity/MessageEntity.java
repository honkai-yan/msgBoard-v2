package com.peter.msgBoard.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageEntity implements Serializable {
    private LocalDateTime dateTime;
    private String content;

    public MessageEntity() {}

    public MessageEntity(String content, LocalDateTime dateTime) {
        this.content = content;
        this.dateTime = dateTime;
    }

    public String getDate() {
        return this.dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
