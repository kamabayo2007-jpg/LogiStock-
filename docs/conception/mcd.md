# MCD : Modèle conceptuel de données LogiStock SA

Ce schéma correspond exactement à `schema.sql` (tables et colonnes de l'équipe). Il s'affiche automatiquement sur GitHub grâce à Mermaid.

## Schéma

```mermaid
erDiagram
    role ||--o{ utilisateur : "possède"
    categorie ||--o{ produit : "classe"
    client ||--o{ commande : "passe"
    utilisateur ||--o{ commande : "crée"
    commande ||--|{ contenir : "contient"
    produit ||--o{ contenir : "est vendu dans"
    commande ||--o| facture : "génère"
    produit ||--o{ mouvement_stock : "subit"
    utilisateur ||--o{ mouvement_stock : "enregistre"
    commande |o--o{ mouvement_stock : "provoque"

    role {
        int id_role PK
        string code_role UK
        string libelle_role
    }
    utilisateur {
        int id_utilisateur PK
        string email UK
        string mot_de_passe_hache
        string nom_complet
        boolean actif
        datetime date_creation
        int id_role FK
    }
    categorie {
        int id_categorie PK
        string nom_categorie UK
    }
    produit {
        int id_produit PK
        string reference UK
        string designation
        text description
        decimal prix_unitaire
        int quantite_stock
        int seuil_alerte
        int id_categorie FK
    }
    client {
        int id_client PK
        string nom_client
        string prenom_client
        string telephone
        string email_client
        string adresse
    }
    commande {
        int id_commande PK
        datetime date_commande
        enum statut "EN_COURS, VALIDEE, ANNULEE"
        int id_client FK
        int id_utilisateur FK
    }
    contenir {
        int id_commande PK, FK
        int id_produit PK, FK
        int quantite
        decimal prix_unitaire_vente
    }
    facture {
        int id_facture PK
        string numero_facture UK
        date date_facture
        decimal montant_total
        int id_commande FK, UK
    }
    mouvement_stock {
        int id_mouvement PK
        datetime date_mouvement
        enum type_mouvement "ENTREE, SORTIE"
        int quantite
        string motif
        int id_produit FK
        int id_utilisateur FK
        int id_commande FK "nullable"
    }
```

## Cardinalités (comment lire le schéma)

| Relation | Lecture |
|---|---|
| role → utilisateur | Un rôle est attribué à plusieurs utilisateurs ; un utilisateur a un seul rôle |
| categorie → produit | Une catégorie contient plusieurs produits ; un produit a une seule catégorie |
| client → commande | Un client passe plusieurs commandes ; une commande concerne un seul client |
| utilisateur → commande | Un commercial crée plusieurs commandes |
| commande ↔ produit (table `contenir`) | Une commande contient au moins un produit ; un produit peut figurer dans plusieurs commandes. La table `contenir` porte la quantité et le prix de vente |
| commande → facture | Une commande a au plus une facture (0 ou 1) |
| produit → mouvement_stock | Un produit a plusieurs mouvements (entrées et sorties) |
| utilisateur → mouvement_stock | Un utilisateur enregistre plusieurs mouvements |
| commande → mouvement_stock | Une commande peut provoquer plusieurs mouvements de SORTIE ; un mouvement n'a pas forcément de commande (ex. entrée de stock : `id_commande` est NULL) |

## Règles de gestion

1. Un utilisateur n'accède à l'application qu'avec une authentification valide, et n'utilise que les fonctions de son rôle (`ADMINISTRATEUR`, `GESTIONNAIRE_STOCK`, `COMMERCIAL`).
2. La quantité en stock d'un produit ne peut jamais être négative (contrainte `quantite_stock >= 0`).
3. Toute entrée ou sortie de stock est enregistrée dans `mouvement_stock` avec la date, l'heure et l'utilisateur.
4. Une alerte s'affiche quand `quantite_stock` est inférieure ou égale à `seuil_alerte`.
5. Le prix de vente est copié dans `contenir.prix_unitaire_vente` : si le prix du produit change plus tard, les anciennes ventes ne changent pas.
6. Le total d'une commande n'est pas stocké : il se calcule par la somme de `quantite × prix_unitaire_vente` de ses lignes.
7. La validation d'une commande (statut `VALIDEE`) diminue le stock et crée un mouvement de SORTIE par ligne, rattaché à la commande. Elle est refusée si le stock d'un produit est insuffisant.
8. Une facture n'existe que pour une commande validée ; son `montant_total` est figé à la création.
