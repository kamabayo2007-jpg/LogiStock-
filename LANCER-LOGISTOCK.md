# Lancer LogiStock SA sur n'importe quelle machine

Dépôt : https://github.com/kamabayo2007-jpg/LogiStock-

---

## MÉTHODE 1 – Avec les outils installés (XAMPP, Java, Node)

### À installer une seule fois
- Git
- XAMPP (pour MySQL/MariaDB)
- JDK 17 + Maven
- Node.js 20+ puis : `npm install -g @angular/cli`

Vérifier : `git --version`, `java -version`, `mvn -v`, `node -v`, `ng version`

### Lancer
1. Récupérer le code :
   ```
   git clone https://github.com/kamabayo2007-jpg/LogiStock-.git
   cd LogiStock-
   ```
2. Ouvrir XAMPP, démarrer MySQL, puis http://localhost/phpmyadmin :
   - créer la base `logistock` (interclassement utf8mb4_general_ci)
   - l'ouvrir, onglet « Importer », importer d'abord `docs/conception/schema.sql`,
     puis `docs/conception/seed.sql`
   (le backend ne crée PAS les tables lui-même : sans cet import, rien ne marche)
3. Terminal 1, le backend :
   ```
   cd backend
   set DB_USER=root
   set DB_PASSWORD=
   mvn spring-boot:run
   ```
   (Linux/Termux : `export DB_USER=root DB_PASSWORD=` à la place de `set`)
   Attendre : `Started LogistockApplication`
4. Terminal 2, le frontend :
   ```
   cd frontend
   npm install
   ng serve
   ```
5. Ouvrir http://localhost:4200

### Arrêter
Ctrl + C dans chaque terminal.

---

## MÉTHODE 2 – Avec Docker (une seule installation)

### À installer une seule fois
- Git
- Docker Desktop

### Lancer
```
git clone https://github.com/kamabayo2007-jpg/LogiStock-.git
cd LogiStock-
docker compose up --build
```
Le premier lancement est long (téléchargements). Quand c'est prêt, ouvrir :
http://localhost:4200

La base est créée toute seule à partir de `docs/conception/schema.sql` et `seed.sql`.

### Arrêter
```
docker compose down          (garde les données)
docker compose down -v       (efface aussi la base : repart de zéro)
```

---

## Problèmes fréquents
| Problème | Solution |
|---|---|
| Port 8080 déjà utilisé | Une ancienne API tourne : la fermer |
| Impossible de joindre le serveur | L'API n'est pas démarrée |
| Table ... doesn't exist | schema.sql pas importé (méthode 1) |
| Erreur MySQL | Démarrer MySQL dans XAMPP, vérifier DB_USER |
| Docker : base non mise à jour | `docker compose down -v` puis relancer |
| `ng` introuvable | `npm install -g @angular/cli` |
