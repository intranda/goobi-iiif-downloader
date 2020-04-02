package de.intranda.iiif.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
     */
    public void testCanvasToFullCanvas() throws URISyntaxException {
        /*
        Canvas inCanvas = new Canvas("https://digi.landesbibliothek.at/viewer/rest/iiif/manifests/AC03885497/canvas/11");
        Optional<Canvas> outCanvas = ManifestQuery.canvasToFullCanvas(inCanvas, testManifest);
        assertNotNull(outCanvas.get());
        assertNotNull(outCanvas.get().getType());
        
        inCanvas = new Canvas("https://shouldnotbefound.tld");
        Optional<Canvas> absentCanvas = ManifestQuery.canvasToFullCanvas(inCanvas, testManifest);
        assertFalse(absentCanvas.isPresent());
        */
    }

    /**
     * Tests sreaming all canvas structures
     * 
     * @throws URISyntaxException
     */
    public void testStreamAllCanvasStructures() throws URISyntaxException {
        //        //this is the second page of a chapter
        //        Canvas secondPageCanvas = new Canvas("https://digi.landesbibliothek.at/viewer/rest/iiif/manifests/AC03885497/canvas/12");
        //        //should be 1 element in the stream:
        //        Stream<Range> allCanvasStructs = ManifestQuery.streamAllCanvasStructures(secondPageCanvas, false, testManifest);
        //        assertEquals(1l, allCanvasStructs.count());
        //        // should be empty:
        //        Stream<Range> firstPageCanvasStructs = ManifestQuery.streamAllCanvasStructures(secondPageCanvas, true, testManifest);
        //        assertEquals(0l, firstPageCanvasStructs.count());
        //
        //        //this is the first page of a chapter
        //        Canvas firstPageCanvas = new Canvas("https://digi.landesbibliothek.at/viewer/rest/iiif/manifests/AC03885497/canvas/11");
        //        // should one element in the stream:
        //        Stream<Range> cCanvasStructs = ManifestQuery.streamAllCanvasStructures(firstPageCanvas, true, testManifest);
        //        assertEquals(1l, cCanvasStructs.count());
        //
        //        //this canvas is in a chapter and has an illustration on it
        //        Canvas imageCanvas = new Canvas("https://digi.landesbibliothek.at/viewer/rest/iiif/manifests/AC03885497/canvas/29");
        //        // should two elements in the stream:
        //        Stream<Range> imageCanvasStructs = ManifestQuery.streamAllCanvasStructures(imageCanvas, false, testManifest);
        //        assertEquals(2l, imageCanvasStructs.count());
    }

    /**
     * test excluding canvases by metadata label/value pairs
     * 
     * @throws URISyntaxException
     */
    public void testFilterExcludeCanvas() throws URISyntaxException {
        //        LabelValuePair filterAbbildung = new LabelValuePair("Strukturtyp", "Abbildung");
        //        LabelValuePair filterKapitel = new LabelValuePair("Strukturtyp", "Kapitel");
        //        // this canvas is in a chapter and has an illustration on it
        //        Canvas imageCanvas = new Canvas("https://digi.landesbibliothek.at/viewer/rest/iiif/manifests/AC03885497/canvas/29");
        //        // filtering out "Strukturtyp::Abbildung" and first pages only should filter this canvas:
        //        assertFalse(ManifestQuery.filterExcludeCanvas(imageCanvas, Collections.singletonList(filterAbbildung), true, testManifest));
        //        // filtering out "Strukturtyp::Kapitel" and first pages only should keep this canvas:
        //        assertTrue(ManifestQuery.filterExcludeCanvas(imageCanvas, Collections.singletonList(filterKapitel), true, testManifest));
        //
        //        // filtering out "Strukturtyp::Kapitel", "Strukturtyp::Abbildung" and first pages only should filter this canvas:
        //        List<LabelValuePair> filterList = new ArrayList<LabelValuePair>();
        //        filterList.add(filterKapitel);
        //        filterList.add(filterAbbildung);
        //        assertFalse(ManifestQuery.filterExcludeCanvas(imageCanvas, filterList, true, testManifest));
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
