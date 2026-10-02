from datetime import datetime

from sqlalchemy import BigInteger, CheckConstraint, DateTime, Identity, Index, String, func
from sqlalchemy.orm import Mapped, mapped_column

from app.core.db import Base


class Note(Base):
    __tablename__ = "notes"
    __table_args__ = (
        CheckConstraint("btrim(title) <> ''", name="title_not_blank"),
        Index("ix_notes_created_at", "created_at"),
    )

    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
