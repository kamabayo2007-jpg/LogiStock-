@echo off
echo Arret de LogiStock...
taskkill /f /fi "WINDOWTITLE eq LogiStock API*" >nul 2>&1
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im httpd.exe >nul 2>&1
C:\xampp\mysql\bin\mysqladmin.exe -u root shutdown >nul 2>&1
echo Tout est arrete.
pause
