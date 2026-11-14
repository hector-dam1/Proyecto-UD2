package modelo.entidades;

/**
 * Representa los datos de un veterinario.
 */
public class Veterinario {

    private int idVeterinario;
    private String dniNif;
    private String nombre;
    private String apellidos;
    private String direccion;
    private String telefono;

    public Veterinario() {
    }

    public Veterinario(String dniNif, String nombre, String apellidos,
            String direccion, String telefono) {
        this.dniNif = dniNif;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public Veterinario(int idVeterinario, String dniNif, String nombre,
            String apellidos, String direccion, String telefono) {
        this.idVeterinario = idVeterinario;
        this.dniNif = dniNif;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.direccion = direccion;
        this.telefono = telefono;
    }

    public int getIdVeterinario() {
        return idVeterinario;
    }

    public void setIdVeterinario(int idVeterinario) {
        this.idVeterinario = idVeterinario;
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
}
