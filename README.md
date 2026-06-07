# CZI Spatial Slide Viewer

Public alpha QuPath extension for Zeiss Axioscan multi-scene CZI files.

This extension does not stitch or fuse scenes. It creates a virtual coordinate-correct canvas and reads CZI pixels lazily through Bio-Formats. Blank gaps between scenes remain white/background regions.

## What It Does

- Opens compatible spatial multi-scene CZI files as one QuPath image.
- Converts stage coordinates into normalized global pixel coordinates.
- Preserves gaps, non-grid layouts, different scene sizes, and overlaps.
- Reads only requested regions from intersecting scenes.
- Uses native pyramid levels where Bio-Formats exposes them as series.
- Detects and reports spatial scenes separately from skipped/non-spatial series such as label, macro, thumbnail, or attachment-like images.
- Exports manifest JSON, debug report, layout preview, contact sheet, HALO validation report, and support bundle.

## Current Backend

The current backend is Bio-Formats. OpenSlide/libCZI support may be added later; placeholder backend classes are present for future work.

Tested target:
- QuPath `0.5.x`
- Java `17`

QuPath `0.6.x` and `0.7.x` use Java 21-based artifacts. This Java 17 build does not claim verified compatibility with those versions yet; a separate Java 21 validation/build lane is needed before advertising 0.6+/0.7 support.

## Build

Windows:

```powershell
gradlew.bat build
```

macOS/Linux:

```bash
./gradlew build
```

Use the all-in-one JAR:

```text
build/libs/CZI-Spatial-Slide-Viewer-0.2.1-alpha-all.jar
```

The smaller non-`all` JAR is not recommended unless you manage dependencies yourself.

## Install In QuPath

1. Build `shadowJar` or `build`.
2. Drag `build/libs/CZI-Spatial-Slide-Viewer-0.2.1-alpha-all.jar` into QuPath.
3. Restart QuPath if prompted.
4. Use `Extensions > CZI Spatial Viewer`.

## Menus

- `Open CZI Spatial Slide`
- `Export CZI Layout Manifest`
- `Export Low-Resolution Layout Preview`
- `Export Contact Sheet`
- `Export Support Bundle`
- `Run Debug Test on Example CZI`
- `Plugin Settings`
- `About CZI Spatial Viewer`

Opening and export commands use a file picker and are not tied to a specific validation file.

## Plugin Settings

`Extensions > CZI Spatial Viewer > Plugin Settings` opens an editable settings dialog. The most important visual setting is `Make light blank scene background transparent while compositing`, which is enabled by default. It helps Axioscan brightfield files match HALO more closely when rectangular scene backgrounds overlap neighboring tissue. It does not move scenes or change their coordinates; it only prevents light neutral blank background pixels from visually covering tissue in the rendered QuPath view.

## Validation File

The optional local validation path is:

```text
data/example_validation.czi
```

Do not commit private or investigator-owned microscopy files. To run debug tasks on a local CZI without renaming it, pass `-PinputCzi`:

```powershell
gradlew.bat exportCziDebug -PinputCzi="C:\path\to\slide.czi"
gradlew.bat exportCziRegionSmoke -PinputCzi="C:\path\to\slide.czi"
gradlew.bat exportCziSupportBundle -PinputCzi="C:\path\to\slide.czi"
```

## Outputs

Exports use safe input-based names:

```text
outputs/[safe_filename]_manifest.json
outputs/[safe_filename]_layout_preview.png
outputs/[safe_filename]_halo_style_preview.png
outputs/[safe_filename]_debug_report.txt
outputs/[safe_filename]_contact_sheet.png
outputs/[safe_filename]_halo_validation_report.txt
outputs/[safe_filename]_performance_report.txt
outputs/[safe_filename]_support_bundle.zip
```

The layout preview is the strict spatial canvas: it preserves coordinate-derived tissue layout, white gaps, and low-resolution real thumbnails where safe. The HALO-style preview adds an inferred top presentation band for readable non-spatial label/macro/thumbnail items, which helps compare against HALO screenshots without inserting those items into analysis coordinates. The contact sheet audits every detected Bio-Formats series/item. The HALO validation report summarizes canvas aspect ratio, left/right/top/bottom scenes, skipped items, readable thumbnails, and a manual comparison checklist.

## Current Limitations

- Alpha quality: tested primarily with the provided Axioscan CZI.
- Bio-Formats exposes some CZI pyramids as separate series; the extension groups same-stage series heuristically.
- Series without usable X/Y stage metadata are excluded from the analysis canvas but listed in the manifest, debug report, contact sheet, and support bundle.
- HALO-style preview placement for non-spatial label/macro items is inferred for visual comparison only; it is not used for QuPath measurements or annotations.
- Settings are editable and persisted locally using Java preferences.
- Thread safety uses a bounded Bio-Formats reader pool for concurrent tile requests.
- Multichannel fluorescence display declares `rgb(false)` metadata and preserves `UINT8`/`UINT16` channel bands where Bio-Formats exposes them. Full channel color/contrast presets beyond Bio-Formats names/default colors are future work.
- Z-stack CZI files display the middle Z plane by default and report the Z count in manifests/support bundles. Full Z navigation and lazy Z projection are future work.
- OpenSlide/libCZI backends are placeholders.

## Troubleshooting

- If a file opens as blank, export a support bundle and inspect warnings.
- If scenes are missing, check whether they lacked stage X/Y metadata.
- If performance is slow, use the performance report and reduce cache pressure.
- Unsupported/corrupt scenes should be marked while other scenes continue when possible.
