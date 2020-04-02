package de.intranda.iiif.downloader;

/**
 * This class contains fully pure functions to query IIIF manifests without side-effects
 * 
 * @author Oliver Paetzel
 *
 */
public class ManifestQuery {
    /*
    public static Optional<> canvasToFullCanvas(Canvas canvas, Manifest manifest) {
        return manifest.getSequences()
                .get(0)
                .getCanvases()
                .stream()
                .filter(c -> c.getId().equals(canvas.getId()))
                .findFirst();
    }
    
    public static Stream<Range> streamAllCanvasStructures(Canvas canvas, boolean firstPageOnly, Manifest manifest) {
        if (firstPageOnly) {
            return manifest.getStructures()
                    .stream()
                    .filter(struct -> struct.getCanvases() != null && !struct.getCanvases().isEmpty()
                            && struct.getCanvases().get(0).getId().equals(canvas.getId()));
        }
        return manifest.getStructures()
                .stream()
                .filter(struct -> struct.getCanvases() != null && struct.getCanvases().stream().anyMatch(c -> c.getId().equals(canvas.getId())));
    }
    
    public static boolean filterExcludeCanvas(Canvas canvas, List<LabelValuePair> exclude, boolean filterStructsFirstPage, Manifest manifest) {
        if (exclude.isEmpty()) {
            return true;
        }
        // get all structures pointing at this canvas, then check that none of these have any value in exclude
        return streamAllCanvasStructures(canvas, filterStructsFirstPage, manifest)
                .flatMap(struct -> struct.getMetadata().stream())
                .noneMatch(meta -> metaContainsAnyLabelValuePair(meta, exclude));
    }
    
    public static boolean filterIncludeStructure(Range struct, List<LabelValuePair> include) {
        if (struct == null || struct.getMetadata() == null || include == null) {
            return false;
        }
        return struct.getMetadata()
                .stream()
                .anyMatch(meta -> metaContainsAnyLabelValuePair(meta, include));
    }
    
    private static boolean metaContainsAnyLabelValuePair(Metadata meta, List<LabelValuePair> include) {
        //check multi-language metadata values
        return meta.getLabel()
                .getLanguages()
                .stream()
                .anyMatch(lang -> {
                    return include.stream()
                            .anyMatch(lvp -> lvp.equals(meta.getLabel().getValue(lang), meta.getValue().getValue(lang)));
                });
    }*/
}
