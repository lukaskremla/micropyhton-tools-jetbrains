"""
Module: 'ntptime' on micropython-v1.21.0-esp32-ESP32_GENERIC_C3
"""

# MCU: {'version': '1.21.0', 'mpy': 'v6.1', 'port': 'esp32', 'board': 'ESP32_GENERIC_C3', 'family': 'micropython', 'build': '', 'arch': '', 'ver': '1.21.0', 'cpu': 'ESP32C3'}
# Stubber: v1.23.0
from __future__ import annotations
from _typeshed import Incomplete

timeout: int = 1
host: str = "pool.ntp.org"

def settime(*args, **kwargs) -> Incomplete: ...
def time(*args, **kwargs) -> Incomplete: ...
