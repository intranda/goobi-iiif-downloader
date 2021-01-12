package de.intranda.iiif.downloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * CLI to download files linked in IIIF manifests.
 * 
 * @author Oliver Paetzel
 *
 */
@Command(name = "java -jar goobi-iiif-downloader.jar", sortOptions = false)
public class IIIFDownloaderMain implements Callable<Integer> {

    @Option(names = { "--manifest", "-m" }, description = "the manifest URL to parse and download from", required = true)
    private String manifestUrl;

    @Option(names = { "--destination", "-d" }, description = "the destination folder to download to", required = true)
    private String destinationFolder;

    @Option(names = { "--include_structure", "-is" },
            description = "structure to include - example: \"Strukturtyp:Abbildung\". The option is repeatable.")
    private List<String> includeStructures;

    @Option(names = { "--exclude_structure", "-es" },
            description = "structure to exclude - example: \"Strukturtyp:Abbildung\". The option is repeatable.")
    private List<String> excludeStructures;

    @Option(names = { "--structure_mode", "-sm" }, description = "structure mode. Possible values: \"firstpage\" and \"all\"")
    private String structureMode;

    @Option(names = { "-max", "--maximum_images" }, description = "the maximum number of images to download")
    private Integer maximumImages;

    @Option(names = { "-ri", "--random_images" }, description = "select random images")
    private boolean selectRandomImages;

    @Option(names = { "-da", "--download_alto" }, description = "download alto (if present)")
    private boolean downloadAlto;

    public static void main(String[] args) {
        if (args.length == 0) {
            CommandLine cl = new CommandLine(new IIIFDownloaderMain());
            cl.setUsageHelpAutoWidth(true);
            cl.usage(System.out);
            System.exit(0);
        }
        try {
            int exitCode = new CommandLine(new IIIFDownloaderMain())
                    .setUsageHelpAutoWidth(true)
                    .execute(args);
            System.exit(exitCode);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    @Override
    public Integer call() throws Exception {
        Path dest = Paths.get(destinationFolder);
        if (!Files.exists(dest)) {
            Files.createDirectories(dest);
        }
        Optional<JsonNode> optManifest = getManifest(manifestUrl);
        if (!optManifest.isPresent()) {
            System.err.println(String.format("could not load manifest from '%s'", manifestUrl));
            return 1;
        }
        JsonNode manifest = optManifest.get();

        try {
            downloadPages(manifest);
        } catch (IOException e) {
            System.err.println("error downloading images/alto");
            return 1;
        }

        return 0;
    }

    private void downloadPages(JsonNode manifest)
            throws MalformedURLException, IOException {

        if (includeStructures != null || excludeStructures != null) {
            downloadStructures(maximumImages, downloadAlto, includeStructures, excludeStructures, selectRandomImages,
                    "firstpage".equals(structureMode), manifest);
        } else {
            JsonNode seq = manifest.get("sequences").get(0);
            JsonNode canvases = seq.get("canvases");
            if (!canvases.isArray()) {
                //error - do something
            }
            ArrayNode lstCanvases = (ArrayNode) canvases;
            if (selectRandomImages && maximumImages != null && maximumImages < lstCanvases.size()) {
                downloadRandom(maximumImages, downloadAlto, lstCanvases);
            } else {
                downloadSequential(maximumImages, downloadAlto, lstCanvases);
            }
        }

    }

    private void downloadStructures(Integer maximumImages, boolean downloadAlto, List<String> includeStructureStrings,
            List<String> excludeStructureStrings, boolean selectRandomImages, boolean filterStructsFirstPage, JsonNode manifest)
            throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
        List<LabelValuePair> includeStructures = includeStructureStrings == null ? new ArrayList<>() : includeStructureStrings.stream()
                .map(s -> s.split("::"))
                .map(sArr -> new LabelValuePair(sArr[0], sArr[1]))
                .collect(Collectors.toList());
        List<LabelValuePair> excludeStructures = excludeStructureStrings == null ? new ArrayList<>() : excludeStructureStrings.stream()
                .map(s -> s.split("::"))
                .map(sArr -> new LabelValuePair(sArr[0], sArr[1]))
                .collect(Collectors.toList());
        List<JsonNode> canvases;
        if (!includeStructures.isEmpty()) {
            // search for includeStructures in manifest and filter excludeStructures
            // first, get all included structures
            List<JsonNode> filteredStructures = ManifestQuery.streamJsonNodeAsArray(manifest.get("structures"))
                    .filter(struct -> ManifestQuery.filterIncludeStructure(struct, includeStructures))
                    .collect(Collectors.toList());
            // now get all canvases and filter out the ones that should be excluded
            Stream<JsonNode> canvasStream;
            if (filterStructsFirstPage) {
                canvasStream = filteredStructures.stream()
                        .map(struct -> struct.get("canvases").get(0));
            } else {
                canvasStream = filteredStructures.stream()
                        .flatMap(struct -> ManifestQuery.streamJsonNodeAsArray(struct.get("canvases")));
            }
            canvases = canvasStream
                    .filter(canvas -> ManifestQuery.filterExcludeCanvas(canvas, excludeStructures, filterStructsFirstPage, manifest))
                    .map(canvas -> ManifestQuery.canvasToFullCanvas(canvas, manifest))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            // take random or all pages and check if they are excluded, then skip them
            canvases = ManifestQuery.streamJsonNodeAsArray(manifest.get("sequences"))
                    .flatMap(seq -> ManifestQuery.streamJsonNodeAsArray(seq.get("canvases")))
                    .filter(canvas -> ManifestQuery.filterExcludeCanvas(canvas, excludeStructures, filterStructsFirstPage, manifest))
                    .collect(Collectors.toList());
        }
        if (selectRandomImages) {
            Collections.shuffle(canvases);
        }
        if (maximumImages != null) {
            canvases = canvases.subList(0, maximumImages);
        }
        // now that everything is filtered, the remaining canvases can be downloaded...
        for (JsonNode c : canvases) {
            downloadImageAndAlto(c, downloadAlto);
        }
    }

    private void downloadRandom(Integer maximumImages, boolean downloadAlto, ArrayNode canvases) throws JsonParseException, JsonMappingException,
            MalformedURLException, IOException {
        Random random = new Random();
        boolean hasAlto = downloadAlto;
        Set<Integer> downloaded = new HashSet<>();
        while (downloaded.size() < maximumImages) {
            int idx = random.nextInt(canvases.size());
            if (downloaded.contains(idx)) {
                continue;
            }
            hasAlto = downloadImageAndAlto(canvases.get(idx), hasAlto);
            downloaded.add(idx);
        }
    }

    public void downloadSequential(Integer maximumImages, boolean downloadAlto, ArrayNode lstCanvases) throws IOException,
            JsonParseException, JsonMappingException, MalformedURLException {
        boolean hasAlto = downloadAlto;
        int downloadCount = 0;
        for (JsonNode canvas : lstCanvases) {
            if (maximumImages != null && downloadCount == maximumImages.intValue()) {
                break;
            }
            hasAlto = downloadImageAndAlto(canvas, hasAlto);
            downloadCount++;
        }
    }

    private Pattern p = Pattern.compile("(.*?/.*)/.*?/(.*?)/.*?/default.jpg$");

    private boolean downloadImageAndAlto(JsonNode canvas, boolean downloadAlto)
            throws JsonParseException, JsonMappingException, MalformedURLException,
            IOException {
        JsonNode image = canvas.get("images").get(0);
        //this is the base uri of the image

        String imageUri = image.get("resource").get("@id").asText();
        String basename = imageUri.substring(imageUri.lastIndexOf('/') + 1);
        int dotIdx = basename.lastIndexOf('.');
        if (dotIdx > 0) {
            basename = basename.substring(0, dotIdx);
        }
        String fullImageUri = imageUri + "/full/full/0/default.jpg";
        Matcher matcher = p.matcher(imageUri);
        if (matcher.matches()) {
            if (!matcher.group(2).equals("max")) {
                fullImageUri = matcher.group(1) + "/full/max/0/default.jpg";
            } else {
                fullImageUri = imageUri;
            }
            String group = matcher.group(1);
            basename = group.substring(group.lastIndexOf('/') + 1);
            System.out.println("new basename: " + basename);
            basename = basename.substring(basename.lastIndexOf('/') + 1);
        }
        //now try to get the ALTO url
        Optional<URI> altoUri = Optional.empty();
        boolean hasAlto = downloadAlto;
        if (downloadAlto) {
            altoUri = getAltoUrl(canvas);
            if (!altoUri.isPresent()) {
                hasAlto = false;
            }
        }
        //
        Path destFile = Paths.get(destinationFolder, basename + ".jpg");
        URLConnection conn = new URL(fullImageUri).openConnection();
        try {
            downloadWithProgress(destFile, conn);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }

        if (altoUri.isPresent()) {
            URLConnection altoConn = altoUri.get().toURL().openConnection();
            downloadWithProgress(Paths.get(destinationFolder, basename + ".xml"), altoConn);
        }
        return hasAlto;
    }

    public void downloadWithProgress(Path destFile, URLConnection conn) throws IOException {
        System.out.println(String.format("Downloading %s to %s:", conn.getURL().toString(), destFile.toString()));
        byte[] buffer = new byte[8192];
        int written = 0;
        int totalWritten = 0;
        StatusRunnable run = new StatusRunnable(totalWritten + " bytes downloaded.");
        Thread statusThread = new Thread(run);
        statusThread.start();
        try (InputStream in = conn.getInputStream(); OutputStream out = Files.newOutputStream(destFile, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE)) {
            while ((written = in.read(buffer)) != -1) {
                out.write(buffer, 0, written);
                totalWritten += written;
                run.setMessage(totalWritten + " bytes downloaded.");
            }
        }
        run.setMessage(totalWritten + " bytes downloaded.");
        run.setShouldStop();
        try {
            statusThread.join(1000);
        } catch (InterruptedException e) {
        }
    }

    public Optional<URI> getAltoUrl(JsonNode canvas) throws IOException, JsonParseException, JsonMappingException,
            MalformedURLException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Stream<JsonNode> nodes;
        JsonNode seeAlsoNode = canvas.get("seeAlso");
        if (seeAlsoNode.isArray()) {
            ArrayNode lstSeeAlso = (ArrayNode) seeAlsoNode;
            nodes = IntStream.range(0, lstSeeAlso.size()).mapToObj(lstSeeAlso::get);
        } else {
            nodes = Collections.singletonList((seeAlsoNode)).stream();
        }
        Optional<JsonNode> optALTOSeeAlso = nodes
                .filter(a -> a.get("label") != null)
                .filter(a -> "ALTO".equals(a.get("label").get(0).get("@value").textValue()))
                .findFirst();
        if (!optALTOSeeAlso.isPresent()) {
            return Optional.empty();
        }
        JsonNode altoAnnotation = optALTOSeeAlso.get();
        try {
            return Optional.of(new URI(altoAnnotation.get("@id").textValue()));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    private Optional<JsonNode> getManifest(String manifest) throws MalformedURLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        JsonNode iiifMani = null;

        StatusRunnable run = new StatusRunnable("Receiving IIIF manifest...");
        Thread statusThread = new Thread(run);
        statusThread.start();
        HttpResponse hr = Request.Get(manifest)
                .execute()
                .returnResponse();
        if (hr.getStatusLine().getStatusCode() >= 400) {
            String response;
            try (InputStream inputStream = hr.getEntity().getContent()) {
                response = new BufferedReader(new InputStreamReader(inputStream))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
            //hr.getEntity().getContent();
            System.err.println(String.format("Could not retrieve Manifest. The server responded with status code %d and message:\n%s",
                    hr.getStatusLine().getStatusCode(), response));
            return Optional.empty();
        }
        try (InputStream in = hr.getEntity().getContent()) {
            iiifMani = mapper.readTree(in);
        }
        run.setMessage("Received IIIF manifest.    ");
        run.setShouldStop();
        try {
            statusThread.join(1000);
        } catch (InterruptedException e) {
        }
        return Optional.ofNullable(iiifMani);
    }
}
