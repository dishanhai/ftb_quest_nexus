# Client integration corrections

## 2026-11-07: isolated javac test classpath

- Status: resolved.
- First isolated header-layout compilation omitted JUnit jars and therefore also reported missing test annotations. Reran with cached junit-jupiter-api, opentest4j and apiguardian jars; the remaining failures were exclusively the not-yet-implemented layout API, then implemented the helper. Use the configured Gradle test task for final verification.

## 2026-11-07: shared UI source locations

- Status: resolved.
- An assumed Java source path for GlassPanelTexture did not exist. Located it with rg --files under src/main/kotlin; GuiMotion resides under client/gui/scaled. Native buttons now reuse the same shared Spring.HOVER and palette interpolation without depending on the LDLib adapter.
