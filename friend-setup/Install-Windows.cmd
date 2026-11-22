@echo off
cd /d "%~dp0"
py -3 --version >nul 2>&1
if not errorlevel 1 (
    py -3 install.py
) else (
    python --version >nul 2>&1
    if errorlevel 1 (
        echo Install Python 3 from https://www.python.org/downloads/ then run this file again.
    ) else (
        python install.py
    )
)
echo.
pause
