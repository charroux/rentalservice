-- Créer la séquence pour l'ID de la table car
CREATE SEQUENCE IF NOT EXISTS car_id_seq START WITH 1 INCREMENT BY 1;

-- Mettre à jour la table car pour utiliser la séquence comme default
ALTER TABLE car ALTER COLUMN id SET DEFAULT nextval('car_id_seq');

-- Ajuster la séquence au cas où il y aurait déjà des données
SELECT setval('car_id_seq', COALESCE((SELECT MAX(id) FROM car), 0) + 1, false);