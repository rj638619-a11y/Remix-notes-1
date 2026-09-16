## 2026-09-09 - Memoize Jetpack Compose list filtering in HomeTab

**Learning:** Jetpack Compose composables like `HomeTab` can re-compose frequently (e.g., during layout passes, animation frames, or state changes). Performing list iteration methods like `.count { ... }` and `.filter { ... }` directly in the top-level body of a `@Composable` forces O(N) calculations on every single composition frame.
**Action:** Wrap collection filtering and counting in `remember(notes, selectedFilter) { ... }` so calculations only re-run when the input list or filter state actually changes.
