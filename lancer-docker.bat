@echo off
chcp 65001 >nul
title LogiStock SA - lancement Docker
cd /d "%~dp0"
echo === LogiStock SA : demarrage avec Docker ===
docker info >nul 2>&1
if errorlevel 1 (
  echo.
  echo Docker n'est pas demarre. Ouvrez Docker Desktop, attendez qu'il soit pret, puis relancez ce fichier.
  pause
  exit /b 1
)
docker compose up --build -d
if errorlevel 1 (
  echo.
  echo Echec du lancement. Regardez les messages ci-dessus.
  pause
  exit /b 1
)
echo.
echo Attente de l'API (jusqu'a 2 minutes)...
for /l %%i in (1,1,60) do (
  curl -s -o nul -f http://localhost:8080/api/health && goto pret
  timeout /t 2 >nul
)
echo L'API ne repond pas. Lancez : docker compose logs backend
pause
exit /b 1
:pret
echo.
echo Pret ! Ouverture de http://localhost:4200
start http://localhost:4200
echo Comptes : admin@logistock.ml / Admin123!   stock@logistock.ml / Stock123!   vente@logistock.ml / Vente123!
timeout /t 8 >nul
