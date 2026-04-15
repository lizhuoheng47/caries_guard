from app.callback_signature import sign_callback


def test_callback_signature_is_stable() -> None:
    signature = sign_callback('{"taskNo":"TASK1"}', "1713150000", "secret")

    assert signature == "WZUvHiQQTQw9GrL4dOP9HCQLJD7OHLFp87WdNOqUhcE"
