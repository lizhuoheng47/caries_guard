from pydantic import BaseModel, ConfigDict


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(part[:1].upper() + part[1:] for part in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True,
        extra="allow",
        protected_namespaces=(),
    )


def dump_camel(model: BaseModel, exclude_none: bool = True) -> dict:
    return model.model_dump(by_alias=True, exclude_none=exclude_none)
