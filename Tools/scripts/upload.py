import os
import contextlib
import hashlib
import hmac
import html
import json
import urllib.request
from asyncio import run, sleep
from pathlib import Path

from pyrogram import Client
from pyrogram.enums import ParseMode
from pyrogram.errors import FloodWait
from pyrogram.types import InputMediaDocument, LinkPreviewOptions, ReplyParameters

api_id = os.environ.get("APP_ID")
api_hash = os.environ.get("APP_HASH")
artifacts_path = Path("artifacts")
abis = ["arm64-v8a", "universal"]

def find_apk(abi: str) -> Path | None:
    return next((apk for apk in artifacts_path.rglob("*.apk") if abi in apk.name), None)

def get_apks() -> dict[str, Path]:
    apks = {abi: apk for abi in abis if (apk := find_apk(abi))}
    if not apks:
        raise FileNotFoundError("No APK artifacts found")
    return apks

def get_caption() -> str:
    repo_url, sha, base = os.environ["REPO_URL"], os.environ["SHA"], os.environ["BASE"]
    if base:
        url, text = f"{repo_url}/compare/{base}...{sha}", f"{base[:7]}...{sha[:7]}"
    else:
        url, text = f"{repo_url}/commit/{sha}", sha[:7]
    caption = f"#{os.environ['CHANNEL']} · "
    caption += f'<a href="{html.escape(url)}">{text}</a>'
    caption += f' · <a href="{html.escape(os.environ["RUN_URL"])}">CI</a>'
    return caption

async def fits_caption(client: "Client", text: str) -> bool:
    message = (await client.parser.parse(text, ParseMode.HTML))["message"]
    return len(message.encode("utf-16-le")) // 2 <= 1024

def notify_published(chat_id: int, messages: dict[str, int]):
    body = json.dumps({
        "run_id": int(os.environ["RUN_ID"]),
        "chat_id": chat_id,
        "messages": messages,
    }).encode()
    signature = hmac.new(os.environ["RELEASE_HOOK_SECRET"].encode(), body, hashlib.sha256).hexdigest()
    request = urllib.request.Request(
        os.environ["RELEASE_HOOK_URL"] + "/published",
        data = body,
        headers = {
            "Content-Type": "application/json",
            "X-Signature-256": f"sha256={signature}",
        },
    )
    urllib.request.urlopen(request).close()

def retry(func):
    async def wrapper(*args, **kwargs):
        for attempt in range(3):
            try:
                return await func(*args, **kwargs)
            except FloodWait as e:
                print(e)
                if attempt == 2:
                    raise
                await sleep(e.value)
    return wrapper

@retry
async def send_apks(client: "Client", chat_id: int | str, apks: dict[str, Path], caption: str):
    documents = [InputMediaDocument(media = str(apk)) for apk in apks.values()]
    documents[-1].caption = caption
    print("Uploading to Telegram:", flush=True)
    for document in documents:
        print(f"- {document.media}", flush=True)
    return await client.send_media_group(
        chat_id,
        media = documents,
    )

@retry
async def send_changelog(client: "Client", chat_id: int | str, reply_to_message_id: int):
    await client.send_message(
        chat_id = chat_id,
        text = os.environ["CHANGELOG"],
        link_preview_options = LinkPreviewOptions(is_disabled = True),
        reply_parameters = ReplyParameters(message_id = reply_to_message_id),
    )

def get_client(bot_token: str):
    return Client(
        "helper_bot",
        api_id=api_id,
        api_hash=api_hash,
        bot_token=bot_token,
        parse_mode=ParseMode.HTML,
    )

async def main():
    chat_id = os.environ["CHAT_ID"]
    with contextlib.suppress(ValueError):
        chat_id = int(chat_id)
    apks = get_apks()
    client = get_client(os.environ["HELPER_BOT_TOKEN"])
    await client.start()
    caption = f"{get_caption()}\n\n{os.environ['CHANGELOG']}"
    if await fits_caption(client, caption):
        messages = await send_apks(client, chat_id, apks, caption)
    else:
        messages = await send_apks(client, chat_id, apks, get_caption())
        await send_changelog(client, chat_id, messages[-1].id)
    await client.log_out()
    notify_published(messages[0].chat.id, {abi: message.id for abi, message in zip(apks, messages)})

if __name__ == "__main__":
    run(main())
