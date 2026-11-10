# API LogiStock : demande de compte (US4)

Route **publique** (sans jeton) : un futur utilisateur demande un compte depuis l'écran de connexion.

| Méthode | Route | Description | Codes |
|---|---|---|---|
| POST | `/auth/inscription` | Crée un compte **inactif** en attente de validation | 201, 400, 409 (e-mail déjà utilisé) |

Corps de la requête :
`{"nom":"Aminata Traoré","email":"aminata@logistock.ml","motDePasse":"Motdepasse1"}`

Réponse `201` : `{"message":"Demande envoyée. Votre compte sera utilisable dès qu'un administrateur l'aura activé."}`

Exemple :

```bash
curl -i -X POST http://localhost:8080/api/auth/inscription \
  -H "Content-Type: application/json" \
  -d '{"nom":"Aminata Traoré","email":"aminata@logistock.ml","motDePasse":"Motdepasse1"}'
```

## Règles de gestion

- Le **rôle est imposé par le serveur** (`COMMERCIAL`, le moins privilégié) : la requête n'accepte pas de rôle.
- Le compte est créé **inactif** : la connexion est refusée (`401`, « Identifiants invalides ») tant qu'un
  administrateur ne l'a pas activé avec `PATCH /users/{id}/actif` (`{"actif": true}`). L'administrateur peut aussi
  corriger le rôle avec `PUT /users/{id}`.
- L'e-mail est unique et enregistré en minuscules (`409` s'il existe déjà).
- Le mot de passe fait au moins 8 caractères (`400` sinon) et est stocké haché (BCrypt).

## Parcours complet

1. L'utilisateur envoie sa demande (`POST /auth/inscription`).
2. L'administrateur ouvre l'écran « Comptes » (`GET /users`) : le compte apparaît désactivé.
3. L'administrateur l'active (`PATCH /users/{id}/actif`) et ajuste le rôle si besoin.
4. L'utilisateur peut se connecter (`POST /auth/login`).
