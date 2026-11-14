package controlador;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import modelo.dao.ConsultaDAO;
import modelo.entidades.Cliente;
import modelo.entidades.Consulta;
import modelo.entidades.Mascota;
import modelo.entidades.Tratamiento;

public class ConsultasTratamientosController {

    @FXML private Label lblMascotaSeleccionada;
    @FXML private TableView<Consulta> tablaConsultas;
    @FXML private TableColumn<Consulta, String> colFechaConsulta, colNotaConsulta,
            colVeterinarioConsulta, colEstadoConsulta;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TableView<TratamientoHistorico> tablaHistorialTratamientos;
    @FXML private TableColumn<TratamientoHistorico, String> colFechaTratamiento,
            colNombreTratamiento, colCategoriaTratamiento, colVeterinarioTratamiento;

    private final ConsultaDAO consultaDAO = new ConsultaDAO();
    private final ObservableList<TratamientoHistorico> tratamientos =
            FXCollections.observableArrayList();
    private Cliente clienteActual;
    private Mascota mascotaActual;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        colFechaConsulta.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : d.getValue().getFecha().format(FORMATO_FECHA)));
        colNotaConsulta.setCellValueFactory(d -> new SimpleStringProperty(valor(d.getValue().getNota())));
        colVeterinarioConsulta.setCellValueFactory(d -> new SimpleStringProperty(nombreVeterinario(d.getValue())));
        colEstadoConsulta.setCellValueFactory(d -> new SimpleStringProperty(valor(d.getValue().getEstado())));
        colFechaTratamiento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().fecha));
        colNombreTratamiento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().nombre));
        colCategoriaTratamiento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().categoria));
        colVeterinarioTratamiento.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().veterinario));
    }

    public void configurarContexto(Cliente cliente, Mascota mascota) {
        clienteActual = cliente;
        mascotaActual = mascota;
        lblMascotaSeleccionada.setText(mascota == null ? "Mascota no seleccionada:"
                : "Historial de " + mascota.getNombre());
        cargarHistorial();
    }

    private void cargarHistorial() {
        if (mascotaActual == null) return;
        try {
            ArrayList<Consulta> consultas = consultaDAO.listarPorMascota(mascotaActual.getIdMascota());
            tablaConsultas.setItems(FXCollections.observableArrayList(consultas));
            tratamientos.clear();
            Set<String> categorias = new LinkedHashSet<>();
            for (Consulta consulta : consultas) {
                String fecha = consulta.getFecha() == null ? "" : consulta.getFecha().format(FORMATO_FECHA);
                String veterinario = nombreVeterinario(consulta);
                for (Tratamiento tratamiento : consulta.getTratamientos()) {
                    tratamientos.add(new TratamientoHistorico(fecha, tratamiento.getNombre(),
                            tratamiento.getCategoria(), veterinario));
                    if (tratamiento.getCategoria() != null) categorias.add(tratamiento.getCategoria());
                }
            }
            cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
            cmbCategoria.getSelectionModel().clearSelection();
            tablaHistorialTratamientos.setItems(tratamientos);
        } catch (Exception e) {
            mostrarError("No se ha podido cargar el historial de la mascota.", e);
        }
    }

    @FXML
    private void filtrarTratamientos() {
        String categoria = cmbCategoria.getValue();
        if (categoria == null || categoria.isBlank()) {
            tablaHistorialTratamientos.setItems(tratamientos);
            return;
        }
        ObservableList<TratamientoHistorico> filtrados = FXCollections.observableArrayList();
        for (TratamientoHistorico item : tratamientos) {
            if (categoria.equals(item.categoria)) filtrados.add(item);
        }
        tablaHistorialTratamientos.setItems(filtrados);
    }

    @FXML
    private void volverInicio() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/vista/Identificacion.fxml"));
            Stage stage = (Stage) tablaConsultas.getScene().getWindow();
            stage.setTitle("Identificación");
            stage.setScene(new Scene(root));
        } catch (IOException | NullPointerException e) {
            mostrarError("No se ha podido volver a identificación.", e);
        }
    }

    private String nombreVeterinario(Consulta c) {
        return (valor(c.getNombreVeterinario()) + " " + valor(c.getApellidosVeterinario())).trim();
    }
    private String valor(String s) { return s == null ? "" : s; }
    private void mostrarError(String mensaje, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Error"); a.setHeaderText(mensaje); a.setContentText(e.getMessage()); a.showAndWait();
    }

    public static class TratamientoHistorico {
        private final String fecha, nombre, categoria, veterinario;
        public TratamientoHistorico(String fecha, String nombre, String categoria, String veterinario) {
            this.fecha=fecha; this.nombre=nombre; this.categoria=categoria; this.veterinario=veterinario;
        }
    }
}