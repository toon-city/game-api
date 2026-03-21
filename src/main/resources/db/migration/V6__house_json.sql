-- V6__house_json.sql
-- Rename house_xml → house_data in both tables and convert seed data to JSON

ALTER TABLE house_schemas RENAME COLUMN house_xml TO house_data;
ALTER TABLE rooms         RENAME COLUMN house_xml TO house_data;

-- Convert existing Jardin schema (id=1) to JSON format
UPDATE house_schemas
SET house_data = $JSON${
  "points": [
    {"x": -340, "y": -780},
    {"x": -340, "y": -40},
    {"x": 40,   "y": -40},
    {"x": 40,   "y": -780},
    {"x": -340, "y": -720},
    {"x": -340, "y": -620},
    {"x": -340, "y": -560},
    {"x": -340, "y": -500},
    {"x": -340, "y": -400},
    {"x": -520, "y": -780},
    {"x": -520, "y": -560},
    {"x": -520, "y": -40},
    {"x": -520, "y": 560},
    {"x": 40,   "y": 560},
    {"x": -200, "y": -40},
    {"x": -100, "y": -40},
    {"x": 440,  "y": -780},
    {"x": 440,  "y": 560},
    {"x": 40,   "y": -500},
    {"x": 40,   "y": -400},
    {"x": 40,   "y": 220},
    {"x": 40,   "y": 320}
  ],
  "walls": [
    {"ptA": 9,  "ptB": 0,  "h": 250},
    {"ptA": 0,  "ptB": 4,  "h": 10},
    {"ptA": 9,  "ptB": 10, "h": 250},
    {"ptA": 10, "ptB": 6,  "h": 10},
    {"ptA": 6,  "ptB": 5,  "h": 10},
    {"ptA": 6,  "ptB": 7,  "h": 10},
    {"ptA": 1,  "ptB": 11, "h": 10},
    {"ptA": 1,  "ptB": 8,  "h": 10},
    {"ptA": 10, "ptB": 11, "h": 250},
    {"ptA": 0,  "ptB": 3,  "h": 250, "enter": true, "door": {"offset": 192}},
    {"ptA": 11, "ptB": 12, "h": 250},
    {"ptA": 12, "ptB": 13, "h": 10},
    {"ptA": 1,  "ptB": 14, "h": 10},
    {"ptA": 2,  "ptB": 15, "h": 10},
    {"ptA": 13, "ptB": 17, "h": 10},
    {"ptA": 17, "ptB": 16, "h": 10},
    {"ptA": 16, "ptB": 3,  "h": 250},
    {"ptA": 3,  "ptB": 18, "h": 10},
    {"ptA": 19, "ptB": 2,  "h": 10},
    {"ptA": 2,  "ptB": 20, "h": 10},
    {"ptA": 21, "ptB": 13, "h": 10}
  ],
  "floors": [
    {"points": [0, 1, 2, 3]},
    {"points": [9, 10, 6, 5, 4, 0]},
    {"points": [10, 11, 1, 8, 7, 6]},
    {"points": [11, 12, 13, 2, 15, 14, 1]},
    {"points": [3, 13, 17, 16]}
  ]
}$JSON$
WHERE id = 1;

-- Sync the room that references schema 1
UPDATE rooms
SET house_data = (SELECT house_data FROM house_schemas WHERE id = 1)
WHERE schema_id = 1;
