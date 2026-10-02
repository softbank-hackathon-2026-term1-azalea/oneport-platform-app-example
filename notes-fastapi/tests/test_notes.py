from fastapi.testclient import TestClient


def test_create_and_list_notes(client: TestClient) -> None:
    first = client.post("/notes", json={"title": "first"})
    second = client.post("/notes", json={"title": "  second  "})
    assert first.status_code == 201
    assert second.status_code == 201
    assert second.json()["title"] == "second"

    notes = client.get("/notes").json()
    assert [note["title"] for note in notes] == ["second", "first"]
    assert all({"id", "title", "created_at"} <= set(note) for note in notes)


def test_blank_title_is_rejected(client: TestClient) -> None:
    assert client.post("/notes", json={"title": "   "}).status_code == 422
    assert client.post("/notes", json={"title": ""}).status_code == 422
    assert client.post("/notes", json={}).status_code == 422


def test_title_longer_than_200_is_rejected(client: TestClient) -> None:
    assert client.post("/notes", json={"title": "x" * 201}).status_code == 422
    assert client.post("/notes", json={"title": "x" * 200}).status_code == 201


def test_delete_note(client: TestClient) -> None:
    note_id = client.post("/notes", json={"title": "to delete"}).json()["id"]
    assert client.delete(f"/notes/{note_id}").status_code == 204
    assert client.get("/notes").json() == []


def test_delete_missing_note_returns_404(client: TestClient) -> None:
    response = client.delete("/notes/999999")
    assert response.status_code == 404
    assert response.json()["note_id"] == 999999


def test_list_limit_is_validated(client: TestClient) -> None:
    assert client.get("/notes", params={"limit": 0}).status_code == 422
    assert client.get("/notes", params={"limit": 201}).status_code == 422
