import importlib.metadata
import json
import resource
import socket
import sys
import time
from pathlib import Path

import requests
from rapidocr import ModelType, OCRVersion, RapidOCR

fixtures = Path(sys.argv[1]).resolve()
models = Path(sys.argv[2]).resolve()
tier = sys.argv[3]
screen_profile = len(sys.argv) > 4 and sys.argv[4] == 'screen'

def deny_network(*args, **kwargs):
    raise RuntimeError('Network disabled for offline evaluation')

# All weights are supplied by local paths; fail instead of silently downloading.
requests.sessions.Session.request = deny_network
socket.create_connection = deny_network
socket.socket.connect = deny_network

params = {
    'Global.use_cls': False,
    'Global.log_level': 'warning',
    'Det.ocr_version': OCRVersion('PP-OCRv6'),
    'Rec.ocr_version': OCRVersion('PP-OCRv6'),
    'Det.model_type': ModelType(tier),
    'Rec.model_type': ModelType(tier),
    'Det.model_path': str(models / f'PP-OCRv6_det_{tier}.onnx'),
    'Rec.model_path': str(models / f'PP-OCRv6_rec_{tier}.onnx'),
    'EngineConfig.onnxruntime.intra_op_num_threads': 4,
    'EngineConfig.onnxruntime.inter_op_num_threads': 1,
}
if screen_profile:
    params.update({'Det.limit_side_len': 1536, 'Det.limit_type': 'max'})
start = time.perf_counter()
engine = RapidOCR(params=params)
init_ms = (time.perf_counter() - start) * 1000
runs = []
samples = json.loads((fixtures / 'samples.json').read_text())
for repeat in range(4):
    for sample in samples:
        start = time.perf_counter()
        result = engine(str(fixtures / f"{sample['id']}.png"))
        elapsed = (time.perf_counter() - start) * 1000
        text = '\n'.join(result.txts or ())
        runs.append({'id': sample['id'], 'pass': repeat, 'ms': elapsed, 'text': text})
        if repeat == 0:
            print(sample['id'], round(elapsed, 1), flush=True)
output = {
    'engine': f'RapidOCR PP-OCRv6 {tier}' + (' (screen max1536)' if screen_profile else ''),
    'rapidocrVersion': importlib.metadata.version('rapidocr'),
    'onnxruntimeVersion': importlib.metadata.version('onnxruntime'),
    'networkDisabled': True,
    'useCls': False,
    'threads': {'intra': 4, 'inter': 1},
    'detectorLimit': {'side': engine.cfg.Det.limit_side_len, 'type': engine.cfg.Det.limit_type},
    'initMs': init_ms,
    'peakRssBytes': resource.getrusage(resource.RUSAGE_SELF).ru_maxrss,
    'runs': runs,
}
suffix = '-screen' if screen_profile else ''
(fixtures / f'rapid-{tier}{suffix}.json').write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
