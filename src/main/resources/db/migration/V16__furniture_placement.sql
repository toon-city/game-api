-- V16: position/orientation for furniture placed in a room.
-- user_items.placed_in_room_id has existed since V10 but nothing ever set it —
-- this adds what's actually needed to place an item, and a check constraint
-- so "placed" is always a consistent tri-state: either fully unplaced (all
-- four columns NULL) or fully placed (room + all three coords set), never a
-- partial mix.

ALTER TABLE user_items
    ADD COLUMN IF NOT EXISTS x           DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS y           DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS orientation SMALLINT;

ALTER TABLE user_items
    ADD CONSTRAINT chk_user_items_placement_consistency
    CHECK (
        (placed_in_room_id IS NULL AND x IS NULL AND y IS NULL AND orientation IS NULL)
        OR
        (placed_in_room_id IS NOT NULL AND x IS NOT NULL AND y IS NOT NULL AND orientation IS NOT NULL)
    );
