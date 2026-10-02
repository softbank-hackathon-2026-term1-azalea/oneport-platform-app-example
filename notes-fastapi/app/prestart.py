import logging
import os
import sys
import time

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, text
from sqlalchemy.exc import OperationalError

from app.core.config import get_settings
from app.core.logging import configure_logging
from app.core.release import project_version

log = logging.getLogger(__name__)


def wait_for_database(timeout_seconds: float, interval_seconds: float = 2.0) -> None:
    settings = get_settings()
    engine = create_engine(settings.sqlalchemy_url, pool_pre_ping=True)
    deadline = time.monotonic() + timeout_seconds
    attempt = 0
    try:
        while True:
            attempt += 1
            try:
                with engine.connect() as connection:
                    connection.execute(text("SELECT 1"))
                log.info("database reachable", extra={"attempt": attempt})
                return
            except OperationalError as exc:
                if time.monotonic() >= deadline:
                    log.error(
                        "database not reachable", extra={"attempt": attempt, "error": str(exc)}
                    )
                    raise
                log.warning("database not ready, retrying", extra={"attempt": attempt})
                time.sleep(interval_seconds)
    finally:
        engine.dispose()


def main() -> None:
    settings = get_settings()
    configure_logging(
        fmt=settings.log_format,
        level=settings.log_level,
        static_fields={"app": settings.app_name, "version": project_version()},
    )
    wait_for_database(timeout_seconds=float(os.environ.get("DB_WAIT_SECONDS", "60")))
    command.upgrade(Config("alembic.ini"), "head")
    log.info("migrations applied")


if __name__ == "__main__":
    try:
        main()
    except Exception:
        log.exception("prestart failed")
        sys.exit(1)
