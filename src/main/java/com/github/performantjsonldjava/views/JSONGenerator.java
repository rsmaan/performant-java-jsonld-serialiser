package com.github.performantjsonldjava.views;

import com.esotericsoftware.kryo.Kryo;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.performantjsonldjava.views.Constants.*;


public class JSONGenerator {

    private Kryo kryo = new Kryo();

    /*
    TODO
    1. Predicate map can be map of map which is selected based on context files logic. (Not urgent)
    2. id or @id or language or @language or type or @type (not urgent)
    3. Set deduping (not urgent as triples are distinct)
     */
    //todo ensure this method not called


    public String toJSONLD(LinkedHashModel statements, List<String> rootSubjects, boolean embedAlways, int maxEmbedDepth, ContextsMap contextsMap, Map <String, String> filterProperties) {
        Map<String, TreeMapBasedView> idToObjectMap = createIdToObjectMap(statements, contextsMap, filterProperties);

        final View view = new View();
        view.setContext(contextsMap.getContexts());
        List<TreeMapBasedView> items = new ArrayList<>(rootSubjects.size());
        rootSubjects.stream().forEach(s -> items.add(idToObjectMap.get(s)));
        view.setGraph(items);

        if(embedAlways) {
            correctEmbededObjects(view.getGraph(), idToObjectMap, new HashSet<>(), maxEmbedDepth);
        }

        return getGsonBuilder().create().toJson(view);
    }

    public Map<String, TreeMapBasedView> createIdToObjectMap(LinkedHashModel linkedHashModel, ContextsMap contextsMap, Map<String, String> filterProperties) {
        return linkedHashModel.subjects().stream().filter(s -> !(s instanceof BNode))
                .map(subject -> generateView(linkedHashModel, filterProperties, subject, contextsMap))
                .collect(Collectors.toMap(o -> o.getId(), o -> o));
    }

    private TreeMapBasedView generateView(LinkedHashModel linkedHashModel, Map <String, String> filterProperties, Resource subject, ContextsMap contextsMap) {
        TreeMapBasedView treeMapBasedView = new TreeMapBasedView();
        linkedHashModel.filter(subject, null, null).stream()
                .forEach(st -> {
                    addViewDetails(linkedHashModel, filterProperties, treeMapBasedView, st, new HashSet<>(), contextsMap);
                });

        treeMapBasedView.setId(subject.toString(), contextsMap);

        return treeMapBasedView;
    }

    private void addViewDetails(LinkedHashModel linkedHashModel, Map <String, String> filterProperties, TreeMapBasedView treeMapBasedView, Statement statement, Set<BNode> ancestors, ContextsMap contextsMap) {
        URI predicate = statement.getPredicate();
        if(filterProperties == null || !filterProperties.containsKey(predicate.toString())) {
            Value object = statement.getObject();
            Object objectToSet;
            boolean isIRI = false;
            boolean hasLanguage = false;
            URI dataType =null;
            if (object instanceof Literal) {
                Literal literal = (Literal) object;
                String language = literal.getLanguage();
                dataType = literal.getDatatype();
                if (language != null) {
                    TreeMap treeMap = new TreeMap();
                    treeMap.put(language, literal.getLabel());
                    objectToSet = treeMap;
                    hasLanguage = true;
                } else {
                    objectToSet = literal.stringValue();
                }
            } else if (object instanceof BNode) {
                if(ancestors.contains(object)) {
                    Map map = new LinkedHashMap();
                    map.put(AT_ID, ((BNode)object).toString());
                    objectToSet =  map;
                } else {
                    objectToSet = completeCBD(predicate, linkedHashModel, (BNode) object, filterProperties, statement, contextsMap, ancestors);
                }
            } else if (object.equals(RDF.NIL)) {
                objectToSet = new ArrayList<>();
            } else {
                isIRI = true;

                //TODO Expandable object is only required if embed=always might save few millis
                //by not using Expandable if embed is not always i.e. have another implementation
                // for addViewDetails method. Don't use if else here

                objectToSet = new Expandable(object.stringValue());
            }
            treeMapBasedView.setProperty(predicate, objectToSet, contextsMap, isIRI, hasLanguage, dataType,  object);
        }
    }

    private Object completeCBD(URI parentPredicate, LinkedHashModel linkedHashModel, BNode bNode, Map <String, String> filterProperties, Statement statement, ContextsMap contextsMap, Set<BNode> ancestors) {
        ancestors.add(bNode);
        Model bnodeModel = linkedHashModel.filter((Resource) bNode, null, null);
        Optional<Statement> first = bnodeModel
                .stream().findFirst();
        if (first.isPresent()) {
            String predicate = first.get().getPredicate().toString();
            final boolean isListType = first.get().getObject().toString().equals(RDF.LIST.toString());
            if (predicate.equals(RDF.FIRST.toString())
                    || predicate.equals(RDF.REST.toString())
                    || isListType) {
                List list = new RDFList(bNode.toString(), isListType);
                completeListCBD(parentPredicate, linkedHashModel, filterProperties, bnodeModel, list, contextsMap);
                return list;
            }
        }
        final TreeMapBasedView treeMapBasedView = new TreeMapBasedView();
        bnodeModel.stream().forEach(st -> addViewDetails(linkedHashModel, filterProperties, treeMapBasedView, st, ancestors, contextsMap));
        treeMapBasedView.setId(bNode.toString(), contextsMap);

        return treeMapBasedView;

    }

    private void completeListCBD(URI parentPredicate, LinkedHashModel linkedHashModel, Map <String, String> filterProperties, Model bnodeModel, List accumulator, ContextsMap contextsMap) {
        Optional<Statement> first = bnodeModel.filter(null, RDF_LIST_FIRST_URI, null).stream().findFirst();
        Optional<Statement> rest = bnodeModel.filter(null, RDF_LIST_REST_URI, null).stream().findFirst();
        if (first.isPresent()) {
            Value object = first.get().getObject();
            if(object instanceof BNode) {
                accumulator.add(generateView(linkedHashModel, filterProperties, (BNode)object, contextsMap));
            } else if(object instanceof URI) {
                //TODO Expandable object is only required if embed=always might save few millis
                //by not using Expandable if embed is not always i.e. have another implementation
                // for addViewDetails method. Don't use if else here
                accumulator.add(new Expandable(object.stringValue()));
            } else {
                final PropertyName propertyName = contextsMap.getPropertyName(parentPredicate, false, contextsMap);
                if(object instanceof Literal
                        && contextsMap.isPropertyUnDefined(((Literal) object).getDatatype(), propertyName)
                        && !((Literal) object).getDatatype().stringValue().equals(XSD_STRING)) {
                    Literal objectLiteral = (Literal) object;
                    Map map = new LinkedHashMap();
                    if(objectLiteral.getLanguage() != null) {
                        map.put(AT_LANGUAGE, objectLiteral.getLanguage());
                    } else {
                        map.put(TYPE, ((Literal) object).getDatatype().stringValue());
                    }
                    map.put(AT_VALUE, object.stringValue());
                    accumulator.add(map);
                } else {
                    accumulator.add(object.stringValue());
                }
            }
        }


        if (rest.isPresent() && !rest.get().getObject().stringValue().equals(RDF_LIST_NIL)) {
            BNode bNode1 = (BNode) rest.get().getObject();
            Model bnodeModel1 = linkedHashModel.filter((Resource) bNode1, null, null);
            completeListCBD(parentPredicate, linkedHashModel, filterProperties, bnodeModel1, accumulator, contextsMap);
        }
    }

    void correctEmbededObjects(Object frame, Map idToObjectMap, Set<String> ancestors, int depth) {
        //TODO if we ever get embedding problems below can be adjust to lower value
        //This is optimisation to deal with objects embedding till the leaf node is reached
        if(ancestors.size() > depth) {
            return;
        }
        if (frame instanceof List) {
            int i = 0;
            for (Object object : (List) frame) {
                final String objectString = object.toString();
                if (object instanceof Expandable
                        && idToObjectMap.containsKey(objectString)
                        && !ancestors.contains(objectString)) {
                    Set<String> ancestorsClone = addToAncesors(ancestors, objectString);
                        Map clone = (Map) clone(idToObjectMap.get(objectString));
                        if(clone != null) {
                            ((List) frame).set(i, clone);
                            correctEmbededObjects(clone, idToObjectMap, ancestorsClone, depth);
                        }

                } else {
                    correctEmbededObjects(object, idToObjectMap, ancestors, depth);
                }
                i++;
            }

        } else if (frame instanceof Map) {
            Map frameMap = (Map) frame;
            Set<Map.Entry> entrySet = frameMap.entrySet();
            if (entrySet.size() == 1) {
                for (Map.Entry entry : entrySet) {
                    Object entryValue = entry.getValue();
                    final String entryValueString = entryValue.toString();
                    if (entry.getKey().toString().equals(ID) && !ancestors.contains(entryValueString)) {
                            Map clone = (Map) clone(idToObjectMap.get(entryValueString));
                            if (clone != null) {
                                frameMap.putAll(clone);
                                Set<String> ancestorClone = addToAncesors(ancestors, entryValueString);
                                correctEmbededObjects(clone, idToObjectMap, ancestorClone, depth);

                            }

                    } else if (entryValueIsExpandableAndCanBeExpanded(idToObjectMap, ancestors, entryValue)) {
                        //We might be able to just ignore this "if else" can't think of any test scenario
                        // for below condition but leaving the code as it does not do any harm
                        expandValue(idToObjectMap, ancestors, entry, entryValue, depth);
                    } else {
                        correctEmbededObjects(entryValue, idToObjectMap, ancestors, depth);
                    }
                }
            } else {
                for (Map.Entry entry : entrySet) {
                    Object key = entry.getKey();
                    Object entryValue = entry.getValue();
                    if (key.toString().equals(ID)) {
                        ancestors = addToAncesors(ancestors, entryValue.toString());
                    } else if (entryValueIsExpandableAndCanBeExpanded(idToObjectMap, ancestors, entryValue)) {
                        expandValue(idToObjectMap, ancestors, entry, entryValue, depth);
                    } else {
                        correctEmbededObjects(entryValue, idToObjectMap, ancestors, depth);
                    }
                }
            }
        }
    }

    private Set<String> addToAncesors(Set<String> ancestors, String newValue) {
        ancestors = (Set<String>) clone(ancestors);
        ancestors.add(newValue);
        return ancestors;
    }

    private void expandValue(Map idToObjectMap, Set<String> ancestors, Map.Entry entry, Object entryValue, int depth) {
        final String entryValueString = entryValue.toString();
        ancestors = addToAncesors(ancestors, entryValueString);
        Map clone = (Map) clone(idToObjectMap.get(entryValueString));
        entry.setValue(clone);
        correctEmbededObjects(clone, idToObjectMap, ancestors, depth);

    }

    private boolean entryValueIsExpandableAndCanBeExpanded(Map idToObjectMap, Set<String> ancestors, Object entryValue) {
        return entryValue instanceof Expandable && !ancestors.contains(entryValue.toString()) && idToObjectMap.containsKey(entryValue.toString());
    }

    // TODO to improve performance further implement below method to clone fast
    protected Object clone(Object value) {
        //return cloner.deepClone(value);
        return kryo.copy(value);
    }

    GsonBuilder getGsonBuilder() {
        final GsonBuilder gsonBuilder = new GsonBuilder();

        //TODO Expandable object is only required if embed=always might save few millis
        //no need to register this serializer if embed is not always
        gsonBuilder.registerTypeAdapter(Expandable.class, new JsonSerializer<Expandable>() {
            @Override
            public JsonPrimitive serialize(Expandable src, Type typeOfSrc, JsonSerializationContext context) {
                JsonPrimitive jsonPrimitive = new JsonPrimitive(src.toString());

                return jsonPrimitive;
            }
        });
        return gsonBuilder;
    }



}