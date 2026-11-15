# Proyecto UD2 – Gestión Veterinaria

Aplicación de escritorio para la gestión de clientes, mascotas, citas veterinarias y tratamientos. Está desarrollada en **Java**, utiliza **JavaFX y FXML** para la interfaz gráfica y **MySQL** para almacenar los datos. La estructura del programa sigue el patrón **MVC (Modelo-Vista-Controlador)**.

Repositorio: https://github.com/hector-dam1/Proyecto-UD2

---

## 1. Funcionalidades

La aplicación permite:

- Buscar clientes mediante su DNI/NIF.
- Registrar un cliente nuevo cuando no existe.
- Consultar y modificar los datos del cliente identificado.
- Registrar mascotas y consultar sus datos.
- Modificar los datos permitidos de una mascota.
- Consultar las citas pendientes y el historial de citas de un cliente.
- Crear y modificar citas, seleccionando mascota, tratamientos, fecha y hora disponibles.
- Marcar una cita como **Asistida** o **No asistida**.
- Consultar el historial de consultas y tratamientos de una mascota.
- Consultar el catálogo de tratamientos. El catálogo es de solo lectura desde la aplicación.

El cliente identificado se conserva durante la navegación entre pantallas mientras la aplicación permanece abierta. Al cerrar y volver a iniciar la aplicación, la sesión comienza sin un cliente seleccionado.

### Recorrido básico

1. Abre la aplicación.
2. En la pantalla **Identificación**, introduce el DNI/NIF y pulsa el botón de búsqueda.
3. Si el cliente existe, se cargarán sus datos y sus mascotas.
4. Si no existe, la aplicación preguntará si deseas registrarlo. Acepta y completa los datos para guardarlo.
5. Desde Identificación, accede a las pantallas de citas, consultas de tratamientos o catálogo.
6. Para crear una cita, selecciona una mascota, añade uno o varios tratamientos y elige una fecha y una hora disponibles.
7. Guarda la cita. Podrás consultarla posteriormente desde la pantalla **Citas**.

### Consideraciones de uso

- El teléfono se guarda con el prefijo español **+34** y nueve cifras. Puedes introducirlo con espacios; la aplicación normaliza el formato antes de guardarlo.
- El correo electrónico del cliente es opcional.
- La edad de la mascota se calcula a partir de su fecha de nacimiento; no se almacena como dato independiente.
- No se pueden reservar citas para mascotas que tengan una fecha de defunción.
- El horario de la clínica es de lunes a viernes de 09:00 a 14:00 y de 16:00 a 20:00; los sábados de 09:00 a 14:00; los domingos permanece cerrada.
- La disponibilidad depende de la duración total de los tratamientos y de la disponibilidad del veterinario.
- Los tratamientos se consultan desde la aplicación. Su mantenimiento se realiza directamente en MySQL, no desde la interfaz.

---

## 2. Requisitos

Para ejecutar el proyecto necesitas:

- **JDK 25**.
- **JavaFX SDK 25.0.5**.
- **MySQL 8.3** o una versión compatible con las instrucciones SQL del proyecto.
- **Visual Studio Code** con las extensiones de Java.
- El conector JDBC de MySQL, incluido en `lib/mysql-connector-j-9.7.0.jar`.

La aplicación utiliza esta conexión:

- Servidor: `localhost`
- Puerto: `3306`
- Base de datos: `proyecto_ud2`

El conector JDBC ya está dentro del repositorio, en la carpeta `lib`. JavaFX debe estar instalado en el equipo; no forma parte del JDK.

---

## 3. Preparar la base de datos

1. Inicia el servidor MySQL.
2. Abre una terminal en la carpeta raíz del proyecto.
3. Ejecuta el script SQL:

**Windows (PowerShell o CMD):**

```powershell
mysql -u root -p < Proyecto_UD2_MySQL.sql
```

**Ubuntu:**

```bash
mysql -u root -p < Proyecto_UD2_MySQL.sql
```

Introduce la contraseña del usuario de MySQL cuando se solicite.

El script crea la base de datos `proyecto_ud2`, sus tablas, relaciones, procedimientos almacenados, triggers, vistas y datos de ejemplo.

> **Importante:** el script incluye inserciones de datos de ejemplo. Ejecútalo inicialmente en una base de datos nueva o vacía. No lo vuelvas a ejecutar sobre una instalación que ya tenga los datos cargados, porque los INSERT pueden duplicar registros o provocar errores por claves únicas.

Puedes comprobar que la base de datos existe entrando en MySQL:

```sql
SHOW DATABASES;
USE proyecto_ud2;
SHOW TABLES;
```

---

## 4. Configurar las variables de entorno de MySQL

La aplicación no contiene las credenciales de MySQL en el código. Las obtiene mediante dos variables de entorno:

| Variable | Contenido |
|---|---|
| `DB_USER` | Usuario de MySQL que utilizará la aplicación. |
| `DB_PASSWORD` | Contraseña de ese usuario. |

Debes configurar ambas variables antes de ejecutar la aplicación. No escribas tus credenciales directamente en `Conexion.java` ni las subas al repositorio.

### 4.1. Windows

#### Opción A: PowerShell (solo para la terminal actual)

Abre PowerShell y ejecuta:

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "TU_CONTRASEÑA_MYSQL"
```

Sustituye `TU_CONTRASEÑA_MYSQL` por la contraseña real de tu usuario de MySQL.

Estas variables estarán disponibles para los programas que se ejecuten desde esa misma sesión de PowerShell. Si cierras la terminal, tendrás que volver a configurarlas.

Para comprobar que están definidas sin mostrar la contraseña:

```powershell
$env:DB_USER
if ($env:DB_PASSWORD) { "DB_PASSWORD configurada" }
```

#### Opción B: variables permanentes de Windows

1. Abre el menú Inicio y busca **Editar las variables de entorno del sistema**.
2. En la ventana que aparece, pulsa **Variables de entorno**.
3. En **Variables de usuario**, pulsa **Nueva**.
4. Crea la variable `DB_USER` con el nombre de usuario de MySQL.
5. Crea la variable `DB_PASSWORD` con la contraseña de MySQL.
6. Acepta todas las ventanas.
7. Cierra y vuelve a abrir Visual Studio Code para que reconozca las nuevas variables.

Las variables permanentes quedan guardadas en el perfil de Windows. Protege el acceso a tu cuenta y evita compartir capturas o archivos que muestren la contraseña.

### 4.2. Ubuntu

Abre una terminal y configura las variables:

```bash
export DB_USER="root"
read -rsp "Contraseña de MySQL: " DB_PASSWORD
echo
export DB_PASSWORD
```

La contraseña se introduce sin que aparezca escrita en pantalla. Las variables estarán disponibles para los programas que se ejecuten desde esa terminal.

Para comprobarlo sin mostrar la contraseña:

```bash
echo "$DB_USER"
if [ -n "$DB_PASSWORD" ]; then echo "DB_PASSWORD configurada"; fi
```

**Importante:** si cierras esa terminal, las variables dejarán de estar disponibles. Si vas a iniciar Visual Studio Code desde esa misma terminal, hazlo con:

```bash
code .
```

Así Visual Studio Code heredará las variables de entorno y podrá ejecutar la aplicación con ellas.

No es recomendable guardar la contraseña directamente en un archivo compartido ni subirla a GitHub.

---

## 5. Configurar JavaFX en Visual Studio Code

El proyecto utiliza JavaFX 25.0.5. Debes tener descargado e instalado el **JavaFX SDK** y conocer la ruta de su carpeta `lib`.

El proyecto ya incluye el conector JDBC de MySQL. La configuración de JavaFX, en cambio, depende de la ruta donde lo hayas instalado.

### Windows

Los archivos de configuración incluidos utilizan esta ruta:

```text
C:/javafx/javafx-sdk-25.0.5/lib
```

Si has instalado JavaFX en otra ubicación, adapta la ruta en estos dos archivos:

- `.vscode/settings.json`
- `.vscode/launch.json`

En `settings.json`, la ruta se encuentra dentro de `java.project.referencedLibraries`. En `launch.json`, se utiliza en el argumento `--module-path`.

### Ubuntu

Por ejemplo, si has descomprimido JavaFX en `/home/TU_USUARIO/javafx-sdk-25.0.5`, la carpeta de librerías será:

```text
/home/TU_USUARIO/javafx-sdk-25.0.5/lib
```

Adapta la ruta de JavaFX en `.vscode/settings.json`:

```json
{
    "java.project.referencedLibraries": [
        "lib/**/*.jar",
        "/home/TU_USUARIO/javafx-sdk-25.0.5/lib/**/*.jar"
    ]
}
```

Y en `.vscode/launch.json`, sustituye el valor de `vmArgs` por:

```json
"vmArgs": "--module-path /home/TU_USUARIO/javafx-sdk-25.0.5/lib --add-modules javafx.controls,javafx.fxml"
```

Cambia `TU_USUARIO` por el nombre de tu usuario de Ubuntu y utiliza la ruta real donde hayas instalado JavaFX.

---

## 6. Ejecutar la aplicación

1. Comprueba que MySQL está iniciado.
2. Configura `DB_USER` y `DB_PASSWORD`.
3. Abre la carpeta del proyecto en Visual Studio Code.
4. Comprueba que las rutas de JavaFX de los archivos de configuración son correctas.
5. Abre `src/App.java`.
6. Ejecuta la aplicación con **Run** o **F5** desde Visual Studio Code.

La clase principal es `App`. La aplicación abrirá la pantalla de Identificación.

Si cambias las variables de entorno con Visual Studio Code abierto, ciérralo y vuelve a abrirlo desde la terminal donde las hayas configurado, o reinícialo después de modificar las variables permanentes de Windows.

---

## 7. Solución de problemas habituales

### «No se han configurado las credenciales de MySQL»

La aplicación no encuentra `DB_USER` o `DB_PASSWORD`.

- Comprueba que las dos variables están definidas.
- Si utilizas Windows, reinicia Visual Studio Code después de configurar variables permanentes.
- En Ubuntu, inicia Visual Studio Code desde la terminal en la que has ejecutado los comandos `export`.

### No se puede establecer conexión con MySQL

Comprueba que:

- El servidor MySQL está iniciado.
- Está escuchando en `localhost:3306`.
- Existe la base de datos `proyecto_ud2`.
- El usuario y la contraseña son correctos.
- El usuario tiene permisos sobre la base de datos.

### JavaFX no se encuentra o aparecen errores de módulos

Comprueba que:

- Has instalado JavaFX SDK.
- La ruta de la carpeta `lib` es correcta.
- Has actualizado tanto `.vscode/settings.json` como `.vscode/launch.json`.
- Estás utilizando un JDK compatible con la versión de JavaFX configurada.

### No se puede encontrar el controlador JDBC

Comprueba que existe el archivo:

```text
lib/mysql-connector-j-9.7.0.jar
```

Y que `.vscode/settings.json` conserva la referencia `lib/**/*.jar`.

---

## 8. Estructura general del proyecto

```text
Proyecto-UD2/
├── .vscode/
│   ├── launch.json
│   └── settings.json
├── lib/
│   └── mysql-connector-j-9.7.0.jar
├── src/
│   ├── App.java
│   ├── controlador/
│   ├── modelo/
│   │   ├── conexion/
│   │   ├── dao/
│   │   ├── entidades/
│   │   └── sesion/
│   └── vista/
│       └── *.fxml
├── Proyecto_UD2_MySQL.sql
└── Readme.md
```

- **Modelo:** entidades, conexión con MySQL y clases DAO para acceder a los datos.
- **Vista:** interfaces gráficas JavaFX definidas mediante archivos FXML.
- **Controlador:** lógica que conecta las acciones del usuario con el modelo y la vista.

---

## 9. Seguridad y datos

- No compartas ni subas las credenciales de MySQL.
- Utiliza un usuario de MySQL con los permisos necesarios para la aplicación.
- Evita utilizar la cuenta `root` en instalaciones que vayan a contener datos reales.
- El script SQL contiene datos de ejemplo; revisa su contenido antes de ejecutarlo en una base de datos existente.
- La conexión está configurada para `localhost:3306`. Si MySQL se ejecuta en otro servidor o puerto, será necesario adaptar la URL JDBC de `src/modelo/conexion/Conexion.java`.

---

**Proyecto académico – Desarrollo de Aplicaciones Multiplataforma (DAM).**
