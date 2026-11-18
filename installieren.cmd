@echo off
setlocal
chcp 65001 >nul

rem Installiert Fabric und den Mod in das normale Minecraft-Verzeichnis.
rem Die Versionen stehen hier zentral und müssen zu gradle.properties passen.
set "MC_VERSION=26.3"
set "LOADER_VERSION=0.20.1"
set "INSTALLER_VERSION=1.1.2"
set "MOD_VERSION=3.1.1"
set "MOD_JAR=pocket-pals-%MOD_VERSION%.jar"

set "PROJECT_DIR=%~dp0"
set "MC_DIR=%APPDATA%\.minecraft"
set "INSTALLER=%TEMP%\fabric-installer-%INSTALLER_VERSION%.jar"

echo.
echo Pocket Pals - Installation
echo ==============================
echo Schliesse bitte vorher Minecraft und den Launcher.
echo.
pause

if not exist "%MC_DIR%\launcher_profiles.json" (
	echo FEHLER: Unter %MC_DIR% wurde kein Minecraft Launcher gefunden.
	goto :failed
)

where java >nul 2>nul
if errorlevel 1 (
	echo FEHLER: Java wurde nicht gefunden. Benoetigt wird Java 25.
	goto :failed
)

rem Mod-JAR suchen: erst neben dem Skript, dann im Build-Ordner
set "MOD_PATH="
if exist "%PROJECT_DIR%%MOD_JAR%" set "MOD_PATH=%PROJECT_DIR%%MOD_JAR%"
if not defined MOD_PATH if exist "%PROJECT_DIR%build\libs\%MOD_JAR%" set "MOD_PATH=%PROJECT_DIR%build\libs\%MOD_JAR%"
if not defined MOD_PATH (
	echo FEHLER: %MOD_JAR% wurde nicht gefunden.
	echo Lege die Datei in denselben Ordner wie dieses Skript. Oder baue sie mit "gradlew build".
	goto :failed
)

echo Lade den Fabric Installer ...
curl -fsSL -o "%INSTALLER%" "https://maven.fabricmc.net/net/fabricmc/fabric-installer/%INSTALLER_VERSION%/fabric-installer-%INSTALLER_VERSION%.jar"
if errorlevel 1 (
	echo FEHLER: Der Fabric Installer konnte nicht geladen werden.
	goto :failed
)

echo Installiere Fabric fuer Minecraft %MC_VERSION% ...
java -jar "%INSTALLER%" client -mcversion %MC_VERSION% -loader %LOADER_VERSION% -dir "%MC_DIR%"
if errorlevel 1 (
	echo FEHLER: Fabric konnte nicht installiert werden.
	goto :failed
)

if not exist "%MC_DIR%\mods" mkdir "%MC_DIR%\mods"
rem Entfernt den frueheren Gesamt-Mod (er laeuft nicht zusammen mit dieser Mod) und aeltere Versionen dieser Mod
del /q "%MC_DIR%\mods\quality-of-mine-*.jar" 2>nul
del /q "%MC_DIR%\mods\pocket-pals-*.jar" 2>nul
copy /y "%MOD_PATH%" "%MC_DIR%\mods\" >nul
if errorlevel 1 (
	echo FEHLER: Der Mod konnte nicht nach %MC_DIR%\mods kopiert werden.
	goto :failed
)

echo.
echo Fertig! Starte jetzt den Launcher, waehle das Profil "fabric-loader-%MC_VERSION%" und klicke auf Spielen.
echo.
pause
exit /b 0

:failed
echo.
pause
exit /b 1
