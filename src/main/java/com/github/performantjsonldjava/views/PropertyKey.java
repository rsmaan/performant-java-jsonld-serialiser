package com.github.performantjsonldjava.views;


public class PropertyKey {
    private JSONDataTypes jsonDataTypes;
    private String fullName;

    public PropertyKey(String fullName, JSONDataTypes jsonDataTypes) {
        assert fullName != null : "fullName cannot be null";
        assert jsonDataTypes != null : "jsonDataTypes cannot be null";
        this.fullName = fullName;
        this.jsonDataTypes = jsonDataTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyKey that = (PropertyKey) o;

        if (jsonDataTypes != that.jsonDataTypes) return false;
        return fullName.equals(that.fullName);
    }

    @Override
    public int hashCode() {
        int result = jsonDataTypes.hashCode();
        result = 31 * result + fullName.hashCode();
        return result;
    }
}
