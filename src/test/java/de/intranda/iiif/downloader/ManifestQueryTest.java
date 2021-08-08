package de.intranda.iiif.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class ManifestQueryTest
        extends TestCase {
    private JsonNode testManifest;

    /**
     * Create the test case
     *
     * @param testName name of the test case
     * @throws IOException
     */
    public ManifestQueryTest(String testName) throws IOException {
        super(testName);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/AC03885497_manifest.json"))) {
            testManifest = mapper.readTree(in);
        }
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ManifestQueryTest.class);
    }

    /**
     * Tests if a full canvas is received when an (valid) ID-only canvas is put into the method
     * 
     * @throws URISyntaxException
     * @throws IOException
     */
    public void testCanvasToFullCanvas() throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = new URL("https://digi.landesbibliothek.at/viewer/api/v1/records/AC03885497/pages/11/canvas/").openStream();
        JsonNode inCanvas = mapper.readTree(in);
        Optional<JsonNode> outCanvas = ManifestQuery.canvasToFullCanvas(inCanvas, testManifest);
        assertTrue(outCanvas.isPresent());
        in.close();
    }

    /**
     * Tests sreaming all canvas structures
     * 
     * @throws URISyntaxException
     * @throws IOException
     * @throws MalformedURLException
     */
    public void testStreamAllCanvasStructures() throws URISyntaxException, MalformedURLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        //this is the second page of a chapter
        InputStream in = new URL("https://digi.landesbibliothek.at/viewer/api/v1/records/AC03885497/pages/12/canvas/").openStream();
        JsonNode secondPageCanvas = mapper.readTree(in);
        //should be 1 element in the stream:
        Stream<JsonNode> allCanvasStructs = ManifestQuery.streamAllCanvasStructures(secondPageCanvas, false, testManifest);
        assertEquals(1l, allCanvasStructs.count());
        // should be empty:
        Stream<JsonNode> firstPageCanvasStructs = ManifestQuery.streamAllCanvasStructures(secondPageCanvas, true, testManifest);
        assertEquals(0l, firstPageCanvasStructs.count());
        in.close();

        //this is the first page of a chapter
        in = new URL("https://digi.landesbibliothek.at/viewer/api/v1/records/AC03885497/pages/11/canvas/").openStream();
        JsonNode firstPageCanvas = mapper.readTree(in);
        // should one element in the stream:
        Stream<JsonNode> cCanvasStructs = ManifestQuery.streamAllCanvasStructures(firstPageCanvas, true, testManifest);
        assertEquals(1l, cCanvasStructs.count());
        in.close();

        //this canvas is in a chapter and has an illustration on it
        in = new URL("https://digi.landesbibliothek.at/viewer/api/v1/records/AC03885497/pages/29/canvas/").openStream();
        JsonNode imageCanvas = mapper.readTree(in);
        // should two elements in the stream:
        Stream<JsonNode> imageCanvasStructs = ManifestQuery.streamAllCanvasStructures(imageCanvas, false, testManifest);
        assertEquals(2l, imageCanvasStructs.count());
    }

    /**
     * test excluding canvases by metadata label/value pairs
     * 
     * @throws URISyntaxException
     * @throws IOException
     * @throws MalformedURLException
     */
    public void testFilterExcludeCanvas() throws URISyntaxException, MalformedURLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        LabelValuePair filterAbbildung = new LabelValuePair("Strukturtyp", "Abbildung");
        LabelValuePair filterKapitel = new LabelValuePair("Strukturtyp", "Kapitel");
        // this canvas is in a chapter and has an illustration on it
        InputStream in = new URL("https://digi.landesbibliothek.at/viewer/api/v1/records/AC03885497/pages/29/canvas/").openStream();
        JsonNode imageCanvas = mapper.readTree(in);
        // filtering out "Strukturtyp::Abbildung" and first pages only should filter this canvas:
        assertFalse(ManifestQuery.filterExcludeCanvas(imageCanvas, Collections.singletonList(filterAbbildung), true, testManifest));
        // filtering out "Strukturtyp::Kapitel" and first pages only should keep this canvas:
        assertTrue(ManifestQuery.filterExcludeCanvas(imageCanvas, Collections.singletonList(filterKapitel), true, testManifest));

        // filtering out "Strukturtyp::Kapitel", "Strukturtyp::Abbildung" and first pages only should filter this canvas:
        List<LabelValuePair> filterList = new ArrayList<LabelValuePair>();
        filterList.add(filterKapitel);
        filterList.add(filterAbbildung);
        assertFalse(ManifestQuery.filterExcludeCanvas(imageCanvas, filterList, true, testManifest));
    }

    /**
     * tests include structure queries
     */
    public void testFilterIncludeStructure() {
        //        LabelValuePair filterKapitel = new LabelValuePair("Strukturtyp", "Kapitel");
        //        LabelValuePair filterTabelle = new LabelValuePair("Strukturtyp", "Tabelle");
        //        // test null
        //        assertFalse(ManifestQuery.filterIncludeStructure(null, null));
        //        // this struct has no metadata - should not match whatsoever
        //        Range noMetaStruct = testManifest.getStructures().get(0);
        //        assertFalse(ManifestQuery.filterIncludeStructure(noMetaStruct, Collections.singletonList(filterKapitel)));
        //
        //        // this struct is a "Strukturtyp::Kapitel" struct
        //        Range kapitelStruct = testManifest.getStructures().get(9);
        //        assertTrue(ManifestQuery.filterIncludeStructure(kapitelStruct, Collections.singletonList(filterKapitel)));
        //        assertFalse(ManifestQuery.filterIncludeStructure(kapitelStruct, Collections.singletonList(filterTabelle)));
    }

}
