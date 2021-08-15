# Goobi IIIF downloader

The Goobi IIIF downloader is a command line utility that allows one to
download images and OCR results using IIIF manifests.

It is also possible to limit the downloaded pages by structures. The structures can be filtered by including or excluding metadata label/value pairs.

## Usage:

The binaries can be downloaded in the github releases section. The CLI can then be called as follows:

```
Usage: java -jar goobi-iiif-downloader.jar [-da] [-ri] -d=<destinationFolder> -m=<manifestUrl> [-max=<maximumImages>] [-sm=<structureMode>] [-es=<excludeStructures>]... [-is=<includeStructures>]...
  -m, --manifest=<manifestUrl>
                             the manifest URL to parse and download from
  -d, --destination=<destinationFolder>
                             the destination folder to download to
      -is, --include_structure=<includeStructures>
                             structure to include - example: "Strukturtyp::Abbildung". The option is repeatable.
      -es, --exclude_structure=<excludeStructures>
                             structure to exclude - example: "Strukturtyp::Abbildung". The option is repeatable.
      -sm, --structure_mode=<structureMode>
                             structure mode. Possible values: "firstpage" and "all"
      -max, --maximum_images=<maximumImages>
                             the maximum number of images to download
      -ri, --random_images   select random images
      -da, --download_alto   download alto (if present)
```


