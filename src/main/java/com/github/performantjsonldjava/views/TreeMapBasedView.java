package com.github.performantjsonldjava.views;

import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.performantjsonldjava.views.Constants.*;


public final class TreeMapBasedView extends TreeMap<PropertyShortName, Object> {
    private static final Logger logger = LoggerFactory.getLogger(TreeMapBasedView.class);

    private transient String id;

    public TreeMapBasedView() {
    }

    public TreeMapBasedView(Comparator<? super PropertyShortName> comparator) {
        super(comparator);
    }

    public void setProperty(URI key, Object value, ContextsMap contextsMap, boolean isIRI, boolean hasLanguage, URI dataType, Value object) {
        PropertyName propertyName = contextsMap.getPropertyName(key, hasLanguage, contextsMap);
        if(propertyName == null
                || ( propertyName.getDefinedType() != null && dataType != null && !propertyName.getDefinedType().equals(dataType.toString()))) {
            // TODO this path is performance killer
            // We might even want to throw error if property is not defined in contexts
            handleUndefinedProperty(key, value, dataType, hasLanguage, object, contextsMap);

        } else {
            final PropertyShortName propertyShortName = propertyName.getPropertyShortName();
            final JSONDataTypes jsonDataTypes = propertyName.getJSONDataTypes();
            Object existingValue = this.get(propertyShortName);
            if (jsonDataTypes.equals(JSONDataTypes.LIST)) {

                if(propertyShortName.isType()) {
                    final Map<Boolean, PropertyName> propertyNameMap = contextsMap.getPredicateToCompactMap().get(value.toString());
                    final PropertyName valueCompacted = propertyNameMap != null ? propertyNameMap.get(Boolean.FALSE) : null;
                    value =
                            valueCompacted != null
                                    ? valueCompacted.getPropertyShortName().toString()
                                    : value.toString();
                    if (existingValue == null) {
                        put(value, propertyShortName);
                    } else if (existingValue instanceof String) {
                        List<Object> arrayList = new ArrayList<>();
                        arrayList.add(existingValue);
                        arrayList.add(value);
                        put(arrayList, propertyShortName);
                    } else {
                        ((List) existingValue).add(value);
                    }

                } else {
                    if (existingValue == null) {
                        List<Object> arrayList;
                        if (value instanceof ArrayList) {
                            arrayList = (ArrayList) value;
                        } else {
                            arrayList = new ArrayList<>();
                            arrayList.add(value);
                        }
                        put(arrayList, propertyShortName);
                    } else {
                        ((List) existingValue).add(value);
                    }
                }
            } else if(jsonDataTypes.equals(JSONDataTypes.LANGUAGE)) {
                if (existingValue == null) {
                    put(value, propertyShortName);
                } else {
                    final Map existingValueMap = (Map) existingValue;
                    final Map incomingValueMap = (Map) value;
                    final String incomingLanguageCode = incomingValueMap.keySet().stream().findFirst().get().toString();
                    final Object existingValueForLanguage = existingValueMap.get(incomingLanguageCode);
                    if(existingValueForLanguage == null) {
                        existingValueMap.putAll(incomingValueMap);
                    } else {
                        final Object incomingValue = incomingValueMap.get(incomingLanguageCode);
                        if(existingValueForLanguage instanceof String) {
                            List list = new ArrayList();
                            list.add(existingValueForLanguage);
                            list.add(incomingValue);
                            existingValueMap.put(incomingLanguageCode, list);
                        } else {
                            List list = (List) existingValueForLanguage;
                            list.add(incomingValue);
                        }
                    }
                }
            } else {
                if(existingValue == null) {
                    put(value, propertyShortName);
                } else if (!(existingValue instanceof List)) {
                    List list = new ArrayList();
                    list.add(existingValue);
                    list.add(value);
                    put(list, propertyShortName);
                } else {
                    ((List)existingValue).add(value);
                }
            }
        }
    }


    private void handleUndefinedProperty(URI key, Object value, URI dataType, boolean hasLanguage, Value object, ContextsMap contextsMap) {
        PropertyShortName propertyShortName = new PropertyShortName(key.stringValue());
        final Object existingValue = this.get(propertyShortName);
        Object objectToPut = null;
        if(hasLanguage) {
            Map map = new LinkedHashMap();
            Literal literal = (Literal) object;
            map.put(AT_LANGUAGE, literal.getLanguage());
            map.put(AT_VALUE, literal.getLabel());
            objectToPut = map;

        } else if (value instanceof Expandable) {
            final TreeMapBasedView mapBasedView = new TreeMapBasedView();
            mapBasedView.setId(value.toString(), contextsMap);
            objectToPut = mapBasedView;
        } else if(value instanceof RDFList) {
            RDFList rdfList = (RDFList)value;
            if(rdfList.isEmpty() && rdfList.hasType()) {
                Map map = new LinkedHashMap();
                //TODO type or @type
                map.put(ID, rdfList.getId());
                map.put(TYPE, RDF.LIST.toString());
                objectToPut = map;
            } else {
                Map map = new LinkedHashMap();
                map.put(AT_LIST, rdfList);
                objectToPut = map;
            }
        } else if (dataType != null && !dataType.equals(XSD_STRING_URI)) {
            Map map = new LinkedHashMap();
            //TODO type or @type
            map.put(TYPE, dataType.stringValue());
            map.put(AT_VALUE, value);
            objectToPut = map;
        } else {
            objectToPut = value;
        }
        if(existingValue == null) {
            put(objectToPut, propertyShortName);
        } else if(existingValue instanceof String || existingValue instanceof Map || existingValue instanceof Expandable) {
            List list = new ArrayList();
            list.add(existingValue);
            list.add(objectToPut);
            put(list, propertyShortName);
        } else {
            ((List)existingValue).add(objectToPut);
        }
        logger.error("Property "+key.stringValue()+ " not defined in context files.");
    }


    public void put(Object value, PropertyShortName propertyShortName) {
        this.put(propertyShortName, value);
    }


    public String getId() {
        return this.id;
    }

    public void setId(String id, ContextsMap contextsMap) {
        this.id = id;
        put(id, new PropertyShortName(Constants.ID));
    }


}
