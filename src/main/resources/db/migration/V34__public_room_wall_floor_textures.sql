-- V34: wall/floor textures for Maison hantée, Quizz and Discotchat (wall
-- only — no disco floor asset recovered), recovered from game-core's own
-- bundled dev assets (game-core/assets/house/*.jpg|png), not game-assets —
-- flagged live: "Dans les assets de game-core tu as les papier peint/
-- tapisseries etc des autres lieux publics". V27's original claim that only
-- Jardin's floor texture existed was wrong; it just wasn't looked for in
-- game-core's own asset bundle. Copied into game-assets/public/textures/
-- {floors,walls}/ (the runtime-served location, resolveFloorTextureUrl/
-- resolveWallTextureUrl's convention) in the same commit as this migration.
--
-- Applied to every non-hidden, non-baseboard wall (h > 10) and the room's
-- only floor — same "one texture, every real zone" pattern already used for
-- Jardin's jardinherbe in V27 (its 2 floor polygons both got the same
-- grass). Maison hantée's 2 low h=10 dividers are left untextured (they're
-- not real walls, no wallpaper zone_index makes sense there); Discotchat's
-- 2 HDN walls don't matter either way since they're never drawn.

INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Ha sol', 'FURNITURE', 'FLOOR', false, 'http://localhost:3001/textures/floors/ha_sol.jpg', 'ha_sol.jpg', 'floors'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'floors' AND sprite_path = 'ha_sol.jpg');

INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Ha mur', 'FURNITURE', 'WALL', false, 'http://localhost:3001/textures/walls/ha_mur1.jpg', 'ha_mur1.jpg', 'walls'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'walls' AND sprite_path = 'ha_mur1.jpg');

INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Quizz sol', 'FURNITURE', 'FLOOR', false, 'http://localhost:3001/textures/floors/quizz_sol.jpg', 'quizz_sol.jpg', 'floors'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'floors' AND sprite_path = 'quizz_sol.jpg');

INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Quizz tapisserie', 'FURNITURE', 'WALL', false, 'http://localhost:3001/textures/walls/quizz_tapisserie.jpg', 'quizz_tapisserie.jpg', 'walls'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'walls' AND sprite_path = 'quizz_tapisserie.jpg');

INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Disco mur', 'FURNITURE', 'WALL', false, 'http://localhost:3001/textures/walls/disco_mur.png', 'disco_mur.png', 'walls'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'walls' AND sprite_path = 'disco_mur.png');

-- ── Maison hantée (room id 6): floor 0, walls 1+2 (the two h=250 real walls) ──
DELETE FROM user_items WHERE placed_in_room_id = 6 AND zone_type IS NOT NULL
  AND user_id = (SELECT id FROM users WHERE username = 'DecorPublic');

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'floors' AND sprite_path = 'ha_sol.jpg'),
       6, 'FLOOR', 0;

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'walls' AND sprite_path = 'ha_mur1.jpg'),
       6, 'WALL', z
FROM (VALUES (1), (2)) AS v(z);

-- ── Quizz (room id 5): floor 0, walls 0+1 ────────────────────────────────────
DELETE FROM user_items WHERE placed_in_room_id = 5 AND zone_type IS NOT NULL
  AND user_id = (SELECT id FROM users WHERE username = 'DecorPublic');

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'floors' AND sprite_path = 'quizz_sol.jpg'),
       5, 'FLOOR', 0;

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'walls' AND sprite_path = 'quizz_tapisserie.jpg'),
       5, 'WALL', z
FROM (VALUES (0), (1)) AS v(z);

-- ── Discotchat (room id 4): walls 0+1 only (no floor asset recovered) ───────
DELETE FROM user_items WHERE placed_in_room_id = 4 AND zone_type IS NOT NULL
  AND user_id = (SELECT id FROM users WHERE username = 'DecorPublic');

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'walls' AND sprite_path = 'disco_mur.png'),
       4, 'WALL', z
FROM (VALUES (0), (1)) AS v(z);
