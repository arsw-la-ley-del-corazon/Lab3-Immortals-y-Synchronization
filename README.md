
# ARSW — (Java 21): **Immortals & Synchronization** — con UI Swing

**Escuela Colombiana de Ingeniería – Arquitecturas de Software**  
Laboratorio de concurrencia: condiciones de carrera, sincronización, suspensión cooperativa y *deadlocks*, con interfaz **Swing** tipo *Highlander Simulator*.


---

## Requisitos

- **JDK 21** (Temurin recomendado)
- **Maven 3.9+**
- SO: Windows, macOS o Linux

---

## Cómo ejecutar

### Interfaz gráfica (Swing) — *Highlander Simulator*

**Opción A (desde `Main`, modo `ui`)**
```bash
mvn -q -DskipTests exec:java -Dmode=ui -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10
```

**Opción B (clase de la UI directamente)**
```bash
mvn -q -DskipTests exec:java   -Dexec.mainClass=edu.eci.arsw.highlandersim.ControlFrame   -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10
```

**Parámetros**  
- `-Dcount=N` → número de inmortales (por defecto 8)  
- `-Dfight=ordered|naive` → estrategia de pelea (`ordered` evita *deadlocks*, `naive` los puede provocar)  
- `-Dhealth`, `-Ddamage` → salud inicial y daño por golpe

### Demos teóricas (sin UI)
```bash
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=1  # 1 = Deadlock ingenuo
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=2  # 2 = Orden total (sin deadlock)
mvn -q -DskipTests exec:java -Dmode=demos -Ddemo=3  # 3 = tryLock + timeout (progreso)
```

---

## Controles en la UI

- **Start**: inicia una simulación con los parámetros elegidos.
- **Pause & Check**: pausa **todos** los hilos y muestra salud por inmortal y **suma total** (invariante).
- **Resume**: reanuda la simulación.
- **Stop**: detiene ordenadamente.

**Invariante**: con N jugadores y salud inicial H, la **suma total** de salud debe permanecer constante (salvo durante un update en curso). Usa **Pause & Check** para validarlo.

---

## Arquitectura (carpetas)

```
edu.eci.arsw
├─ app/                 # Bootstrap (Main): modes ui|immortals|demos
├─ highlandersim/       # UI Swing: ControlFrame (Start, Pause & Check, Resume, Stop)
├─ immortals/           # Dominio: Immortal, ImmortalManager, ScoreBoard
├─ concurrency/         # PauseController (Lock/Condition; paused(), awaitIfPaused())
├─ demos/               # DeadlockDemo, OrderedTransferDemo, TryLockTransferDemo
└─ core/                # BankAccount, TransferService (para demos teóricas)
```

---

# Actividades del laboratorio

## Parte I — (Antes de terminar la clase) `wait/notify`: Productor/Consumidor
1. Ejecuta el programa de productor/consumidor y monitorea CPU con **jVisualVM**. ¿Por qué el consumo alto? ¿Qué clase lo causa?  
2. Ajusta la implementación para **usar CPU eficientemente** cuando el **productor es lento** y el **consumidor es rápido**. Valida de nuevo con VisualVM.  
3. Ahora **productor rápido** y **consumidor lento** con **límite de stock** (cola acotada): garantiza que el límite se respete **sin espera activa** y valida CPU con un stock pequeño.

> Nota: la Parte I se realiza en el repositorio dedicado https://github.com/DECSIS-ECI/Lab_busy_wait_vs_wait_notify — clona ese repo y realiza los ejercicios allí; contiene el código de productor/consumidor, variantes con busy-wait y las soluciones usando wait()/notify(), además de instrucciones para ejecutar y validar con jVisualVM.


> Usa monitores de Java: **`synchronized` + `wait()` + `notify/notifyAll()`**, evitando *busy-wait*.

---

## Parte II — (Antes de terminar la clase) Búsqueda distribuida y condición de parada
Reescribe el **buscador de listas negras** para que la búsqueda **se detenga tan pronto** el conjunto de hilos detecte el número de ocurrencias que definen si el host es confiable o no (`BLACK_LIST_ALARM_COUNT`). Debe:
- **Finalizar anticipadamente** (no recorrer servidores restantes) y **retornar** el resultado.  
- Garantizar **ausencia de condiciones de carrera** sobre el contador compartido.

> Puedes usar `AtomicInteger` o sincronización mínima sobre la región crítica del contador.

---

## Parte III — (Avance) Sincronización y *Deadlocks* con *Highlander Simulator*
1. Revisa la simulación: N inmortales; cada uno **ataca** a otro. El que ataca **resta M** al contrincante y **suma M/2** a su propia vida.  
2. **Invariante**: con N y salud inicial `H`, la suma total debería permanecer constante (salvo durante un update). Calcula ese valor y úsalo para validar.  
3. Ejecuta la UI y prueba **“Pause & Check”**. ¿Se cumple el invariante? Explica.  
4. **Pausa correcta**: asegura que **todos** los hilos queden pausados **antes** de leer/imprimir la salud; implementa **Resume** (ya disponible).  
5. Haz *click* repetido y valida consistencia. ¿Se mantiene el invariante?  
6. **Regiones críticas**: identifica y sincroniza las secciones de pelea para evitar carreras; si usas múltiples *locks*, anida con **orden consistente**:
   ```java
   synchronized (lockA) {
     synchronized (lockB) {
       // ...
     }
   }
   ```
7. Si la app se **detiene** (posible *deadlock*), usa **`jps`** y **`jstack`** para diagnosticar.  
8. Aplica una **estrategia** para corregir el *deadlock* (p. ej., **orden total** por nombre/id, o **`tryLock(timeout)`** con reintentos y *backoff*).  
9. Valida con **N=100, 1000 o 10000** inmortales. Si falla el invariante, revisa la pausa y las regiones críticas.  
10. **Remover inmortales muertos** sin bloquear la simulación: analiza si crea una **condición de carrera** con muchos hilos y corrige **sin sincronización global** (colección concurrente o enfoque *lock-free*).  
11. Implementa completamente **STOP** (apagado ordenado).

---

## Respuestas y cambios implementados — Parte III

He realizado cambios en el código para habilitar una pausa cooperativa consistente, permitir snapshots seguros sin bloquear la simulación y mejorar la parada ordenada. A continuación se resumen las modificaciones, cómo validar y el estado de cada requisito del enunciado.

- Archivos modificados:
  - `src/main/java/edu/eci/arsw/concurrency/PauseController.java`
    - Añadido conteo de hilos pausados (`pausedThreads`) y método `waitForAllPaused(int expected, long timeoutMillis)` para que la UI espere hasta que los hilos alcancen la pausa cooperativa.
  - `src/main/java/edu/eci/arsw/immortals/ImmortalManager.java`
    - `population` ahora es `CopyOnWriteArrayList<Immortal>` para permitir snapshots seguros y remociones sin sincronización global.
    - `stop()` mejora la parada cooperativa y limpia futuros.
  - `src/main/java/edu/eci/arsw/highlandersim/ControlFrame.java`
    - `onPauseAndCheck` espera (hasta 2s) a que todos los hilos lleguen a la pausa antes de tomar el snapshot; si expira el timeout se informa en la UI.

- Cómo validar (pasos rápidos):
  1. Compilar y ejecutar UI:
     ```powershell
     mvn -DskipTests exec:java -Dexec.mainClass=edu.eci.arsw.highlandersim.ControlFrame -Dcount=8 -Dfight=ordered -Dhealth=100 -Ddamage=10
     ```
  2. Click `Start`.
  3. Click `Pause & Check` — la UI esperará hasta 2s a que los hilos lleguen al punto de pausa cooperativa; luego muestra la salud por inmortal, la suma total y el contador de fights.
  4. Click `Resume` para reanudar, `Stop` para detener ordenadamente.

- Invariante (comentario importante):
  - Enunciado esperado: "la suma total de salud debe permanecer constante". Sin embargo, en el código actual la operación de pelea está implementada como:
    ```java
    other.health -= damage;
    this.health += damage / 2;
    ```
    por lo que cada pelea provoca un cambio neto en la suma total igual a `-damage/2` (la suma total decrece). Por tanto, con la implementación actual la suma NO se mantiene constante.
  - Si deseas que la suma sea invariante debes cambiar la regla de pelea a una transferencia cero-suma — por ejemplo:
    ```java
    other.health -= damage;
    this.health += damage; // transferencia total
    ```
    o cualquier regla donde lo restado al oponente sea exactamente lo sumado al atacante.
  - Con la implementación original del enunciado (si asumimos transferencia total) el valor esperado es `Total = N * H` (donde N = número de inmortales, H = salud inicial). En el estado actual, la suma esperada decrece con cada pelea en `damage/2`.

- Resumen del estado de requisitos (Parte III):
  - Pausa correcta (esperar antes de leer): Done — `PauseController.waitForAllPaused(...)` y espera en `ControlFrame`.
  - Resume: Preservado (ya existía) — Done.
  - Regiones críticas / deadlocks: El código mantiene `fightNaive` (potencial deadlock) y `fightOrdered` (evita deadlocks por orden total). No se cambiaron las estrategias de pelea — Done (existing).
  - Diagnóstico de deadlocks: Mantener `jps`/`jstack` como herramienta — Deferred (herramienta externa).
  - Remover inmortales muertos: Mejora parcial — `CopyOnWriteArrayList` permite remover sin CME y sin sincronización global; se puede agregar una tarea periódica de limpieza si se desea — Partial.
  - STOP (apagado ordenado): Mejorado — `stop()` solicita parada y apaga el executor; se puede añadir `awaitTermination` para esperar completitud — Partial/Done.

- Notas y recomendaciones adicionales:
  - Para validar invariante con `Pause & Check` en modo experimental, usa `-Dfight=ordered` para evitar deadlocks. Si la suma no se mantiene, revisa la regla de pelea (ver nota anterior sobre transferencia neta).
  - Para remover inmortales muertos de forma no bloqueante en simulaciones grandes recomiendo implementar una tarea que periódicamente recorra la `CopyOnWriteArrayList` y elimine los que `!isAlive()` (la operación de eliminación en `CopyOnWriteArrayList` es segura aunque costosa).
  - Si quieres que implemente: validación automática del invariante en la UI (PASS/FAIL), limpieza periódica de muertos o la estrategia `tryLock(timeout)` como alternativa anti-deadlock, dime cuál prefieres y lo implemento.

---

## Entregables

1. **Código fuente** (Java 21) con la UI funcionando.  
2. **`Informe de laboratorio en formato pdf`** con:
   - Parte I: diagnóstico de CPU y cambios para eliminar espera activa.  
   - Parte II: diseño de **parada temprana** y cómo evitas condiciones de carrera en el contador.  
   - Parte III:  
     - Regiones críticas y estrategia adoptada (**orden total** o **tryLock+timeout**).  
     - Evidencia de *deadlock* (si ocurrió) con `jstack` y corrección aplicada.  
     - Validación del **invariante** con **Pause & Check** (distintos N).  
     - Estrategia para **remover inmortales muertos** sin sincronización global.
3. Instrucciones de ejecución si cambias *defaults*.

---

## Criterios de evaluación (10 pts)

- (3) **Concurrencia correcta**: sin *data races*; sincronización bien localizada; no hay espera activa.  
- (2) **Pausa/Reanudar**: consistencia del estado e invariante bajo **Pause & Check**.  
- (2) **Robustez**: corre con N alto; sin `ConcurrentModificationException`, sin *deadlocks* no gestionados.  
- (1.5) **Calidad**: arquitectura clara, nombres y comentarios; separación UI/lógica.  
- (1.5) **Documentación**: **`RESPUESTAS.txt`** claro con evidencia (dumps/capturas) y justificación técnica.

---

## Tips y configuración útil

- **Estrategias de pelea**:  
  - `-Dfight=naive` → útil para **reproducir** carreras y *deadlocks*.  
  - `-Dfight=ordered` → **evita** *deadlocks* (orden total por nombre/id).
- **Pausa cooperativa**: usa `PauseController` (Lock/Condition), **sin** `suspend/resume/stop`.  
- **Colecciones**: evita estructuras no seguras; prefiere inmutabilidad o colecciones concurrentes.  
- **Diagnóstico**: `jps`, `jstack`, **jVisualVM**; revisa *thread dumps* cuando sospeches *deadlock*.  
- **Virtual Threads**: favorecen esperar con bloqueo (no *busy-wait*); usa timeouts.

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y pruebas JUnit.

---

## Créditos y licencia

Laboratorio basado en el enunciado histórico del curso (Highlander, Productor/Consumidor, Búsqueda distribuida), modernizado a **Java 21**.  
<a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-nc/4.0/88x31.png" /></a><br />Este contenido hace parte del curso Arquitecturas de Software (ECI) y está licenciado como <a rel="license" href="http://creativecommons.org/licenses/by-nc/4.0/">Creative Commons Attribution-NonCommercial 4.0 International License</a>.
