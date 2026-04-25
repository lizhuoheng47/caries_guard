# `grading_v1` Manifest Sample

This manifest drives grading adapter creation and asset validation. The runtime reads it before loading the checkpoint.

## Required fields

- `artifact.checkpointPath`
  Example: `assets/models/checkpoints/grading_v1/model.onnx`
- `artifact.checkpointFormat`
  Example: `onnx`
- `dataset.classMapPath`
  Example: `assets/datasets/caries_v1/meta/class_map.json`
- `inputSpec.preprocessConfigPath`
  Example: `assets/models/configs/preprocess.yaml`
- `outputSpec.postprocessConfigPath`
  Example: `assets/models/configs/postprocess.yaml`
- `artifact.checkpointSha256`
  Example: `4b53aa...<64 hex chars>`
- `artifact.exportedAt`
  Example: `2026-04-21T18:10:00Z`
- `status`
  Use `READY` only after the checkpoint file exists and the sha256/export time are filled.

## Example

```yaml
manifestVersion: "1.0"
modelCode: "caries-grading-v1"
taskType: "GRADING"
status: "READY"
dataset:
  datasetCode: "caries_v1"
  annotationVersion: "caries-annot-v1.0"
  classMapPath: "assets/datasets/caries_v1/meta/class_map.json"
artifact:
  checkpointPath: "assets/models/checkpoints/grading_v1/model.onnx"
  checkpointFormat: "onnx"
  checkpointSha256: "4b53aa114cead774d0d9d52f8efbc7796d4b9cd2331dc7e66530d28f28c8fd81"
  exportedAt: "2026-04-21T18:10:00Z"
inputSpec:
  imageChannels: 1
  expectedImageSize: [512, 512]
  preprocessConfigPath: "assets/models/configs/preprocess.yaml"
labelSpec:
  labelOrder:
    - "C0"
    - "C1"
    - "C2"
    - "C3"
outputSpec:
  outputType: "grade_logits"
  primaryField: "gradeLabel"
  uncertaintyField: "uncertaintyScore"
  postprocessConfigPath: "assets/models/configs/postprocess.yaml"
release:
  runtimeCompatible: true
  failOnMissingCheckpoint: true
```

## Notes

- `status: SPEC_ONLY` or `status: MISSING_CHECKPOINT` is non-runnable and will fail validation.
- `labelSpec.labelOrder` must match `class_map.json`. The validator rejects `G*`/`C*` drift.
- Keep `expectedImageSize` aligned with `preprocess.yaml`. The validator rejects size drift.
- Grading smoke inference requires real lesion regions, so provide `--regions-json` or `--region-bbox` when running the smoke script.
