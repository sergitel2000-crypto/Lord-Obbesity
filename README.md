# Lord Obesity – Forge 1.20.1

## Compilar
`gradlew.bat build` (Windows) o `./gradlew build` -> `build/libs/*.jar`. Necesita JDK 17.
O sube la carpeta a GitHub y usa el workflow "Build mod" (Actions -> Artifacts).

## Cómo se juega
1. **Encuentra** el Sombrero, la Chaqueta y el Bastón (15% en cofres de estructuras: mazmorras,
   minas, templos, fortalezas, mansiones, tesoros enterrados, ciudades antiguas...).
2. **Échalos** (clic derecho con cada pieza) en un **caldero**. Con las 3 empieza el ritual.
   Hazlo al aire libre: Lord Obesity sale de ahí.
3. **Tendero**: te pide 2-3 objetos al azar (clic derecho con ellos para entregarlos).
   Si los das todos antes de que amanezca -> recompensa valiosa y se va. Si no, o si le pegas -> se enfada.
4. **Forma enfadada** (lo de siempre): Aplastamiento, Embestida y Eructo. Al "derrotarlo" se hunde bajo tierra.
5. Entre **5 y 15 minutos** después te da sustos a lo lejos (inofensivos) y al final vuelve como
   **el Devorador**: más grande, 1000 de vida y ataques nuevos (Salto, Rayo de eructo, Succión, Invocación).
6. El Devorador suelta la **Máscara** y el **Bastón**.

### Máscara
Te transforma en Lord Obesity y te hace más grande (+20 vida, +4 armadura, +4 daño, +alcance, más lento).
Al caer desde 3+ bloques aplastas a los enemigos cercanos.

### Bastón
- Golpe: mucho daño.
- Clic derecho: eructo letal en cono.
- Agachado + clic derecho: gancho hacia el bloque que miras.

## Pruebas rápidas (creativo)
- Huevo generador = tendero. Forzar fases con comandos:
  `/summon lordobesity:lord_obesity ~ ~ ~ {Stage:1}`  (enfadado)
  `/summon lordobesity:lord_obesity ~ ~ ~ {Stage:3}`  (Devorador)
- Objetos en la pestaña de Combate del inventario creativo.

## Música y sonidos
`assets/lordobesity/sounds/` (.ogg). Los sonidos goofy están sintetizados; sustitúyelos por los tuyos
manteniendo los nombres (ambient1-3, hurt1-2, death, roar, burp, slam, boing, honk, scare, whistle).
Los sonidos con posición deben ser MONO; la música (boss_music) puede ser estéreo.
