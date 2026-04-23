-- ============================================================
--  Bomb.io  ·  Auth / Account Database  (MySQL 8.0)
--  Rule: NO game state here. Coordinates, live match data,
--        and tick data live in Battle Server memory only.
-- ============================================================

CREATE DATABASE IF NOT EXISTS bombio_auth
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE bombio_auth;

-- ── Players (accounts) ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS players (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username        VARCHAR(32)  NOT NULL,
  email           VARCHAR(255) NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,

  -- Account state
  is_active       TINYINT(1)   NOT NULL DEFAULT 1,
  is_banned       TINYINT(1)   NOT NULL DEFAULT 0,
  ban_reason      VARCHAR(255)          DEFAULT NULL,
  ban_expires_at  DATETIME              DEFAULT NULL,

  -- Soft currency earned in matches
  coins           INT UNSIGNED NOT NULL DEFAULT 0,
  -- Hard currency (purchased)
  gems            INT UNSIGNED NOT NULL DEFAULT 0,
  -- Overall player level (unlocks content)
  player_level    SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  player_xp       INT UNSIGNED      NOT NULL DEFAULT 0,

  -- Timestamps
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at   DATETIME              DEFAULT NULL,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uq_username (username),
  UNIQUE KEY uq_email    (email)
) ENGINE=InnoDB;

-- ── Sessions ────────────────────────────────────────────────
-- Persistent session records (truth lives in Redis TTL cache,
-- this table is the audit log / forced-logout source)
CREATE TABLE IF NOT EXISTS sessions (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  player_id       BIGINT UNSIGNED NOT NULL,
  token_hash      VARCHAR(64)     NOT NULL,   -- SHA-256 of JWT
  ip_address      VARCHAR(45)              DEFAULT NULL,
  user_agent      VARCHAR(255)             DEFAULT NULL,
  is_revoked      TINYINT(1)  NOT NULL DEFAULT 0,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expires_at      DATETIME    NOT NULL,

  UNIQUE KEY uq_token    (token_hash),
  KEY        idx_player  (player_id),
  CONSTRAINT fk_sessions_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Account bans ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS bans (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  player_id       BIGINT UNSIGNED NOT NULL,
  issued_by       BIGINT UNSIGNED          DEFAULT NULL,   -- admin player_id
  reason          VARCHAR(255)    NOT NULL,
  ban_type        ENUM('TEMP','PERMANENT') NOT NULL DEFAULT 'TEMP',
  expires_at      DATETIME                 DEFAULT NULL,
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_ban_player (player_id),
  CONSTRAINT fk_bans_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Player inventory (owned items / skins) ──────────────────
CREATE TABLE IF NOT EXISTS inventory (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  player_id       BIGINT UNSIGNED NOT NULL,
  item_type       ENUM('SKIN','BOMB_SKIN','TRAIL','VICTORY_ANIM','TAUNT') NOT NULL,
  item_id         VARCHAR(64)     NOT NULL,
  acquired_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  source          ENUM('PURCHASE','REWARD','EVENT','DEFAULT') NOT NULL DEFAULT 'PURCHASE',

  UNIQUE KEY uq_player_item (player_id, item_type, item_id),
  CONSTRAINT fk_inventory_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Purchase history ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS purchases (
  id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  player_id       BIGINT UNSIGNED NOT NULL,
  item_id         VARCHAR(64)     NOT NULL,
  currency_type   ENUM('COINS','GEMS','REAL_MONEY') NOT NULL,
  amount          INT UNSIGNED    NOT NULL,
  transaction_ref VARCHAR(128)             DEFAULT NULL,
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

  KEY idx_purchase_player (player_id),
  CONSTRAINT fk_purchases_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ── Default inventory for new players (trigger) ─────────────
DELIMITER $$
CREATE TRIGGER trg_new_player_inventory
AFTER INSERT ON players
FOR EACH ROW
BEGIN
  -- Every new player starts with the default skin set
  INSERT INTO inventory (player_id, item_type, item_id, source) VALUES
    (NEW.id, 'SKIN',         'skin_default',       'DEFAULT'),
    (NEW.id, 'BOMB_SKIN',    'bomb_default',        'DEFAULT'),
    (NEW.id, 'TRAIL',        'trail_none',          'DEFAULT'),
    (NEW.id, 'VICTORY_ANIM', 'victory_default',     'DEFAULT'),
    (NEW.id, 'TAUNT',        'taunt_default',       'DEFAULT');
END$$
DELIMITER ;
