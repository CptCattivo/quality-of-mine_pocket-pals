@echo off
setlocal
chcp 65001 >nul

rem Baut die Mod komplett neu und packt JAR und installieren.cmd in ein ZIP zur Weitergabe.
rem Das Archiv liegt danach unter dist\pocket-pals-<Version>.zip.
rem Mit dem Parameter "nopause" entfällt die Abfrage am Ende, z. B. für Aufrufe aus anderen Skripten.

set "PROJECT_DIR=%~dp0"
set "DIST_DIR=%PROJECT_DIR%dist"
cd /d "%PROJECT_DIR%"

echo.
echo Pocket Pals - Paket erstellen
echo =================================

rem Version aus gradle.properties lesen
set "VERSION="
for /f "tokens=2 delims==" %%v in ('findstr /b /c:"version=" gradle.properties') do set "VERSION=%%v"
if not defined VERSION (
	echo FEHLER: In gradle.properties wurde keine Version gefunden.
	goto :failed
)

rem Sicherstellen, dass installieren.cmd dieselbe Version erwartet
set "SCRIPT_VERSION="
for /f "tokens=2 delims==" %%v in ('findstr /r /c:"^set .MOD_VERSION=" installieren.cmd') do set "SCRIPT_VERSION=%%v"
if defined SCRIPT_VERSION set "SCRIPT_VERSION=%SCRIPT_VERSION:"=%"
if not "%SCRIPT_VERSION%"=="%VERSION%" (
	echo FEHLER: Die Versionen passen nicht zusammen.
	echo   gradle.properties:  %VERSION%
	echo   installieren.cmd:   %SCRIPT_VERSION%
	echo Passe MOD_VERSION in installieren.cmd an.
	goto :failed
)

set "MOD_JAR=pocket-pals-%VERSION%.jar"
set "ARCHIVE=%DIST_DIR%\pocket-pals-%VERSION%.zip"

echo Version: %VERSION%
echo.
echo Baue die Mod neu und fuehre die Tests aus ...
call "%PROJECT_DIR%gradlew.bat" clean build
if errorlevel 1 (
	echo FEHLER: Der Build ist fehlgeschlagen.
	goto :failed
)

if not exist "build\libs\%MOD_JAR%" (
	echo FEHLER: build\libs\%MOD_JAR% wurde nicht erzeugt.
	goto :failed
)

echo.
echo Erstelle das Archiv ...
if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
if exist "%ARCHIVE%" del /q "%ARCHIVE%"
"%SystemRoot%\System32\tar.exe" -a -c -f "%ARCHIVE%" -C "%PROJECT_DIR%build\libs" "%MOD_JAR%" -C "%PROJECT_DIR%." installieren.cmd
if errorlevel 1 (
	echo FEHLER: Das Archiv konnte nicht erstellt werden.
	goto :failed
)

echo.
echo Fertig! Das Paket liegt unter:
echo %ARCHIVE%
echo.
if /i not "%~1"=="nopause" pause
exit /b 0

:failed
echo.
if /i not "%~1"=="nopause" pause
exit /b 1
