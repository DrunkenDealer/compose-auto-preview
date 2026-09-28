# Changelog

## 0.3.0

- **Languages.** `@AutoPreview(locales = ["en", "de", "uk"])` renders every language on the full device × theme matrix, in Studio and in the report. The report gets a language picker (`L` cycles) that sticks across screens, and a **Languages** view that puts every language of one theme side by side. Compose Multiplatform `Res.string` follows the language too. `locale` still works and is deprecated.
- **Images are stored per language:** `<screen>/<locale>/<device>/<theme>/<state>.png`. Every cell in `manifest.json` has a `locale`, every screen a `locales` list.
- **System bars.** Phones, tablets and foldables render edge to edge with status bar and navigation bar insets, and a status bar and gesture handle drawn over the screen.
- **Device frames** for phone, tablet, TV and watch now come from Android Studio's device art (AOSP, Apache 2.0).
- A function with both `@AutoPreview` and a wrapper annotation now fails with a clear error instead of `FileAlreadyExistsException`.

## 0.2.0

- Screen groups (`group`), such as the tabs of a bottom bar, drawn as a box in the report.
- Multi-module reports with `--modules`, and `navigatesTo` across modules.

## 0.1.0

- First release under `app.mashlab.autopreview`.
