# Innovation PDF Toolkit

PDF utility app — Kotlin native, Jetpack Compose + Material3, no external PDF library.
Brand: Innovation-313 | App ID: `com.innovation313.pdftoolkit`

## Scope (v1 MVP)

- **Merge PDFs** — combine 2+ PDFs into one
- **Split PDF** — extract a page range into a new file
- **Images to PDF** — combine selected photos into a single PDF (scan-to-PDF alternative)
- **Compress PDF** — re-render pages at a lower resolution to shrink file size

Trilingual: English (default), Urdu (اردو, RTL), Roman Urdu (`values-b+ur+Latn`).

## Important honest trade-off — please read before testing

This app uses only Android's built-in `android.graphics.pdf.PdfRenderer` / `PdfDocument` APIs —
no iText, no PdfBox, no paid/licensed library (per instruction, to avoid unapproved dependencies).

The Android framework's `PdfRenderer` only exposes **rasterized page images**, not the original
vector/text content. This means:

- **Merge / Split / Compress** output is **image-based** — visually identical to the source pages,
  but text in the output PDF is **not selectable or searchable** (it was a picture of the text,
  not the text itself). File size and visual sharpness are controlled by the `RenderQuality` level
  (Compressed / Standard / High) chosen in the UI.
- **Images → PDF** has no such limitation — it was never text to begin with.

This is a genuine limitation of building without a third-party PDF library, not a bug. If
text-selectable merge/split turns out to be a hard requirement later, the realistic options are:
a licensed library (commercial cost) or a permissively-licensed one (would need review) —
flagging this now rather than after the app is built.

## Architecture

- `data/PdfEngine.kt` — all PDF operations (merge, split, images-to-PDF, compress), coroutine-based,
  runs off the main thread via `Dispatchers.IO`
- `viewmodel/PdfToolViewModel.kt` — shared state across all 4 tool screens (file selection,
  processing state, results), reset on navigating back to Home
- `ui/screens/HomeScreen.kt` — tool grid
- `ui/screens/{Merge,Split,ImagesToPdf,Compress}Screen.kt` — one screen per tool
- `ui/screens/Common.kt` — shared quality selector + result/progress display
- File access uses the **Storage Access Framework** (`OpenDocument` / `OpenMultipleDocuments` /
  `CreateDocument`) — no broad storage permission needed on any Android version

## Not yet done (next session)

- Launcher icon is a placeholder vector (brand teal + document glyph) — refine before Play Store
- No unit tests yet for `PdfEngine` (the render/write functions need an instrumented test since
  they depend on Android framework classes, not plain JVM — different from Converter's pure-Kotlin
  `ConversionEngine`)
- No page reordering / rotation / delete-single-page tools yet — could be v1.1
- Compression only controls render resolution; no separate JPEG re-encoding pass for embedded
  images within already-image-based source PDFs

## CI

`.github/workflows/build.yml` builds a debug APK and **publishes it as a GitHub Release asset**
on every push to `main` — this was added from the start here (learned from the Converter app,
where Actions-artifact downloads hit a network restriction and had to be fixed after the fact).
