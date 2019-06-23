package de.intranda.iiif.downloader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.intranda.api.annotation.LinkedAnnotation;
import de.intranda.api.iiif.presentation.AnnotationList;
import de.intranda.api.iiif.presentation.Canvas;
import de.intranda.api.iiif.presentation.Manifest;
import de.intranda.api.iiif.presentation.Range;
import de.intranda.api.iiif.presentation.Sequence;
import de.intranda.api.iiif.presentation.content.ImageContent;
import de.intranda.api.iiif.presentation.content.LinkingContent;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "java -jar goobi-iiif-downloader.jar", sortOptions = false)
public class IIIFDownloaderMain implements Callable<Integer> {

    @Option(names = { "--manifest", "-m" }, description = "the manifest URL to parse and download from", required = true)
    private String manifestUrl;

    @Option(names = { "--destination", "-d" }, description = "the destination folder to download to", required = true)
    private String destinationFolder;

    @Option(names = { "--include_structure", "-is" }, description = "structure to include - can be multiple")
    private List<String> includeStructures;

    @Option(names = { "--exclude_structure", "-es" }, description = "structure to exclude - can be multiple")
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
        Optional<Manifest> optManifest = getManifest(manifestUrl);
        if (!optManifest.isPresent()) {
            System.err.println(String.format("could not load manifest from '%s'", manifestUrl));
            return 1;
        }
        Manifest manifest = optManifest.get();

        try {
            downloadPages(manifest);
        } catch (IOException e) {
            System.err.println("error downloading images/alto");
            return 1;
        }

        return 0;
    }

    private void downloadPages(Manifest manifest)
            throws MalformedURLException, IOException {

        if (includeStructures != null || excludeStructures != null) {
            downloadStructures(maximumImages, downloadAlto, includeStructures, excludeStructures, selectRandomImages,
                    "firstpage".equals(structureMode), manifest);
        } else {
            Sequence seq = manifest.getSequences().get(0);
            List<Canvas> canvases = seq.getCanvases();
            if (selectRandomImages && maximumImages != null && maximumImages < canvases.size()) {
                downloadRandom(maximumImages, downloadAlto, canvases);
            } else {
                downloadSequential(maximumImages, downloadAlto, canvases);
            }
        }

    }

    private void downloadStructures(Integer maximumImages, boolean downloadAlto, List<String> includeStructureStrings,
            List<String> excludeStructureStrings, boolean selectRandomImages, boolean filterStructsFirstPage, Manifest manifest)
            throws JsonParseException, JsonMappingException, MalformedURLException, IOException {
        List<LabelValuePair> includeStructures = includeStructureStrings == null ? new ArrayList<>() : includeStructureStrings.stream()
                .map(s -> s.split("::"))
                .map(sArr -> new LabelValuePair(sArr[0], sArr[1]))
                .collect(Collectors.toList());
        List<LabelValuePair> excludeStructures = excludeStructureStrings == null ? new ArrayList<>() : excludeStructureStrings.stream()
                .map(s -> s.split("::"))
                .map(sArr -> new LabelValuePair(sArr[0], sArr[1]))
                .collect(Collectors.toList());
        List<Canvas> canvases;
        if (!includeStructures.isEmpty()) {
            // search for includeStructures in manifest and filter excludeStructures
            // first, get all included structures
            List<Range> filteredStructures = manifest.getStructures()
                    .stream()
                    .filter(struct -> ManifestQuery.filterIncludeStructure(struct, includeStructures))
                    .collect(Collectors.toList());
            // now get all canvases and filter out the ones that should be excluded
            Stream<Canvas> canvasStream;
            if (filterStructsFirstPage) {
                canvasStream = filteredStructures.stream()
                        .map(struct -> struct.getCanvases().get(0));
            } else {
                canvasStream = filteredStructures.stream()
                        .flatMap(struct -> struct.getCanvases().stream());
            }
            canvases = canvasStream
                    .filter(canvas -> ManifestQuery.filterExcludeCanvas(canvas, excludeStructures, filterStructsFirstPage, manifest))
                    .map(canvas -> ManifestQuery.canvasToFullCanvas(canvas, manifest))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } else {
            // take random or all pages and check if they are excluded, then skip them
            canvases = manifest.getSequences()
                    .stream()
                    .flatMap(seq -> seq.getCanvases().stream())
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
        for (Canvas c : canvases) {
            downloadImageAndAlto(c, downloadAlto);
        }
    }

    private void downloadRandom(Integer maximumImages, boolean downloadAlto, List<Canvas> canvases) throws JsonParseException, JsonMappingException,
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

    public void downloadSequential(Integer maximumImages, boolean downloadAlto, List<Canvas> canvases) throws IOException,
            JsonParseException, JsonMappingException, MalformedURLException {
        boolean hasAlto = downloadAlto;
        int downloadCount = 0;
        for (Canvas canvas : canvases) {
            if (maximumImages != null && downloadCount == maximumImages.intValue()) {
                break;
            }
            hasAlto = downloadImageAndAlto(canvas, hasAlto);
            downloadCount++;
        }
    }

    private boolean downloadImageAndAlto(Canvas canvas, boolean downloadAlto) throws JsonParseException, JsonMappingException, MalformedURLException,
            IOException {
        LinkedAnnotation lanno = canvas.getImages().get(0);
        //this is the base uri of the image
        ImageContent imageContent = ((ImageContent) (lanno.getResource()));
        String imageUri = imageContent.getService().getId();
        String basename = imageUri.substring(imageUri.lastIndexOf('/') + 1);
        int dotIdx = basename.lastIndexOf('.');
        if (dotIdx > 0) {
            basename = basename.substring(0, dotIdx);
        }
        String fullImageUri = imageUri + "/full/full/0/default.jpg";
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

    public Optional<URI> getAltoUrl(Canvas canvas) throws IOException, JsonParseException, JsonMappingException,
            MalformedURLException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Optional<AnnotationList> optAltoAnnotation = canvas.getOtherContent()
                .stream()
                .filter(a -> a.getLabel() != null)
                .filter(a -> a.getLabel().getValue().orElseGet(() -> "").equals("ALTO"))
                .findFirst();
        if (!optAltoAnnotation.isPresent()) {
            return Optional.empty();
        }
        AnnotationList altoAnnotation = optAltoAnnotation.get();
        try (InputStream in = altoAnnotation.getId().toURL().openStream()) {
            AnnotationList fullAltoAnno = mapper.readValue(in, AnnotationList.class);
            LinkingContent altoContent = (LinkingContent) ((LinkedAnnotation) (fullAltoAnno.getResources().get(0))).getResource();
            return Optional.of(altoContent.getId());
        }
    }

    private Optional<Manifest> getManifest(String manifest) throws MalformedURLException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Manifest iiifMani = null;

        StatusRunnable run = new StatusRunnable("Receiving IIIF manifest...");
        Thread statusThread = new Thread(run);
        statusThread.start();
        try (InputStream in = new URL(manifest).openStream()) {
            iiifMani = mapper.readValue(in, Manifest.class);
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
