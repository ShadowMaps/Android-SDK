package com.shadowmaps.data;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 2/23/15.
 */
public class Message {
    @JsonProperty()
    String message;

    public Message() {
    }

    public Message(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
