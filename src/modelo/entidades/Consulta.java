package modelo.entidades;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Representa una consulta veterinaria y los tratamientos asociados.
 */
public class Consulta {

    private int idConsulta;
    private int idCliente;
    private int idMascota;
    private int idVeterinario;
    private LocalDateTime fecha;
    private String nota;
    private String estado;
    private ArrayList<Tratamiento> tratamientos;
    private String nombreMascota;
    private String nombreVeterinario;
    private String apellidosVeterinario;

    public Consulta() {
        tratamientos = new ArrayList<>();
    }

    public Consulta(int idCliente, int idMascota, int idVeterinario,
            LocalDateTime fecha, String nota, String estado,
            ArrayList<Tratamiento> tratamientos) {
        this.idCliente = idCliente;
        this.idMascota = idMascota;
        this.idVeterinario = idVeterinario;
        this.fecha = fecha;
        this.nota = nota;
        this.estado = estado;
        this.tratamientos = tratamientos == null
                ? new ArrayList<>() : new ArrayList<>(tratamientos);
    }

    public Consulta(int idConsulta, int idCliente, int idMascota,
            int idVeterinario, LocalDateTime fecha, String nota,
            String estado, ArrayList<Tratamiento> tratamientos) {
        this(idCliente, idMascota, idVeterinario, fecha, nota, estado,
                tratamientos);
        this.idConsulta = idConsulta;
    }

    public int getIdConsulta() {
        return idConsulta;
    }

    public void setIdConsulta(int idConsulta) {
        this.idConsulta = idConsulta;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdMascota() {
        return idMascota;
    }

    public void setIdMascota(int idMascota) {
        this.idMascota = idMascota;
    }

    public int getIdVeterinario() {
        return idVeterinario;
    }

    public void setIdVeterinario(int idVeterinario) {
        this.idVeterinario = idVeterinario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getNota() {
        return nota;
    }

    public void setNota(String nota) {
        this.nota = nota;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public ArrayList<Tratamiento> getTratamientos() {
        return tratamientos;
    }

    public void setTratamientos(ArrayList<Tratamiento> tratamientos) {
        this.tratamientos = tratamientos == null
                ? new ArrayList<>() : new ArrayList<>(tratamientos);
    }

    public String getNombreMascota() {
        return nombreMascota;
    }

    public void setNombreMascota(String nombreMascota) {
        this.nombreMascota = nombreMascota;
    }

    public String getNombreVeterinario() {
        return nombreVeterinario;
    }

    public void setNombreVeterinario(String nombreVeterinario) {
        this.nombreVeterinario = nombreVeterinario;
    }

    public String getApellidosVeterinario() {
        return apellidosVeterinario;
    }

    public void setApellidosVeterinario(String apellidosVeterinario) {
        this.apellidosVeterinario = apellidosVeterinario;
    }

    /**
     * Calcula el precio total sumando los tratamientos asociados.
     */
    public BigDecimal calcularPrecioTotal() {
        BigDecimal total = BigDecimal.ZERO;

        for (Tratamiento tratamiento : tratamientos) {
            if (tratamiento.getPrecio() != null) {
                total = total.add(tratamiento.getPrecio());
            }
        }

        return total;
    }

    /**
     * Calcula la duracion total sumando los minutos de los tratamientos.
     */
    public int calcularDuracionTotal() {
        int total = 0;

        for (Tratamiento tratamiento : tratamientos) {
            total += tratamiento.getDuracion();
        }

        return total;
    }
}
