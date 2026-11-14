
package modelo.conexion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Conexion {

    private static final String URL =
            "jdbc:mysql://localhost:3306/proyecto_ud2";

    public static Connection conectar() throws SQLException {

        String usuario = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (usuario == null || password == null) {
            throw new SQLException(
                "No se han configurado las credenciales de MySQL."
            );
        }

        return DriverManager.getConnection(URL, usuario, password);
    }
}
