package modelo.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import modelo.conexion.Conexion;
import modelo.entidades.Consulta;
import modelo.entidades.Tratamiento;

/**
 * Realiza las operaciones de acceso a datos de la entidad Consulta.
 */
public class ConsultaDAO {

    /**
     * Obtiene el historial de consultas y tratamientos de una mascota.
     *
     * @param idMascota identificador de la mascota
     * @return consultas de la mascota, con sus tratamientos asociados
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public ArrayList<Consulta> listarPorMascota(int idMascota) throws SQLException {

        ArrayList<Consulta> consultas = new ArrayList<>();
        String sql = "{CALL sp_historial_mascota(?)}";

        try (Connection conexion = Conexion.conectar();
             CallableStatement sentencia = conexion.prepareCall(sql)) {

            sentencia.setInt(1, idMascota);

            try (ResultSet resultado = sentencia.executeQuery()) {
                consultas = agruparConsultas(resultado, false);
            }
        }

        return consultas;
    }

    /**
     * Obtiene las consultas de un cliente, incluyendo mascota, veterinario
     * y tratamientos, para mostrar sus citas e historial.
     *
     * @param idCliente identificador del cliente
     * @return consultas del cliente
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public ArrayList<Consulta> listarPorCliente(int idCliente) throws SQLException {

        ArrayList<Consulta> consultas = new ArrayList<>();

        String sql = "SELECT c.ID_Consulta, c.ID_Cliente, c.ID_Mascota, "
                + "c.ID_Veterinario, c.Fecha, c.Nota, c.Estado, "
                + "m.Nombre AS Nombre_Mascota, "
                + "v.Nombre AS Nombre_Veterinario, "
                + "v.Apellidos AS Apellidos_Veterinario, "
                + "t.ID_Tratamiento, t.Categoria, t.Nombre AS Nombre_Tratamiento, "
                + "t.Precio, t.Duracion, t.Descripcion, "
                + "t.Material_Necesario "
                + "FROM Consulta c "
                + "JOIN Mascota m ON m.ID_Mascota = c.ID_Mascota "
                + "JOIN Veterinario v ON v.ID_Veterinario = c.ID_Veterinario "
                + "LEFT JOIN Consulta_Tratamiento ct "
                + "ON ct.ID_Consulta = c.ID_Consulta "
                + "LEFT JOIN Tratamiento t "
                + "ON t.ID_Tratamiento = ct.ID_Tratamiento "
                + "WHERE c.ID_Cliente = ? "
                + "ORDER BY c.Fecha DESC, c.ID_Consulta, t.Categoria, t.Nombre";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setInt(1, idCliente);

            try (ResultSet resultado = sentencia.executeQuery()) {
                consultas = agruparConsultas(resultado, true);
            }
        }

        return consultas;
    }

    /**
     * Inserta una consulta y sus tratamientos dentro de una transaccion.
     * Si falla la consulta o alguna asociacion, se deshacen todos los cambios.
     *
     * @param consulta consulta que se va a insertar
     * @return identificador generado en la base de datos
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int insertar(Consulta consulta) throws SQLException {

        validarConsulta(consulta);

        String sql = "INSERT INTO Consulta "
                + "(ID_Cliente, ID_Mascota, ID_Veterinario, Fecha, Nota, Estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar()) {

            conexion.setAutoCommit(false);

            try {
                int idGenerado;

                try (PreparedStatement sentencia = conexion.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    establecerDatos(sentencia, consulta);
                    sentencia.executeUpdate();

                    try (ResultSet clavesGeneradas = sentencia.getGeneratedKeys()) {
                        if (!clavesGeneradas.next()) {
                            throw new SQLException(
                                    "No se ha podido obtener el ID de la consulta insertada.");
                        }
                        idGenerado = clavesGeneradas.getInt(1);
                    }
                }

                insertarTratamientos(conexion, idGenerado,
                        consulta.getTratamientos());

                conexion.commit();
                consulta.setIdConsulta(idGenerado);
                return idGenerado;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza una consulta pendiente y sustituye sus tratamientos asociados.
     * Toda la operacion se realiza en una transaccion.
     *
     * @param consulta consulta con los datos modificados
     * @return 1 si se actualiza; 0 si no existe o no esta pendiente
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int actualizar(Consulta consulta) throws SQLException {

        validarConsulta(consulta);

        String sqlComprobar = "SELECT Estado FROM Consulta "
                + "WHERE ID_Consulta = ? FOR UPDATE";
        String sqlActualizar = "UPDATE Consulta SET ID_Cliente = ?, "
                + "ID_Mascota = ?, ID_Veterinario = ?, Fecha = ?, Nota = ? "
                + "WHERE ID_Consulta = ?";
        String sqlEliminarTratamientos =
                "DELETE FROM Consulta_Tratamiento WHERE ID_Consulta = ?";

        try (Connection conexion = Conexion.conectar()) {

            conexion.setAutoCommit(false);

            try {
                String estadoActual;

                try (PreparedStatement sentencia =
                        conexion.prepareStatement(sqlComprobar)) {

                    sentencia.setInt(1, consulta.getIdConsulta());

                    try (ResultSet resultado = sentencia.executeQuery()) {
                        if (!resultado.next()) {
                            conexion.rollback();
                            return 0;
                        }
                        estadoActual = resultado.getString("Estado");
                    }
                }

                if (!"Pendiente".equals(estadoActual)) {
                    conexion.rollback();
                    return 0;
                }

                /*
                 * Se eliminan primero las asociaciones antiguas dentro de la
                 * transaccion. Asi el trigger que valida el cambio de fecha
                 * no calcula la duracion con los tratamientos anteriores.
                 * Las nuevas asociaciones volveran a validar el horario.
                 */
                try (PreparedStatement sentencia =
                        conexion.prepareStatement(sqlEliminarTratamientos)) {

                    sentencia.setInt(1, consulta.getIdConsulta());
                    sentencia.executeUpdate();
                }

                try (PreparedStatement sentencia =
                        conexion.prepareStatement(sqlActualizar)) {

                    sentencia.setInt(1, consulta.getIdCliente());
                    sentencia.setInt(2, consulta.getIdMascota());
                    sentencia.setInt(3, consulta.getIdVeterinario());
                    sentencia.setTimestamp(4, Timestamp.valueOf(consulta.getFecha()));

                    if (consulta.getNota() == null) {
                        sentencia.setNull(5, Types.VARCHAR);
                    } else {
                        sentencia.setString(5, consulta.getNota());
                    }

                    sentencia.setInt(6, consulta.getIdConsulta());
                    sentencia.executeUpdate();
                }

                insertarTratamientos(conexion, consulta.getIdConsulta(),
                        consulta.getTratamientos());

                conexion.commit();
                return 1;

            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }

    /**
     * Cambia el estado de una consulta que se encuentre Pendiente.
     *
     * @param idConsulta identificador de la consulta
     * @param estado nuevo estado: Asistida o No asistida
     * @return numero de filas modificadas
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int actualizarEstado(int idConsulta, String estado) throws SQLException {

        if (!"Asistida".equals(estado) && !"No asistida".equals(estado)) {
            throw new IllegalArgumentException(
                    "El estado debe ser Asistida o No asistida.");
        }

        String sql = "UPDATE Consulta SET Estado = ? "
                + "WHERE ID_Consulta = ? AND Estado = 'Pendiente'";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, estado);
            sentencia.setInt(2, idConsulta);

            return sentencia.executeUpdate();
        }
    }

    /**
     * Asigna los datos de la consulta a los parametros del INSERT.
     */
    private void establecerDatos(PreparedStatement sentencia, Consulta consulta)
            throws SQLException {

        sentencia.setInt(1, consulta.getIdCliente());
        sentencia.setInt(2, consulta.getIdMascota());
        sentencia.setInt(3, consulta.getIdVeterinario());
        sentencia.setTimestamp(4, Timestamp.valueOf(consulta.getFecha()));

        if (consulta.getNota() == null) {
            sentencia.setNull(5, Types.VARCHAR);
        } else {
            sentencia.setString(5, consulta.getNota());
        }

        sentencia.setString(6, consulta.getEstado() == null
                ? "Pendiente" : consulta.getEstado());
    }

    /**
     * Inserta las relaciones entre una consulta y sus tratamientos.
     */
    private void insertarTratamientos(Connection conexion, int idConsulta,
            ArrayList<Tratamiento> tratamientos) throws SQLException {

        String sql = "INSERT INTO Consulta_Tratamiento "
                + "(ID_Consulta, ID_Tratamiento) VALUES (?, ?)";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            for (Tratamiento tratamiento : tratamientos) {
                sentencia.setInt(1, idConsulta);
                sentencia.setInt(2, tratamiento.getIdTratamiento());
                sentencia.addBatch();
            }

            sentencia.executeBatch();
        }
    }

    /**
     * Agrupa las filas del resultado para no duplicar consultas cuando
     * tienen asociados varios tratamientos.
     */
    private ArrayList<Consulta> agruparConsultas(ResultSet resultado,
            boolean incluirIdentificadores) throws SQLException {

        Map<Integer, Consulta> consultasAgrupadas = new LinkedHashMap<>();

        while (resultado.next()) {

            int idConsulta = resultado.getInt("ID_Consulta");
            Consulta consulta = consultasAgrupadas.get(idConsulta);

            if (consulta == null) {
                consulta = crearConsulta(resultado, incluirIdentificadores);
                consultasAgrupadas.put(idConsulta, consulta);
            }

            if (resultado.getObject("ID_Tratamiento") != null) {
                consulta.getTratamientos().add(
                        crearTratamiento(resultado, incluirIdentificadores));
            }
        }

        return new ArrayList<>(consultasAgrupadas.values());
    }

    /**
     * Construye una consulta a partir de la fila actual del ResultSet.
     */
    private Consulta crearConsulta(ResultSet resultado,
            boolean incluirIdentificadores) throws SQLException {

        Consulta consulta = new Consulta();
        consulta.setIdConsulta(resultado.getInt("ID_Consulta"));

        if (incluirIdentificadores) {
            consulta.setIdCliente(resultado.getInt("ID_Cliente"));
            consulta.setIdMascota(resultado.getInt("ID_Mascota"));
            consulta.setIdVeterinario(resultado.getInt("ID_Veterinario"));
            consulta.setNombreMascota(resultado.getString("Nombre_Mascota"));
        }

        Timestamp fecha = resultado.getTimestamp("Fecha");
        consulta.setFecha(fecha == null ? null : fecha.toLocalDateTime());
        consulta.setNota(resultado.getString("Nota"));
        consulta.setEstado(resultado.getString("Estado"));
        consulta.setNombreVeterinario(
                resultado.getString("Nombre_Veterinario"));
        consulta.setApellidosVeterinario(
                resultado.getString("Apellidos_Veterinario"));

        return consulta;
    }

    /**
     * Construye un tratamiento a partir de la fila actual del ResultSet.
     */
    private Tratamiento crearTratamiento(ResultSet resultado,
            boolean incluirIdentificadores) throws SQLException {

        String nombre = incluirIdentificadores
                ? resultado.getString("Nombre_Tratamiento")
                : resultado.getString("Tratamiento");

        return new Tratamiento(
                resultado.getInt("ID_Tratamiento"),
                resultado.getString("Categoria"),
                nombre,
                resultado.getBigDecimal("Precio"),
                resultado.getInt("Duracion"),
                incluirIdentificadores
                        ? resultado.getString("Descripcion") : null,
                incluirIdentificadores
                        ? resultado.getString("Material_Necesario") : null
        );
    }

    /**
     * Comprueba los datos minimos necesarios antes de guardar una consulta.
     */
    private void validarConsulta(Consulta consulta) {

        if (consulta == null) {
            throw new IllegalArgumentException("La consulta no puede ser null.");
        }

        if (consulta.getFecha() == null) {
            throw new IllegalArgumentException("La fecha de la consulta es obligatoria.");
        }

        if (consulta.getTratamientos() == null
                || consulta.getTratamientos().isEmpty()) {
            throw new IllegalArgumentException(
                    "La consulta debe tener al menos un tratamiento.");
        }

        for (Tratamiento tratamiento : consulta.getTratamientos()) {
            if (tratamiento == null || tratamiento.getIdTratamiento() <= 0) {
                throw new IllegalArgumentException(
                        "Todos los tratamientos deben tener un identificador valido.");
            }
        }
    }
}
