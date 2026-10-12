# OCR evaluation harness

These scripts are assessment tools only. They do not register IPC handlers, add app dependencies, or ship OCR functionality. The fixed PNG fixtures, expected text, raw recognition results and scores are in `../../doc/ocr-evaluation/`.

`probe-ppocr.mjs` validates the later production JavaScript pipeline through Electron's actual OCR IPC and isolated utility process. Prepare macOS arm64 test resources with `npm run ocr:prepare -- --platform darwin --arch arm64`, build, then run `node scripts/ocr-evaluation/probe-ppocr.mjs /tmp/ppocr-electron-results.json`. It reuses the fixed corpus, records two passes with fresh processes per image, and does not overwrite the original Python/Vision assessment output. Results and limitations are documented in `../../doc/ppocr-backend.md`.

Run from `next/`. Keep model weights and temporary dependencies outside the repository:

```sh
python3 -m venv /tmp/mootool-ocr-eval/venv
/tmp/mootool-ocr-eval/venv/bin/python -m pip install rapidocr==3.10.0 onnxruntime==1.31.0
npm install --prefix /tmp/mootool-ocr-eval/node tesseract.js@7.0.0 @tesseract.js-data/chi_sim@1.0.0 @tesseract.js-data/chi_tra@1.0.0 @tesseract.js-data/eng@1.0.0
python3 scripts/ocr-evaluation/download-models.py doc/ocr-evaluation/sizes.json /tmp/mootool-ocr-eval/models

/tmp/mootool-ocr-eval/venv/bin/python scripts/ocr-evaluation/rapid.py doc/ocr-evaluation /tmp/mootool-ocr-eval/models tiny screen
/tmp/mootool-ocr-eval/venv/bin/python scripts/ocr-evaluation/rapid.py doc/ocr-evaluation /tmp/mootool-ocr-eval/models small screen
node scripts/ocr-evaluation/tesseract.cjs /tmp/mootool-ocr-eval/node doc/ocr-evaluation
node scripts/ocr-evaluation/tesseract.cjs /tmp/mootool-ocr-eval/node doc/ocr-evaluation chi_sim+eng
```

For the RapidOCR defaults, omit `screen`. For Apple Vision, macOS with a Swift compiler is required:

```sh
swiftc scripts/ocr-evaluation/vision.swift -o /tmp/mootool-ocr-eval/vision
/tmp/mootool-ocr-eval/vision doc/ocr-evaluation
python3 scripts/ocr-evaluation/summarize.py doc/ocr-evaluation
```

Run engines sequentially when comparing latency. Each engine uses a fresh process, performs one initial pass, then three warm passes; the same initialized engine handles all eight fixtures. Models remain cached locally, so first-call timing is process/model initialization with cached assets, not first-install download time. GPU acceleration is disabled for RapidOCR. The Python inference loop blocks Python HTTP/socket access; the recorded `offline-probe.json` and `tesseract-offline-probe.json` additionally ran with macOS `sandbox-exec` denying outgoing network connections.

The fixed fixture images should be used for comparisons across operating systems. To regenerate on the original macOS font environment:

```sh
npm run build
node scripts/ocr-evaluation/generate.cjs
```

Regeneration uses installed PingFang SC / Songti SC fonts and Electron's canvas. Rasterization can vary with font, OS and Electron versions; regenerated images are a new benchmark corpus, not interchangeable with the recorded corpus.

`probe-electron.cjs` checks only native ONNX loading and one blank-input recognition inference, not the full OCR detection/cropping/reading-order pipeline:

```sh
npm install --prefix /tmp/mootool-ocr-eval/ort --ignore-scripts onnxruntime-node@1.30.0
node scripts/ocr-evaluation/probe-electron.cjs /tmp/mootool-ocr-eval/ort/node_modules/onnxruntime-node /tmp/mootool-ocr-eval/models/PP-OCRv6_rec_tiny.onnx doc/ocr-evaluation/electron-probe.json
```

Package byte counts and pruning probes are specific to the downloaded artifacts. They are not Electron installer-size measurements. Production packaging must preserve required libraries, licenses and the worker's dynamic module paths, and must be tested again for each target OS/architecture.
