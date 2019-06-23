package de.intranda.iiif.downloader;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LabelValuePair {
    String label;
    String value;

    public boolean equals(Optional<String> label, Optional<String> value) {
        if (!label.isPresent() || !value.isPresent()) {
            return false;
        }
        return label.get().equals(this.label) && value.get().equals(this.value);
    }
}
