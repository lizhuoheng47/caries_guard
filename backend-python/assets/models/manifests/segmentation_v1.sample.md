# `segmentation_v1` Manifest Sample

This manifest is the single source of truth for segmentation runtime assets. The runtime reads it before creating the adapter.

## Required fields

- `artifact.checkpointPath`
  Example: `assets/models/checkpoints/segmentation_v1/model.onnx`
- `artifact.checkpointFormat`
  Example: `onnx`
- `dataset.classMapPath`
  Example: `assets/datasets/caries_v1/meta/class_map.json`
- `inputSpec.preprocessConfigPath`
  Example: `assets/models/configs/preprocess.yaml`
- `outputSpec.postprocessConfigPath`
  Example: `assets/models/configs/postprocess.yaml`
- `artifact.checkpointSha256`
  Example: `8f4b3f...<64 hex chars>`
- `artifact.exportedAt`
  Example: `2026-04-21T18:00:00Z`
- `status`
  Use `READY` only after the checkpoint file exists and the sha256/export time are filled.

## Example

```yaml
manifestVersion: "1.0"
modelCode: "caries-segmentation-v1"
taskType: "SEGMENTATION"
status: "READY"
dataset:
  datasetCode: "caries_v1"
  annotationVersion: "caries-annot-v1.0"
  classMapPath: "assets/datasets/caries_v1/meta/class_map.json"
artifact:
  checkpointPath: "assets/models/checkpoints/segmentation_v1/model.onnx"
  checkpointFormat: "onnx"
  checkpointSha256: "8f4b3f8c3b8d0c9d7c97db2d4a0c56f0f6e0ed8a92d824a2f5f48f4f3c5a1e22"
  exportedAt: "2026-04-21T18:00:00Z"
inputSpec:
  imageChannels: 1
  expectedImageSize: [512, 512]
  preprocessConfigPath: "assets/models/configs/preprocess.yaml"
outputSpec:
  outputType: "binary_mask"
  foregroundClassCode: "CARIES_LESION"
  postprocessConfigPath: "assets/models/configs/postprocess.yaml"
release:
  runtimeCompatible: true
  failOnMissingCheckpoint: true
```

## Notes

- `status: SPEC_ONLY` or `status: MISSING_CHECKPOINT` is non-runnable and will fail validation.
- `checkpointPath`, `classMapPath`, `preprocessConfigPath`, and `postprocessConfigPath` are all consumed by the runtime adapter chain.
- Keep `expectedImageSize` aligned with `preprocess.yaml`. The validator rejects size drift.
