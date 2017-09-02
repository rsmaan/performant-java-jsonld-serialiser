package com.github.performantjsonldjava.views;

import static com.github.performantjsonldjava.views.Constants.*;

public enum JSONDataTypes {
    LIST, SET, STRING, LANGUAGE;

    public static JSONDataTypes fromValue(Object value) {
        JSONDataTypes jsonDataTypes = STRING;
        if(value != null) {
            switch (value.toString()) {
                case AT_LANGUAGE:
                    jsonDataTypes = LANGUAGE;
                    break;
                case AT_SET:
                case AT_LIST:
                    jsonDataTypes = LIST;
                    break;
            }
        }
        return jsonDataTypes;
    }
}
