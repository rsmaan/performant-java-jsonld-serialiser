package com.github.performantjsonldjava.views;

import org.openrdf.model.*;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.openrdf.model.vocabulary.SKOS.NARROWER;

public class JSONGeneratorTestFixturesUtil {
    final private ValueFactory valueFactory = ValueFactoryImpl.getInstance();
    final protected String exampleNamespace = "http://example.org/";
    final protected URI ADDRESS_URI = valueFactory.createURI(exampleNamespace, "address");

    class Address{
        String houseNumber;
        String streetName;

        public Address(String houseNumber, String streetName) {
            this.houseNumber = houseNumber;
            this.streetName = streetName;
        }

        public String getHouseNumber() {
            return houseNumber;
        }

        public String getStreetName() {
            return streetName;
        }
    }

    List<Statement> addNarrowers(List<Statement> graphStatements , Collection<Statement> children ) {

        return addStatementsWithPredicate(graphStatements, children, NARROWER);
    }

    private List<Statement> addStatementsWithPredicate(List<Statement> graphStatements, Collection<Statement> children, URI predicate) {
        List<Statement> statements = new ArrayList<>();
        children.stream()
                .filter(st -> !(st.getSubject() instanceof BNode))
                .map(st -> st.getSubject().stringValue())
                .distinct()
                .forEach(id -> {
                    statements.add(valueFactory.createStatement(
                            valueFactory.createURI(getGraphId(graphStatements)),
                            predicate,
                            valueFactory.createURI(id)
                    ));
                });
        graphStatements.forEach(st -> statements.add(st));

        //TODO below can be cleaned
        Statement[] statements1 = new Statement[statements.size()];
        int i = 0;
        for(Statement statement:statements) {
            statements1[i++] = statement;
        }
        return asList(statements1);
    }

    private String getGraphId(List<Statement> graphStatements) {
        return graphStatements.stream().filter(s -> !(s.getSubject() instanceof BNode)).findFirst().get().getSubject().stringValue();
    }

    List<Statement> createGraphNode(String nodeId) {
        final URI uri = valueFactory.createURI(nodeId);
        return asList(
                valueFactory.createStatement(
                        uri,
                        RDF.TYPE,
                        valueFactory.createURI(exampleNamespace+"Type")
                ),
                valueFactory.createStatement(
                        uri,
                        RDF.TYPE,
                        valueFactory.createURI("http://schema.org/Thing")
                ),
                valueFactory.createStatement(
                        uri,
                        valueFactory.createURI("http://www.w3.org/2000/01/rdf-schema#label"),
                        valueFactory.createLiteral(nodeId + " the label", "en")
                ));
    }

    List<Statement> createGraphWithRoot(List<Statement> root, List<Statement> children, List<Statement> otherNodes) {
        root = addNarrowers(root, children);

        Stream<Statement> concat1 = Stream.concat(root.stream(), children.stream());
        Stream<Statement> concat2 = Stream.concat(concat1, otherNodes.stream());
        return concat2.collect(Collectors.toList());

    }

    protected void addAddressListAsBlankNode(Model model, URI subject, Address... addresses) {
        List<BNode> addressBnodeList = new ArrayList<>();
        for(Address addressObj: addresses) {
            BNode addressBnode = valueFactory.createBNode();
            URI street = valueFactory.createURI(exampleNamespace, "street");
            URI house = valueFactory.createURI(exampleNamespace, "house");

            addressBnodeList.add(addressBnode);
            model.add(addressBnode, house, valueFactory.createLiteral(addressObj.getHouseNumber()));
            model.add(addressBnode, street, valueFactory.createLiteral(addressObj.getStreetName()));
        }
        Model addressModel = new LinkedHashModel();
        BNode rdfList =  asRDFList(addressModel, addressBnodeList);
        model.addAll(addressModel);
        model.add(subject, ADDRESS_URI, rdfList);

    }
    private BNode asRDFList(Model model, Iterable<?> iterable, boolean hasType) {
        BNode head = valueFactory.createBNode();
        BNode current = head;
        if (hasType) {
            model.add(valueFactory.createStatement(current, RDF.TYPE, RDF.LIST));
        }

        final Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            final Value literal = (Value) iterator.next();
            model.add(valueFactory.createStatement(current, RDF.FIRST, literal));

            if (iterator.hasNext()) {
                BNode next = valueFactory.createBNode();
                model.add(valueFactory.createStatement(current, RDF.REST, next));
                current = next;
            } else {
                model.add(valueFactory.createStatement(current, RDF.REST, RDF.NIL));
            }
        }
        return head;
    }

    private BNode asRDFList(Model model, Iterable<?> iterable) {
        return asRDFList(model, iterable, true);
    }

    List<Statement> getFlatStatements(List<List> children) {
        return children.stream().collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    String serializeTo(Collection statements, RDFFormat format) throws RDFHandlerException {
        StringWriter stringWriter = new StringWriter();

        RDFWriter writer = Rio.createWriter(format, stringWriter);
        writer.startRDF();
        for (Object statement : statements) {
                writer.handleStatement((Statement) statement);
        }
        writer.endRDF();
        return stringWriter.toString();
    }

}
