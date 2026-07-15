#!/usr/bin/env python3
"""DEPRECATED: network translation is slow/unreliable. Use build_ja_from_zh.py instead.

Generate messages_ja.properties from messages_en.properties using Google Translate.
"""

from __future__ import annotations

import re
import time
from pathlib import Path

try:
    from deep_translator import GoogleTranslator
except ImportError:
    raise SystemExit("Install deep-translator: pip install deep-translator")

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/resources/i18n/messages_en.properties"
DST = ROOT / "src/main/resources/i18n/messages_ja.properties"

SKIP_VALUE_PATTERN = re.compile(
    r"^(https?://|git@|<html|Ctrl\+|Alt\+|option\+|MooTool|Flat |GitHub|Gitee|Bing|Google|JSON|XML|SQL|HTML|CSS|Java|Base64|OK|PicPick|PicPick|WePush|MIT |Copyright|v\d|\{[0-9]\}|✓|✗|[\d.]+%|[A-Z][a-z]+ [A-Z])",
    re.IGNORECASE,
)

MANUAL: dict[str, str] = {
    "language.en": "English",
    "language.zhCN": "簡体中国語",
    "language.ja": "日本語",
    "language.prompt.title": "言語を選択",
    "language.prompt.message": "MooTool の表示言語を選択してください。",
    "language.prompt.switchToZh": "簡体中国語に切り替え",
    "language.prompt.switchToJa": "日本語に切り替え",
    "language.prompt.keepEnglish": "英語のまま",
    "language.prompt.continue": "続行",
    "language.restart.title": "再起動が必要です",
    "language.restart.message": "言語を変更しました。MooTool を再起動してください。",
    "common.ok": "OK",
    "tab.mootool": "MooTool",
    "base64Dialog.title": "Base64",
    "translation.translator.google": "Google翻訳",
    "translation.translator.bing": "Bing翻訳",
}


def should_skip(value: str) -> bool:
    stripped = value.strip()
    if not stripped:
        return True
    if stripped in MANUAL.values():
        return True
    if "{" in stripped and "}" in stripped and len(stripped) < 120:
        return False
    if SKIP_VALUE_PATTERN.match(stripped):
        return True
    if re.fullmatch(r"[\w\s\-\.+/:,;!?#&=()\\*\"'<>[\]{}|@$%^~`]+", stripped) and not re.search(
        r"[ぁ-んァ-ン一-龥]", stripped
    ):
        # Mostly ASCII technical tokens
        if len(stripped) < 40 and not any(w in stripped.lower() for w in ("please", "click", "select", "enter", "failed", "success")):
            return True
    return False


def translate_batch(translator: GoogleTranslator, texts: list[str]) -> list[str]:
    if not texts:
        return []
    try:
        return translator.translate_batch(texts)
    except Exception:
        out = []
        for text in texts:
            try:
                out.append(translator.translate(text))
                time.sleep(0.05)
            except Exception:
                out.append(text)
        return out


def main() -> None:
    lines = SRC.read_text(encoding="utf-8").splitlines()
    translator = GoogleTranslator(source="en", target="ja")

    out_lines: list[str] = []
    pending_keys: list[str] = []
    pending_values: list[str] = []
    pending_line_indices: list[int] = []

    def flush_batch() -> None:
        nonlocal pending_keys, pending_values, pending_line_indices
        if not pending_values:
            return
        translated = translate_batch(translator, pending_values)
        for key, val, tr in zip(pending_keys, pending_values, translated):
            out_lines.append(f"{key}={tr if tr else val}")
        pending_keys = []
        pending_values = []
        pending_line_indices = []
        time.sleep(0.1)

    for line in lines:
        if not line or line.lstrip().startswith("#"):
            flush_batch()
            out_lines.append(line)
            continue
        if "=" not in line:
            flush_batch()
            out_lines.append(line)
            continue
        key, value = line.split("=", 1)
        if key in MANUAL:
            flush_batch()
            out_lines.append(f"{key}={MANUAL[key]}")
            continue
        if should_skip(value):
            flush_batch()
            out_lines.append(line)
            continue
        pending_keys.append(key)
        pending_values.append(value)
        if len(pending_values) >= 40:
            flush_batch()

    flush_batch()
    DST.write_text("\n".join(out_lines) + "\n", encoding="utf-8")
    print(f"Wrote {DST} ({len(out_lines)} lines)")


if __name__ == "__main__":
    main()
