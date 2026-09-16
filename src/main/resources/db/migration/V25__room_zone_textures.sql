-- Wallpaper/floor: same "possessable item, placed via user_items" model as
-- furniture (ItemSubType.WALLPAPER/FLOOR already existed, unused until now —
-- see FurnitureStateService's PLACEABLE_SUBTYPE comment), but a texture
-- targets a room *zone* (one of house_data's wall or floor polygons) instead
-- of an x/y point, so it needs its own pair of placement columns alongside
-- the existing x/y/orientation ones.

ALTER TABLE user_items
    ADD COLUMN zone_type  VARCHAR(10),
    ADD COLUMN zone_index INTEGER;

-- Replaces the old two-way check (unplaced XOR placed-with-x/y/orientation)
-- with a three-way one: unplaced, placed-as-furniture (x/y/orientation set,
-- zone columns null), or placed-as-texture (zone columns set, x/y/orientation
-- null). A row can never mix both placement shapes.
ALTER TABLE user_items DROP CONSTRAINT chk_user_items_placement_consistency;
ALTER TABLE user_items ADD CONSTRAINT chk_user_items_placement_consistency CHECK (
    (placed_in_room_id IS NULL
        AND x IS NULL AND y IS NULL AND orientation IS NULL
        AND zone_type IS NULL AND zone_index IS NULL)
    OR (placed_in_room_id IS NOT NULL
        AND x IS NOT NULL AND y IS NOT NULL AND orientation IS NOT NULL
        AND zone_type IS NULL AND zone_index IS NULL)
    OR (placed_in_room_id IS NOT NULL
        AND x IS NULL AND y IS NULL AND orientation IS NULL
        AND zone_type IS NOT NULL AND zone_index IS NOT NULL)
);

-- One texture per zone at a time — applying a new one to an already-textured
-- zone must go through "remove" first (service-layer swap does both).
CREATE UNIQUE INDEX idx_user_items_room_zone
    ON user_items (placed_in_room_id, zone_type, zone_index)
    WHERE zone_type IS NOT NULL;
