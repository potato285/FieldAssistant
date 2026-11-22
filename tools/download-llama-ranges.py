"""Resumable, checksum-verified development download of the pinned public Llama export."""
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
import hashlib
import subprocess

ROOT = Path(__file__).resolve().parent / 'llama'
ROOT.mkdir(exist_ok=True)
PARTS = ROOT / 'parts'
PARTS.mkdir(exist_ok=True)
URL = ('https://huggingface.co/executorch-community/'
       'Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8-ET/resolve/'
       '911221e0f49d3354cbd4697a9c09feee25cadec7/'
       'Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8.pte')
SIZE = 1135951488
CHUNK = 8 * 1024 * 1024
EXPECTED = '8715cdba9e91f6bede00cc5f2d6b12397b95225da5630c4972a8da03001cda3b'

def fetch(start):
    end = min(SIZE, start + CHUNK) - 1
    part = PARTS / f'{start:012d}'
    if part.exists() and part.stat().st_size == end - start + 1:
        return part
    tmp = part.with_suffix('.tmp')
    headers = part.with_suffix('.headers')
    subprocess.run(['curl', '-fsSL', '--retry', '3', '--max-time', '180',
                    '--range', f'{start}-{end}', '-D', str(headers),
                    f'{URL}?download=true&chunk={start}', '-o', str(tmp)], check=True)
    assert f'content-range: bytes {start}-{end}/{SIZE}' in headers.read_text().lower()
    assert tmp.stat().st_size == end - start + 1
    tmp.replace(part)
    return part

if __name__ == '__main__':
    starts = list(range(0, SIZE, CHUNK))
    with ThreadPoolExecutor(max_workers=6) as pool:
        for n, future in enumerate(as_completed([pool.submit(fetch, s) for s in starts]), 1):
            future.result()
            print(f'Completed {n}/{len(starts)} ranges', flush=True)
    digest = hashlib.sha256()
    target = ROOT / 'verified.pte.tmp'
    with target.open('wb') as out:
        for start in starts:
            data = (PARTS / f'{start:012d}').read_bytes()
            digest.update(data)
            out.write(data)
    assert digest.hexdigest() == EXPECTED, 'Llama SHA-256 mismatch'
    target.replace(ROOT / 'llama.pte')
    print(f'VERIFIED {SIZE} bytes SHA256 {EXPECTED}', flush=True)
