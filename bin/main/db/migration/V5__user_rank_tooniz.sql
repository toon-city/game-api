-- Add rank (0=user, 1=moderator, 2=admin, ...) and tooniz_level (0=none, 1/2/3) to users table.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS rank          INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tooniz_level  INTEGER NOT NULL DEFAULT 0;
