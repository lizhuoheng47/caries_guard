from app.core.config import Settings
from app.services.image_fetch_service import TaskWorkspace


def test_task_workspace_cleans_temp_files(tmp_path) -> None:
    settings = Settings(temp_dir=str(tmp_path))

    with TaskWorkspace(settings, "TASK1") as workspace:
        marker = workspace / "marker.txt"
        marker.write_text("x")
        assert marker.exists()

    assert not workspace.exists()

