-- V35: Discotchat's floor — no texture_disco.jpg asset was ever recovered
-- (V34's own comment), but the dancefloor piece's own sprite (disco/
-- dancefloor.png) has a solid magenta background (#C7047D, sampled
-- directly off the asset) — flagged live: "Le sol de la disco devrait
-- être rose comme le fond du dancefloor". Generated a small solid-color
-- tile at that exact color (game-assets/public/textures/floors/
-- disco_sol.png) rather than leaving the engine's default floor look.
INSERT INTO items (name, item_type, sub_type, possessable, display_image, sprite_path, sprite_key)
SELECT 'Disco sol', 'FURNITURE', 'FLOOR', false, 'http://localhost:3001/textures/floors/disco_sol.png', 'disco_sol.png', 'floors'
WHERE NOT EXISTS (SELECT 1 FROM items WHERE sprite_key = 'floors' AND sprite_path = 'disco_sol.png');

DELETE FROM user_items WHERE placed_in_room_id = 4 AND zone_type = 'FLOOR'
  AND user_id = (SELECT id FROM users WHERE username = 'DecorPublic');

INSERT INTO user_items (user_id, item_id, placed_in_room_id, zone_type, zone_index)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'),
       (SELECT id FROM items WHERE sprite_key = 'floors' AND sprite_path = 'disco_sol.png'),
       4, 'FLOOR', 0;
