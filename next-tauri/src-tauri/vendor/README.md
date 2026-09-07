# Vendored Rust patches

## `tauri-runtime-wry` 2.11.4

This is the crates.io source for `tauri-runtime-wry` 2.11.4 with one focused
change in `WebviewDispatcher::reparent`: copy the current window ID and release
its mutex before waiting for the event-loop reply.

Without the patch, a concurrent IPC reply can deadlock with WebView reparenting
on Windows and macOS. The native acceptance suite reproduced it while detaching
the Config tool on Windows.

Upstream tracking issue: <https://github.com/tauri-apps/tauri/issues/15489>

Remove the `[patch.crates-io]` override and this directory once a released
`tauri-runtime-wry` contains the upstream fix.

## `glib` 0.18.5

This is the crates.io source for `glib` 0.18.5, including its original copyright
and license files, with the two-line `VariantStrIter::impl_get` safety fix from
[gtk-rs/gtk-rs-core#1343](https://github.com/gtk-rs/gtk-rs-core/pull/1343)
(commit `b5a4071e439bef2b5eea76c3aa25e5ae84839e34`, merged as
`05dff0ee696f9bcd8617cd48c4b812d046d440cb`). No other upstream source changes
are applied.

Source archive: `glib-0.18.5.crate` from crates.io.
SHA-256: `233daaf6e83ae6a12a52055f568f9d7cf4671dabb78ff9560ab6da230ce00ee5`.

The patch makes the C out-pointer mutable and passes `&mut p` rather than
`&p`, addressing [RUSTSEC-2024-0429](https://rustsec.org/advisories/RUSTSEC-2024-0429.html).
The unpatched immutable out-pointer violates Rust's aliasing rules and can
produce null-pointer crashes in optimized builds. Tauri's GTK3 stack requires
GLib 0.18, so the upstream GLib 0.20 release cannot be substituted independently.

`tests/glib_variant_str_iter.rs` exercises forward/backward iteration, skipping,
last-element access, empty/exhausted arrays and borrowed Unicode strings. Linux
CI runs it in both the ordinary test suite and explicitly in `--release` mode.
Do not treat a macOS/Windows test run, where this Linux-only test is excluded,
as evidence that the regression passed.

Remove this override and vendor directory once the GTK3-compatible dependency
chain provides the upstream fix, or after a separately verified runtime upgrade
to a fixed GLib release. The original version is retained to reflect the source
provenance; this is a documented backport, not an upstream 0.20 release or a
security-alert exemption.
