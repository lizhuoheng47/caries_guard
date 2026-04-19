from __future__ import annotations

import json
from pathlib import Path

from app.repositories.eval_repository import EvalRepository


class EvalBootstrapService:
    def __init__(self, eval_repository: EvalRepository) -> None:
        self.eval_repository = eval_repository

    def bootstrap(self) -> None:
        seed_path = Path(__file__).resolve().parent.parent / "data" / "rag_eval_dataset_seed.json"
        if not seed_path.exists():
            return
        payload = json.loads(seed_path.read_text(encoding="utf-8"))
        dataset = self.eval_repository.ensure_dataset(
            dataset_code=payload["datasetCode"],
            dataset_name=payload["datasetName"],
            description_text=payload.get("descriptionText"),
            org_id=1,
            created_by=0,
        )
        self.eval_repository.replace_questions(
            dataset_id=dataset["id"],
            questions=payload["questions"],
            org_id=1,
            created_by=0,
        )
