-- LogiStock SA - Données de démonstration (seed) pour le schéma de l'équipe
-- Ordre d'exécution : schema.sql puis seed.sql, sur la base "logistock".
-- Les rôles sont déjà insérés par schema.sql.
-- Montants en FCFA.
USE logistock;

-- ---------------------------------------------------------------
-- UTILISATEURS (un par rôle)
-- Le mot de passe est un marqueur : le backend le remplace par un vrai hash
-- BCrypt au premier démarrage (DataSeeder).
-- Mots de passe de test : Admin123! / Stock123! / Vente123!
-- ---------------------------------------------------------------
INSERT INTO utilisateur (id_utilisateur, email, mot_de_passe_hache, nom_complet, actif, id_role) VALUES
(1, 'admin@logistock.ml', 'HASH_A_REMPLACER', 'Administrateur LogiStock', TRUE,
    (SELECT id_role FROM role WHERE code_role = 'ADMINISTRATEUR')),
(2, 'stock@logistock.ml', 'HASH_A_REMPLACER', 'Gestionnaire de stock', TRUE,
    (SELECT id_role FROM role WHERE code_role = 'GESTIONNAIRE_STOCK')),
(3, 'vente@logistock.ml', 'HASH_A_REMPLACER', 'Commercial', TRUE,
    (SELECT id_role FROM role WHERE code_role = 'COMMERCIAL'));

-- ---------------------------------------------------------------
-- CATEGORIES
-- ---------------------------------------------------------------
INSERT INTO categorie (id_categorie, nom_categorie) VALUES
(1, 'Papeterie'),
(2, 'Consommables'),
(3, 'Équipement de bureau'),
(4, 'Petit équipement informatique');

-- ---------------------------------------------------------------
-- PRODUITS (quantité = état final, après les mouvements ci-dessous)
-- Produits 5 et 10 : en alerte (quantité <= seuil)
-- Produit 11 : en rupture (quantité = 0)
-- ---------------------------------------------------------------
INSERT INTO produit (id_produit, reference, designation, description, prix_unitaire, quantite_stock, seuil_alerte, id_categorie) VALUES
(1,  'PAP-001', 'Ramette papier A4 80g',          'Ramette de 500 feuilles',             3500.00, 90, 20, 1),
(2,  'PAP-002', 'Stylo bille bleu (boîte de 50)', 'Boîte de 50 stylos',                  4000.00, 35, 10, 1),
(3,  'PAP-003', 'Classeur à levier A4',           'Classeur dos 80 mm',                  1500.00, 60, 15, 1),
(4,  'CON-001', 'Cartouche d''encre noire',       'Compatible imprimantes jet d''encre', 18000.00, 11, 5, 2),
(5,  'CON-002', 'Toner laser noir',               'Toner haute capacité',                45000.00,  3, 3, 2),
(6,  'EQU-001', 'Chaise de bureau ergonomique',   'Accoudoirs réglables',                55000.00,  6, 3, 3),
(7,  'EQU-002', 'Bureau 120 cm',                  'Plateau mélaminé, 2 tiroirs',         85000.00,  3, 2, 3),
(8,  'INF-001', 'Clé USB 32 Go',                  'USB 3.0',                              5000.00, 30, 10, 4),
(9,  'INF-002', 'Souris sans fil',                'Récepteur USB',                        7500.00, 20, 8, 4),
(10, 'PAP-004', 'Agrafeuse de bureau',            'Agrafeuse métal',                      3000.00,  2, 3, 1),
(11, 'EQU-003', 'Calculatrice de bureau',         'Écran 12 chiffres',                    6000.00,  0, 2, 3);

-- ---------------------------------------------------------------
-- CLIENTS
-- ---------------------------------------------------------------
INSERT INTO client (id_client, nom_client, prenom_client, telephone, email_client, adresse) VALUES
(1, 'Société Sahel Services',    NULL,       '+223 20 22 10 10', 'contact@sahelservices.ml',    'ACI 2000, Bamako'),
(2, 'Cabinet Diarra & Associés', NULL,       '+223 66 10 20 30', 'cabinet.diarra@example.ml',   'Hamdallaye, Bamako'),
(3, 'École Horizon',             NULL,       '+223 76 40 50 60', 'direction@ecolehorizon.ml',   'Badalabougou, Bamako'),
(4, 'Pharmacie Niger',           NULL,       '+223 79 11 22 33', NULL,                          'Quartier du Fleuve, Bamako'),
(5, 'Coulibaly',                 'Mamadou',  '+223 65 55 44 33', NULL,                          'Kalaban Coura, Bamako');

-- ---------------------------------------------------------------
-- COMMANDES (commercial, id 3)
-- 1 à 5 : VALIDEE | 6 : EN_COURS | 7 : ANNULEE
-- ---------------------------------------------------------------
INSERT INTO commande (id_commande, date_commande, statut, id_client, id_utilisateur) VALUES
(1, '2026-10-27 10:15:00', 'VALIDEE',  1, 3),
(2, '2026-10-29 14:30:00', 'VALIDEE',  2, 3),
(3, '2026-11-02 09:45:00', 'VALIDEE',  3, 3),
(4, '2026-11-03 11:20:00', 'VALIDEE',  1, 3),
(5, '2026-11-04 16:05:00', 'VALIDEE',  4, 3),
(6, '2026-11-05 08:30:00', 'EN_COURS', 2, 3),
(7, '2026-11-01 13:00:00', 'ANNULEE',  5, 3);

INSERT INTO contenir (id_commande, id_produit, quantite, prix_unitaire_vente) VALUES
(1, 1,  30,  3500.00),
(1, 2,   5,  4000.00),
(2, 4,   4, 18000.00),
(2, 5,   3, 45000.00),
(3, 6,   4, 55000.00),
(3, 7,   2, 85000.00),
(4, 8,  20,  5000.00),
(4, 9,  10,  7500.00),
(5, 10,  6,  3000.00),
(5, 11,  5,  6000.00),
(6, 3,  10,  1500.00),
(7, 1,  10,  3500.00);

-- ---------------------------------------------------------------
-- MOUVEMENTS DE STOCK : entrées initiales (gestionnaire, id 2)
-- ---------------------------------------------------------------
INSERT INTO mouvement_stock (date_mouvement, type_mouvement, quantite, motif, id_produit, id_utilisateur, id_commande) VALUES
('2026-10-20 09:00:00', 'ENTREE', 120, 'Stock initial', 1,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  40, 'Stock initial', 2,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  60, 'Stock initial', 3,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  15, 'Stock initial', 4,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',   6, 'Stock initial', 5,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  10, 'Stock initial', 6,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',   5, 'Stock initial', 7,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  50, 'Stock initial', 8,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',  30, 'Stock initial', 9,  2, NULL),
('2026-10-20 09:00:00', 'ENTREE',   8, 'Stock initial', 10, 2, NULL),
('2026-10-20 09:00:00', 'ENTREE',   5, 'Stock initial', 11, 2, NULL);

-- ---------------------------------------------------------------
-- MOUVEMENTS DE SORTIE générés par les commandes validées (1 à 5)
-- ---------------------------------------------------------------
INSERT INTO mouvement_stock (date_mouvement, type_mouvement, quantite, motif, id_produit, id_utilisateur, id_commande) VALUES
('2026-10-27 10:15:00', 'SORTIE', 30, 'Vente commande #1', 1,  3, 1),
('2026-10-27 10:15:00', 'SORTIE',  5, 'Vente commande #1', 2,  3, 1),
('2026-10-29 14:30:00', 'SORTIE',  4, 'Vente commande #2', 4,  3, 2),
('2026-10-29 14:30:00', 'SORTIE',  3, 'Vente commande #2', 5,  3, 2),
('2026-11-02 09:45:00', 'SORTIE',  4, 'Vente commande #3', 6,  3, 3),
('2026-11-02 09:45:00', 'SORTIE',  2, 'Vente commande #3', 7,  3, 3),
('2026-11-03 11:20:00', 'SORTIE', 20, 'Vente commande #4', 8,  3, 4),
('2026-11-03 11:20:00', 'SORTIE', 10, 'Vente commande #4', 9,  3, 4),
('2026-11-04 16:05:00', 'SORTIE',  6, 'Vente commande #5', 10, 3, 5),
('2026-11-04 16:05:00', 'SORTIE',  5, 'Vente commande #5', 11, 3, 5);

-- ---------------------------------------------------------------
-- FACTURES (montant = somme des lignes de la commande)
-- ---------------------------------------------------------------
INSERT INTO facture (numero_facture, date_facture, montant_total, id_commande) VALUES
('FAC-2026-0001', '2026-10-27', 125000.00, 1),
('FAC-2026-0002', '2026-10-29', 207000.00, 2),
('FAC-2026-0003', '2026-11-02', 390000.00, 3);
