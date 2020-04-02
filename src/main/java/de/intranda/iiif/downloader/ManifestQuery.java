package de.intranda.iiif.downloader;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * This class contains fully pure functions to query IIIF manifests without side-effects
 * 
 * @author Oliver Paetzel
 *
 */
public class ManifestQuery {

    public static Optional<JsonNode> canvasToFullCanvas(JsonNode canvas, JsonNode manifest) {
        return streamJsonNodeAsArray(manifest.get("sequences").get(0).get("canvases"))
                .filter(c -> idsEqual(c.isTextual() ? c.asText() : c.get("@id").asText(),
                        canvas.isTextual() ? canvas.asText() : canvas.get("@id").asText()))
                .findFirst();
    }

    public static Stream<JsonNode> streamJsonNodeAsArray(JsonNode arr) {
        if (arr == null) {
            return Stream.empty();
        }
        return IntStream.range(0, arr.size())
                .mapToObj(arr::get);
    }

    public static Stream<JsonNode> streamAllCanvasStructures(JsonNode canvas, boolean firstPageOnly, JsonNode manifest) {
        Stream<JsonNode> structStream = streamJsonNodeAsArray(manifest.get("structures"));
        if (firstPageOnly) {
            return structStream
                    .filter(struct -> struct.get("canvases") != null && struct.get("canvases").isArray()
                            && idsEqual(struct.get("canvases").get(0).asText(), (canvas.get("@id").asText())));
        }
        return structStream
                .filter(s -> s != null)
                .filter(struct -> struct.get("canvases") != null &&
                        streamJsonNodeAsArray(struct.get("canvases"))
                                .anyMatch(c -> idsEqual(c.asText(), canvas.get("@id").asText())));
    }

    public static boolean filterExcludeCanvas(JsonNode canvas, List<LabelValuePair> exclude, boolean filterStructsFirstPage, JsonNode manifest) {
        if (exclude.isEmpty()) {
            return true;
        }
        // get all structures pointing at this canvas, then check that none of these have any value in exclude
        return streamAllCanvasStructures(canvas, filterStructsFirstPage, manifest)
                .flatMap(struct -> streamJsonNodeAsArray(struct.get("metadata")))
                .noneMatch(meta -> metaContainsAnyLabelValuePair(meta, exclude));
    }

    public static boolean filterIncludeStructure(JsonNode struct, List<LabelValuePair> include) {
        if (struct == null || struct.get("metadata") == null || include == null) {
            return false;
        }
        return streamJsonNodeAsArray(struct.get("metadata"))
                .anyMatch(meta -> metaContainsAnyLabelValuePair(meta, include));
    }

    private static boolean metaContainsAnyLabelValuePair(JsonNode meta, List<LabelValuePair> include) {
        JsonNode labelNode = meta.get("label");
        JsonNode valueNode = meta.get("value");
        //check multi-language metadata values
        if (labelNode == null || valueNode == null) {
            return false;
        }
        if (labelNode.isArray()) {
            if (!valueNode.isArray()) {
                //TODO: check when this happens...
                return false;
            }
            ArrayNode labelArr = (ArrayNode) meta.get("label");
            ArrayNode valueArr = (ArrayNode) meta.get("value");
            for (int i = 0; i < labelArr.size(); i++) {
                JsonNode currLabelNode = labelArr.get(i);
                JsonNode currValueNode = valueArr.get(i);
                if (currValueNode == null) {
                    continue;
                }
                String label = currLabelNode.isTextual() ? currLabelNode.asText() : currLabelNode.get("@value").asText();
                String value = currValueNode.isTextual() ? currValueNode.asText() : currValueNode.get("@value").asText();
                if (include.stream().anyMatch(lvp -> lvp.equals(label, value))) {
                    return true;
                }
            }
        } else {
            String label = labelNode.isTextual() ? labelNode.asText() : labelNode.get("@value").asText();
            String value = valueNode.isTextual() ? valueNode.asText() : valueNode.get("@value").asText();
            return include.stream().anyMatch(lvp -> lvp.equals(label, value));
        }
        return false;
    }

    private static boolean idsEqual(final String id1, final String id2) {
        if (id1 == null || id2 == null) {
            return false;
        }
        String cmp1 = id1;
        String cmp2 = id2;
        if (!cmp1.endsWith("/")) {
            cmp1 += "/";
        }
        if (!cmp2.endsWith("/")) {
            cmp2 += cmp2;
        }
        return cmp1.equals(cmp2);
    }
}
