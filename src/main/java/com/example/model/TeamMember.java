package com.example.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TeamMember {
    private long id;
    private String name;
    private String surname;

    public TeamMember(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }
}
