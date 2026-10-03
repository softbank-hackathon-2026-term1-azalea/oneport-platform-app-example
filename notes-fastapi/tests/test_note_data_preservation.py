"""The release demo contract: an existing note survives 0001 → 0002 with its state."""

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine, text

from app.prestart import migrate


def _connect(database_url: str):
    return create_engine(database_url)


def test_upgrade_0001_to_0002_preserves_note_and_done_state(database_url: str) -> None:
    config = Config("alembic.ini")
    engine = _connect(database_url)

    # The v1 release serves schema 0001 and a user creates a note.
    command.downgrade(config, "0001")
    with engine.begin() as connection:
        connection.execute(text("INSERT INTO notes (title) VALUES ('SoftBank Demo')"))

    # The v2 release migrates 0001 → 0002 before its candidate takes traffic.
    assert migrate(config) is True

    # The note created under 0001 is still there, with the new default state.
    with engine.connect() as connection:
        title, done = connection.execute(
            text("SELECT title, done FROM notes WHERE title = 'SoftBank Demo'")
        ).one()
        version = connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one()
    assert (title, done, version) == ("SoftBank Demo", False, "0002")

    # The user checks the note off on v2.
    with engine.begin() as connection:
        connection.execute(text("UPDATE notes SET done = true WHERE title = 'SoftBank Demo'"))

    # A later release restarts prestart on an up-to-date database: nothing moves.
    assert migrate(config) is True
    with engine.connect() as connection:
        title, done = connection.execute(
            text("SELECT title, done FROM notes WHERE title = 'SoftBank Demo'")
        ).one()
        version = connection.execute(text("SELECT version_num FROM alembic_version")).scalar_one()
    assert (title, done, version) == ("SoftBank Demo", True, "0002")
    engine.dispose()

    # Leave the shared session database at head for the other tests.
    command.upgrade(config, "head")
