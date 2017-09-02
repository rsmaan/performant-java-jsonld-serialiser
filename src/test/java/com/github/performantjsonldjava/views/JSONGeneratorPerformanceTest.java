package com.github.performantjsonldjava.views;

import com.google.common.base.Stopwatch;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class JSONGeneratorPerformanceTest extends JSONGeneratorTestFixturesUtil {
    final private ValueFactoryImpl valueFactory = ValueFactoryImpl.getInstance();
    final private JSONGenerator jsonGenerator = new JSONGenerator();


    @Test
    public void testTreeMapWithGSONPerformanceEmbedAlways() throws Exception {

        String rootId = exampleNamespace+"root";
        String level2Id = exampleNamespace+"level2";

        String level3Id = exampleNamespace+"level3";

        List<Statement> allStatements = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            allStatements.addAll(createGraphNode(level3Id + ":" + i));
        }

        List<Statement> level2 = addNarrowers(createGraphNode(level2Id), allStatements);

        int resultCount = 20;
        List<Statement> other = new ArrayList<>();
        for (int i = 0; i < resultCount; i++) {

            other.addAll(addNarrowers(createGraphNode(level2Id + ":" + i), allStatements));
        }


        List<Statement> root = createGraphWithRoot(createGraphNode(rootId), getFlatStatements(asList(level2, other)), allStatements);

        int totalIterations = 10;
        int warmUPiterations = 4;
        Stopwatch stopwatch = Stopwatch.createStarted();
        String jsonString = null;
        LinkedHashModel model = new LinkedHashModel(root);
        File file = new File(ContextsMap.class.getResource("/test-data/contexts/").getFile());

        String rdfSchemaContextContent = IOUtils.toString(new FileInputStream(file.getAbsolutePath() + File.separator + "rdf-schema-context.jsonld"), Charsets.UTF_8);
        String schemaOrgContextContent = IOUtils.toString(new FileInputStream(file.getAbsolutePath() + File.separator + "schema-org-context.jsonld"), Charsets.UTF_8);
        String skosContextContent = IOUtils.toString(new FileInputStream(file.getAbsolutePath() + File.separator + "skos-context.jsonld"), Charsets.UTF_8);

        final ContextsMap contextsMap = new ContextsMap(Arrays.asList(rdfSchemaContextContent, schemaOrgContextContent, skosContextContent),
                Arrays.asList(exampleNamespace+"/rdf-schema-context.jsonld",
                        exampleNamespace+"/schema-org-context.jsonld",
                        exampleNamespace+"/skos-context.jsonld"));


        for (int i = 0; i < totalIterations; i++) {
            if (i == warmUPiterations) {
                stopwatch = Stopwatch.createStarted();
            }
            jsonString = jsonGenerator.toJSONLD(model, Arrays.asList(rootId), true, 1000, contextsMap, new HashMap<>());

        }


        int iterations = totalIterations - warmUPiterations;
        final long averageTime = stopwatch.elapsed(TimeUnit.MILLISECONDS) / iterations;
        System.out.println(String.format("testTreeMapWithGSONPerformance Statements count : %d iterations : %d Average time : %d millis",
                root.size(), iterations, averageTime));
        System.out.println(" json length : " + jsonString.length());
        /*
        Output from run
        testTreeMapWithGSONPerformance Statements count : 27155 iterations : 4 Average time : 443 millis
         json length : 7471336

         */
        Assert.assertTrue(averageTime < 800);
    }

    List<Statement> createGraphNode(String nodeId) {
        List<Statement> allStatements = new ArrayList<>();
        final URI subject = valueFactory.createURI(nodeId);
        List<Statement> statements = asList(
                valueFactory.createStatement(
                        subject,
                        RDF.TYPE,
                        valueFactory.createURI(exampleNamespace+"Type")
                ),
                valueFactory.createStatement(
                        subject,
                        RDF.TYPE,
                        valueFactory.createURI("http://schema.org/Thing")
                ),
                valueFactory.createStatement(
                        subject,
                        valueFactory.createURI("http://www.w3.org/2000/01/rdf-schema#label"),
                        valueFactory.createLiteral(nodeId + " the label", "en")
                ));
        allStatements.addAll(statements);
        Statement statement = valueFactory.createStatement(
                subject,
                valueFactory.createURI("http://schema.org/contentSize"),
                valueFactory.createLiteral("1001", valueFactory.createURI("http://www.w3.org/2001/XMLSchema#positiveInteger"))
        );
        allStatements.add(statement);

        statement = valueFactory.createStatement(
                subject,
                valueFactory.createURI("http://schema.org/contentSizeUndefined"),
                valueFactory.createLiteral("9009", valueFactory.createURI("http://www.w3.org/2001/XMLSchema#positiveInteger"))
        );
        allStatements.add(statement);

        statement = valueFactory.createStatement(
                subject,
                valueFactory.createURI(exampleNamespace+"filename"),
                valueFactory.createLiteral(nodeId + "filename")
        );
        allStatements.add(statement);

        statement = valueFactory.createStatement(
                subject,
                valueFactory.createURI(exampleNamespace+"uuid"),
                valueFactory.createLiteral(nodeId + "some-uuid")
        );
        allStatements.add(statement);


        statement = valueFactory.createStatement(
                subject,
                valueFactory.createURI("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"),
                valueFactory.createURI(nodeId + "-isDefinedBy")
        );
        allStatements.add(statement);

        final LinkedHashModel model = new LinkedHashModel(allStatements);
        addAddressListAsBlankNode(model, subject, new Address("HN 1", "ST 1"), new Address("HN 2", "ST 2"));

        return model.stream().collect(Collectors.toList());
    }

}
