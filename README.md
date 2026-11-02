# Lord Obesity – Forge 1.20.1 (modelo 3D propio)

## Sacar el .jar

### Opción A – en tu PC (JDK 17 instalado)
    Windows:  gradlew.bat build
    Mac/Linux: ./gradlew build
El .jar queda en `build/libs/lordobesity-1.20.1-1.0.0.jar`.
Cópialo a la carpeta `mods` de Minecraft con Forge 1.20.1 (47.x).
Probar sin instalar: `gradlew runClient`.

### Opción B – sin instalar nada (GitHub)
1. Crea un repositorio en github.com y sube todo el contenido de esta carpeta.
2. Pestaña **Actions** -> el workflow "Build mod" se ejecuta solo.
3. Cuando termine, descarga el artefacto **lordobesity-jar** (contiene el .jar).

## En el juego
`/summon lordobesity:lord_obesity` o el huevo en la pestaña de huevos generadores.

## Editar el modelo / la textura
- Modelo: `src/main/java/com/lordobesity/LordObesityModel.java`
- Textura (128x128): `src/main/resources/assets/lordobesity/textures/entity/lord_obesity.png`

## Contenido (v2)
- Jefe amarillo con 3 ataques animados: Aplastamiento, Embestida y Eructo (fase 2 al 50% de vida).
- Drop garantizado: Máscara de Lord Obesity.
- Con la máscara puesta (clic derecho / ranura de casco) te transformas en Lord Obesity:
  +20 vida, +4 armadura, +4 daño, resistencia al empuje y algo más lento.
  Al caer desde 3+ bloques aplastas a los enemigos cercanos.

## Música de jefe
Suena en bucle (con fundido) mientras estás a menos de 40 bloques de Lord Obesity.
Para cambiar la canción, sustituye `src/main/resources/assets/lordobesity/sounds/boss_music.ogg`
(debe ser .ogg Vorbis, estéreo).
