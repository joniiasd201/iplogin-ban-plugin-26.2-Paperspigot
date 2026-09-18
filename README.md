# IPLoginBan

Automatisches IP-Login-System und Ban-System für Paper 26.2 (Offline-Modus).

## Features
- Automatische IP-Zuordnung beim ersten Join
- Kein `/login` oder `/register` nötig
- Vollständiges Ban-System mit UUID-Unterstützung
- IP-Bans mit automatischer Verknüpfung
- Temporäre und permanente Bans

## Kompilieren
mvn clean package

Die JAR liegt danach in `target/iplogin-ban-plugin-1.0.0.jar`.

## Befehle
- /ban <spieler> <dauer> <grund>
- /unban <spieler>
- /banip <ip> <dauer> <grund>
- /unbanip <ip>
- /baninfo <spieler>
- /banlist
- /iplogin <spieler>

## Permissions
iplogin.admin, iplogin.ban, iplogin.unban, iplogin.baninfo, iplogin.banlist, iplogin.ipban
