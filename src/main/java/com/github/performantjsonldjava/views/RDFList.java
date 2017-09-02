package com.github.performantjsonldjava.views;

import java.util.ArrayList;

public class RDFList extends ArrayList {

    private String id;
    private boolean hasType;

    public RDFList() {
    }

    public RDFList(String id, boolean hasType) {
        this.id = id;
        this.hasType = hasType;
    }

    public String getId() {
        return id;
    }

    public boolean hasType() {
        return hasType;
    }
}
