# ADR 004 — Algorithm compatibility

Status: Accepted for P0 JSON  
Date: 2026-09-09

## Context

Electron JSON tools use `JSON.parse` / `JSON.stringify`, a custom duplicate-key walker, jsonpath-plus, and fast-xml-parser. JavaFX must not silently adopt Jackson or Jayway defaults that change user-visible results.

## Decision

- JSON is implemented in-process with Jackson 2.19.2 `JsonNode`, `USE_BIG_INTEGER_FOR_INTS`, and `USE_BIG_DECIMAL_FOR_FLOATS`.
- Duplicate keys are detected by a port of Electron's `DuplicateKeyParser` **before** Map collapse. Format can refuse duplicates when the option is on.
- JSONPath uses Jayway 2.9.0 with Jackson `JsonNode` provider. Unsupported or dialect-different expressions fail visibly rather than running JavaScript.
- XML conversion is a local DOM implementation with external entities disabled. Root element name is preserved on XML→JSON.
- JavaBean conversion is a field-pattern port of `jsonTools.ts`, not a Java compiler.
- FX-D001: integers such as `9007199254740993` are preserved. Electron `JSON.parse` would lose that integer; this is an intentional correction, not a silent format change of keys or structure.

## Consequences

Pretty-print whitespace may differ from `JSON.stringify` (Jackson default colon spacing is normalized in tests via compress round-trip). JSONPath filters/unions need extra fixtures in P2. Vault/Git are not part of this algorithm ADR.
