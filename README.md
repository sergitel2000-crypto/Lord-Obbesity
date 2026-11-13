# Lord Obesity – Forge 1.20.1

## Compilar
`gradlew.bat build` (Windows) o `./gradlew build` -> `build/libs/*.jar` (JDK 17),
o sube la carpeta a GitHub y usa el workflow "Build mod" (Actions -> Artifacts).

## Cómo se juega
1. **Piezas**: Sombrero, Chaqueta y Bastón de Lord Obesity (15 % cada una en cofres de estructuras).
2. **Ritual** (al terminar de emerger, Lord Obesity baila el griddy unos 4 s antes de hablar): caldero **lleno de agua** y **tira** (tecla de soltar) las tres piezas dentro. El agua se va
   tiñendo de amarillo; con la tercera empieza una invocación de unos 12 segundos. Hazlo al aire libre.
3. **Tendero**: se queda. Cada día pide UNA cosa distinta; si se la das, recompensa (mejora con la racha).
   Si a la mañana siguiente no se la has dado (o le pegas) se enfada.
4. **Enfadado**: Aplastamiento, Embestida y Eructo. Al derrotarlo se hunde bajo tierra.
5. **5-10 minutos** de acecho, con un susto cada 15-50 s y mucha variedad:
   - te observa de lejos y, si lo miras, aparece pegado a tu cara (screamer);
   - aparece a tu espalda (screamer al girarte);
   - **acechador**: se queda inmóvil si lo miras y avanza cuando apartas la vista; si te alcanza, screamer;
   - **ojos brillantes** en la oscuridad que parpadean;
   - **pasos** pesados que se acercan por detrás;
   - **apagón** con caja de música desafinada;
   - solo sonidos (susurros, crujidos, golpes, respiración...);
   - **grietas en el suelo**: si la pisas caes a una de **tres pesadillas de 1 minuto**.
6. **El Devorador** (1000 de vida): Salto, Rayo de eructo, Succión e invoca mini Lord Obesity.
   Suelta solo la **Máscara** y el **Bastón**.

### Equipo
- **Sombrero** (casco) y **Chaqueta** (peto): armadura, dureza, vida extra (+2 / +4) y suerte (chaqueta).
- **Máscara**: te transforma y te hace 1,5x más grande (+20 vida, +4 armadura, +4 daño, +alcance).
- **Bastón**: golpe fuerte, clic derecho = eructo letal, agachado + clic derecho = gancho (sonido suave).

## Pruebas rápidas (creativo)
- `/summon lordobesity:lord_obesity ~ ~ ~ {Stage:1}` enfadado, `{Stage:3}` Devorador, `{Stage:2}` susto.
- Objetos en la pestaña de Combate.

## Texturas
Tus texturas en `assets/lordobesity/textures/` (entity + item) NO se tocan al compilar.
Nuevas: `models/armor/lord_hat.png`, `models/armor/lord_jacket.png`, `gui/screamer.png`.

## Pesadillas (se elige una al azar al caer por la grieta; todas duran 1 minuto)
0. **Mundo abstracto** (`lordobesity:nightmare`): sombrero gigante, boca de dientes, pilares con ojos, torre de tartas, escalera sin fin, puertas, pared-barriga, sonrisa en el cielo.
1. **Casa encantada** (`lordobesity:nightmare_house`): noche con niebla, casa oscura con retrato sonriente, velas, sangre y telarañas, jardín con árboles muertos, lápidas y espantapájaros. Sustos dentro.
2. **Persecución** (`lordobesity:nightmare_chase`): laberinto nuevo cada vez. A los 6 s despierta Lord Obesity y te persigue (cada vez más rápido).
   Si te alcanza, el sueño acaba (con screamer); si llegas a la columna de luz de la salida, también. Tampoco dura más de 1 minuto.
Se construyen con datos en `data/lordobesity/{dimension,dimension_type,worldgen/biome}/` y código en `NightmareBuilder.java`.

## Comandos de prueba (operadores)
- `/lordobesity crack` crea una grieta cerca de ti (pisala para caer).
- `/lordobesity nightmare [0|1|2]` entra directo en una pesadilla (0 abstracta, 1 casa, 2 persecución).
- `/lordobesity scare` lanza un susto al azar.
- `/lordobesity haunt` empieza el acecho: sustos ya y el Devorador a los 60 s.

## Modelo 3D
Articulado: cintura, cuello, hombros, codos, muñecas, rodillas y tobillos, barriga con rebote propio.
Usa las mismas zonas de textura que antes (partidas por la mitad), así que tus texturas encajan sin cambios.

## Mini Lord Obesity
Deformes (cabeza enorme y ladeada, brazos desiguales, encorvados) y con sonidos muy suaves al herirse o morir.

## Lore (libros)
Cada pesadilla esconde un libro que brilla (se ve desde lejos):
- Mundo abstracto: **Libro I, El Banquete Sin Fin** (sobre un pedestal, en el centro de la lengua roja).
- Casa encantada: **Libro II, La Casa de los Invitados** (sobre el atril del estudio).
- Persecución: **Libro III, La Última Carrera** (en la celda de salida, junto a ti).
Clic derecho para leerlos. Los tres juntos en la mesa de crafteo (sin forma) dan **La Crónica Completa**, que añade un epílogo.
Los libros reaparecen si entras de nuevo en esa pesadilla y no están.

### Cambiar el texto del lore
Está en `src/main/resources/assets/lordobesity/lore/` (`book1.txt`, `book2.txt`, `book3.txt`, `epilogue.txt`).
Las páginas se separan con una línea que solo ponga `---` (unos 230 caracteres por página caben bien). Se puede editar sin tocar código.
