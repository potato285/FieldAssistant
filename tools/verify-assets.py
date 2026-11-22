"""Verify pinned inference assets; does not claim inference or model accuracy."""
from pathlib import Path
import hashlib
import json

base = Path(__file__).resolve().parent.parent / 'app/src/main/assets'
expected = {
    'vision/plantulus_doctor_fp16.tflite': '3f9b1ef6e7bcc603c6844429ae4ac28607cbfb48fd7b1f856362c824a29a3e65',
    'vision/classes.json': '234d239690b39a1af32e1c4e6a2a56fa2738183a7c888a494eaca188ece754e7',
    'vision/class_info.json': '65e408b9b5eb80ef97a81543eb532d29fcb87413f6e4ea5df8c8b4c9d6c30176',
    'vision/preprocessing.json': 'd3523c3ebdc78296573e292da7daf713fdd4eb9508395025c934ef40cbc77c52'
}
for name, value in expected.items():
    actual = hashlib.sha256((base / name).read_bytes()).hexdigest()
    assert actual == value, f'Checksum mismatch: {name}'
    print(name, actual)
labels = json.loads((base / 'vision/classes.json').read_text())
info = json.loads((base / 'vision/class_info.json').read_text())
assert len(labels) == 54 == len(set(labels))
assert labels == [entry['label'] for entry in info]
print(f'{len(labels)} ordered labels / {len(set(x["plant"] for x in info))} plants')
assert (base / 'vision/plantulus_doctor_fp16.tflite').read_bytes()[4:8] == b'TFL3'
print('TFLite file identifier verified. Interpreter execution is tested separately on Android.')
