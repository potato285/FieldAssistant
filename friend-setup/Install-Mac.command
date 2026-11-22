#!/bin/bash
cd "$(dirname "$0")" || exit 1
if command -v python3 >/dev/null 2>&1; then
    python3 install.py
else
    echo 'Install Python 3 from https://www.python.org/downloads/ and run this file again.'
fi
echo
read -r -p 'Press Enter to close this window. '
