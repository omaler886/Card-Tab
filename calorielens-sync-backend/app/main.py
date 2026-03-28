from __future__ import annotations

import base64
import hashlib
import hmac
import os
import secrets
import smtplib
import sqlite3
import time
from contextlib import contextmanager
from email.message import EmailMessage
from pathlib import Path
from typing import Generator, Optional

from fastapi import Depends, FastAPI, Header, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field

APP_DIR = Path(__file__).resolve().parent
PROJECT_DIR = APP_DIR.parent
DATA_DIR = PROJECT_DIR / "data"
OUTBOX_DIR = DATA_DIR / "outbox"
DATA_DIR.mkdir(parents=True, exist_ok=True)
OUTBOX_DIR.mkdir(parents=True, exist_ok=True)
DB_PATH = DATA_DIR / "sync.db"

ACCESS_TOKEN_TTL_SECONDS = int(os.getenv("ACCESS_TOKEN_TTL_SECONDS", "3600"))
REFRESH_TOKEN_TTL_SECONDS = int(os.getenv("REFRESH_TOKEN_TTL_SECONDS", str(60 * 60 * 24 * 30)))
EMAIL_TOKEN_TTL_SECONDS = int(os.getenv("EMAIL_TOKEN_TTL_SECONDS", str(60 * 60 * 24)))
PASSWORD_RESET_TOKEN_TTL_SECONDS = int(os.getenv("PASSWORD_RESET_TOKEN_TTL_SECONDS", str(60 * 30)))
PBKDF2_ITERATIONS = 120_000
PUBLIC_BASE_URL = os.getenv("PUBLIC_BASE_URL", "").rstrip("/")
SMTP_HOST = os.getenv("SMTP_HOST", "").strip()
SMTP_PORT = int(os.getenv("SMTP_PORT", "587"))
SMTP_USERNAME = os.getenv("SMTP_USERNAME", "").strip()
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "")
SMTP_FROM = os.getenv("SMTP_FROM", "no-reply@calorielens.local")
SMTP_USE_TLS = os.getenv("SMTP_USE_TLS", "true").lower() != "false"
REQUIRE_VERIFIED_EMAIL = os.getenv("REQUIRE_VERIFIED_EMAIL", "false").lower() == "true"
ENVIRONMENT = os.getenv("APP_ENV", "development").lower()


def init_db() -> None:
    with sqlite3.connect(DB_PATH) as connection:
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS users (
                id TEXT PRIMARY KEY,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                email_verified INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """
        )
        _ensure_column(connection, "users", "email_verified", "INTEGER NOT NULL DEFAULT 0")

        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS access_tokens (
                token TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS refresh_tokens (
                token TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                revoked_at INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS email_verification_tokens (
                token TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                used_at INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS password_reset_tokens (
                token TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                used_at INTEGER,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS sync_heads (
                user_id TEXT PRIMARY KEY,
                revision INTEGER NOT NULL,
                backup_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                updated_by_device TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS sync_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                revision INTEGER NOT NULL,
                base_revision INTEGER NOT NULL,
                forced INTEGER NOT NULL DEFAULT 0,
                backup_json TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                updated_by_device TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS devices (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id TEXT NOT NULL,
                device_id TEXT NOT NULL,
                device_name TEXT NOT NULL,
                last_seen_at INTEGER NOT NULL,
                last_revision INTEGER NOT NULL,
                UNIQUE(user_id, device_id),
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """
        )
        connection.commit()


def _ensure_column(connection: sqlite3.Connection, table: str, column: str, ddl: str) -> None:
    existing = [row[1] for row in connection.execute(f"PRAGMA table_info({table})").fetchall()]
    if column not in existing:
        connection.execute(f"ALTER TABLE {table} ADD COLUMN {column} {ddl}")


@contextmanager
def db() -> Generator[sqlite3.Connection, None, None]:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    try:
        yield connection
        connection.commit()
    finally:
        connection.close()


def now_ts() -> int:
    return int(time.time())


def hash_password(password: str) -> str:
    salt = os.urandom(16)
    derived = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, PBKDF2_ITERATIONS)
    return (
        f"pbkdf2_sha256${PBKDF2_ITERATIONS}$"
        f"{base64.urlsafe_b64encode(salt).decode()}$"
        f"{base64.urlsafe_b64encode(derived).decode()}"
    )


def verify_password(password: str, encoded_hash: str) -> bool:
    algorithm, raw_iterations, salt_b64, digest_b64 = encoded_hash.split("$", 3)
    if algorithm != "pbkdf2_sha256":
        return False
    salt = base64.urlsafe_b64decode(salt_b64.encode())
    expected = base64.urlsafe_b64decode(digest_b64.encode())
    actual = hashlib.pbkdf2_hmac(
        "sha256",
        password.encode("utf-8"),
        salt,
        int(raw_iterations),
    )
    return hmac.compare_digest(actual, expected)


def issue_access_token(user_id: str) -> tuple[str, int]:
    token = secrets.token_urlsafe(32)
    issued_at = now_ts()
    expires_at = issued_at + ACCESS_TOKEN_TTL_SECONDS
    with db() as connection:
        connection.execute(
            "INSERT INTO access_tokens(token, user_id, expires_at, created_at) VALUES (?, ?, ?, ?)",
            (token, user_id, expires_at, issued_at),
        )
    return token, expires_at


def issue_refresh_token(user_id: str) -> tuple[str, int]:
    token = secrets.token_urlsafe(48)
    issued_at = now_ts()
    expires_at = issued_at + REFRESH_TOKEN_TTL_SECONDS
    with db() as connection:
        connection.execute(
            "INSERT INTO refresh_tokens(token, user_id, expires_at, created_at, revoked_at) VALUES (?, ?, ?, ?, NULL)",
            (token, user_id, expires_at, issued_at),
        )
    return token, expires_at


def revoke_refresh_token(token: str) -> None:
    with db() as connection:
        connection.execute(
            "UPDATE refresh_tokens SET revoked_at = ? WHERE token = ? AND revoked_at IS NULL",
            (now_ts(), token),
        )


def create_one_time_token(table: str, user_id: str, ttl_seconds: int) -> tuple[str, int]:
    token = secrets.token_urlsafe(36)
    created_at = now_ts()
    expires_at = created_at + ttl_seconds
    with db() as connection:
        connection.execute(
            f"INSERT INTO {table}(token, user_id, expires_at, created_at, used_at) VALUES (?, ?, ?, ?, NULL)",
            (token, user_id, expires_at, created_at),
        )
    return token, expires_at


def consume_one_time_token(table: str, token: str) -> sqlite3.Row:
    current_ts = now_ts()
    with db() as connection:
        row = connection.execute(
            f"SELECT * FROM {table} WHERE token = ? AND used_at IS NULL AND expires_at > ?",
            (token, current_ts),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Token invalid or expired.")
        connection.execute(
            f"UPDATE {table} SET used_at = ? WHERE token = ?",
            (current_ts, token),
        )
    return row


def send_email(kind: str, email: str, subject: str, body: str) -> None:
    timestamp = int(time.time() * 1000)
    safe_email = email.replace("@", "_at_").replace("/", "_")
    outbox_path = OUTBOX_DIR / f"{timestamp}-{kind}-{safe_email}.txt"
    outbox_path.write_text(f"TO: {email}\nSUBJECT: {subject}\n\n{body}", encoding="utf-8")

    if not SMTP_HOST:
        return

    message = EmailMessage()
    message["Subject"] = subject
    message["From"] = SMTP_FROM
    message["To"] = email
    message.set_content(body)

    with smtplib.SMTP(SMTP_HOST, SMTP_PORT, timeout=30) as server:
        if SMTP_USE_TLS:
            server.starttls()
        if SMTP_USERNAME:
            server.login(SMTP_USERNAME, SMTP_PASSWORD)
        server.send_message(message)


def build_email_link(path: str, token: str) -> str:
    if PUBLIC_BASE_URL:
        return f"{PUBLIC_BASE_URL}{path}?token={token}"
    return token


def get_user_from_access_token(authorization: str | None = Header(default=None)) -> sqlite3.Row:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Missing bearer token.")
    token = authorization.removeprefix("Bearer ").strip()
    current_ts = now_ts()
    with db() as connection:
        row = connection.execute(
            """
            SELECT users.*
            FROM access_tokens
            JOIN users ON users.id = access_tokens.user_id
            WHERE access_tokens.token = ? AND access_tokens.expires_at > ?
            """,
            (token, current_ts),
        ).fetchone()
    if row is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Token expired or invalid.")
    return row


class RegisterRequest(BaseModel):
    email: str = Field(min_length=3, max_length=255)
    password: str = Field(min_length=8, max_length=128)


class LoginRequest(BaseModel):
    email: str = Field(min_length=3, max_length=255)
    password: str = Field(min_length=8, max_length=128)


class RefreshRequest(BaseModel):
    refresh_token: str = Field(min_length=10, max_length=255)


class EmailRequest(BaseModel):
    email: str = Field(min_length=3, max_length=255)


class VerifyEmailRequest(BaseModel):
    token: str = Field(min_length=10, max_length=255)


class ResetPasswordRequest(BaseModel):
    token: str = Field(min_length=10, max_length=255)
    new_password: str = Field(min_length=8, max_length=128)


class BackupEnvelope(BaseModel):
    schemaVersion: int
    encrypted: bool
    algorithm: str
    kdf: str
    iterations: int
    saltBase64: str
    ivBase64: str
    cipherTextBase64: str


class PushRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=128)
    device_name: str = Field(min_length=1, max_length=128)
    revision: int = Field(ge=1)
    base_revision: int = Field(ge=0, default=0)
    force: bool = False
    backup_envelope: BackupEnvelope


class UserResponse(BaseModel):
    id: str
    email: str
    email_verified: bool


class AuthResponse(BaseModel):
    access_token: str
    refresh_token: str
    access_expires_at: int
    refresh_expires_at: int
    email_verification_required: bool
    user: UserResponse


class EmailActionResponse(BaseModel):
    status: str
    debug_token: Optional[str] = None


class PullResponse(BaseModel):
    revision: int
    updated_at: int
    updated_by_device: str
    backup_envelope: BackupEnvelope


class DeviceResponse(BaseModel):
    device_id: str
    device_name: str
    last_seen_at: int
    last_revision: int


class ConflictResponse(BaseModel):
    code: str
    message: str
    server_revision: int
    server_updated_at: int
    server_updated_by_device: str
    client_base_revision: int


init_db()

app = FastAPI(title="CalorieLens Sync Backend", version="2.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "env": ENVIRONMENT}


@app.post("/api/auth/register", response_model=AuthResponse)
def register(request: RegisterRequest) -> AuthResponse:
    email = request.email.strip().lower()
    user_id = secrets.token_hex(16)
    created_at = now_ts()
    with db() as connection:
        existing = connection.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()
        if existing is not None:
            raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="Email already registered.")
        connection.execute(
            "INSERT INTO users(id, email, password_hash, email_verified, created_at) VALUES (?, ?, ?, ?, ?)",
            (user_id, email, hash_password(request.password), 0, created_at),
        )

    verification_token, _ = create_one_time_token(
        table="email_verification_tokens",
        user_id=user_id,
        ttl_seconds=EMAIL_TOKEN_TTL_SECONDS,
    )
    verification_link = build_email_link("/verify-email", verification_token)
    send_email(
        kind="verify",
        email=email,
        subject="CalorieLens Verify Email",
        body=f"Use this token or link to verify your email:\n{verification_link}",
    )
    return _auth_response(user_id)


@app.post("/api/auth/login", response_model=AuthResponse)
def login(request: LoginRequest) -> AuthResponse:
    email = request.email.strip().lower()
    with db() as connection:
        row = connection.execute(
            "SELECT id, email, password_hash, email_verified FROM users WHERE email = ?",
            (email,),
        ).fetchone()
    if row is None or not verify_password(request.password, row["password_hash"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid email or password.")
    if REQUIRE_VERIFIED_EMAIL and not bool(row["email_verified"]):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Email verification required.")
    return _auth_response(row["id"])


@app.post("/api/auth/refresh", response_model=AuthResponse)
def refresh_token(request: RefreshRequest) -> AuthResponse:
    current_ts = now_ts()
    with db() as connection:
        token_row = connection.execute(
            """
            SELECT refresh_tokens.token, refresh_tokens.user_id, users.email_verified
            FROM refresh_tokens
            JOIN users ON users.id = refresh_tokens.user_id
            WHERE refresh_tokens.token = ?
              AND refresh_tokens.expires_at > ?
              AND refresh_tokens.revoked_at IS NULL
            """,
            (request.refresh_token, current_ts),
        ).fetchone()
    if token_row is None:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Refresh token invalid or expired.")
    revoke_refresh_token(request.refresh_token)
    return _auth_response(token_row["user_id"])


@app.post("/api/auth/request-email-verification", response_model=EmailActionResponse)
def request_email_verification(request: EmailRequest) -> EmailActionResponse:
    email = request.email.strip().lower()
    with db() as connection:
        user = connection.execute("SELECT id, email_verified FROM users WHERE email = ?", (email,)).fetchone()
    if user is None:
        return EmailActionResponse(status="ok")
    if bool(user["email_verified"]):
        return EmailActionResponse(status="already_verified")

    token, _ = create_one_time_token("email_verification_tokens", user["id"], EMAIL_TOKEN_TTL_SECONDS)
    link = build_email_link("/verify-email", token)
    send_email(
        kind="verify",
        email=email,
        subject="CalorieLens Verify Email",
        body=f"Use this token or link to verify your email:\n{link}",
    )
    return EmailActionResponse(status="sent", debug_token=token if ENVIRONMENT != "production" else None)


@app.post("/api/auth/verify-email", response_model=EmailActionResponse)
def verify_email(request: VerifyEmailRequest) -> EmailActionResponse:
    token_row = consume_one_time_token("email_verification_tokens", request.token)
    with db() as connection:
        connection.execute(
            "UPDATE users SET email_verified = 1 WHERE id = ?",
            (token_row["user_id"],),
        )
    return EmailActionResponse(status="verified")


@app.post("/api/auth/request-password-reset", response_model=EmailActionResponse)
def request_password_reset(request: EmailRequest) -> EmailActionResponse:
    email = request.email.strip().lower()
    with db() as connection:
        user = connection.execute("SELECT id FROM users WHERE email = ?", (email,)).fetchone()
    if user is None:
        return EmailActionResponse(status="ok")

    token, _ = create_one_time_token("password_reset_tokens", user["id"], PASSWORD_RESET_TOKEN_TTL_SECONDS)
    link = build_email_link("/reset-password", token)
    send_email(
        kind="password-reset",
        email=email,
        subject="CalorieLens Reset Password",
        body=f"Use this token or link to reset your password:\n{link}",
    )
    return EmailActionResponse(status="sent", debug_token=token if ENVIRONMENT != "production" else None)


@app.post("/api/auth/reset-password", response_model=EmailActionResponse)
def reset_password(request: ResetPasswordRequest) -> EmailActionResponse:
    token_row = consume_one_time_token("password_reset_tokens", request.token)
    with db() as connection:
        connection.execute(
            "UPDATE users SET password_hash = ? WHERE id = ?",
            (hash_password(request.new_password), token_row["user_id"]),
        )
        connection.execute(
            "UPDATE refresh_tokens SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
            (now_ts(), token_row["user_id"]),
        )
    return EmailActionResponse(status="password_reset")


@app.get("/api/auth/me", response_model=UserResponse)
def me(user: sqlite3.Row = Depends(get_user_from_access_token)) -> UserResponse:
    return _user_response(user)


@app.post("/api/sync/push", response_model=None)
def push_sync(payload: PushRequest, user: sqlite3.Row = Depends(get_user_from_access_token)):
    current_ts = now_ts()
    with db() as connection:
        current = connection.execute(
            "SELECT revision, updated_at, updated_by_device FROM sync_heads WHERE user_id = ?",
            (user["id"],),
        ).fetchone()
        current_revision = int(current["revision"]) if current else 0

        if current and payload.base_revision < current_revision and not payload.force:
            conflict = ConflictResponse(
                code="sync_conflict",
                message="Server has a newer revision. Pull latest backup or force overwrite.",
                server_revision=current_revision,
                server_updated_at=int(current["updated_at"]),
                server_updated_by_device=current["updated_by_device"],
                client_base_revision=payload.base_revision,
            )
            return JSONResponse(status_code=status.HTTP_409_CONFLICT, content=conflict.model_dump())

        final_revision = max(payload.revision, current_revision + 1 if current else payload.revision)

        connection.execute(
            """
            INSERT INTO sync_heads(user_id, revision, backup_json, updated_at, updated_by_device)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id) DO UPDATE SET
                revision=excluded.revision,
                backup_json=excluded.backup_json,
                updated_at=excluded.updated_at,
                updated_by_device=excluded.updated_by_device
            """,
            (
                user["id"],
                final_revision,
                payload.backup_envelope.model_dump_json(),
                current_ts,
                payload.device_id,
            ),
        )
        connection.execute(
            """
            INSERT INTO sync_versions(user_id, revision, base_revision, forced, backup_json, updated_at, updated_by_device)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                user["id"],
                final_revision,
                payload.base_revision,
                1 if payload.force else 0,
                payload.backup_envelope.model_dump_json(),
                current_ts,
                payload.device_id,
            ),
        )
        connection.execute(
            """
            INSERT INTO devices(user_id, device_id, device_name, last_seen_at, last_revision)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id, device_id) DO UPDATE SET
                device_name=excluded.device_name,
                last_seen_at=excluded.last_seen_at,
                last_revision=excluded.last_revision
            """,
            (user["id"], payload.device_id, payload.device_name, current_ts, final_revision),
        )
    return {"status": "ok", "revision": final_revision}


@app.get("/api/sync/pull", response_model=PullResponse)
def pull_sync(device_id: str, user: sqlite3.Row = Depends(get_user_from_access_token)) -> PullResponse:
    current_ts = now_ts()
    with db() as connection:
        row = connection.execute(
            "SELECT revision, backup_json, updated_at, updated_by_device FROM sync_heads WHERE user_id = ?",
            (user["id"],),
        ).fetchone()
        if row is None:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="No backup found.")
        connection.execute(
            """
            INSERT INTO devices(user_id, device_id, device_name, last_seen_at, last_revision)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(user_id, device_id) DO UPDATE SET
                last_seen_at=excluded.last_seen_at,
                last_revision=excluded.last_revision
            """,
            (user["id"], device_id, device_id, current_ts, int(row["revision"])),
        )
    return PullResponse(
        revision=int(row["revision"]),
        updated_at=int(row["updated_at"]),
        updated_by_device=row["updated_by_device"],
        backup_envelope=BackupEnvelope.model_validate_json(row["backup_json"]),
    )


@app.get("/api/sync/devices", response_model=list[DeviceResponse])
def list_devices(user: sqlite3.Row = Depends(get_user_from_access_token)) -> list[DeviceResponse]:
    with db() as connection:
        rows = connection.execute(
            """
            SELECT device_id, device_name, last_seen_at, last_revision
            FROM devices
            WHERE user_id = ?
            ORDER BY last_seen_at DESC
            """,
            (user["id"],),
        ).fetchall()
    return [
        DeviceResponse(
            device_id=row["device_id"],
            device_name=row["device_name"],
            last_seen_at=int(row["last_seen_at"]),
            last_revision=int(row["last_revision"]),
        )
        for row in rows
    ]


def _auth_response(user_id: str) -> AuthResponse:
    with db() as connection:
        user = connection.execute(
            "SELECT id, email, email_verified FROM users WHERE id = ?",
            (user_id,),
        ).fetchone()
    access_token, access_expires_at = issue_access_token(user_id)
    refresh_token, refresh_expires_at = issue_refresh_token(user_id)
    return AuthResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        access_expires_at=access_expires_at,
        refresh_expires_at=refresh_expires_at,
        email_verification_required=not bool(user["email_verified"]),
        user=_user_response(user),
    )


def _user_response(user: sqlite3.Row) -> UserResponse:
    return UserResponse(
        id=user["id"],
        email=user["email"],
        email_verified=bool(user["email_verified"]),
    )
