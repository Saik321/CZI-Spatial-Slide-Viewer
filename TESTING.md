# Testing Matrix

Automated tests use fake metadata so they can run without huge CZI files.

## Automated Tests

- Global bounding box calculation
- Coordinate normalization
- Negative coordinates
- Fractional coordinates
- Huge coordinates
- Gaps and overlaps
- Duplicate scene coordinates
- Single-scene metadata
- 100+ scene metadata
- Physical-to-pixel conversion
- Region intersection logic
- Regular pyramid levels: `1, 2, 4, 8, 16`
- Irregular pyramid levels: `1, 3.9, 7.8`
- Missing pyramid fallback
- Manifest field output
- Non-spatial/skipped series manifest fields
- HALO validation report export
- Safe output file naming

Run:

```powershell
gradlew.bat test
```

## Manual Real-File Tests

Use a local non-public validation file, for example:

```text
data/example_validation.czi
```

Checklist:

- Build all-in-one JAR.
- Install into QuPath 0.5.x.
- Record the QuPath version. QuPath 0.6.x/0.7.x require a Java 21 compatibility build and are not fully validated by the Java 17 build.
- Open a spatial multi-scene CZI with `Open CZI Spatial Slide`.
- Import a CZI into a QuPath project and verify CZI Spatial Viewer is selected as the server.
- Confirm one global canvas appears.
- Confirm blank gaps remain white.
- Confirm far-left and far-right scenes show real pixels.
- Confirm overlapping brightfield scene backgrounds do not visibly cover neighboring tissue when background transparency is enabled.
- Zoom out and pan across multiple rows of tissue.
- Zoom into at least three different scenes.
- Open a fluorescence/multichannel CZI and verify the default RGB composite displays multiple channels rather than only a single plane.
- Draw annotation on Scene 0.
- Draw annotation on a far-right scene.
- Draw annotation crossing a blank gap.
- Save project.
- Reopen project.
- Verify annotation alignment.
- Run a small QuPath detection on a tissue region.
- Export layout preview and compare to HALO screenshot.
- Export HALO-style preview and compare the top label/macro band plus tissue layout to HALO.
- Export contact sheet and confirm skipped/non-spatial items are labeled.
- Inspect `outputs/[safe_filename]_halo_validation_report.txt`.
- Export support bundle.
- Run for 5 minutes of pan/zoom and watch memory.

## Additional File Classes To Test

- Small single-scene CZI.
- Multi-scene CZI with large gaps.
- Multi-scene CZI with overlapping scenes.
- CZI with no pyramid.
- CZI with native or series-based pyramid.
- Brightfield RGB CZI.
- Single-channel grayscale CZI.
- Fluorescence/multichannel CZI.
- Huge file over 10 GB.
- Partially corrupt/readable CZI.

## Smoke Test

This reads a real pixel region through the QuPath `ImageServer` path:

```powershell
gradlew.bat exportCziRegionSmoke -PinputCzi="C:\path\to\slide.czi"
```

Expected output:

```text
outputs/[safe_filename]_performance_report.txt
outputs/[safe_filename]_halo_validation_report.txt
outputs/[safe_filename]_halo_style_preview.png
outputs/czi_region_smoke.png
```
