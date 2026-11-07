# API LogiStock : clients, commandes et factures (US10 à US14)

Accès : le **COMMERCIAL** peut tout faire ; l'**ADMINISTRATEUR** peut seulement consulter (GET) ;
le **GESTIONNAIRE_STOCK** n'a pas accès. Autre rôle : `403`. Sans token : `401`.

## Clients (US10)

| Méthode | Route | Description | Codes |
|---|---|---|---|
| GET | `/clients?q=sahel` | Liste (recherche facultative sur nom ou prénom) | 200 |
| GET | `/clients/{id}` | Détail | 200, 404 |
| POST | `/clients` | Création | 201, 400 |
| PUT | `/clients/{id}` | Modification | 200, 400, 404 |
| DELETE | `/clients/{id}` | Suppression | 204, 404, 409 (client avec commandes) |
| GET | `/clients/{id}/commandes` | **Historique d'achats** du client (US14) | 200, 404 |

Corps : `{"nom":"Coulibaly","prenom":"Mamadou","telephone":"+223 65 55 44 33","email":"m@x.ml","adresse":"Kalaban Coura"}`
(seul `nom` est obligatoire ; `prenom` sert aux particuliers).

## Commandes (US11 à US13)

| Méthode | Route | Description | Codes |
|---|---|---|---|
| POST | `/commandes` | Crée une commande **EN_COURS** (le stock ne bouge pas) | 201, 400, 404, 409 |
| GET | `/commandes?statut=VALIDEE&clientId=1` | Liste, filtres facultatifs, plus récentes d'abord | 200 |
| GET | `/commandes/{id}` | Détail avec ses lignes et son total | 200, 404 |
| POST | `/commandes/{id}/validation` | **Valide** : diminue le stock, crée les sorties, émet la facture | 200, 404, 409 |
| POST | `/commandes/{id}/annulation` | Annule une commande EN_COURS | 200, 404, 409 |
| GET | `/commandes/{id}/facture` | Facture en JSON | 200, 404 |
| GET | `/commandes/{id}/facture.pdf` | Facture au format PDF | 200, 404 |

Création : `{"clientId":1,"lignes":[{"produitId":1,"quantite":10},{"produitId":2,"quantite":2}]}`

## Règles de gestion

- Le prix de chaque ligne est le **prix du produit au moment de la commande** ; il ne change plus ensuite.
- Un même produit demandé plusieurs fois est regroupé sur une seule ligne.
- La **validation** se fait dans une seule transaction : le stock de **toutes** les lignes est vérifié d'abord.
  Si un produit manque, rien n'est modifié et l'API répond `409` avec le message
  `Stock insuffisant pour « Agrafeuse de bureau » : disponible 3, demandé 5`.
- Validation réussie : stock diminué, un mouvement de SORTIE par ligne (rattaché à la commande, motif
  « Vente commande #N ») et une facture `FAC-AAAA-NNNN`.
- Seule une commande EN_COURS peut être validée ou annulée (`409` sinon). Une commande validée ne s'annule pas.
- La commande est verrouillée pendant la validation : elle ne peut pas être validée deux fois en même temps.
