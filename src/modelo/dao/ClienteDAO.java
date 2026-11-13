package modelo.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import modelo.conexion.Conexion;
import modelo.entidades.Cliente;

/**
 * Realiza las operaciones de acceso a datos de la entidad Cliente.
 */
public class ClienteDAO {

    /**
     * Busca un cliente mediante el procedimiento almacenado
     * sp_buscar_cliente_por_dni.
     *
     * @param dniNif documento identificativo del cliente
     * @return el cliente encontrado o null si no existe
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public Cliente buscarPorDni(String dniNif) throws SQLException {

        String sql = "{CALL sp_buscar_cliente_por_dni(?)}";

        try (Connection conexion = Conexion.conectar();
             CallableStatement sentencia = conexion.prepareCall(sql)) {

            sentencia.setString(1, dniNif);

            try (ResultSet resultado = sentencia.executeQuery()) {

                if (resultado.next()) {
                    return crearCliente(resultado);
                }
            }
        }

        return null;
    }

    /**
     * Inserta un cliente y asigna al objeto el identificador generado.
     *
     * @param cliente cliente que se va a insertar
     * @return identificador generado en la base de datos
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int insertar(Cliente cliente) throws SQLException {

        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser null.");
        }

        String sql = "INSERT INTO Cliente "
                + "(DNI_NIF, Nombre, Apellidos, Telefono, Direccion, Email) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            establecerDatos(sentencia, cliente);

            sentencia.executeUpdate();

            try (ResultSet clavesGeneradas = sentencia.getGeneratedKeys()) {

                if (clavesGeneradas.next()) {
                    int idGenerado = clavesGeneradas.getInt(1);
                    cliente.setIdCliente(idGenerado);
                    return idGenerado;
                }
            }
        }

        throw new SQLException("No se ha podido obtener el ID del cliente insertado.");
    }

    /**
     * Actualiza los datos de un cliente identificado por su ID.
     *
     * @param cliente cliente con los datos actualizados
     * @return numero de filas modificadas
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public int actualizar(Cliente cliente) throws SQLException {

        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser null.");
        }

        String sql = "UPDATE Cliente SET DNI_NIF = ?, Nombre = ?, "
                + "Apellidos = ?, Telefono = ?, Direccion = ?, Email = ? "
                + "WHERE ID_Cliente = ?";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            establecerDatos(sentencia, cliente);
            sentencia.setInt(7, cliente.getIdCliente());

            return sentencia.executeUpdate();
        }
    }

    /**
     * Copia los datos del objeto Cliente en los seis parametros SQL.
     */
    private void establecerDatos(PreparedStatement sentencia, Cliente cliente)
            throws SQLException {

        sentencia.setString(1, cliente.getDniNif());
        sentencia.setString(2, cliente.getNombre());
        sentencia.setString(3, cliente.getApellidos());
        sentencia.setString(4, cliente.getTelefono());
        sentencia.setString(5, cliente.getDireccion());
        sentencia.setString(6, cliente.getEmail());
    }

    /**
     * Construye un objeto Cliente a partir de la fila actual del ResultSet.
     */
    private Cliente crearCliente(ResultSet resultado) throws SQLException {

        return new Cliente(
                resultado.getInt("ID_Cliente"),
                resultado.getString("DNI_NIF"),
                resultado.getString("Nombre"),
                resultado.getString("Apellidos"),
                resultado.getString("Telefono"),
                resultado.getString("Direccion"),
                resultado.getString("Email")
        );
    }
}
