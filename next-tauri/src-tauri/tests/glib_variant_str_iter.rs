//! RUSTSEC-2024-0429: also run this test with --release, because optimization can
//! expose the invalid immutable out-pointer in the unpatched GLib 0.18.5.
#![cfg(target_os = "linux")]

use glib::{Variant, prelude::*};

fn strings(values: &[&str]) -> Variant {
    Variant::array_from_iter::<String>(values.iter().map(|value| value.to_variant()))
}

#[test]
fn string_variant_iteration_preserves_borrowed_unicode_in_both_directions() {
    let values = ["alpha", "中文", "日本語", "", "😀", "omega"];
    let variant = strings(&values);
    assert_eq!(
        variant.array_iter_str().unwrap().collect::<Vec<_>>(),
        values
    );
    assert_eq!(
        variant.array_iter_str().unwrap().rev().collect::<Vec<_>>(),
        values.into_iter().rev().collect::<Vec<_>>()
    );
    assert_eq!(variant.array_iter_str().unwrap().last(), Some("omega"));
}

#[test]
fn string_variant_iteration_supports_skipping_and_mixed_ends() {
    let variant = strings(&["alpha", "中文", "日本語", "", "😀", "omega"]);
    let mut iter = variant.array_iter_str().unwrap();
    assert_eq!(iter.len(), 6);
    assert_eq!(iter.nth(1), Some("中文"));
    assert_eq!(iter.next_back(), Some("omega"));
    assert_eq!(iter.nth_back(1), Some(""));
    assert_eq!(iter.next(), Some("日本語"));
    assert_eq!(iter.len(), 0);
    assert_eq!(iter.next(), None);
    assert_eq!(iter.next_back(), None);
}

#[test]
fn empty_and_exhausted_string_variants_do_not_dereference_output_pointers() {
    let empty = strings(&[]);
    assert_eq!(empty.array_iter_str().unwrap().next(), None);
    assert_eq!(empty.array_iter_str().unwrap().next_back(), None);
    assert_eq!(empty.array_iter_str().unwrap().last(), None);

    let variant = strings(&["one"]);
    let mut iter = variant.array_iter_str().unwrap();
    assert_eq!(iter.nth(usize::MAX), None);
    assert_eq!(iter.next(), None);
    assert_eq!(iter.next_back(), None);
    let mut iter = variant.array_iter_str().unwrap();
    assert_eq!(iter.nth_back(usize::MAX), None);
    assert_eq!(iter.next(), None);
    assert_eq!(iter.next_back(), None);
}
