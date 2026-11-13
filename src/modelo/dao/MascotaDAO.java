package modelo.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import modelo.conexion.Conexion;
import modelo.entidades.Mascota;

/**
 * Realiza las operaciones de acceso a datos de la entidad Mascota.
 */
public class MascotaDAO {

    /**
     * Obtiene las mascotas asociadas a un cliente mediante el procedimiento
     * almacenado sp_mascotas_de_cliente.
     *
     * @param idCliente identificador del cliente
     * @return lista de mascotas del cliente
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public ArrayList<Mascota> listarPorCliente(int idCliente) throws SQLException {

        ArrayList<Mascota> mascotas = new ArrayList<>();
        String sql = "{CALL sp_mascotas_de_cliente(?)}";

        try (Connection conexion = Conexion.conectar();
             CallableStatement sentencia = conexion.prepareCall(sql)) {

            sentencia.setInt(1, idCliente);

            try (ResultSet resultado = sentencia.executeQuery()) {

                while (resultado.next()) {
                    mascotas.add(crearMascota(resultado, idCliente));
                }
            }
        }

        return mascotas;
    }

    /**
     * Inserta una mascota y asigna al objeto el identificador generado.
     *
     * @param mascota mascota que se va a insertar
     * @return identificador generado en la base de datos
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int insertar(Mascota mascota) throws SQLException {

        if (mascota == null) {
            throw new IllegalArgumentException("La mascota no puede ser null.");
        }

        String sql = "INSERT INTO Mascota "
                + "(ID_Cliente, Nombre, Raza, Fecha_Nacimiento, "
                + "Fecha_Defuncion, Num_Chip, Peso, Estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            establecerDatos(sentencia, mascota);

            sentencia.executeUpdate();

            try (ResultSet clavesGeneradas = sentencia.getGeneratedKeys()) {

                if (clavesGeneradas.next()) {
                    int idGenerado = clavesGeneradas.getInt(1);
                    mascota.setIdMascota(idGenerado);
                    return idGenerado;
                }
            }
        }

        throw new SQLException("No se ha podido obtener el ID de la mascota insertada.");
    }

    /**
     * Actualiza los campos que pueden modificarse despues del registro:
     * nombre, fecha de defuncion, peso y estado.
     *
     * @param mascota mascota con los datos actualizados
     * @return numero de filas modificadas
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int actualizar(Mascota mascota) throws SQLException {

        if (mascota == null) {
            throw new IllegalArgumentException("La mascota no puede ser null.");
        }

        String sql = "UPDATE Mascota SET Nombre = ?, Fecha_Defuncion = ?, "
                + "Peso = ?, Estado = ? WHERE ID_Mascota = ?";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            sentencia.setString(1, mascota.getNombre());

            if (mascota.getFechaDefuncion() == null) {
                sentencia.setNull(2, java.sql.Types.DATE);
            } else {
                sentencia.setDate(2, Date.valueOf(mascota.getFechaDefuncion()));
            }

            if (mascota.getPeso() <= 0) {
                sentencia.setNull(3, java.sql.Types.DECIMAL);
            } else {
                sentencia.setDouble(3, mascota.getPeso());
            }

            sentencia.setString(4, mascota.getEstado());
            sentencia.setInt(5, mascota.getIdMascota());

            return sentencia.executeUpdate();
        }
    }

    /**
     * Asigna los datos de una mascota a los parametros del INSERT.
     */
    private void establecerDatos(PreparedStatement sentencia, Mascota mascota)
            throws SQLException {

        sentencia.setInt(1, mascota.getIdCliente());
        sentencia.setString(2, mascota.getNombre());
        sentencia.setString(3, mascota.getRaza());
        sentencia.setDate(4, Date.valueOf(mascota.getFechaNacimiento()));

        if (mascota.getFechaDefuncion() == null) {
            sentencia.setNull(5, java.sql.Types.DATE);
        } else {
            sentencia.setDate(5, Date.valueOf(mascota.getFechaDefuncion()));
        }

        sentencia.setString(6, mascota.getNumChip());

        if (mascota.getPeso() <= 0) {
            sentencia.setNull(7, java.sql.Types.DECIMAL);
        } else {
            sentencia.setDouble(7, mascota.getPeso());
        }

        sentencia.setString(8, mascota.getEstado());
    }

    /**
     * Construye un objeto Mascota a partir de la fila actual del ResultSet.
     */
    private Mascota crearMascota(ResultSet resultado, int idCliente)
            throws SQLException {

        Date fechaNacimiento = resultado.getDate("Fecha_Nacimiento");
        Date fechaDefuncion = resultado.getDate("Fecha_Defuncion");

        double peso = resultado.getDouble("Peso");
        if (resultado.wasNull()) {
            peso = 0;
        }

        return new Mascota(
                resultado.getInt("ID_Mascota"),
                idCliente,
                resultado.getString("Nombre"),
                resultado.getString("Raza"),
                fechaNacimiento.toLocalDate(),
                fechaDefuncion == null ? null : fechaDefuncion.toLocalDate(),
                resultado.getString("Num_Chip"),
                peso,
                resultado.getString("Estado")
        );
    }
}
