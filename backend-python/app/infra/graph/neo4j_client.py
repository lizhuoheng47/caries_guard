from __future__ import annotations

from neo4j import GraphDatabase
from neo4j import Driver

from app.core.config import Settings


def create_neo4j_driver(settings: Settings) -> Driver:
    return GraphDatabase.driver(
        settings.neo4j_uri,
        auth=(settings.neo4j_username, settings.neo4j_password),
    )
