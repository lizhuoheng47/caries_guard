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

