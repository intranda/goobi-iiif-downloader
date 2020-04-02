package de.intranda.iiif.downloader;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data class to hold label-value pairs for querying manifests
 * 
 * @author Oliver Paetzel
 *
 */
@Data
@AllArgsConstructor
public class LabelValuePair {
    String label;
    String value;

    public boolean equals(String label, String value) {
        if (label == null || value == null) {
            return false;
        }
        return label.equals(this.label) && value.equals(this.value);
    }
}
