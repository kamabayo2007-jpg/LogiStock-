# API LogiStock : authentification et stock

Base : `http://localhost:8080/api`. Toutes les routes (sauf `/auth/login` et `/health`) demandent le header `Authorization: Bearer <token>`.
Les erreurs sont renvoyées au format `{"message": "..."}`.

## Rôles et droits

| Route | ADMINISTRATEUR | GESTIONNAIRE_STOCK | COMMERCIAL |
|---|---|---|---|
| `GET /produits`, `/categories` (et sous-routes) | oui | oui | oui |
| `POST/PUT/DELETE /produits`, `/categories` | non | oui | non |
| `GET /mouvements` | oui | oui | non |
| `POST /mouvements` | non | oui | non |

Un rôle non autorisé reçoit `403`. Sans token valide : `401`.

## Authentification (US1)

| Méthode | Route | Description |
|---|---|---|
| POST | `/auth/login` | Corps `{"email","password"}` → `{"token","user"}` |
| GET | `/auth/me` | Utilisateur du token |
| GET | `/health` | Test de disponibilité (sans token) |

## Catégories (US6)

| Méthode | Route | Description | Codes |
|---|---|---|---|
| GET | `/categories` | Liste triée par nom | 200 |
| POST | `/categories` | Corps `{"nom"}` | 201, 400, 409 (doublon) |
| PUT | `/categories/{id}` | Renommer | 200, 404, 409 |
| DELETE | `/categories/{id}` | Supprimer | 204, 404, 409 (catégorie utilisée) |

## Produits (US5, US8)

| Méthode | Route | Description | Codes |
|---|---|---|---|
| GET | `/produits?q=` | Liste, recherche facultative par désignation ou référence | 200 |
| GET | `/produits/alertes` | Produits dont quantité ≤ seuil d'alerte | 200 |
| GET | `/produits/ruptures` | Produits dont quantité = 0 | 200 |
| GET | `/produits/{id}` | Détail | 200, 404 |
| POST | `/produits` | Création (une quantité > 0 crée une ENTREE « Stock initial ») | 201, 400, 404, 409 (référence déjà utilisée) |
| PUT | `/produits/{id}` | Modification (la quantité est ignorée : elle change par les mouvements) | 200, 400, 404, 409 |
| DELETE | `/produits/{id}` | Suppression | 204, 404, 409 (produit déjà utilisé) |

Corps de création :
`{"reference":"PAP-010","designation":"Cahier 96 pages","description":"","prix":1200,"quantite":20,"seuilAlerte":5,"categorieId":1}`

## Mouvements de stock (US7, US9)

| Méthode | Route | Description | Codes |
|---|---|---|---|
| POST | `/mouvements` | Corps `{"produitId","type":"ENTREE\|SORTIE","quantite","motif"}` | 201, 400, 404, 409 (stock insuffisant) |
| GET | `/mouvements?produitId=&type=` | Historique, du plus récent au plus ancien ; filtres facultatifs | 200 |

Le stock ne peut jamais devenir négatif : une sortie supérieure au stock disponible est refusée avec `409` et rien n'est enregistré. Le produit est verrouillé pendant l'opération (transaction), donc deux sorties simultanées ne peuvent pas faire passer le stock sous 0.
