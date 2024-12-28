"""
Module: 'aioble.central' on micropython-v1.24.1-rp2-ARDUINO_NANO_RP2040_CONNECT
"""

# MCU: {'family': 'micropython', 'version': '1.24.1', 'build': '', 'ver': '1.24.1', 'port': 'rp2', 'board': 'ARDUINO_NANO_RP2040_CONNECT', 'cpu': 'RP2040', 'mpy': 'v6.3', 'arch': 'armv6m'}
# Stubber: v1.24.0
from __future__ import annotations
from typing import Generator
from _typeshed import Incomplete

def _central_shutdown(*args, **kwargs) -> Incomplete: ...
def ensure_active(*args, **kwargs) -> Incomplete: ...
def _central_irq(*args, **kwargs) -> Incomplete: ...
def register_irq_handler(*args, **kwargs) -> Incomplete: ...
def log_warn(*args, **kwargs) -> Incomplete: ...
def log_error(*args, **kwargs) -> Incomplete: ...
def log_info(*args, **kwargs) -> Incomplete: ...
def const(*args, **kwargs) -> Incomplete: ...

ble: Incomplete  ## <class 'BLE'> = <BLE>

class DeviceTimeout:
    _timeout_sleep: Generator  ## = <generator>
    def __init__(self, *argv, **kwargs) -> None: ...

class scan:
    cancel: Generator  ## = <generator>
    def __init__(self, *argv, **kwargs) -> None: ...

_active_scanner: Incomplete  ## <class 'NoneType'> = None
_cancel_pending: Generator  ## = <generator>

class ScanResult:
    def _update(self, *args, **kwargs) -> Incomplete: ...
    def name(self, *args, **kwargs) -> Incomplete: ...

    manufacturer: Generator  ## = <generator>
    services: Generator  ## = <generator>
    _decode_field: Generator  ## = <generator>
    def __init__(self, *argv, **kwargs) -> None: ...

_connecting: set  ## = set()
_connect: Generator  ## = <generator>

class DeviceConnection:
    _connected: dict = {}
    def is_connected(self, *args, **kwargs) -> Incomplete: ...
    def _run_task(self, *args, **kwargs) -> Incomplete: ...
    def services(self, *args, **kwargs) -> Incomplete: ...
    def timeout(self, *args, **kwargs) -> Incomplete: ...

    l2cap_connect: Generator  ## = <generator>
    l2cap_accept: Generator  ## = <generator>
    pair: Generator  ## = <generator>
    service: Generator  ## = <generator>
    disconnect: Generator  ## = <generator>
    device_task: Generator  ## = <generator>
    disconnected: Generator  ## = <generator>
    exchange_mtu: Generator  ## = <generator>
    def __init__(self, *argv, **kwargs) -> None: ...

class Device:
    def addr_hex(self, *args, **kwargs) -> Incomplete: ...

    connect: Generator  ## = <generator>
    def __init__(self, *argv, **kwargs) -> None: ...