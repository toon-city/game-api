-- V26: 3 new public rooms (Discotchat, Quizz, Maison hantée) with their own
-- house_schemas shape + decor, alongside Jardin's existing shape. All 4
-- shapes are marked non-selectable so a player creating their own private
-- house never sees them in the picker (they're for the public rooms only).
--
-- No original shape/placement data survived for these 3 (unlike Jardin,
-- whose house_data + a recovered raw placement dump both exist) — only the
-- furniture sprites themselves (game-assets/public/furnitures/{disco,quizz,
-- halloween}/) and their catalog items (already seeded, ids below) did. Each
-- shape here is a simple rectangular room (same points/walls/floors format
-- HouseParser expects, one door centered on the south wall) sized to fit its
-- room's furniture count, and decor placement is hand-authored.

-- ── Schéma non-sélectionnable par les joueurs ────────────────────────────────
ALTER TABLE house_schemas ADD COLUMN selectable BOOLEAN NOT NULL DEFAULT TRUE;

-- Jardin was already implicitly public-only (it's the only schema, used both
-- as the public room's shape and — accidentally — as every private house's
-- shape, since it was the only option); explicitly excluding it here per
-- the same rule now applied to the 3 new ones.
UPDATE house_schemas SET selectable = FALSE WHERE id = 1;

-- ── Fix: "Dancefloor" is a placeable decoration (like Bar/Dj), not a room-wide
-- floor texture — it was mistagged FLOOR instead of PIECE when first seeded.
UPDATE items SET sub_type = 'PIECE' WHERE id = 218 AND sub_type = 'FLOOR';

-- ── Compte système : propriétaire du mobilier des salles publiques ──────────
-- Pas de password_hash (connexion impossible) + banned=true en ceinture et
-- bretelles — jamais destiné à être un compte jouable.
INSERT INTO users (username, banned, ban_reason, rank)
VALUES ('DecorPublic', true, 'Compte système : détient le mobilier posé dans les salles publiques (non jouable).', 2);

-- ── Discotchat ────────────────────────────────────────────────────────────────
INSERT INTO house_schemas (name, description, house_data, selectable) VALUES (
  'Disco',
  'La discothèque publique : piste de danse, bar et banquettes',
  $JSON${
    "points": [
      {"x": -400, "y": -350},
      {"x": 400,  "y": -350},
      {"x": 400,  "y": 350},
      {"x": -400, "y": 350}
    ],
    "walls": [
      {"ptA": 0, "ptB": 1, "h": 250},
      {"ptA": 1, "ptB": 2, "h": 250},
      {"ptA": 2, "ptB": 3, "h": 250, "enter": true, "door": {"offset": 400}},
      {"ptA": 3, "ptB": 0, "h": 250}
    ],
    "floors": [
      {"points": [0, 1, 2, 3]}
    ]
  }$JSON$,
  false
);

INSERT INTO rooms (name, house_data, max_users, type, access, schema_id)
SELECT 'Discotchat', house_data, 100, 'PUBLIC', 'OPEN', id FROM house_schemas WHERE name = 'Disco';

INSERT INTO user_items (user_id, item_id, placed_in_room_id, x, y, orientation)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'), v.item_id, r.id, v.x, v.y, v.orientation
FROM (SELECT id FROM rooms WHERE name = 'Discotchat') r,
(VALUES
  (219, 400.0, 180.0, 1),  -- Dj
  (218, 400.0, 350.0, 1),  -- Dancefloor
  (217, 650.0, 250.0, 4),  -- Bar
  (212, 180.0, 250.0, 2),  -- Banquette 1
  (213, 180.0, 400.0, 2),  -- Banquette 2
  (214, 620.0, 430.0, 4),  -- Banquette 3
  (215, 350.0, 550.0, 1),  -- Banquette 4
  (216, 500.0, 550.0, 1),  -- Banquette 5
  (220, 700.0, 550.0, 1)   -- Plante
) AS v(item_id, x, y, orientation);

-- ── Quizz ─────────────────────────────────────────────────────────────────────
-- 375/300, not the rounder 350/300: a room whose floor's projected screen
-- height lands in roughly [700, 748) triggers a pre-existing PixiJS crash
-- ("can't access property updateRenderable, renderPipes[renderPipeId] is
-- undefined") somewhere in WallView/AreaView's mesh building — reproduced
-- deterministically by bisecting this exact room's house_data live (see
-- commit message), independent of which furniture is placed in it. Root
-- cause not fully isolated (narrowed to WallView.rebuild()'s segment-slicing
-- or AreaView's texture-repeat mesh, not confirmed further); sidestepped
-- here by picking a size outside the danger zone rather than leaving a
-- broken public room. Worth root-causing properly later.
INSERT INTO house_schemas (name, description, house_data, selectable) VALUES (
  'Quizz',
  'La salle de quizz publique',
  $JSON${
    "points": [
      {"x": -375, "y": -300},
      {"x": 375,  "y": -300},
      {"x": 375,  "y": 300},
      {"x": -375, "y": 300}
    ],
    "walls": [
      {"ptA": 0, "ptB": 1, "h": 250},
      {"ptA": 1, "ptB": 2, "h": 250},
      {"ptA": 2, "ptB": 3, "h": 250, "enter": true, "door": {"offset": 375}},
      {"ptA": 3, "ptB": 0, "h": 250}
    ],
    "floors": [
      {"points": [0, 1, 2, 3]}
    ]
  }$JSON$,
  false
);

INSERT INTO rooms (name, house_data, max_users, type, access, schema_id)
SELECT 'Quizz', house_data, 100, 'PUBLIC', 'OPEN', id FROM house_schemas WHERE name = 'Quizz';

INSERT INTO user_items (user_id, item_id, placed_in_room_id, x, y, orientation)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'), v.item_id, r.id, v.x, v.y, v.orientation
FROM (SELECT id FROM rooms WHERE name = 'Quizz') r,
(VALUES
  (248, 400.0, 350.0, 1),  -- Table
  (246, 400.0, 180.0, 3),  -- Camera
  (247, 220.0, 300.0, 1),  -- Pilier
  (245, 580.0, 200.0, 1),  -- Cadre
  (244, 580.0, 450.0, 1)   -- Bumper
) AS v(item_id, x, y, orientation);

-- ── Maison hantée ─────────────────────────────────────────────────────────────
INSERT INTO house_schemas (name, description, house_data, selectable) VALUES (
  'Maison hantée',
  'La maison hantée publique, pour Halloween',
  $JSON${
    "points": [
      {"x": -450, "y": -375},
      {"x": 450,  "y": -375},
      {"x": 450,  "y": 375},
      {"x": -450, "y": 375}
    ],
    "walls": [
      {"ptA": 0, "ptB": 1, "h": 250},
      {"ptA": 1, "ptB": 2, "h": 250},
      {"ptA": 2, "ptB": 3, "h": 250, "enter": true, "door": {"offset": 450}},
      {"ptA": 3, "ptB": 0, "h": 250}
    ],
    "floors": [
      {"points": [0, 1, 2, 3]}
    ]
  }$JSON$,
  false
);

INSERT INTO rooms (name, house_data, max_users, type, access, schema_id)
SELECT 'Maison hantée', house_data, 100, 'PUBLIC', 'OPEN', id FROM house_schemas WHERE name = 'Maison hantée';

INSERT INTO user_items (user_id, item_id, placed_in_room_id, x, y, orientation)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'), v.item_id, r.id, v.x, v.y, v.orientation
FROM (SELECT id FROM rooms WHERE name = 'Maison hantée') r,
(VALUES
  (223, 250.0, 250.0, 2),  -- Divan
  (224, 550.0, 250.0, 4),  -- Divan 2
  (229, 250.0, 450.0, 1),  -- Table 1
  (230, 550.0, 450.0, 1),  -- Table 2
  (232, 400.0, 550.0, 1),  -- Tablem 1
  (233, 400.0, 180.0, 1),  -- Tablem 2
  (221, 180.0, 350.0, 1),  -- Chandelier
  (222, 620.0, 350.0, 1),  -- Chandelier 2
  (227, 180.0, 180.0, 1),  -- Plasma
  (228, 620.0, 180.0, 1),  -- Plasma 2
  (231, 400.0, 650.0, 1),  -- Tableau
  (225, 700.0, 400.0, 1),  -- Fenetre
  (226, 300.0, 350.0, 1)   -- Lutin
) AS v(item_id, x, y, orientation);

-- ── Jardin : nettoyage des données de test + vrai décor ──────────────────────
-- room 1 n'avait jamais reçu de décor "officiel" — seulement des meubles
-- placés par un compte de test pendant les vérifications de déplacement/
-- rotation plus tôt dans le projet. On les déplace (pas supprime) vers
-- l'inventaire de leur propriétaire, puis on pose le vrai décor.
UPDATE user_items
SET placed_in_room_id = NULL, x = NULL, y = NULL, orientation = NULL
WHERE placed_in_room_id = 1
  AND user_id <> (SELECT id FROM users WHERE username = 'DecorPublic');

INSERT INTO user_items (user_id, item_id, placed_in_room_id, x, y, orientation)
SELECT (SELECT id FROM users WHERE username = 'DecorPublic'), v.item_id, 1, v.x, v.y, v.orientation
FROM (VALUES
  (238, 400.0, 350.0, 1),  -- Rond centre

  (239, 400.0, 220.0, 1),  -- Statut
  (240, 220.0, 250.0, 2),  -- Statut 1
  (241, 580.0, 250.0, 4),  -- Statut 2
  (242, 220.0, 480.0, 2),  -- Statut 3
  (243, 580.0, 480.0, 4),  -- Statut 4

  (234, 300.0, 500.0, 1),  -- Banc
  (234, 500.0, 500.0, 1),
  (234, 250.0, 330.0, 4),
  (234, 550.0, 330.0, 2),

  (235, 150.0, 180.0, 1),  -- Haie (haie border)
  (235, 250.0, 150.0, 1),
  (235, 350.0, 150.0, 1),
  (235, 450.0, 150.0, 1),
  (235, 550.0, 150.0, 1),
  (235, 650.0, 180.0, 1),
  (235, 150.0, 550.0, 1),
  (235, 650.0, 550.0, 1),

  (236, 300.0, 250.0, 1),  -- Herbe
  (236, 500.0, 250.0, 1),
  (236, 200.0, 400.0, 1),
  (236, 600.0, 400.0, 1),
  (236, 350.0, 600.0, 1),
  (236, 450.0, 600.0, 1),

  (237, 400.0, 470.0, 1),  -- Pave (chemin)
  (237, 400.0, 420.0, 1),
  (237, 350.0, 440.0, 1),
  (237, 450.0, 440.0, 1)
) AS v(item_id, x, y, orientation);
