# Changelog

## 0.2.1-alpha

- Switched project save/reopen support to QuPath's default serializable image-server builder to fix "unable to serialize" errors when saving annotations, view settings, and project state.
- Hardened project-import URI validation and error messages for missing, remote, unreadable, or non-CZI inputs.
- Hardened Bio-Formats OME metadata lookups so short channel/plane metadata arrays become warnings instead of open failures.
- Verified the release all-JAR contains the plugin settings command used by the QuPath menu.
- Fixed native pyramid selection so zooming uses the selected Bio-Formats resolution instead of always reading resolution 0.
- Preserved internal Bio-Formats pyramid resolutions when grouping CZI series into spatial scenes.
- Added a conservative same-column vertical overlap correction for scenes whose CZI metadata appears to use tissue-bounding-box origins instead of a shared tile origin.
- Added multidimensional CZI diagnostics and default middle-Z display for Z-stacks instead of silently showing Z=0.
- Added native non-RGB multichannel metadata and banded `UINT8`/`UINT16` pixel returns for QuPath channel rendering.
- Added a bounded Bio-Formats reader pool for concurrent tile reads.
- Lowered default tile cache memory from 512 MB to 128 MB.
- Replaced Windows-only JavaFX dependencies with platform-aware JavaFX Gradle plugin configuration.
- Removed private validation slide filename from public docs, menu text, and debug defaults.
- Added generic `data/example_validation.czi` debug default plus `-PinputCzi=...` task support.
- Improved QuPath project import selection by increasing CZI Spatial Viewer server support priority for `.czi/.czis` files.
- Improved default fluorescence/multichannel rendering by compositing non-RGB `UINT8`/`UINT16` channel planes into a stable RGB display.
- Reworded CZI compression diagnostics to clarify that Bio-Formats pixel reading is attempted.

## 0.2.0-alpha

- Added generic CZI file picker for QuPath open/export commands.
- Added safe per-file output naming.
- Added support bundle export.
- Added contact sheet export.
- Added all-series contact sheet behavior with safe placeholders for large non-spatial items.
- Added HALO validation report export and support-bundle inclusion.
- Added HALO-style preview export with inferred non-spatial label/macro presentation band.
- Added editable plugin settings dialog with persisted cache, preview, overlay, and compositing options.
- Added optional light-background transparency while compositing overlapping brightfield scene rectangles.
- Added explicit non-spatial/skipped series metadata in manifest/debug report.
- Added `Export Contact Sheet` and `Run Debug Test on Example CZI` menu items.
- Added real thumbnail layout preview fallback behavior.
- Added about menu placeholder.
- Added leveled diagnostics in manifest/debug report.
- Added runtime/file/plugin metadata to manifest.
- Added bounded LRU image cache for rendered regions.
- Added performance stats and performance report export.
- Added Bio-Formats backend version reporting.
- Added backend interface hooks for future OpenSlide/libCZI backends.
- Added source series index to pyramid manifest entries.
- Added more coordinate and pyramid tests.

## 0.1.0-alpha

- Initial QuPath extension skeleton.
- Bio-Formats metadata reader.
- Spatial manifest/debug/preview export.
- Global coordinate mapper.
- Mock QuPath rectangle viewer.
- Real lazy Bio-Formats pixel reading and composition.
