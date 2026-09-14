-- A declined proposal loses its ring (consumed, not returned) but keeps its
-- row (status DECLINED) — the original ON DELETE CASCADE would have deleted
-- the whole proposal the moment its ring was deleted. Switch to SET NULL so
-- the ring can be removed while the proposal record survives.
ALTER TABLE marriage_proposals DROP CONSTRAINT marriage_proposals_ring_user_item_id_fkey;
ALTER TABLE marriage_proposals ALTER COLUMN ring_user_item_id DROP NOT NULL;
ALTER TABLE marriage_proposals ADD CONSTRAINT marriage_proposals_ring_user_item_id_fkey
    FOREIGN KEY (ring_user_item_id) REFERENCES user_items(id) ON DELETE SET NULL;
