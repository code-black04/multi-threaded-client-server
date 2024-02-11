package org.assignment.dto;

import java.time.LocalDateTime;

public class RecievedMessage {
    private String senderUserId;

    private LocalDateTime dateTime;

    private String messageBody;

    public RecievedMessage(String senderUserId, LocalDateTime dateTime, String messageBody) {
        this.senderUserId = senderUserId;
        this.dateTime = dateTime;
        this.messageBody = messageBody;
    }

    public String getSenderUserId() {
        return senderUserId;
    }

    public void setSenderUserId(String senderUserId) {
        this.senderUserId = senderUserId;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }
}
