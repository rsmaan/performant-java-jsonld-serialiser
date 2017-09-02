package com.github.performantjsonldjava.views;


import static com.github.performantjsonldjava.views.Constants.ID;

final public class PropertyShortName implements Comparable<PropertyShortName>  {
    private String propertyShortName;
    private boolean isId = false;
    private boolean isType = false;
    private int hashCode ;
    private transient boolean isIRI;

    public PropertyShortName() {
    }

    public PropertyShortName(String propertyShortName) {
        assert propertyShortName != null :"propertyShortName can't be null";
        this.propertyShortName = propertyShortName;
        this.isId = ID.equals(propertyShortName);
        this.isType = "type".equals(propertyShortName);
        this.hashCode = propertyShortName.hashCode();

    }

    public void setIRI(boolean IRI) {
        isIRI = IRI;
    }

    public boolean isIRI() {
        return isIRI;
    }

    @Override
    public int compareTo(PropertyShortName o) {
        if(this.isId) {
            if(o.isId) {
                return 0;
            }
            return -1;
        } else if(o.isId) {
            return 1;
        } else if(this.isType) {
            if(o.isType) {
                return 0;
            }
            return -1;
        } else if(o.isType) {
            return 1;
        }


        return this.propertyShortName.compareTo(o.propertyShortName);
    }

    @Override
    public boolean equals(Object o) {
        PropertyShortName that = (PropertyShortName) o;

        return propertyShortName.equals(that.propertyShortName);
    }

    public boolean equals(String o) {
        return propertyShortName.equals(o);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return propertyShortName;
    }

    public boolean isType() {
        return isType;
    }
}
