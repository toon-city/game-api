-- V2__houses.sql
-- House schemas (admin-defined templates) + house access system

-- ── Gabarits de maisons ───────────────────────────────────────────────────────
CREATE TABLE house_schemas (
    id          BIGSERIAL   PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    description TEXT,
    house_xml   TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Seed : schéma Jardin ──────────────────────────────────────────────────────
INSERT INTO house_schemas (name, description, house_xml) VALUES (
  'Jardin',
  'Le grand jardin public avec haies et statues',
  $MAPXML$<MAP>
  <P YPOS="-340" XPOS="-780"  />
  <P YPOS="-340" XPOS="-40"  />
  <P YPOS="40" XPOS="-40"  />
  <P YPOS="40" XPOS="-780"  />
  <P YPOS="-340" XPOS="-720"  />
  <P YPOS="-340" XPOS="-620"  />
  <P YPOS="-340" XPOS="-560"  />
  <P YPOS="-340" XPOS="-500"  />
  <P YPOS="-340" XPOS="-400"  />
  <P YPOS="-520" XPOS="-780"  />
  <P YPOS="-520" XPOS="-560"  />
  <P YPOS="-520" XPOS="-40"  />
  <P YPOS="-520" XPOS="560"  />
  <P YPOS="40" XPOS="560"  />
  <P YPOS="-200" XPOS="-40"  />
  <P YPOS="-100" XPOS="-40"  />
  <P YPOS="440" XPOS="-780"  />
  <P YPOS="440" XPOS="560"  />
  <P YPOS="40" XPOS="-500"  />
  <P YPOS="40" XPOS="-400"  />
  <P YPOS="40" XPOS="220"  />
  <P YPOS="40" XPOS="320"  />
  <W SF="2.25" H="250" PTB="0" PTA="9"  />
  <W SF="0.03" H="10" PTB="4" PTA="0"  />
  <W SF="2.75" H="250" PTB="10" PTA="9"  />
  <W SF="0.09" H="10" PTB="6" PTA="10"  />
  <W SF="0.03" H="10" PTB="5" PTA="6"  />
  <W SF="0.03" H="10" PTB="7" PTA="6"  />
  <W SF="0.09" H="10" PTB="11" PTA="1"  />
  <W SF="0.18" H="10" PTB="8" PTA="1"  />
  <W SF="6.5" H="250" PTB="11" PTA="10"  />
  <W ENTER="0" D0="192" SF="4.75" H="250" PTB="3" PTA="0"  />
  <W SF="7.5" H="250" PTB="12" PTA="11"  />
  <W SF="0.28" H="10" PTB="13" PTA="12"  />
  <W SF="0.07" H="10" PTB="14" PTA="1"  />
  <W SF="0.07" H="10" PTB="15" PTA="2"  />
  <W SF="2" H="10" PTB="17" PTA="13"  />
  <W SF="6.7" H="10" PTB="16" PTA="17"  />
  <W SF="5" H="250" PTB="3" PTA="16"  />
  <W SF="1.4" H="10" PTB="18" PTA="3"  />
  <W SF="1.8" H="10" PTB="2" PTA="19"  />
  <W SF="1.3" H="10" PTB="20" PTA="2"  />
  <W SF="1.2" H="10" PTB="13" PTA="21"  />
  <F PT3="3" PT2="2" PT1="1" PT0="0" SF="0"  />
  <F PT5="0" PT4="4" PT3="5" PT2="6" PT1="10" PT0="9" SF="0"  />
  <F PT5="6" PT4="7" PT3="8" PT2="1" PT1="11" PT0="10" SF="0"  />
  <F PT6="1" PT5="14" PT4="15" PT3="2" PT2="13" PT1="12" PT0="11" SF="0" />
  <F PT3="16" PT2="17" PT1="13" PT0="3" SF="0"  />
</MAP>$MAPXML$
);

-- ── Nouvelles colonnes sur rooms ──────────────────────────────────────────────
ALTER TABLE rooms
    ADD COLUMN type          VARCHAR(8)   NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN access        VARCHAR(8)   NOT NULL DEFAULT 'OPEN',
    ADD COLUMN owner_id      UUID         REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN password_hash VARCHAR(255),
    ADD COLUMN schema_id     BIGINT       REFERENCES house_schemas(id) ON DELETE SET NULL;

-- ── Update la room Jardin seed : lier au schéma ───────────────────────────────
UPDATE rooms
SET schema_id = 1,
    house_xml = (SELECT house_xml FROM house_schemas WHERE id = 1)
WHERE id = 1;
