# caries_v1 Dataset Card

## Summary

- Dataset code: `caries_v1`
- Purpose: shared asset layer for caries segmentation, grading, and image-quality labeling
- Storage root: `backend-python/assets/datasets/caries_v1/`
- Annotation version: `caries-annot-v1.0`
- Desensitization requirement: all exported manifests must set `desensitized=true`

## Directory Layout

```text
assets/datasets/caries_v1/
  raw/images/
  labels/masks/
  manifests/
  meta/
```

## Task Scope

- Segmentation target: binary lesion mask with `BACKGROUND=0` and `CARIES_LESION=1`
- Grading labels: `C0`, `C1`, `C2`, `C3`
- Quality labels: `PASS`, `WARN`, `FAIL`
- Supported image types: `BITEWING`, `PERIAPICAL`, `PANORAMIC`, `OCCLUSAL`

## Manifest Contract

Each JSONL row in `manifests/train.jsonl`, `manifests/val.jsonl`, and
`manifests/test.jsonl` must contain these fields:

| Field | Type | Description |
| --- | --- | --- |
| `imageId` | string | stable desensitized image identifier |
| `imagePath` | string | image path relative to `backend-python/` |
| `maskPath` | string | segmentation mask path relative to `backend-python/` |
| `gradeLabel` | string | one of `C0`, `C1`, `C2`, `C3` |
| `qualityLabel` | string | one of `PASS`, `WARN`, `FAIL` |
| `imageType` | string | image acquisition type |
| `annotationVersion` | string | label snapshot version |
| `desensitized` | boolean | must be `true` for exported manifests |

## Label Governance

- `class_map.json` is the single source of truth for class indices and label codes.
- Mask files must be single-channel PNGs encoded as `uint8`.
- Background pixel value is `0`.
- Ignore index is reserved as `255`.

## Split Policy

- `train.jsonl`: model fitting
- `val.jsonl`: threshold selection and early stopping
- `test.jsonl`: holdout evaluation and release gating

Do not mix patient-level samples across splits unless a dedicated governance
rule explicitly allows it.

## Training / Inference Sharing

- Training reads manifests, `class_map.json`, and shared preprocessing and
  postprocessing configs under `assets/models/configs/`.
- Inference reads the same class map and config files to guarantee the same
  image normalization, mask semantics, grade ordering, and postprocessing
  thresholds used during training and release validation.
- Model release manifests under `assets/models/manifests/` bind a trained
  artifact to the dataset version, preprocessing config, and postprocessing
  config used to produce it.
