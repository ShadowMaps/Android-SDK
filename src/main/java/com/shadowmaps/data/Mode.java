package com.shadowmaps.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 7/19/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Mode {
    @JsonProperty()
    public String id;
    @JsonProperty()
    public String key;
    @JsonProperty()
    public String mode;

    public Mode(String id, String key, String mode) {
        this.key = key;
        this.id = id;
        this.mode = mode;
    }

    public Mode() {}

    public String getId() { return id; }
    public String getMode() { return mode; }
    public String getKey() { return key; }

    public void setId(String id) {
        this.id = id;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
    public void setKey(String key) {
        this.key = key;
    }
}