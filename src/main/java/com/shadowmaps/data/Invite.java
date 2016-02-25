package com.shadowmaps.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Danny Iland on 7/19/15.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Invite {
    @JsonProperty()
    public String email;
    @JsonProperty()
    public String name;
    @JsonProperty()
    public String org;

    public Invite(String email, String name, String org) {
        this.email = email;
        this.name = name;
        this.org = org;
    }

    public Invite() {}

    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getOrg() { return org; }

    public void setEmail(String id) {
        this.email = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setOrg(String org) {
        this.org = org;
    }
}