package modelo.entidades;

/**
 * Representa a un cliente de la clinica veterinaria.
 */
public class Cliente {

    private int idCliente;
    private String dniNif;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String direccion;
    private String email;

    public Cliente() {
    }

    public Cliente(String dniNif, String nombre, String apellidos,
                   String telefono, String direccion, String email) {
        this.dniNif = dniNif;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.direccion = direccion;
        this.email = email;
    }

    public Cliente(int idCliente, String dniNif, String nombre,
                   String apellidos, String telefono,
                   String direccion, String email) {
        this(dniNif, nombre, apellidos, telefono, direccion, email);
        this.idCliente = idCliente;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getDniNif() {
        return dniNif;
    }

    public void setDniNif(String dniNif) {
        this.dniNif = dniNif;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
