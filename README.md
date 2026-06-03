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
build/libs/CZI-Spatial-Slide-Viewer-0.2.0-alpha-all.jar
```

The smaller non-`all` JAR is not recommended unless you manage dependencies yourself.

## Install In QuPath

1. Build `shadowJar` or `build`.
2. Drag `build/libs/CZI-Spatial-Slide-Viewer-0.2.0-alpha-all.jar` into QuPath.
3. Restart QuPath if prompted.
4. Use `Extensions > CZI Spatial Viewer`.

## Menus

- `Open CZI Spatial Slide`
- `Export CZI Layout Manifest`
- `Export Low-Resolution Layout Preview`
- `Export Contact Sheet`
- `Export Support Bundle`
- `Run Debug Test on Sepsis CZI`
- `Plugin Settings`
- `About CZI Spatial Viewer`

Opening and export commands now use a file picker and are not tied to the sepsis validation file.

## Plugin Settings

`Extensions > CZI Spatial Viewer > Plugin Settings` opens an editable settings dialog. The most important visual setting is `Make light blank scene background transparent while compositing`, which is enabled by default. It helps Axioscan brightfield files match HALO more closely when rectangular scene backgrounds overlap neighboring tissue. It does not move scenes or change their coordinates; it only prevents light neutral blank background pixels from visually covering tissue in the rendered QuPath view.

## Validation File

The repository test file path remains:

```text
data/Axio_4_100a_primary sci_sepsis_2025_03_10_032.czi
```

CLI debug tasks use that file by default:

```powershell
gradlew.bat exportCziDebug
gradlew.bat exportCziRegionSmoke
gradlew.bat exportCziSupportBundle
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
- Thread safety uses synchronized Bio-Formats reader access rather than a reader pool.
- Multichannel display uses Bio-Formats default `BufferedImage` conversion; advanced channel controls are future work.
- OpenSlide/libCZI backends are placeholders.

## Troubleshooting

- If a file opens as blank, export a support bundle and inspect warnings.
- If scenes are missing, check whether they lacked stage X/Y metadata.
- If performance is slow, use the performance report and reduce cache pressure.
- Unsupported/corrupt scenes should be marked while other scenes continue when possible.
