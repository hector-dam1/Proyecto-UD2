package modelo.dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import modelo.conexion.Conexion;
import modelo.entidades.Tratamiento;

/**
 * Realiza las operaciones de consulta de datos de la entidad Tratamiento.
 * El catalogo se consulta desde la aplicacion, pero su mantenimiento se
 * realiza directamente mediante SQL.
 */
public class TratamientoDAO {

    /**
     * Obtiene todos los tratamientos mediante el procedimiento almacenado
     * sp_listar_tratamientos.
     *
     * @return lista de tratamientos disponibles
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public ArrayList<Tratamiento> listarTodos() throws SQLException {

        ArrayList<Tratamiento> tratamientos = new ArrayList<>();
        String sql = "{CALL sp_listar_tratamientos()}";

        try (Connection conexion = Conexion.conectar();
             CallableStatement sentencia = conexion.prepareCall(sql);
             ResultSet resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                tratamientos.add(crearTratamiento(resultado));
            }
        }

        return tratamientos;
    }

    /**
     * Construye un objeto Tratamiento a partir de la fila actual del ResultSet.
     */
    private Tratamiento crearTratamiento(ResultSet resultado) throws SQLException {

        return new Tratamiento(
                resultado.getInt("ID_Tratamiento"),
                resultado.getString("Categoria"),
                resultado.getString("Nombre"),
                resultado.getBigDecimal("Precio"),
                resultado.getInt("Duracion"),
                resultado.getString("Descripcion"),
                resultado.getString("Material_Necesario")
        );
    }
}
