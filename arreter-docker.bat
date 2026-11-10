@echo off
cd /d "%~dp0"
docker compose down
echo LogiStock est arrete (les donnees sont conservees).
pause
