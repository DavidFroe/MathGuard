# MathGuard (Paper/Spigot)

Ein leichtgewichtiges Lern-Plugin: Bevor Kisten/Barrels oder der Crafting Table geöffnet werden, muss eine kleine Matheaufgabe gelöst werden. Perfekt zum spielerischen Üben.

## Features
- Aufgaben: Addition, Subtraktion (mehrere Varianten), Multiplikation (kleines 1×1, Verdopplung).
- Einstellige Ergebnisse: ein Klick.
- Zweistufige Eingabe (Zehner → Einer) mit stabilem GUI-Flow.
- Aufgaben bleiben bestehen, bis sie gelöst sind.
- Drag/Shift-Klick im GUI komplett blockiert.
- Belohnungen (XP, Item, Vault-Geld) & optionale Strafe (Mobspawn) konfigurierbar.
- Bypass-Permission für Erwachsene.

## Installation
1. JAR bauen: `mvn -DskipTests package`
2. `target/mathguard-*.jar` nach `plugins/` kopieren.
3. Server starten, `config.yml` anpassen, `/mathguard` zum Reload.

## Konfiguration (Auszug)
```yaml
enabled-worlds: ["world"]
block-types: ["CHEST","TRAPPED_CHEST","BARREL","CRAFTING_TABLE"]
max-result: 99
modes:
  addition: true
  subtraction_full: true
  subtraction_simple_ones: true
  subtraction_mid_10_20: true
  multiplication_small_table: true
  multiplication_double_only: true
rewards:
  exp: { enabled: true, amount: 3 }
  vault: { enabled: false, amount: 2.0 }
  item: { enabled: false, type: "COOKED_BEEF", amount: 1 }
penalties:
  mob_on_wrong: { enabled: false, type: "SILVERFISH", count: 1, chance: 1.0 }
