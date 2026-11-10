@echo off
chcp 65001 >nul
title LogiStock SA - lancement
REM ============================================================
REM  Un seul clic : MySQL + Apache (XAMPP) + API + navigateur.
REM  Dossier attendu : C:\logistock\  avec dedans
REM    logistock-api.jar, schema.sql, seed.sql  (et jdk17\ si Java 17 n'est pas installe)
REM  Site web attendu dans : C:\xampp\htdocs\logistock\
REM ============================================================
set XAMPP=C:\xampp
set DOSSIER=C:\logistock
set DB_USER=root
set DB_PASSWORD=

if not exist "%XAMPP%\mysql\bin\mysqld.exe" (echo XAMPP introuvable dans %XAMPP% & pause & exit /b 1)
if not exist "%DOSSIER%\logistock-api.jar" (echo logistock-api.jar introuvable dans %DOSSIER% & pause & exit /b 1)

set JAVA=java
if exist "%DOSSIER%\jdk17\bin\java.exe" set JAVA=%DOSSIER%\jdk17\bin\java.exe

echo [1/5] Demarrage de MySQL...
start "" /min "%XAMPP%\mysql\bin\mysqld.exe" --defaults-file="%XAMPP%\mysql\bin\my.ini" --standalone
for /l %%i in (1,1,30) do (
  "%XAMPP%\mysql\bin\mysqladmin.exe" -u root ping >nul 2>&1 && goto mysql_ok
  timeout /t 1 >nul
)
echo MySQL ne repond pas. Verifiez qu'il n'est pas deja lance autrement.
pause
exit /b 1
:mysql_ok

echo [2/5] Preparation de la base...
"%XAMPP%\mysql\bin\mysql.exe" -u root -e "CREATE DATABASE IF NOT EXISTS logistock CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"
set NB=0
for /f %%n in ('"%XAMPP%\mysql\bin\mysql.exe" -u root -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='logistock'"') do set NB=%%n
if "%NB%"=="0" (
  echo      Base vide : import du schema et des donnees de demonstration...
  "%XAMPP%\mysql\bin\mysql.exe" -u root logistock < "%DOSSIER%\schema.sql"
  "%XAMPP%\mysql\bin\mysql.exe" -u root logistock < "%DOSSIER%\seed.sql"
)

echo [3/5] Demarrage d'Apache...
start "" /min "%XAMPP%\apache\bin\httpd.exe"

echo [4/5] Demarrage de l'API (patientez, environ 20 secondes)...
start "LogiStock API" /min cmd /c "cd /d %DOSSIER% && %JAVA% -jar logistock-api.jar"
for /l %%i in (1,1,90) do (
  curl -s -o nul -f http://localhost:8080/api/health && goto api_ok
  timeout /t 1 >nul
)
echo L'API ne repond pas. Verifiez Java 17 : java -version
pause
exit /b 1
:api_ok

echo [5/5] Ouverture de LogiStock...
start http://localhost/logistock/
echo.
echo Pret ! Comptes : admin@logistock.ml / Admin123!   stock@logistock.ml / Stock123!   vente@logistock.ml / Vente123!
timeout /t 8 >nul
