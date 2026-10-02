import os
import socket
import tomllib
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from functools import cache
from pathlib import Path
from typing import Any

_PYPROJECT = Path(__file__).resolve().parents[2] / "pyproject.toml"


@cache
def project_version() -> str:
    with _PYPROJECT.open("rb") as file:
        return str(tomllib.load(file)["project"]["version"])


@dataclass(frozen=True)
class ReleaseInfo:
    app: str
    version: str
    git_sha: str
    built_at: str
    color: str
    hostname: str
    started_at: str

    def as_dict(self) -> dict[str, Any]:
        return asdict(self)


def load_release(*, app_name: str, color: str) -> ReleaseInfo:
    return ReleaseInfo(
        app=app_name,
        version=project_version(),
        git_sha=os.environ.get("GIT_SHA", "unknown"),
        built_at=os.environ.get("BUILT_AT", "unknown"),
        color=color,
        hostname=socket.gethostname(),
        started_at=datetime.now(tz=UTC).isoformat(timespec="seconds"),
    )
