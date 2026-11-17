# Pocket Pals

Pokébälle zum Fangen und Mitnehmen von Tieren (Pokéball, Superball, Hyperball) und ein Gürtel mit 6 Plätzen für Bälle mit Tier. Dazu ein Köcher mit 6 Plätzen für Pfeile.

Teil der Mod-Reihe *Quality of Mine*. Jedes Feature des früheren Gesamt-Mods ist jetzt eine eigene Mod, die sich einzeln
installieren lässt. Alle Mods der Reihe laufen auf Einzelspielerwelten und auf Mehrspieler-Servern und funktionieren
unabhängig voneinander.

| Mod | Projekt | Inhalt |
|---|---|---|
| Extra Enchants | `extra-enchants` | Fünf neue Verzauberungen |
| Tidy Inventory | `tidy-inventory` | Items nicht aufheben und Mülleimer im Inventar |
| Wide Hotbar | `wide-hotbar` | Item-Leiste mit 11 Plätzen |
| Sleep Lapse | `sleep-lapse` | Zeitraffer beim Schlafen |
| Breeding Break | `breeding-break` | Ruhepause nach der Paarung |
| Pocket Pals (diese Mod) | `pocket-pals` | Bälle zum Fangen von Tieren, Gürtel dafür und Köcher für Pfeile |

## Installation

Voraussetzungen:

- Windows (das Installationsskript läuft nur dort, unter macOS und Linux musst du Fabric und die Mod von Hand installieren)
- Minecraft Java Edition mit dem offiziellen Launcher
- Java 25 (eine JRE reicht für die Installation)

Das Repository ist privat. Die Installationsdateien `installieren.cmd` und `pocket-pals-2.0.0.jar` werden
direkt weitergegeben, zum Beispiel als ZIP-Datei.

1. **Sichere deine Welten.** Kopiere dazu den Ordner `%APPDATA%\.minecraft\saves` an einen anderen Ort.
2. Schließe Minecraft und den Launcher.
3. Entpacke die ZIP-Datei in einen beliebigen Ordner. `installieren.cmd` und die JAR müssen im selben Ordner liegen.
4. Doppelklicke `installieren.cmd`. Das Skript lädt Fabric selbst herunter. Windows kann dabei vor dem
   unbekannten Skript warnen, dann auf *Weitere Informationen > Trotzdem ausführen* klicken.
5. Starte den Launcher, wähle das Profil **fabric-loader-26.3** und klicke auf **Spielen**.

Wer das Repository geklont hat, baut die JAR mit `gradlew build`. Das Skript findet sie dann in `build\libs`.

Das Skript installiert Fabric und kopiert die Mod nach `%APPDATA%\.minecraft\mods`. Die Fabric API steckt
bereits in der Mod und muss nicht extra installiert werden. Dein normales Vanilla-Profil bleibt erhalten.
Mehrere Mods der Reihe kannst du nebeneinander installieren, führe dazu das Skript jeder Mod einmal aus.

### Umstieg vom alten Gesamt-Mod

Den früheren Gesamt-Mod *Quality of Mine* (`quality-of-mine-1.3.0.jar`) und diese Mod darfst du **nicht gleichzeitig** installieren.
Beide würden dieselben Inhalte anmelden. Die Mod meldet das beim Start mit einer verständlichen Fehlermeldung, statt abzustürzen.

- **Auf dem Rechner:** `installieren.cmd` löscht die alte JAR selbst aus dem Ordner `mods`.
- **Auf dem Server:** Lösche `quality-of-mine-*.jar` im Ordner `mods` von Hand, bevor du die neue JAR hineinlegst.
- **Deine Welten:** Alle Kennungen bleiben unverändert (`qualityofmine:...`). Bestehende Welten behalten deshalb ihre Daten,
  solange die Mod mit dem jeweiligen Inhalt installiert ist. Fehlt eine Mod der Reihe, fehlt ihr Inhalt in der Welt, siehe unten.
- **Danach:** Jede Mod lässt sich einzeln aktualisieren, ohne die anderen anzufassen. Auf einem Server müssen Spieler und Server
  pro Mod dieselbe Version haben.

### Update und Deinstallation

- **Update:** Neue `installieren.cmd` und neue JAR in einen Ordner legen und das Skript erneut ausführen. Die alte Version dieser Mod wird ersetzt.
  Wer selbst baut, führt vorher `gradlew build` aus.
- **Deinstallation:** `%APPDATA%\.minecraft\mods\pocket-pals-2.0.0.jar` löschen.

Ohne die Mod verschwindet aus einer Welt alles, was zu ihr gehört: alle Bälle samt gefangener Tiere, alle Gürtel und alle Köcher samt Pfeilen darin.

## Server

Die Mod Pocket Pals läuft auf einem Fabric-Server für Minecraft 26.3. **Sie muss auf dem Server und bei allen Spielern installiert
sein, und zwar in derselben Version.** Ein Spieler ohne sie würde sich mit dem
Server nicht verstehen. Der Server prüft deshalb beim Beitritt die Version und lehnt andere Spieler mit einer
Meldung ab, die beide Versionen nennt.

1. **Fabric-Server einrichten.** Voraussetzung ist Java 25. Lade den Fabric Installer von <https://fabricmc.net/use/server/>
   und installiere in den Serverordner (ersetze den Dateinamen durch den des Installers):

   ```cmd
   java -jar fabric-installer-1.1.2.jar server -mcversion 26.3 -loader 0.20.1 -downloadMinecraft
   ```

2. Starte den Server einmal mit `java -jar fabric-server-launch.jar nogui` und stimme der Mojang-EULA in `eula.txt` zu
   (`eula=true`). Das ist deine Entscheidung als Betreiber.
3. Kopiere `pocket-pals-2.0.0.jar` in den Ordner `mods` des Servers. Die Fabric API steckt bereits im Mod.
4. Starte den Server neu. Deine Mitspieler installieren den Mod wie oben beschrieben mit `installieren.cmd`.

Das Update läuft genauso: alte JAR im Ordner `mods` des Servers durch die neue ersetzen und alle Mitspieler
aktualisieren lassen.

Das ändert sich auf einem Server:

- **Gürtel, Köcher und gefangene Tiere** gehören jedem Spieler einzeln und stehen in dessen Spielerdaten beziehungsweise im Ball.
  Sie bleiben nach einem Neustart des Servers erhalten.
- **Server ohne die Mod:** Wer mit installierter Mod auf einen fremden Server ohne sie geht, sollte sie dort nicht nutzen. Dafür nimmst du das normale Vanilla-Profil des Launchers.

## Features

### Pokéball, Superball und Hyperball

Bälle zum Fangen und Mitnehmen von Tieren, wie im Anime. Rechtsklick wirft sie wie ein Ei.

| Ball | Rezept | Grundchance |
|---|---|---|
| Pokéball | oben 3 Redstone, in der Mitte links und rechts je 1 Kohle (oder Holzkohle) und dazwischen 1 Diamant, unten 3 Netherquarz | 35 % |
| Superball | Pokéball in der Mitte, ringsherum 8 Lapislazuli | 50 % |
| Hyperball | Superball in der Mitte, ringsherum 8 Goldbarren | 70 % |

- **Fangen:** Trifft der leere Ball ein Tier, verschwindet es im Ball, der Ball fällt zu Boden und wackelt dreimal.
  Gelingt der Fang, klickt es, der Knopf leuchtet blau und der volle Ball liegt zum Aufheben da. Sonst bricht das Tier aus
  und der Ball ist verbraucht.
- **Ausbruch:** Wer ausbricht, gilt als getroffen: Das Tier flüchtet, und wehrhafte Tiere wie Wölfe greifen den Werfer an.
- **Chance:** Zur Grundchance kommen bis zu 50 Prozentpunkte, je weniger Leben das Tier hat, und 10 bei Jungtieren
  (höchstens 95 %). Wer das Tier vorher verletzt, fängt es also leichter.
- **Wieder einfangen:** Ein Tier, das schon einmal gefangen war, lässt sich ohne Risiko erneut fangen. Dazu klickst du
  mit dem leeren Ball in der Hand mit der rechten Maustaste auf das Tier, auch bei Haustieren, die sonst sitzen oder
  aufsteigen würden. Der Ball erscheint, das Tier verschwindet darin, und es klickt nach einem Wackeln. Ein Wurf auf
  das Tier wirkt genauso.
- **Welche Tiere:** Alle Tiere wie Kuh, Schaf, Pferd, Wolf oder Katze, nicht aber Monster, Dorfbewohner oder Fische. Haustiere
  eines anderen Spielers sind tabu.
- **Zähmung:** Ein wildes Tier, das man zähmen kann (Wolf, Katze, Papagei, Pferd, Esel und so weiter), ist nach einem gelungenen Fang
  gezähmt und gehört dem Fänger. Beim Ausbrechen bleibt es wild.
- **Gefangenes Tier:** Der Name zeigt es in Klammern, zum Beispiel *Pokéball (Schaf)*. Das Tier bleibt mit allem erhalten:
  Leben, Alter, Name, Sattel, Zähmung. Volle Bälle lassen sich nicht stapeln und nicht aufwerten.
- **Freilassen:** Rechtsklick mit dem vollen Ball. Wo er auftrifft, öffnet er sich und das Tier erscheint. Der leere Ball
  bleibt liegen und lässt sich wieder verwenden.
- Ein Ball, der nichts Fangbares trifft, geht nicht verloren, sondern liegt am Boden.

### Gürtel

Ein Gürtel mit 6 Plätzen für volle Bälle.

**Rezept:** in einer Reihe Leder, Eisenbarren, Leder.

- **Anlegen:** Der Gürtel hat einen eigenen Platz im Inventar, rechts neben der Rüstung über dem Platz für die zweite Hand.
  Lege ihn dort ab oder klicke mit dem Gürtel in der Hand die rechte Maustaste, dann wird er angelegt.
- **Die 6 Plätze** erscheinen unter dem Inventar, sobald ein Gürtel getragen wird. Es passen nur Bälle mit gefangenem Tier hinein,
  keine leeren Bälle und keine anderen Gegenstände.
- **Im Spiel:** Ein Gürtel mit Inhalt zeigt seine 6 Plätze als Leiste über den Statusanzeigen, beim Wechseln des Platzes erscheint kurz der Name des gewählten Tiers darüber.
  Mit gedrückter **Alt-Taste** wählt das Mausrad einen Platz, **Alt + Rechtsklick** oder die Taste **G** (änderbar in den Steuerungs-Einstellungen) wirft den gewählten Ball.
- **Aufheben:** Ein voller Ball, den du aufhebst, landet zuerst im ersten freien Gürtelplatz. Erst bei vollem Gürtel kommt er ins normale Inventar.
- Der Inhalt steckt im Gürtel: Legst du ihn ab, nimmt er die Bälle mit.
- **Tod:** Der Gürtel fällt wie die Rüstung mit seinem Inhalt zu Boden. Mit der Spielregel `keep_inventory` bleibt er angelegt.
- Im Kreativ-Modus ist der Gürtel nicht erreichbar, weil das Kreativ-Inventar keinen Platz dafür hat.

### Köcher

Ein Köcher mit 6 Plätzen für Pfeile. Er wird auf demselben Platz getragen wie der Gürtel, also immer nur einer von beiden.

**Rezept:** untereinander Faden, Kaninchenfell, Leder.

- **Anlegen, Ablegen, Tod und Kreativ-Modus** funktionieren wie beim Gürtel. Der Köcher nimmt seine Pfeile beim Ablegen mit.
- **Die 6 Plätze** nehmen nur Pfeile auf (normale, Spektral- und Trankpfeile), jeder Platz einen vollen Stapel von 64.
- **Aufheben:** Pfeile vom Boden, aus abgeschossenen Pfeilen und per Umschalt-Klick aus Truhen, Öfen und anderen Menüs landen zuerst
  im Köcher, auf gleichen Stapeln oder in freien Plätzen. Erst was dort nicht mehr passt, kommt ins normale Inventar.
- **Im Spiel:** Die 6 Plätze stehen mit der Anzahl als Leiste über den Statusanzeigen. Mit gedrückter **Alt-Taste** wählt das Mausrad
  einen Platz, dabei erscheint kurz der Name des Pfeils.
- **Schießen:** Bogen und Armbrust verschießen den gewählten Pfeil aus dem Köcher. Ist der gewählte Platz leer, nehmen sie den ersten
  Pfeil im Köcher, ohne Pfeile im Köcher wie gewohnt aus Hand und Inventar. Werfen per Taste oder Alt + Rechtsklick gibt es beim Köcher nicht.
- **Unendlichkeit** verbraucht mit dieser Mod keinen Pfeil mehr, egal welcher Typ, auch nicht aus dem Inventar. Wie im Originalspiel
  lassen sich die so verschossenen Pfeile nicht aufheben.

## Entwicklung

Der Build läuft über den mitgelieferten Gradle Wrapper. Das Fabric-Build-Plugin Loom gibt es nur für Gradle,
eine eigene Gradle-Installation ist nicht nötig. Benötigt wird JDK 25.

```cmd
gradlew build
gradlew test
gradlew runClient
gradlew runClientGameTest
gradlew productionClientTest
```

| Befehl | Zweck |
|---|---|
| `build` | Baut `build\libs\pocket-pals-2.0.0.jar` und führt die Unit-Tests aus |
| `test` | Unit-Tests der Logik |
| `runClient` | Startet das Spiel mit dem Mod in einem eigenen Testordner |
| `runClientGameTest` | Spielt Pocket Pals automatisch im echten Spiel durch, anschließend gegen einen echten dedizierten Server (Beitritt mit Versionsabfrage) |
| `runServer` | Startet einen dedizierten Server mit dem Mod in `run`, zum Beispiel für manuelle Tests mit `runClient` als zweitem Spieler |
| `productionClientTest` | Derselbe Test mit dem fertigen JAR, wie bei einer echten Installation |

Für IDE-Projektdateien gibt es `gradlew eclipse` und `gradlew genSources` (dekompilierter Spielcode).

`productionClientTest` stürzte auf dem Entwicklungsrechner beim Start gelegentlich nativ ab (`0xC0000005`), auch ganz ohne Mod. Die Ursache ist nicht geklärt. In dem Fall den Test mit `--rerun` wiederholen.

### Neues Feature hinzufügen

1. Neues Paket unter `src/main/java/de/qualityofmine/pocketpals/feature/` anlegen.
2. Eine Klasse mit `Feature` anlegen und in `PocketPals` eintragen. Client-Anteile kommen nach `src/client/java` und in `PocketPalsClient`.
3. Eingriffe ins Spiel als möglichst dünne Mixins unter `mixin` anlegen und in `pocketpals.mixins.json` bzw. `pocketpals.client.mixins.json` eintragen. Die Logik gehört in die Feature-Klassen.
4. Texte in `assets/qualityofmine/lang/de_de.json` und `en_us.json` ergänzen.

### Neue Minecraft-Version

Die Versionen stehen in `gradle.properties` und `installieren.cmd`, aktuelle Werte unter <https://fabricmc.net/develop>.
Danach zeigt `gradlew productionClientTest`, ob noch alles funktioniert.

### Neue Version weitergeben

1. Version in `gradle.properties` und `MOD_VERSION` in `installieren.cmd` anpassen. Auch die Dateinamen in dieser README stehen mit Version.
2. `paket-erstellen.cmd` ausführen. Das Skript baut den Mod komplett neu (`gradlew clean build`, inklusive Unit-Tests),
   prüft, ob die Versionen in `gradle.properties` und `installieren.cmd` zusammenpassen, und packt die JAR
   und `installieren.cmd` in `dist\pocket-pals-<Version>.zip`.
3. Diese ZIP-Datei weitergeben.

## Lizenz

[MIT](LICENSE)
