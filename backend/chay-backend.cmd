@echo off
rem Chay backend khong can cai Maven (lan dau tu tai Maven ve backend\.maven)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0chay-backend.ps1" %*
