package com.github.performantjsonldjava.views;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Set;

public class View {
    @SerializedName("@context")
    private List<String> context;

    @SerializedName("@graph")
    private List<TreeMapBasedView> graph;

    public void setContext(List<String> context) {
        this.context = context;
    }

    public void setGraph(List<TreeMapBasedView> graph) {
        this.graph = graph;
    }

    public List<TreeMapBasedView> getGraph() {
        return graph;
    }
}
