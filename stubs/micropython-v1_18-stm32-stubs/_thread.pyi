"""
multithreading support. See: https://docs.micropython.org/en/v1.18/library/_thread.html

|see_cpython_module| :mod:`python:_thread` https://docs.python.org/3/library/_thread.html .

This module implements multithreading support.

This module is highly experimental and its API is not yet fully settled
and not yet described in this documentation.
"""
from typing import Any

class LockType:
    def __init__(self, *argv, **kwargs) -> None: ...
    def acquire(self, *args, **kwargs) -> Any: ...
    def locked(self, *args, **kwargs) -> Any: ...
    def release(self, *args, **kwargs) -> Any: ...

def allocate_lock(*args, **kwargs) -> Any: ...
def exit(*args, **kwargs) -> Any: ...
def get_ident(*args, **kwargs) -> Any: ...
def stack_size(*args, **kwargs) -> Any: ...
def start_new_thread(*args, **kwargs) -> Any: ...
