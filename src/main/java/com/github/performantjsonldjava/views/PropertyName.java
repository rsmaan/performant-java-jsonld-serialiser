package com.github.performantjsonldjava.views;



final public class PropertyName implements Comparable<PropertyName> {
    private String propertyShortNameString;
    private PropertyShortName propertyShortName;
    private String propertyName;
    private JSONDataTypes jsonDataTypes;
    private String definedType;
    private boolean isId = false;

    public PropertyName(String propertyShortNameString, String propertyLongName, JSONDataTypes jsonDataTypes, String definedType) {
        this.propertyShortName = new PropertyShortName(propertyShortNameString);
        this.propertyShortNameString = propertyShortNameString;
        this.propertyName = propertyLongName;
        this.jsonDataTypes = jsonDataTypes;
        this.isId = Constants.ID.equals(propertyShortName);
        this.definedType = definedType;
    }

    public PropertyShortName getPropertyShortName() {
        return propertyShortName;
    }

    public JSONDataTypes getJSONDataTypes() {
        return jsonDataTypes;
    }

    public String getDefinedType() {
        return definedType;
    }

    @Override
    public int compareTo(PropertyName o) {
        if(this.isId) {
            return 1;
        }
        return propertyShortNameString.compareTo(o.propertyShortNameString);
    }
}
