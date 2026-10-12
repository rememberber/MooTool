import concurrent.futures
import hashlib
import json
import subprocess
import sys
from pathlib import Path

manifest = json.loads(Path(sys.argv[1]).read_text())['ppOcrModels']
directory = Path(sys.argv[2]).resolve()
directory.mkdir(parents=True, exist_ok=True)

def download(item):
    path = directory / Path(item['name']).name
    if not path.exists():
        subprocess.run(['curl', '-fsSL', '--retry', '2', '--max-time', '180', item['url'], '-o', str(path)], check=True)
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != item['sha256']:
        raise ValueError(f"Checksum mismatch: {item['name']}")
    print(item['name'], len(data), flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    list(pool.map(download, manifest))
