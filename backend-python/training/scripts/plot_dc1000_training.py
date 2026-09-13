from __future__ import annotations

import argparse
import csv
import json
import time
from pathlib import Path
from typing import Any

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402


REPO_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_RUN_DIR = (
    REPO_ROOT / "artifacts" / "training" / "dc1000_segmentation" / "dc1000_unet_v1"
)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Plot readable DC1000 U-Net training curves.")
    parser.add_argument("--run-dir", default=str(DEFAULT_RUN_DIR))
    return parser


def read_history(path: Path, attempts: int = 5) -> list[dict[str, Any]]:
    for attempt in range(attempts):
        try:
            rows = json.loads(path.read_text(encoding="utf-8"))
            if not isinstance(rows, list) or not rows:
                raise RuntimeError(f"history is empty: {path}")
            return rows
        except json.JSONDecodeError:
            if attempt == attempts - 1:
                raise
            time.sleep(0.2)
    raise RuntimeError(f"could not read history: {path}")


def flatten(rows: list[dict[str, Any]]) -> list[dict[str, float | int]]:
    output: list[dict[str, float | int]] = []
    for row in rows:
        validation = row["validation"]
        output.append(
            {
                "epoch": int(row["epoch"]),
                "train_loss": float(row["trainLoss"]),
                "dice": float(validation["dice"]),
                "iou": float(validation["iou"]),
                "precision": float(validation["precision"]),
                "recall": float(validation["recall"]),
                "specificity": float(validation["specificity"]),
                "false_positives": int(validation["fp"]),
                "false_negatives": int(validation["fn"]),
                "epoch_seconds": float(row["epochSeconds"]),
            }
        )
    return output


def save_csv(path: Path, rows: list[dict[str, float | int]]) -> None:
    with path.open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def style_axis(axis: Any) -> None:
    axis.grid(axis="y", color="#D8DEE8", linewidth=0.8, alpha=0.8)
    axis.spines["top"].set_visible(False)
    axis.spines["right"].set_visible(False)
    axis.spines["left"].set_color("#667085")
    axis.spines["bottom"].set_color("#667085")
    axis.tick_params(colors="#344054", labelsize=9)
    axis.set_xlabel("Epoch", color="#344054")


def main() -> int:
    args = build_parser().parse_args()
    run_dir = Path(args.run_dir).resolve()
    rows = flatten(read_history(run_dir / "history.json"))
    epochs = [int(row["epoch"]) for row in rows]
    best = max(rows, key=lambda row: float(row["dice"]))
    best_epoch = int(best["epoch"])

    colors = {
        "blue": "#175CD3",
        "orange": "#E87500",
        "pink": "#C11574",
        "olive": "#667C26",
        "charcoal": "#344054",
    }
    figure, axes = plt.subplots(2, 2, figsize=(12, 7.5), constrained_layout=True)
    figure.patch.set_facecolor("#FFFFFF")
    figure.suptitle(
        f"DC1000 U-Net Training | Best Dice {float(best['dice']):.3f} at epoch {best_epoch}",
        fontsize=15,
        color="#101828",
        fontweight="bold",
    )

    loss_axis = axes[0, 0]
    loss_axis.plot(
        epochs,
        [float(row["train_loss"]) for row in rows],
        marker="o",
        color=colors["blue"],
        linewidth=2,
        label="Train loss",
    )
    loss_axis.set_title("Optimization", loc="left", fontweight="bold")
    loss_axis.set_ylabel("Combined loss")
    style_axis(loss_axis)

    overlap_axis = axes[0, 1]
    overlap_axis.plot(
        epochs,
        [float(row["dice"]) for row in rows],
        marker="o",
        color=colors["blue"],
        linewidth=2,
        label="Dice",
    )
    overlap_axis.plot(
        epochs,
        [float(row["iou"]) for row in rows],
        marker="s",
        color=colors["orange"],
        linewidth=2,
        label="IoU",
    )
    overlap_axis.axvline(best_epoch, color=colors["charcoal"], linestyle="--", linewidth=1)
    overlap_axis.set_title("Lesion overlap", loc="left", fontweight="bold")
    overlap_axis.set_ylabel("Score")
    overlap_axis.set_ylim(bottom=0)
    overlap_axis.legend(frameon=False, ncol=2)
    style_axis(overlap_axis)

    detection_axis = axes[1, 0]
    detection_axis.plot(
        epochs,
        [float(row["precision"]) for row in rows],
        marker="o",
        color=colors["pink"],
        linewidth=2,
        label="Precision",
    )
    detection_axis.plot(
        epochs,
        [float(row["recall"]) for row in rows],
        marker="s",
        color=colors["olive"],
        linewidth=2,
        label="Recall",
    )
    detection_axis.set_title("Detection trade-off at threshold 0.50", loc="left", fontweight="bold")
    detection_axis.set_ylabel("Score")
    detection_axis.set_ylim(0, 1)
    detection_axis.legend(frameon=False, ncol=2)
    style_axis(detection_axis)

    error_axis = axes[1, 1]
    error_axis.plot(
        epochs,
        [int(row["false_positives"]) / 1_000_000 for row in rows],
        marker="o",
        color=colors["orange"],
        linewidth=2,
        label="False-positive pixels",
    )
    error_axis.plot(
        epochs,
        [int(row["false_negatives"]) / 1_000_000 for row in rows],
        marker="s",
        color=colors["blue"],
        linewidth=2,
        label="False-negative pixels",
    )
    error_axis.set_title("Pixel errors", loc="left", fontweight="bold")
    error_axis.set_ylabel("Million pixels")
    error_axis.set_ylim(bottom=0)
    error_axis.legend(frameon=False, ncol=1)
    style_axis(error_axis)

    for axis in axes.flat:
        axis.set_xticks(epochs)

    output_path = run_dir / "training_curves.png"
    csv_path = run_dir / "training_metrics.csv"
    figure.savefig(output_path, dpi=180, facecolor="white")
    plt.close(figure)
    save_csv(csv_path, rows)
    print(
        json.dumps(
            {
                "epochs": len(rows),
                "bestEpoch": best_epoch,
                "bestDice": float(best["dice"]),
                "chart": str(output_path),
                "csv": str(csv_path),
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
