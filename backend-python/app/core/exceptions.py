class BusinessException(Exception):
    def __init__(self, code: str, message: str, retryable: bool = False) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        self.retryable = retryable


class ResourceNotFoundException(BusinessException):
    def __init__(self, message: str) -> None:
        super().__init__("A0404", message, retryable=False)


class DownstreamException(BusinessException):
    def __init__(self, message: str) -> None:
        super().__init__("C3003", message, retryable=True)


class AnalysisRuntimeException(BusinessException):
    def __init__(
        self,
        code: str,
        message: str,
        *,
        missing_items: list[dict] | None = None,
        details: dict | None = None,
        retryable: bool = False,
    ) -> None:
        super().__init__(code, message, retryable=retryable)
        self.missing_items = list(missing_items or [])
        self.details = dict(details or {})


_MODEL_RUNTIME_ERROR_CODE_MAP = {
    "segmentation": {
        "validate": "M5005",
        "load": "M5006",
    },
    "grading": {
        "validate": "M5007",
        "load": "M5008",
    },
}


def model_runtime_error_code(module_name: str, stage: str) -> str:
    module_key = str(module_name or "").strip().lower()
    stage_key = str(stage or "").strip().lower()
    bucket = "validate" if stage_key in {"validate", "manifest", "metadata", "assets", "config", "label_map"} else "load"
    return _MODEL_RUNTIME_ERROR_CODE_MAP.get(module_key, {}).get(bucket, "M5999")


class ModelRuntimeException(AnalysisRuntimeException):
    def __init__(
        self,
        module_name: str,
        stage: str,
        message: str,
        *,
        missing_items: list[dict] | None = None,
        details: dict | None = None,
        retryable: bool = False,
    ) -> None:
        super().__init__(
            model_runtime_error_code(module_name, stage),
            message,
            missing_items=missing_items,
            details=details,
            retryable=retryable,
        )
        self.module_name = str(module_name or "").strip().lower()
        self.stage = str(stage or "").strip().lower()
