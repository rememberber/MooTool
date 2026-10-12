import json
import re
import statistics
import sys
from pathlib import Path

directory = Path(sys.argv[1])
samples = {item['id']: item for item in json.loads((directory / 'samples.json').read_text())}

def normalize(text, han=False):
    if han:
        return ''.join(re.findall(r'[\u3400-\u9fff\U00020000-\U0002fa1f]', text))
    return re.sub(r'\s+', '', text)

def distance(a, b):
    previous = list(range(len(b) + 1))
    for i, ca in enumerate(a, 1):
        current = [i]
        for j, cb in enumerate(b, 1):
            current.append(min(current[-1] + 1, previous[j] + 1, previous[j - 1] + (ca != cb)))
        previous = current
    return previous[-1]

output = []
for file in sorted(directory.glob('*.json')):
    data = json.loads(file.read_text())
    if 'runs' not in data:
        continue
    result = {'engine': data['engine'], 'file': file.name, 'perSample': []}
    totals = {key: [0, 0] for key in ['all', 'han']}
    for sample_id, sample in samples.items():
        runs = [run for run in data['runs'] if run['id'] == sample_id]
        first = next(run for run in runs if run['pass'] == 0)
        row = {'id': sample_id, 'warmMedianMs': statistics.median(run['ms'] for run in runs if run['pass'] > 0), 'recognized': first['text']}
        for key, han in [('all', False), ('han', True)]:
            expected = normalize(sample['expected'], han)
            actual = normalize(first['text'], han)
            edits = distance(expected, actual)
            totals[key][0] += edits; totals[key][1] += len(expected)
            row[key + 'CER'] = edits / len(expected)
            row[key + 'Edits'] = edits
            row[key + 'Characters'] = len(expected)
        result['perSample'].append(row)
    result['allCER'] = totals['all'][0] / totals['all'][1]
    result['hanCER'] = totals['han'][0] / totals['han'][1]
    result['allCharacters'] = totals['all'][1]; result['hanCharacters'] = totals['han'][1]
    result['warmMedianMs'] = statistics.median(run['ms'] for run in data['runs'] if run['pass'] > 0)
    output.append(result)
    print(data['engine'], 'CER', round(result['allCER'] * 100, 2), 'Han CER', round(result['hanCER'] * 100, 2), 'warm ms', round(result['warmMedianMs'], 1))
(directory / 'summary.json').write_text(json.dumps(output, ensure_ascii=False, indent=2) + '\n')
