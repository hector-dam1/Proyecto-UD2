package modelo.entidades;

import java.math.BigDecimal;

/**
 * Representa un tratamiento disponible en el catalogo.
 */
public class Tratamiento {

    private int idTratamiento;
    private String categoria;
    private String nombre;
    private BigDecimal precio;
    private int duracion;
    private String descripcion;
    private String materialNecesario;

    public Tratamiento() {
    }

    public Tratamiento(String categoria, String nombre, BigDecimal precio,
            int duracion, String descripcion, String materialNecesario) {
        this.categoria = categoria;
        this.nombre = nombre;
        this.precio = precio;
        this.duracion = duracion;
        this.descripcion = descripcion;
        this.materialNecesario = materialNecesario;
    }

    public Tratamiento(int idTratamiento, String categoria, String nombre,
            BigDecimal precio, int duracion, String descripcion,
            String materialNecesario) {
        this.idTratamiento = idTratamiento;
        this.categoria = categoria;
        this.nombre = nombre;
        this.precio = precio;
        this.duracion = duracion;
        this.descripcion = descripcion;
        this.materialNecesario = materialNecesario;
    }

    public int getIdTratamiento() {
        return idTratamiento;
    }

    public void setIdTratamiento(int idTratamiento) {
        this.idTratamiento = idTratamiento;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMaterialNecesario() {
        return materialNecesario;
    }

    public void setMaterialNecesario(String materialNecesario) {
        this.materialNecesario = materialNecesario;
    }
}
