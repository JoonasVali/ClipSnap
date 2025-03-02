@echo off
set "APP_DIR=%~dp0"
"%APP_DIR%jre/bin/java.exe" -Dlogback.configurationFile="%APP_DIR%logback.xml" -Xmx2048M -jar "%APP_DIR%lib/bookreader-core.jar"