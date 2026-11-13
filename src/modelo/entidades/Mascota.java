package modelo.entidades;

import java.time.LocalDate;
import java.time.Period;

/**
 * Representa a una mascota asociada a un cliente.
 */
public class Mascota {

    private int idMascota;
    private int idCliente;
    private String nombre;
    private String raza;
    private LocalDate fechaNacimiento;
    private LocalDate fechaDefuncion;
    private String numChip;
    private double peso;
    private String estado;

    public Mascota() {
    }

    public Mascota(int idCliente, String nombre, String raza,
                   LocalDate fechaNacimiento, LocalDate fechaDefuncion,
                   String numChip, double peso, String estado) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.raza = raza;
        this.fechaNacimiento = fechaNacimiento;
        this.fechaDefuncion = fechaDefuncion;
        this.numChip = numChip;
        this.peso = peso;
        this.estado = estado;
    }

    public Mascota(int idMascota, int idCliente, String nombre, String raza,
                   LocalDate fechaNacimiento, LocalDate fechaDefuncion,
                   String numChip, double peso, String estado) {
        this(idCliente, nombre, raza, fechaNacimiento, fechaDefuncion,
                numChip, peso, estado);
        this.idMascota = idMascota;
    }

    public int getIdMascota() {
        return idMascota;
    }

    public void setIdMascota(int idMascota) {
        this.idMascota = idMascota;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRaza() {
        return raza;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public LocalDate getFechaDefuncion() {
        return fechaDefuncion;
    }

    public void setFechaDefuncion(LocalDate fechaDefuncion) {
        this.fechaDefuncion = fechaDefuncion;
    }

    public String getNumChip() {
        return numChip;
    }

    public void setNumChip(String numChip) {
        this.numChip = numChip;
    }

    public double getPeso() {
        return peso;
    }

    public void setPeso(double peso) {
        this.peso = peso;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Calcula la edad en años completos a partir de la fecha de nacimiento.
     */
    public int calcularEdad() {
        if (fechaNacimiento == null) {
            return 0;
        }

        return Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }
}
