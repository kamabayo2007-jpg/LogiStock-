#!/data/data/com.termux/files/usr/bin/bash
pkill -f spring-boot
pkill -f "ng serve"
echo "API et front arrêtés (MariaDB reste lancée)."
