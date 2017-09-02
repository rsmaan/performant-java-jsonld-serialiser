package com.github.performantjsonldjava.views;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.openrdf.model.URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.performantjsonldjava.views.Constants.*;
import static com.github.performantjsonldjava.views.JSONDataTypes.LIST;
import static com.github.performantjsonldjava.views.JSONDataTypes.STRING;


public class ContextsMap {

    private Map<String, Map<Boolean, PropertyName>> predicateToCompactMap =  null;
    private List<String> contexts = new ArrayList<>();
    private boolean init = false;

    public ContextsMap(List<String> contextJSONObjects, List<String> contexts) {
        init();
        contextJSONObjects.forEach(s -> addToPredicateToCompactMap(s));
        this.contexts = contexts;
    }

    public void init() {
        predicateToCompactMap = new HashMap<>();
        addBasicProperties(predicateToCompactMap);
    }

    void addToPredicateToCompactMap(String fileContent) {
        LinkedTreeMap linkedHashMap = new Gson().fromJson(fileContent, LinkedTreeMap.class);
        LinkedTreeMap linkedTreeMap = (LinkedTreeMap) linkedHashMap.get(AT_CONTEXT);
        linkedTreeMap.keySet()
                .forEach(key -> {
                    Object o = linkedTreeMap.get(key);
                    if (o instanceof LinkedTreeMap) {
                        LinkedTreeMap treeMap = (LinkedTreeMap) o;
                        final JSONDataTypes jsonType = getJSONType(treeMap);
                        final String fullName = treeMap.get(AT_ID).toString();
                        Map<Boolean, PropertyName> nameMap = predicateToCompactMap.get(fullName);
                        final Boolean hasLanguage = jsonType.equals(JSONDataTypes.LANGUAGE);
                        String type = treeMap.get(AT_TYPE) == null ? null : (String)treeMap.get(AT_TYPE);
                        final PropertyName propertyName = new PropertyName(key.toString(), fullName, jsonType, type);
                        addToPredicateToCompactMap(fullName, nameMap, hasLanguage, propertyName);
                    } else {
                        final String fullName = o.toString();
                        Map<Boolean, PropertyName> nameMap = predicateToCompactMap.get(fullName);
                        final Boolean hasLanguage = false;
                        final PropertyName propertyName = new PropertyName(key.toString(), fullName, STRING, null);
                        addToPredicateToCompactMap(fullName, nameMap, hasLanguage, propertyName);
                    }
                });
    }

    void addBasicProperties(Map<String, Map<Boolean, PropertyName>> predicateToCompactMap) {
        addToPredicateToCompactMap(predicateToCompactMap, TYPE, RDF_TYPE, LIST, null);
        addToPredicateToCompactMap(predicateToCompactMap, AT_VALUE, AT_VALUE_STRING_URI, STRING, null);
        addToPredicateToCompactMap(predicateToCompactMap, FIRST, RDF_LIST_FIRST, STRING, null);
        addToPredicateToCompactMap(predicateToCompactMap, REST, RDF_LIST_REST, STRING, null);
    }

    private void addToPredicateToCompactMap(Map<String, Map<Boolean, PropertyName>> predicateToCompactMap,
                                            String propertyShortNameString, String propertyLongName,
                                            JSONDataTypes jsonDataTypes, String definedType) {
        predicateToCompactMap.put(propertyLongName,
                newPropertyNameMap(false, new PropertyName(propertyShortNameString, propertyLongName, jsonDataTypes, definedType)));
    }

    private void addToPredicateToCompactMap(String fullName, Map<Boolean, PropertyName> nameMap, Boolean hasLanguage, PropertyName propertyName) {
        if(nameMap == null) {
            nameMap = newPropertyNameMap(hasLanguage, propertyName);
            predicateToCompactMap.put(fullName, nameMap);
        } else {
            nameMap.put(hasLanguage, propertyName);
        }
    }

    private Map<Boolean, PropertyName> newPropertyNameMap(Boolean hasLanguage, PropertyName propertyName) {
        final Map<Boolean, PropertyName> map = new HashMap<>();
        map.put(hasLanguage, propertyName);
        return map;
    }

    private JSONDataTypes getJSONType(Map map)  {
        final Object value = map.get(AT_CONTAINER);
        return JSONDataTypes.fromValue(value);
    }

    public Map<String, Map<Boolean, PropertyName>> getPredicateToCompactMap() {
        return predicateToCompactMap;
    }

    public List<String> getContexts() {
        return contexts;
    }

    public PropertyName getPropertyName(URI key, boolean hasLanguage, ContextsMap contextsMap) {
        final Map<Boolean, PropertyName> propertyNameMap = contextsMap.getPredicateToCompactMap().get(key.toString());
        return propertyNameMap == null ? null: propertyNameMap.get(hasLanguage);
    }

    public boolean isPropertyUnDefined(URI dataType, PropertyName propertyName) {
        return propertyName == null
                || ( propertyName.getDefinedType() != null && dataType != null && !propertyName.getDefinedType().equals(dataType.toString()));
    }

}
