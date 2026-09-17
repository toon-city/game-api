-- V33: items.render_type — the old game's STYPE (18/19/20), needed by
-- game-core's collision/depth-sort logic (HouseView.checkCollision only
-- treats base.type===18 as a blocking obstacle; FurnitureView.updateDepthAndAppearance
-- only gives base.type===18 the SCENE-layer avatar-priority depth, anything
-- else sorts at the lower FLOOR layer instead).
--
-- Flagged live: "Les items de type floor devrait avoir une profondeur plus
-- basse que tous les items de type meuble, ils ont des collisions avec les
-- murs mais pas avec les objets et avatars" — exactly what base.type!==18
-- already does in the engine. The gap was entirely on the data side: every
-- spawnFurniture call (game-web's game-canvas.component.ts, both the
-- initial room-snapshot loop and the furniture-place echo) hardcoded the
-- literal 18 regardless of the item, so the STYPE 19 (wall-mounted decor:
-- fenetre/tableau/cadre) and 20 (floor decal: herbe/pave/rond_centre/
-- dancefloor) distinction recovered from the old apparts/appart_models dump
-- never reached the client.
--
-- 18 stays the default for every other item (regular blocking furniture —
-- everything already seeded before this, and everything a player places
-- from their own inventory in a private house).
-- INTEGER (not SMALLINT): Hibernate maps Java `int` to INTEGER by default
-- with no explicit @Column columnDefinition — schema-validation on both
-- game-api and game-server-java's boot fails on a type mismatch otherwise.
ALTER TABLE items ADD COLUMN render_type INTEGER NOT NULL DEFAULT 18;

-- STYPE 19 in the recovered placement data: wall-mounted, non-blocking.
UPDATE items SET render_type = 19 WHERE id IN (225, 231, 245); -- Fenetre, Tableau, Cadre

-- STYPE 20 in the recovered placement data: flat floor decal, non-blocking.
UPDATE items SET render_type = 20 WHERE id IN (218, 236, 237, 238); -- Dancefloor, Herbe, Pave, Rond centre
