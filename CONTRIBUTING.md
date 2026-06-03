# Contributing

Thanks for helping improve CZI Spatial Slide Viewer.

## Development Setup

- Use Java 17.
- Use QuPath 0.5.x for manual testing.
- Keep large microscopy files out of Git. The `data/` and `outputs/` folders are ignored.

## Build And Test

Windows:

```powershell
gradlew.bat build
```

macOS/Linux:

```bash
./gradlew build
```

Run automated tests before opening a pull request:

```bash
./gradlew test
```

## Pull Requests

- Keep changes focused.
- Add or update tests for coordinate mapping, pyramid selection, overlap handling, or manifest fields.
- Update `README.md`, `TESTING.md`, or `CHANGELOG.md` when behavior changes.
- Do not commit `.czi`, `.svs`, `.tif`, `.zarr`, generated outputs, local JDKs, or build artifacts.

## Support Bundles

For debugging private or very large files, prefer sharing the generated support bundle rather than raw image data:

```text
outputs/[safe_filename]_support_bundle.zip
```

Support bundles include manifests, previews, reports, and system metadata, but not full-resolution raw image pixels.
