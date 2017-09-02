package com.github.performantjsonldjava.views;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

public class Constants {
    public static final String RDF_LIST_FIRST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#first";
    public static final URI RDF_LIST_FIRST_URI = new URIImpl(RDF_LIST_FIRST);
    public static final String RDF_LIST_REST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest";
    public static final URI RDF_LIST_REST_URI =  new URIImpl(RDF_LIST_REST);
    public static final String RDF_LIST_NIL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil";
    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
    public static final String XSD_STRING = XSD_NS + "string";
    public static final URI XSD_STRING_URI = new URIImpl(XSD_NS + "string");

    public static final String TYPE = "type";
    public static final String ID = "id";

    public static final String AT_VALUE = "@value";
    public static final String AT_LANGUAGE = "@language";
    public static final String AT_SET = "@set";
    public static final String AT_LIST = "@list";
    public static final String AT_ID = "@id";
    public static final String AT_TYPE = "@type";
    public static final String AT_CONTAINER = "@container";
    public static final String AT_CONTEXT = "@context";


    public static final String AT_VALUE_STRING_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#value";
    public static final URI AT_VALUE_URI = new URIImpl(AT_VALUE_STRING_URI);
    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final URI RDF_TYPE_URI = new URIImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
    public static final String GRAPH = "graph";
    public static final String FIRST = "first";
    public static final String REST = "rest";


}
