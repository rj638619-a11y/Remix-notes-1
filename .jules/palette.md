## 2026-09-09 - Descriptive accessibility labels for search input actions

**Learning:** Search field controls in Android/Jetpack Compose (such as clear buttons and search decorative/action icons) require descriptive `contentDescription` text instead of vague labels like "Clear" or `null` so screen readers (TalkBack) can articulate the exact action (e.g., "Clear search text").
**Action:** Always provide unambiguous action-oriented `contentDescription` strings for icon-only buttons in search inputs.
