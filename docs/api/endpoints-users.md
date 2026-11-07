# API LogiStock : gestion des comptes (US2)

Toutes les routes `/api/users/**` sont **réservées à l'ADMINISTRATEUR** : un autre rôle reçoit `403`, sans token `401`.
Le mot de passe n'est jamais renvoyé par l'API, et il est stocké haché (BCrypt).

| Méthode | Route | Description | Codes |
|---|---|---|---|
| GET | `/users` | Liste des comptes, triée par nom | 200 |
| GET | `/users/{id}` | Détail d'un compte | 200, 404 |
| POST | `/users` | Création d'un compte actif | 201, 400, 409 (e-mail déjà utilisé) |
| PUT | `/users/{id}` | Modification (nom, e-mail, rôle, mot de passe facultatif) | 200, 400, 404, 409 |
| PATCH | `/users/{id}/actif` | Activer ou désactiver un compte | 200, 400, 404, 409 |

Corps de création ou de modification :
`{"nom":"Aminata Traoré","email":"aminata@logistock.ml","motDePasse":"Motdepasse1","role":"COMMERCIAL"}`

Rôles possibles : `ADMINISTRATEUR`, `GESTIONNAIRE_STOCK`, `COMMERCIAL`.

Corps de l'activation : `{"actif": false}`. Un compte désactivé ne peut plus se connecter.

## Règles de gestion

- L'e-mail est unique, enregistré en minuscules.
- Le mot de passe fait au moins 8 caractères. À la modification, un mot de passe vide ou absent conserve l'ancien.
- Un administrateur ne peut pas désactiver son propre compte ni changer son propre rôle (`409`), pour ne pas se retirer l'accès.
