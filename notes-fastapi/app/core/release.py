import os
import socket
from dataclasses import asdict, dataclass
from datetime import UTC, datetime
from typing import Any


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
        version=os.environ.get("APP_VERSION", "dev"),
        git_sha=os.environ.get("GIT_SHA", "unknown"),
        built_at=os.environ.get("BUILT_AT", "unknown"),
        color=color,
        hostname=socket.gethostname(),
        started_at=datetime.now(tz=UTC).isoformat(timespec="seconds"),
    )
