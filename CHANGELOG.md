# Changelog

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
- Added `Export Contact Sheet` and `Run Debug Test on Sepsis CZI` menu items.
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
