# Backlog produit LogiStock SA

Priorité : P1 = indispensable (MVP, Jalon 2), P2 = important (Jalon 3), P3 = bonus.

## Module 1 — Authentification et rôles

**US1 (P1)** — En tant qu'utilisateur, je veux me connecter avec mon e-mail et mon mot de passe afin d'accéder à l'application.
- Critères : connexion réussie renvoie un token JWT ; identifiants faux → message d'erreur 401 ; aucune page accessible sans token valide.

**US2 (P1)** — En tant qu'administrateur, je veux créer, modifier et désactiver des comptes et leur attribuer un rôle afin de contrôler les accès.
- Critères : seul l'ADMIN accède à `/api/users` (403 sinon) ; mot de passe stocké haché ; e-mail unique.

**US3 (P1)** — En tant qu'utilisateur, je veux voir uniquement les fonctions de mon rôle afin de ne pas accéder à ce qui ne me concerne pas.
- Critères : contrôle RBAC fait côté API ; menu du frontend adapté au rôle.

**US4 (P2)** — En tant qu'utilisateur, je veux m'inscrire afin de demander un compte.
- Critères : compte créé inactif jusqu'à validation par l'administrateur.

## Module 2 — Gestion des stocks

**US5 (P1)** — En tant que gestionnaire de stock, je veux ajouter, modifier et supprimer des produits (référence, désignation, catégorie, prix, quantité, seuil) afin de tenir le catalogue à jour.
- Critères : référence unique ; prix ≥ 0 ; quantité ≥ 0.

**US6 (P1)** — En tant que gestionnaire de stock, je veux gérer les catégories de produits afin de classer le catalogue.
- Critères : une catégorie utilisée ne peut pas être supprimée.

**US7 (P1)** — En tant que gestionnaire de stock, je veux enregistrer des entrées et sorties de stock afin de garder l'historique.
- Critères : chaque mouvement a date, heure, utilisateur, quantité > 0 ; la quantité du produit est mise à jour ; une sortie supérieure au stock est refusée.

**US8 (P1)** — En tant que gestionnaire de stock, je veux voir une alerte quand un produit atteint son seuil afin de réapprovisionner à temps.
- Critères : alerte visible quand quantité ≤ seuil ; liste des produits en rupture (quantité = 0).

**US9 (P2)** — En tant que gestionnaire de stock, je veux consulter l'historique des mouvements d'un produit avec filtres par date et par type.

## Module 3 — Ventes et clients

**US10 (P1)** — En tant que commercial, je veux créer et modifier des fiches clients afin de suivre mes clients.
- Critères : nom obligatoire ; recherche par nom ou téléphone.

**US11 (P1)** — En tant que commercial, je veux créer une commande avec un client et plusieurs produits afin d'enregistrer une vente.
- Critères : le total est calculé automatiquement ; le prix unitaire est copié au moment de la commande.

**US12 (P1)** — En tant que commercial, je veux valider une commande afin que le stock soit mis à jour.
- Critères : validation → stock décrémenté + mouvements de SORTIE créés ; stock insuffisant → validation refusée avec message précisant le produit ; opération faite dans une transaction.

**US13 (P2)** — En tant que commercial, je veux générer une facture PDF pour une commande validée afin de la remettre au client.
- Critères : numéro de facture unique ; PDF contient client, lignes, total, date.

**US14 (P2)** — En tant que commercial, je veux voir l'historique d'achats d'un client.

## Module 4 — Tableau de bord KPI

**US15 (P2)** — En tant que directeur, je veux voir le chiffre d'affaires sur une période donnée afin de suivre l'activité.
- Critères : filtre par dates ; seules les commandes VALIDÉES comptent.

**US16 (P2)** — En tant que directeur, je veux voir le palmarès des produits les plus vendus (top 5 ou 10) sous forme de graphique.

**US17 (P2)** — En tant que directeur, je veux voir la liste des produits en rupture ou en alerte.

**US18 (P2)** — En tant que directeur, je veux un indicateur supplémentaire justifié (proposition : meilleurs clients par montant d'achats).
- Critères : justification écrite dans la documentation ; accessible seulement aux rôles autorisés.

## Exigences transverses

**T1 (P1)** — Dépôt Git avec GitFlow (main, develop, feature/*).
**T2 (P1)** — Docker Compose lançant base de données, backend et frontend.
**T3 (P2)** — README d'installation et documentation des endpoints.
**T4 (P2)** — Tests automatisés sur : refus de vente si stock insuffisant, stock jamais négatif, accès refusé selon le rôle.

## Répartition proposée par sprint

- Sprint 1 (S01-S04, Jalon 1) : conception, backlog, MCD/MLD, ADR, schéma d'architecture, initialisation Git + Docker.
- Sprint 2 (S05-S08, Jalon 2) : US1, US2, US3, US5, US6, US7, US8 (Authentification/RBAC + Stocks).
- Sprint 3 (S09-S12, Jalon 3) : US10 à US18, tests, README, préparation de la soutenance.
