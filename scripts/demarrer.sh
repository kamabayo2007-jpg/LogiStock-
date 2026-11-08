#!/data/data/com.termux/files/usr/bin/bash
# Démarre MariaDB, l'API Spring Boot (port 8080) et le front Angular (port 4200).
cd "$(dirname "$0")/.." || exit 1
RACINE=$(pwd)
export DB_USER=logistock DB_PASSWORD=logistock123
mkdir -p "$RACINE/.logs"

attendre() {  # attendre <url> <secondes> <journal>
  for i in $(seq 1 "$2"); do
    curl -s "$1" >/dev/null 2>&1 && return 0
    sleep 1
  done
  echo "ERREUR : pas de réponse de $1. Dernières lignes du journal :"
  tail -15 "$3"
  exit 1
}

# 1. Base de données
if ! mariadb -u logistock -plogistock123 -h 127.0.0.1 -e "SELECT 1" >/dev/null 2>&1; then
  echo "Démarrage de MariaDB..."
  (mysqld_safe > "$RACINE/.logs/mariadb.log" 2>&1 &)
  for i in $(seq 1 30); do
    mariadb -u logistock -plogistock123 -h 127.0.0.1 -e "SELECT 1" >/dev/null 2>&1 && break
    sleep 1
  done
fi
echo "Base de données : OK"

# 2. API
if ! curl -s localhost:8080/api/health >/dev/null 2>&1; then
  echo "Démarrage de l'API (1 à 2 minutes)..."
  (cd backend && nohup mvn -q spring-boot:run > "$RACINE/.logs/api.log" 2>&1 &)
  attendre localhost:8080/api/health 240 "$RACINE/.logs/api.log"
fi
echo "API : OK"

# 3. Front
if ! curl -s localhost:4200 >/dev/null 2>&1; then
  echo "Démarrage du front..."
  (cd frontend && NG_CLI_ANALYTICS=false nohup ng serve > "$RACINE/.logs/front.log" 2>&1 &)
  attendre localhost:4200 240 "$RACINE/.logs/front.log"
fi
echo "Front : OK  ->  ouvrez http://localhost:4200"
