-- V14: Ajout du nom aux packs de Kreds
ALTER TABLE kreds_packages
    ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '';
