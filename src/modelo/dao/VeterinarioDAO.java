package modelo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import modelo.conexion.Conexion;
import modelo.entidades.Veterinario;

/**
 * Realiza las operaciones de acceso a datos de la entidad Veterinario.
 */
public class VeterinarioDAO {

    /**
     * Obtiene todos los veterinarios registrados en la base de datos.
     *
     * @return lista de veterinarios
     * @throws SQLException si se produce un error de acceso a la base de datos
     */
    public ArrayList<Veterinario> listarTodos() throws SQLException {

        ArrayList<Veterinario> veterinarios = new ArrayList<>();

        String sql = "SELECT ID_Veterinario, DNI_NIF, Nombre, Apellidos, "
                + "Direccion, Telefono FROM Veterinario ORDER BY Apellidos, Nombre";

        try (Connection conexion = Conexion.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultado = sentencia.executeQuery()) {

            while (resultado.next()) {
                veterinarios.add(crearVeterinario(resultado));
            }
        }

        return veterinarios;
    }

    /**
     * Construye un objeto Veterinario a partir de la fila actual del ResultSet.
     */
    private Veterinario crearVeterinario(ResultSet resultado) throws SQLException {

        return new Veterinario(
                resultado.getInt("ID_Veterinario"),
                resultado.getString("DNI_NIF"),
                resultado.getString("Nombre"),
                resultado.getString("Apellidos"),
                resultado.getString("Direccion"),
                resultado.getString("Telefono")
        );
    }
}
