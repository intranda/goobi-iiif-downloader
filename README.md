# Goobi IIIF downloader

The Goobi IIIF downloader is a command line utility that allows one to
download images and OCR results using IIIF manifests.

Usage:
```bash
Usage: <main class> [-da] [-ri] [-d=<destinationFolder>] [-m=<manifestUrl>]
                    [-max=<maximumImages>]
  -d, --destination=<destinationFolder>
                             the destination folder to download to
      -da, --download_alto   download alto (if present)
  -m, --manifest=<manifestUrl>
                             the manifest URL to parse and download from
      -max, --maximum_images=<maximumImages>
                             the maximum number of images to download
      -ri, --random_images   select random images
```
