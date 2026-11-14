/*
===============================================================================
PROYECTO UD2 - BASE DE DATOS DE GESTION VETERINARIA
Motor previsto: MySQL 8.3
Codificacion: utf8mb4
Esquema: proyecto_ud2

IMPORTANTE
- Este archivo CREA la estructura y carga datos de ejemplo.
- No contiene DROP DATABASE ni DROP TABLE: no elimina datos existentes.
- Ejecutarlo en una base de datos nueva o vacia llamada proyecto_ud2.
- Si ya existen tablas con estos nombres, CREATE TABLE IF NOT EXISTS no las
  modifica. En ese caso, revisar la estructura antes de ejecutar los INSERT.
- Los datos de ejemplo utilizan identificadores reservados del 1 al 3.
- Edad no se almacena: se calcula desde Fecha_Nacimiento en la aplicacion.
===============================================================================
*/

CREATE DATABASE IF NOT EXISTS proyecto_ud2
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE proyecto_ud2;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

/* ============================================================================
   1. TABLAS PRINCIPALES
   ============================================================================ */

CREATE TABLE IF NOT EXISTS Cliente (
    ID_Cliente       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    DNI_NIF          VARCHAR(15) NOT NULL,
    Nombre           VARCHAR(60) NOT NULL,
    Apellidos        VARCHAR(100) NOT NULL,
    Telefono         VARCHAR(16) NOT NULL,
    Direccion        VARCHAR(180) NOT NULL,
    Email            VARCHAR(120) NULL,
    PRIMARY KEY (ID_Cliente),
    CONSTRAINT uq_cliente_dni UNIQUE (DNI_NIF),
    CONSTRAINT chk_cliente_nombre CHECK (CHAR_LENGTH(TRIM(Nombre)) > 0),
    CONSTRAINT chk_cliente_apellidos CHECK (CHAR_LENGTH(TRIM(Apellidos)) > 0),
    CONSTRAINT chk_cliente_telefono CHECK (Telefono REGEXP '^\\+34[0-9]{9}$'),
    CONSTRAINT chk_cliente_email CHECK (
        Email IS NULL OR Email REGEXP '^[^[:space:]@]+@[^[:space:]@]+\\.[^[:space:]@]+$'
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Veterinario (
    ID_Veterinario   INT UNSIGNED NOT NULL AUTO_INCREMENT,
    DNI_NIF          VARCHAR(15) NOT NULL,
    Nombre           VARCHAR(60) NOT NULL,
    Apellidos        VARCHAR(100) NOT NULL,
    Direccion        VARCHAR(180) NOT NULL,
    Telefono         VARCHAR(16) NOT NULL,
    PRIMARY KEY (ID_Veterinario),
    CONSTRAINT uq_veterinario_dni UNIQUE (DNI_NIF),
    CONSTRAINT chk_veterinario_telefono CHECK (Telefono REGEXP '^\\+34[0-9]{9}$')
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Mascota (
    ID_Mascota       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Cliente       INT UNSIGNED NOT NULL,
    Nombre           VARCHAR(60) NOT NULL,
    Raza             VARCHAR(80) NOT NULL,
    Fecha_Nacimiento DATE NOT NULL,
    Fecha_Defuncion  DATE NULL,
    Num_Chip         VARCHAR(30) NOT NULL,
    Peso             DECIMAL(6,2) NULL,
    Estado           VARCHAR(500) NULL,
    PRIMARY KEY (ID_Mascota),
    CONSTRAINT uq_mascota_chip UNIQUE (Num_Chip),
    CONSTRAINT fk_mascota_cliente
        FOREIGN KEY (ID_Cliente) REFERENCES Cliente(ID_Cliente)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_mascota_peso CHECK (Peso IS NULL OR Peso > 0),
    INDEX idx_mascota_cliente (ID_Cliente)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Tratamiento (
    ID_Tratamiento       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    Categoria            VARCHAR(60) NOT NULL,
    Nombre               VARCHAR(100) NOT NULL,
    Precio               DECIMAL(10,2) NOT NULL,
    Duracion             SMALLINT UNSIGNED NOT NULL COMMENT 'Duracion en minutos',
    Descripcion          TEXT NULL,
    Material_Necesario   TEXT NULL,
    PRIMARY KEY (ID_Tratamiento),
    CONSTRAINT chk_tratamiento_precio CHECK (Precio >= 0),
    CONSTRAINT chk_tratamiento_duracion CHECK (Duracion > 0),
    INDEX idx_tratamiento_categoria (Categoria),
    INDEX idx_tratamiento_nombre (Nombre)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Consulta (
    ID_Consulta       INT UNSIGNED NOT NULL AUTO_INCREMENT,
    ID_Cliente        INT UNSIGNED NOT NULL,
    ID_Mascota        INT UNSIGNED NOT NULL,
    ID_Veterinario    INT UNSIGNED NOT NULL,
    Fecha             DATETIME NOT NULL,
    Nota              TEXT NULL,
    Estado            ENUM('Pendiente', 'Asistida', 'No asistida')
                      NOT NULL DEFAULT 'Pendiente',
    PRIMARY KEY (ID_Consulta),
    CONSTRAINT fk_consulta_cliente
        FOREIGN KEY (ID_Cliente) REFERENCES Cliente(ID_Cliente)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_consulta_mascota
        FOREIGN KEY (ID_Mascota) REFERENCES Mascota(ID_Mascota)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_consulta_veterinario
        FOREIGN KEY (ID_Veterinario) REFERENCES Veterinario(ID_Veterinario)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_consulta_fecha (Fecha),
    INDEX idx_consulta_mascota_fecha (ID_Mascota, Fecha),
    INDEX idx_consulta_estado_fecha (Estado, Fecha),
    INDEX idx_consulta_veterinario_fecha (ID_Veterinario, Fecha)
) ENGINE=InnoDB;

/*
   Tabla asociativa aprobada para la relacion N:M:
   una consulta puede incluir varios tratamientos y un tratamiento puede
   aparecer en distintas consultas.
*/
CREATE TABLE IF NOT EXISTS Consulta_Tratamiento (
    ID_Consulta       INT UNSIGNED NOT NULL,
    ID_Tratamiento    INT UNSIGNED NOT NULL,
    PRIMARY KEY (ID_Consulta, ID_Tratamiento),
    CONSTRAINT fk_ct_consulta
        FOREIGN KEY (ID_Consulta) REFERENCES Consulta(ID_Consulta)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ct_tratamiento
        FOREIGN KEY (ID_Tratamiento) REFERENCES Tratamiento(ID_Tratamiento)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_ct_tratamiento (ID_Tratamiento)
) ENGINE=InnoDB;

/* ============================================================================
   2. TRIGGERS
   ============================================================================ */

/*
   TRIGGER 1: al registrar una fecha de defuncion, las citas pendientes de esa
   mascota cuya fecha sea igual o posterior a la defuncion pasan a No asistida.
   Las citas anteriores y las que ya tengan otro estado no se modifican.
*/
DROP TRIGGER IF EXISTS trg_mascota_defuncion_citas;
DELIMITER $$
CREATE TRIGGER trg_mascota_defuncion_citas
AFTER UPDATE ON Mascota
FOR EACH ROW
BEGIN
    IF NEW.Fecha_Defuncion IS NOT NULL
       AND (OLD.Fecha_Defuncion IS NULL
            OR NEW.Fecha_Defuncion <> OLD.Fecha_Defuncion) THEN

        UPDATE Consulta
           SET Estado = 'No asistida'
         WHERE ID_Mascota = NEW.ID_Mascota
           AND Estado = 'Pendiente'
           AND DATE(Fecha) >= NEW.Fecha_Defuncion;
    END IF;
END$$
DELIMITER ;

/*
   TRIGGER 2: comprueba el horario y la duracion total antes de asociar un
   tratamiento. El horario es:
   - Lunes a viernes: 09:00-14:00 y 16:00-20:00.
   - Sabado: 09:00-14:00.
   - Domingo: cerrado.

   La duracion se calcula sumando los tratamientos ya asociados y el nuevo.
   La cita debe terminar dentro del mismo tramo horario.
*/
DROP TRIGGER IF EXISTS trg_ct_comprobar_horario_insert;
DELIMITER $$
CREATE TRIGGER trg_ct_comprobar_horario_insert
BEFORE INSERT ON Consulta_Tratamiento
FOR EACH ROW
BEGIN
    DECLARE v_fecha DATETIME;
    DECLARE v_duracion INT DEFAULT 0;
    DECLARE v_duracion_nuevo INT DEFAULT 0;
    DECLARE v_inicio TIME;
    DECLARE v_fin TIME;
    DECLARE v_dia INT;

    SELECT Fecha INTO v_fecha
      FROM Consulta
     WHERE ID_Consulta = NEW.ID_Consulta;

    SELECT COALESCE(SUM(t.Duracion), 0) INTO v_duracion
      FROM Consulta_Tratamiento ct
      JOIN Tratamiento t ON t.ID_Tratamiento = ct.ID_Tratamiento
     WHERE ct.ID_Consulta = NEW.ID_Consulta;

    SELECT Duracion INTO v_duracion_nuevo
      FROM Tratamiento
     WHERE ID_Tratamiento = NEW.ID_Tratamiento;

    SET v_duracion = v_duracion + v_duracion_nuevo;
    SET v_inicio = TIME(v_fecha);
    SET v_fin = ADDTIME(v_inicio, SEC_TO_TIME(v_duracion * 60));
    SET v_dia = DAYOFWEEK(v_fecha);

    IF v_dia = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pueden programar citas en domingo';
    ELSEIF v_dia = 7 THEN
        IF v_inicio < '09:00:00' OR v_fin > '14:00:00' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario del sabado (09:00-14:00)';
        END IF;
    ELSE
        IF NOT (
            (v_inicio >= '09:00:00' AND v_fin <= '14:00:00')
            OR
            (v_inicio >= '16:00:00' AND v_fin <= '20:00:00')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario de lunes a viernes (09:00-14:00 o 16:00-20:00)';
        END IF;
    END IF;
END$$
DELIMITER ;

/*
   Comprueba tambien las modificaciones de una asociacion tratamiento-consulta.
*/
DROP TRIGGER IF EXISTS trg_ct_comprobar_horario_update;
DELIMITER $$
CREATE TRIGGER trg_ct_comprobar_horario_update
BEFORE UPDATE ON Consulta_Tratamiento
FOR EACH ROW
BEGIN
    DECLARE v_fecha DATETIME;
    DECLARE v_duracion INT DEFAULT 0;
    DECLARE v_duracion_nuevo INT DEFAULT 0;
    DECLARE v_inicio TIME;
    DECLARE v_fin TIME;
    DECLARE v_dia INT;

    SELECT Fecha INTO v_fecha
      FROM Consulta
     WHERE ID_Consulta = NEW.ID_Consulta;

    SELECT COALESCE(SUM(t.Duracion), 0) INTO v_duracion
      FROM Consulta_Tratamiento ct
      JOIN Tratamiento t ON t.ID_Tratamiento = ct.ID_Tratamiento
     WHERE ct.ID_Consulta = NEW.ID_Consulta
       AND NOT (ct.ID_Consulta = OLD.ID_Consulta
                AND ct.ID_Tratamiento = OLD.ID_Tratamiento);

    SELECT Duracion INTO v_duracion_nuevo
      FROM Tratamiento
     WHERE ID_Tratamiento = NEW.ID_Tratamiento;

    SET v_duracion = v_duracion + v_duracion_nuevo;
    SET v_inicio = TIME(v_fecha);
    SET v_fin = ADDTIME(v_inicio, SEC_TO_TIME(v_duracion * 60));
    SET v_dia = DAYOFWEEK(v_fecha);

    IF v_dia = 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pueden programar citas en domingo';
    ELSEIF v_dia = 7 THEN
        IF v_inicio < '09:00:00' OR v_fin > '14:00:00' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario del sabado (09:00-14:00)';
        END IF;
    ELSE
        IF NOT (
            (v_inicio >= '09:00:00' AND v_fin <= '14:00:00')
            OR
            (v_inicio >= '16:00:00' AND v_fin <= '20:00:00')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario de lunes a viernes (09:00-14:00 o 16:00-20:00)';
        END IF;
    END IF;
END$$
DELIMITER ;

/*
   TRIGGER 3: al cambiar la fecha/hora de una consulta ya asociada a
   tratamientos, vuelve a comprobar que toda su duracion cabe en el horario.
*/
DROP TRIGGER IF EXISTS trg_consulta_comprobar_horario_update;
DELIMITER $$
CREATE TRIGGER trg_consulta_comprobar_horario_update
BEFORE UPDATE ON Consulta
FOR EACH ROW
BEGIN
    DECLARE v_duracion INT DEFAULT 0;
    DECLARE v_inicio TIME;
    DECLARE v_fin TIME;
    DECLARE v_dia INT;

    IF NEW.Fecha <> OLD.Fecha THEN
        SELECT COALESCE(SUM(t.Duracion), 0) INTO v_duracion
          FROM Consulta_Tratamiento ct
          JOIN Tratamiento t ON t.ID_Tratamiento = ct.ID_Tratamiento
         WHERE ct.ID_Consulta = OLD.ID_Consulta;

        SET v_inicio = TIME(NEW.Fecha);
        SET v_fin = ADDTIME(v_inicio, SEC_TO_TIME(v_duracion * 60));
        SET v_dia = DAYOFWEEK(NEW.Fecha);

        IF v_dia = 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No se pueden programar citas en domingo';
        ELSEIF v_dia = 7 THEN
            IF v_inicio < '09:00:00' OR v_fin > '14:00:00' THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario del sabado (09:00-14:00)';
            END IF;
        ELSE
            IF NOT (
                (v_inicio >= '09:00:00' AND v_fin <= '14:00:00')
                OR
                (v_inicio >= '16:00:00' AND v_fin <= '20:00:00')
            ) THEN
                SIGNAL SQLSTATE '45000'
                    SET MESSAGE_TEXT = 'La cita debe quedar dentro del horario de lunes a viernes (09:00-14:00 o 16:00-20:00)';
            END IF;
        END IF;
    END IF;
END$$
DELIMITER ;

/*
   TRIGGER 4: valida que la mascota pertenece al cliente indicado en Consulta
   y que no esta fallecida al crear una nueva cita pendiente.
*/
DROP TRIGGER IF EXISTS trg_consulta_validar_mascota_insert;
DELIMITER $$
CREATE TRIGGER trg_consulta_validar_mascota_insert
BEFORE INSERT ON Consulta
FOR EACH ROW
BEGIN
    DECLARE v_cliente_mascota INT UNSIGNED;
    DECLARE v_defuncion DATE;

    SELECT ID_Cliente, Fecha_Defuncion
      INTO v_cliente_mascota, v_defuncion
      FROM Mascota
     WHERE ID_Mascota = NEW.ID_Mascota;

    IF v_cliente_mascota <> NEW.ID_Cliente THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La mascota no pertenece al cliente indicado';
    END IF;

    IF v_defuncion IS NOT NULL AND DATE(NEW.Fecha) >= v_defuncion
       AND NEW.Estado = 'Pendiente' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No se pueden reservar citas para una mascota fallecida';
    END IF;
END$$
DELIMITER ;

/*
   Validacion de fechas de nacimiento y defuncion. Se hace mediante triggers
   porque CURRENT_DATE() no debe utilizarse en una restriccion CHECK.
*/
DROP TRIGGER IF EXISTS trg_mascota_validar_fechas_insert;
DELIMITER $$
CREATE TRIGGER trg_mascota_validar_fechas_insert
BEFORE INSERT ON Mascota
FOR EACH ROW
BEGIN
    IF NEW.Fecha_Nacimiento > CURRENT_DATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de nacimiento no puede ser futura';
    END IF;

    IF NEW.Fecha_Defuncion IS NOT NULL
       AND (NEW.Fecha_Defuncion < NEW.Fecha_Nacimiento
            OR NEW.Fecha_Defuncion > CURRENT_DATE()) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de defuncion debe estar entre el nacimiento y hoy';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_mascota_validar_fechas_update;
DELIMITER $$
CREATE TRIGGER trg_mascota_validar_fechas_update
BEFORE UPDATE ON Mascota
FOR EACH ROW
BEGIN
    IF NEW.Fecha_Nacimiento > CURRENT_DATE() THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de nacimiento no puede ser futura';
    END IF;

    IF NEW.Fecha_Defuncion IS NOT NULL
       AND (NEW.Fecha_Defuncion < NEW.Fecha_Nacimiento
            OR NEW.Fecha_Defuncion > CURRENT_DATE()) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de defuncion debe estar entre el nacimiento y hoy';
    END IF;
END$$
DELIMITER ;

/* Mismo control si se modifica la mascota, cliente, fecha o estado. */
DROP TRIGGER IF EXISTS trg_consulta_validar_mascota_update;
DELIMITER $$
CREATE TRIGGER trg_consulta_validar_mascota_update
BEFORE UPDATE ON Consulta
FOR EACH ROW
BEGIN
    DECLARE v_cliente_mascota INT UNSIGNED;
    DECLARE v_defuncion DATE;

    IF NEW.ID_Mascota <> OLD.ID_Mascota
       OR NEW.ID_Cliente <> OLD.ID_Cliente
       OR (NEW.Estado = 'Pendiente'
           AND (OLD.Estado <> 'Pendiente' OR NEW.Fecha <> OLD.Fecha)) THEN

        SELECT ID_Cliente, Fecha_Defuncion
          INTO v_cliente_mascota, v_defuncion
          FROM Mascota
         WHERE ID_Mascota = NEW.ID_Mascota;

        IF v_cliente_mascota <> NEW.ID_Cliente THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'La mascota no pertenece al cliente indicado';
        END IF;

        IF v_defuncion IS NOT NULL AND DATE(NEW.Fecha) >= v_defuncion
           AND NEW.Estado = 'Pendiente' THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No se pueden dejar citas pendientes para una mascota fallecida';
        END IF;
    END IF;
END$$
DELIMITER ;

/* ============================================================================
   3. PROCEDIMIENTOS ALMACENADOS
   ============================================================================ */

/* Buscar un cliente por DNI/NIF. */
DROP PROCEDURE IF EXISTS sp_buscar_cliente_por_dni;
DELIMITER $$
CREATE PROCEDURE sp_buscar_cliente_por_dni(IN p_dni_nif VARCHAR(15))
BEGIN
    SELECT ID_Cliente, DNI_NIF, Nombre, Apellidos, Telefono, Direccion, Email
      FROM Cliente
     WHERE DNI_NIF = p_dni_nif;
END$$
DELIMITER ;

/* Listar mascotas asociadas a un cliente. */
DROP PROCEDURE IF EXISTS sp_mascotas_de_cliente;
DELIMITER $$
CREATE PROCEDURE sp_mascotas_de_cliente(IN p_id_cliente INT UNSIGNED)
BEGIN
    SELECT ID_Mascota, Nombre, Raza, Fecha_Nacimiento, Fecha_Defuncion,
           Num_Chip, Peso, Estado
      FROM Mascota
     WHERE ID_Cliente = p_id_cliente
     ORDER BY Nombre;
END$$
DELIMITER ;

/* Catalogo de tratamientos: solo lectura desde la aplicacion. */
DROP PROCEDURE IF EXISTS sp_listar_tratamientos;
DELIMITER $$
CREATE PROCEDURE sp_listar_tratamientos()
BEGIN
    SELECT ID_Tratamiento, Categoria, Nombre, Precio, Duracion,
           Descripcion, Material_Necesario
      FROM Tratamiento
     ORDER BY Categoria, Nombre;
END$$
DELIMITER ;

/* Historial de citas y tratamientos de una mascota. */
DROP PROCEDURE IF EXISTS sp_historial_mascota;
DELIMITER $$
CREATE PROCEDURE sp_historial_mascota(IN p_id_mascota INT UNSIGNED)
BEGIN
    SELECT c.ID_Consulta, c.Fecha, c.Estado,
           v.Nombre AS Nombre_Veterinario,
           v.Apellidos AS Apellidos_Veterinario,
           c.Nota,
           t.ID_Tratamiento, t.Categoria, t.Nombre AS Tratamiento,
           t.Precio, t.Duracion
      FROM Consulta c
      JOIN Veterinario v ON v.ID_Veterinario = c.ID_Veterinario
      LEFT JOIN Consulta_Tratamiento ct ON ct.ID_Consulta = c.ID_Consulta
      LEFT JOIN Tratamiento t ON t.ID_Tratamiento = ct.ID_Tratamiento
     WHERE c.ID_Mascota = p_id_mascota
     ORDER BY c.Fecha DESC, t.Categoria, t.Nombre;
END$$
DELIMITER ;

/* ============================================================================
   4. VISTAS PARA CONSULTAS HABITUALES
   ============================================================================ */

CREATE OR REPLACE VIEW vw_citas_con_totales AS
SELECT
    c.ID_Consulta,
    c.ID_Cliente,
    c.ID_Mascota,
    m.Nombre AS Nombre_Mascota,
    c.ID_Veterinario,
    CONCAT(v.Nombre, ' ', v.Apellidos) AS Veterinario,
    c.Fecha,
    c.Estado,
    c.Nota,
    COALESCE(SUM(t.Precio), 0.00) AS Precio_Total,
    COALESCE(SUM(t.Duracion), 0) AS Duracion_Total_Minutos
FROM Consulta c
JOIN Mascota m ON m.ID_Mascota = c.ID_Mascota
JOIN Veterinario v ON v.ID_Veterinario = c.ID_Veterinario
LEFT JOIN Consulta_Tratamiento ct ON ct.ID_Consulta = c.ID_Consulta
LEFT JOIN Tratamiento t ON t.ID_Tratamiento = ct.ID_Tratamiento
GROUP BY c.ID_Consulta, c.ID_Cliente, c.ID_Mascota, m.Nombre,
         c.ID_Veterinario, v.Nombre, v.Apellidos, c.Fecha, c.Estado, c.Nota;

/* ============================================================================
   5. DATOS DE EJEMPLO
   Ejecutar una sola vez en el esquema vacio.
   ============================================================================ */

INSERT INTO Cliente
    (ID_Cliente, DNI_NIF, Nombre, Apellidos, Telefono, Direccion, Email)
VALUES
    (1, '12345678Z', 'Laura', 'Garcia Martin', '+34600111222',
     'Calle Mayor 12, Sevilla', 'laura.garcia@example.com'),
    (2, '23456789D', 'Carlos', 'Ruiz Lopez', '+34600222333',
     'Avenida de Andalucia 25, Sevilla', NULL),
    (3, '34567890V', 'Marta', 'Sanchez Perez', '+34600333444',
     'Calle Feria 8, Sevilla', 'marta.sanchez@example.com');

INSERT INTO Veterinario
    (ID_Veterinario, DNI_NIF, Nombre, Apellidos, Direccion, Telefono)
VALUES
    (1, '45678901H', 'Ana', 'Moreno Diaz',
     'Clinica Veterinaria, Sevilla', '+34600444555'),
    (2, '56789012L', 'David', 'Fernandez Gil',
     'Clinica Veterinaria, Sevilla', '+34600555666');

INSERT INTO Mascota
    (ID_Mascota, ID_Cliente, Nombre, Raza, Fecha_Nacimiento,
     Fecha_Defuncion, Num_Chip, Peso, Estado)
VALUES
    (1, 1, 'Luna', 'Labrador', '2021-04-12', NULL,
     '941000000000001', 24.50, 'Buen estado general'),
    (2, 1, 'Milo', 'Europeo comun', '2020-09-03', NULL,
     '941000000000002', 5.20, 'Control veterinario rutinario'),
    (3, 2, 'Rocky', 'Pastor aleman', '2019-02-18', NULL,
     '941000000000003', 31.00, 'Presenta una ligera cojera');

INSERT INTO Tratamiento
    (ID_Tratamiento, Categoria, Nombre, Precio, Duracion,
     Descripcion, Material_Necesario)
VALUES
    (1, 'Consulta', 'Consulta general', 35.00, 30,
     'Revision general de la mascota', 'Material de exploracion'),
    (2, 'Vacunacion', 'Vacuna antirrabica', 25.00, 15,
     'Administracion de vacuna antirrabica', 'Vacuna y material de inyeccion'),
    (3, 'Pruebas', 'Analisis de sangre', 55.00, 60,
     'Extraccion y analisis de una muestra de sangre', 'Material de extraccion y analisis'),
    (4, 'Higiene', 'Corte de unas', 12.00, 15,
     'Corte y revision de unas', 'Cortaunas y material de higiene'),
    (5, 'Vacunacion', 'Vacuna polivalente', 30.00, 20,
     'Vacunacion polivalente', 'Vacuna y material de inyeccion');

 /*
   Las fechas de ejemplo son deliberadas:
   - 2026-11-05 fue jueves y queda dentro del horario.
   - 2026-11-16 es lunes y queda dentro del horario.
   Ajustarlas si se desea ejecutar las pruebas en otro contexto temporal.
 */
INSERT INTO Consulta
    (ID_Consulta, ID_Cliente, ID_Mascota, ID_Veterinario, Fecha, Nota, Estado)
VALUES
    (1, 1, 1, 1, '2026-11-05 10:00:00',
     'Revision general. Se recomienda mantener el seguimiento.', 'Asistida'),
    (2, 1, 2, 2, '2026-11-16 09:00:00',
     'Cita programada para vacunacion.', 'Pendiente'),
    (3, 2, 3, 1, '2026-11-16 11:00:00',
     'Revision de la cojera y valoracion.', 'Pendiente');

INSERT INTO Consulta_Tratamiento (ID_Consulta, ID_Tratamiento)
VALUES
    (1, 1),
    (1, 2),
    (2, 1),
    (2, 5),
    (3, 1),
    (3, 3);

/* ============================================================================
   6. CONSULTAS DE COMPROBACION Y EJEMPLOS DE USO
   ============================================================================ */

/* Ver todas las tablas y sus registros. */
SELECT * FROM Cliente;
SELECT * FROM Mascota;
SELECT * FROM Veterinario;
SELECT * FROM Tratamiento;
SELECT * FROM Consulta;
SELECT * FROM Consulta_Tratamiento;

/* Consultar un cliente por DNI/NIF. */
CALL sp_buscar_cliente_por_dni('12345678Z');

/* Consultar las mascotas de un cliente. */
CALL sp_mascotas_de_cliente(1);

/* Consultar el catalogo de tratamientos. */
CALL sp_listar_tratamientos();

/* Consultar el historial completo de una mascota. */
CALL sp_historial_mascota(1);

/* Consultar citas con precio y duracion total calculados. */
SELECT * FROM vw_citas_con_totales ORDER BY Fecha;

/* Consultar solo las citas pendientes futuras. */
SELECT *
FROM vw_citas_con_totales
WHERE Estado = 'Pendiente'
  AND Fecha >= NOW()
ORDER BY Fecha;

/* ============================================================================
   7. EJEMPLOS DE TRANSACCIONES
   La aplicacion Java debe controlar estas operaciones con JDBC:
   setAutoCommit(false), commit() y rollback().
   ============================================================================ */

/*
   EJEMPLO A: insertar una consulta y asociarle varios tratamientos.
   Si cualquiera de las operaciones falla, ejecutar ROLLBACK.

START TRANSACTION;

INSERT INTO Consulta
    (ID_Cliente, ID_Mascota, ID_Veterinario, Fecha, Nota, Estado)
VALUES
    (1, 1, 1, '2026-11-17 10:00:00',
     'Ejemplo de nueva cita', 'Pendiente');

SET @id_consulta_nueva = LAST_INSERT_ID();

INSERT INTO Consulta_Tratamiento (ID_Consulta, ID_Tratamiento)
VALUES
    (@id_consulta_nueva, 1),
    (@id_consulta_nueva, 2);

COMMIT;

   Si se produce un error antes del COMMIT:
   ROLLBACK;
*/

/*
   EJEMPLO B: cambiar el estado de una cita pendiente.

START TRANSACTION;

UPDATE Consulta
   SET Estado = 'Asistida'
 WHERE ID_Consulta = 2
   AND Estado = 'Pendiente';

COMMIT;

   Ante un error:
   ROLLBACK;
*/

/* ============================================================================
   8. PRUEBAS MANUALES DE LOS TRIGGERS (NO EJECUTAR TODAS A LA VEZ)
   ============================================================================ */

/*
   PRUEBA DEL HORARIO:
   Debe fallar por domingo:
   INSERT INTO Consulta
       (ID_Cliente, ID_Mascota, ID_Veterinario, Fecha, Estado)
   VALUES (1, 1, 1, '2026-11-15 10:00:00', 'Pendiente');

   Debe fallar al asociar un tratamiento si la duracion supera el cierre:
   Crear una consulta de lunes a viernes a las 13:45 y asociar un tratamiento
   de 30 minutos. El trigger de Consulta_Tratamiento debe rechazarlo.

   PRUEBA DE DEFUNCION:
   En un entorno de pruebas, establecer una fecha de defuncion para una mascota
   que tenga citas pendientes posteriores. El trigger debe cambiar esas citas
   a 'No asistida'. No usar esta prueba sobre datos reales.
*/

/* ============================================================================
   9. REGLAS DE INTEGRIDAD Y DECISIONES DEL PROYECTO
   ============================================================================

   - Cliente.DNI_NIF y Mascota.Num_Chip son unicos.
   - El telefono del cliente y del veterinario se guarda con prefijo +34.
   - El correo del cliente es opcional.
   - Mascota.Peso es opcional, pero si se informa debe ser mayor que cero.
   - Mascota.Fecha_Defuncion es opcional y no puede ser anterior al nacimiento
     ni posterior al dia actual.
   - La edad NO se almacena; se calcula en Java a partir de Fecha_Nacimiento.
   - Consulta.Estado admite Pendiente, Asistida y No asistida.
   - La tabla Consulta_Tratamiento resuelve la relacion muchos a muchos.
   - Precio total y duracion total se calculan sumando los tratamientos; no se
     almacenan como columnas redundantes.
   - Los tratamientos se consultan desde la aplicacion; su mantenimiento se
     realiza directamente mediante SQL, no desde la interfaz.
   - No se permite borrar clientes, mascotas, veterinarios ni tratamientos que
     tengan registros relacionados, gracias a las claves foraneas RESTRICT.
   - Las consultas y sus asociaciones se eliminan en cascada solo al eliminar
     una consulta.
   - La disponibilidad de un veterinario frente a otras citas solapadas debe
     comprobarse tambien desde la aplicacion antes de guardar. Los triggers
     incluidos validan horario y duracion, pero no sustituyen la comprobacion
     de solapamiento entre citas ni la asignacion automatica del veterinario.
   - Consulta conserva ID_Cliente e ID_Mascota segun el modelo entidad-relacion
     aprobado. Los triggers verifican que la mascota pertenece a ese cliente.
   - No se incluyen campos de diagnostico ni medicacion recomendada, segun las
     decisiones funcionales aprobadas.
===============================================================================
*/
