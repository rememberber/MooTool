#!/usr/bin/env python3
"""Generate unified toolbar SVG icons from Lucide (consistent stroke weight)."""
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LUCIDE_DIR = ROOT / "package/icons"
OUT_DIR = ROOT / "src/main/resources/icon"
COLOR = "#cdcdcd"
SKIP = {"MooInfo.svg"}

# project filename -> lucide icon filename (without .svg)
ICON_MAP = {
    "refresh.svg": "refresh-cw",
    "QRcode.svg": "qr-code",
    "actual_size.svg": "ratio",
    "add.svg": "plus",
    "bean.svg": "bean",
    "calculate.svg": "calculator",
    "check.svg": "check",
    "close.svg": "x",
    "color.svg": "palette",
    "color_picker.svg": "pipette",
    "compress_json.svg": "minimize-2",
    "copy.svg": "copy",
    "diff.svg": "git-compare",
    "down.svg": "arrow-down",
    "edit.svg": "pencil",
    "exchange.svg": "arrow-left-right",
    "export.svg": "upload",
    "favorite.svg": "star",
    "favorite-filling.svg": "star",
    "find.svg": "search",
    "fit_size.svg": "expand",
    "format_painter.svg": "paintbrush",
    "full_screen.svg": "maximize",
    "fx.svg": "function-square",
    "global.svg": "globe",
    "help.svg": "circle-question-mark",
    "help-filling.svg": "circle-question-mark",
    "history.svg": "history",
    "host.svg": "monitor",
    "image.svg": "image",
    "info.svg": "info",
    "java.svg": "coffee",
    "json.svg": "braces",
    "left_arrow.svg": "arrow-left",
    "list.svg": "list",
    "list_ordered.svg": "list-ordered",
    "list_unordered.svg": "list",
    "method.svg": "box",
    "more.svg": "ellipsis",
    "network.svg": "network",
    "nuclear.svg": "atom",
    "pdf.svg": "file-text",
    "play.svg": "play",
    "protobuf.svg": "binary",
    "reg.svg": "whole-word",
    "remove.svg": "trash-2",
    "remove2.svg": "x",
    "replace.svg": "replace",
    "right_arrow.svg": "arrow-right",
    "run.svg": "circle-play",
    "save.svg": "save",
    "schedule.svg": "calendar-clock",
    "send.svg": "send",
    "smile.svg": "smile",
    "suffix-yml.svg": "file-code",
    "time.svg": "clock",
    "translate.svg": "languages",
    "up.svg": "arrow-up",
    "wrap.svg": "wrap-text",
    "zoom_in.svg": "zoom-in",
    "zoom_out.svg": "zoom-out",
}

# filename -> svg root overrides
VARIANTS = {
    "help-filling.svg": {"stroke_width": "2.5"},
}

# filename -> (tag, index) -> extra attrs; index=-1 means all
FILL_RULES = {
    "favorite-filling.svg": {("path", -1): {"fill": COLOR}},
}


def lucide_to_svg(lucide_name: str, out_name: str) -> str:
    src = LUCIDE_DIR / f"{lucide_name}.svg"
    if not src.exists():
        raise FileNotFoundError(f"Missing lucide icon: {lucide_name}")

    tree = ET.parse(src)
    root = tree.getroot()
    fill_rules = FILL_RULES.get(out_name, {})

    parts = []
    counters = {}
    for child in root:
        tag = child.tag.split("}")[-1]
        idx = counters.get(tag, 0)
        counters[tag] = idx + 1

        attrs = dict(child.attrib)
        attrs.pop("class", None)

        for (rule_tag, rule_idx), extra in fill_rules.items():
            if tag == rule_tag and (rule_idx == -1 or rule_idx == idx):
                attrs.update(extra)

        if "stroke" in attrs and attrs["stroke"] == "currentColor":
            attrs["stroke"] = COLOR
        if "fill" in attrs and attrs["fill"] == "currentColor":
            attrs["fill"] = COLOR

        attr_str = " ".join(f'{k}="{v}"' for k, v in attrs.items())
        if len(child) == 0 and (child.text is None or not child.text.strip()):
            parts.append(f"<{tag} {attr_str}/>")
        else:
            inner = (child.text or "") + "".join(ET.tostring(c, encoding="unicode") for c in child)
            parts.append(f"<{tag} {attr_str}>{inner}</{tag}>")

    inner_xml = "".join(parts)
    variant = VARIANTS.get(out_name, {})
    stroke_width = variant.get("stroke_width", "2")
    return (
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<!-- Icons derived from Lucide (ISC) https://lucide.dev -->'
        f'<svg class="icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" '
        f'viewBox="0 0 24 24" fill="none" stroke="{COLOR}" stroke-width="{stroke_width}" '
        f'stroke-linecap="round" stroke-linejoin="round">{inner_xml}</svg>'
    )


def ensure_lucide_icons():
    if LUCIDE_DIR.exists():
        return
    import subprocess
    import tarfile
    import tempfile

    print("Downloading lucide-static...")
    with tempfile.TemporaryDirectory() as tmp:
        subprocess.run(["npm", "pack", "lucide-static"], cwd=tmp, check=True, capture_output=True)
        tgz = next(Path(tmp).glob("lucide-static-*.tgz"))
        with tarfile.open(tgz) as tar:
            tar.extractall(ROOT / "package")
    if not LUCIDE_DIR.exists():
        raise SystemExit("Failed to extract lucide-static icons")


def main():
    ensure_lucide_icons()

    missing = []
    for out_name, lucide_name in sorted(ICON_MAP.items()):
        try:
            content = lucide_to_svg(lucide_name, out_name)
            (OUT_DIR / out_name).write_text(content, encoding="utf-8")
        except FileNotFoundError:
            missing.append((out_name, lucide_name))

    if missing:
        print("Missing lucide icons:")
        for item in missing:
            print(f"  {item}")
        raise SystemExit(1)

    print(f"Generated {len(ICON_MAP)} icons into {OUT_DIR}")
    skipped = sorted(p.name for p in OUT_DIR.glob("*.svg") if p.name not in ICON_MAP and p.name not in SKIP)
    if skipped:
        print(f"Unchanged ({len(skipped)}): {', '.join(skipped)}")


if __name__ == "__main__":
    main()
