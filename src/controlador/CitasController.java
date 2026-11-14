package controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import modelo.dao.ConsultaDAO;
import modelo.entidades.Cliente;
import modelo.entidades.Consulta;

public class CitasController {

    @FXML private Label lblMascotaSeleccionada;
    @FXML private TableView<Consulta> tablaCitasPendientes;
    @FXML private TableColumn<Consulta, String> colFechaPendiente, colMascotaPendiente,
            colTratamientosPendiente, colVeterinarioPendiente, colPrecioPendiente, colEstadoPendiente;
    @FXML private TableView<Consulta> tablaHistorialCitas;
    @FXML private TableColumn<Consulta, String> colFechaHistorial, colMascotaHistorial,
            colTratamientosHistorial, colVeterinarioHistorial, colPrecioHistorial, colEstadoHistorial;

    private final ConsultaDAO consultaDAO = new ConsultaDAO();
    private Cliente clienteActual;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        configurarColumnas(tablaCitasPendientes, colFechaPendiente, colMascotaPendiente,
                colTratamientosPendiente, colVeterinarioPendiente, colPrecioPendiente, colEstadoPendiente);
        configurarColumnas(tablaHistorialCitas, colFechaHistorial, colMascotaHistorial,
                colTratamientosHistorial, colVeterinarioHistorial, colPrecioHistorial, colEstadoHistorial);
    }

    public void configurarCliente(Cliente cliente) {
        clienteActual = cliente;
        lblMascotaSeleccionada.setText(cliente == null ? "Cliente no seleccionado:"
                : "Citas de " + cliente.getNombre() + " " + cliente.getApellidos());
        cargarCitas();
    }

    private void configurarColumnas(TableView<Consulta> tabla,
            TableColumn<Consulta, String> fecha, TableColumn<Consulta, String> mascota,
            TableColumn<Consulta, String> tratamientos, TableColumn<Consulta, String> veterinario,
            TableColumn<Consulta, String> precio, TableColumn<Consulta, String> estado) {
        fecha.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getFecha() == null ? "" : d.getValue().getFecha().format(FORMATO_FECHA)));
        mascota.setCellValueFactory(d -> new SimpleStringProperty(valor(d.getValue().getNombreMascota())));
        tratamientos.setCellValueFactory(d -> new SimpleStringProperty(nombresTratamientos(d.getValue())));
        veterinario.setCellValueFactory(d -> new SimpleStringProperty(nombreVeterinario(d.getValue())));
        precio.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().calcularPrecioTotal().toPlainString() + " €"));
        estado.setCellValueFactory(d -> new SimpleStringProperty(valor(d.getValue().getEstado())));
    }

    private void cargarCitas() {
        if (clienteActual == null) return;
        try {
            ArrayList<Consulta> consultas = consultaDAO.listarPorCliente(clienteActual.getIdCliente());
            ArrayList<Consulta> pendientes = new ArrayList<>();
            ArrayList<Consulta> historial = new ArrayList<>();
            for (Consulta consulta : consultas) {
                if ("Pendiente".equals(consulta.getEstado())) pendientes.add(consulta);
                else historial.add(consulta);
            }
            tablaCitasPendientes.setItems(FXCollections.observableArrayList(pendientes));
            tablaHistorialCitas.setItems(FXCollections.observableArrayList(historial));
        } catch (Exception e) {
            mostrarError("No se han podido cargar las citas.", e);
        }
    }

    @FXML private void completarCita() { cambiarEstadoSeleccionada("Asistida"); }
    @FXML private void cancelarCita() { cambiarEstadoSeleccionada("No asistida"); }

    private void cambiarEstadoSeleccionada(String estado) {
        Consulta seleccionada = obtenerCitaSeleccionada();
        if (seleccionada == null) {
            mostrarAlerta("Selecciona una cita pendiente para continuar.");
            return;
        }
        try {
            if (consultaDAO.actualizarEstado(seleccionada.getIdConsulta(), estado) > 0) cargarCitas();
            else mostrarAlerta("La cita ya no está pendiente o ha sido modificada.");
        } catch (Exception e) {
            mostrarError("No se ha podido actualizar el estado de la cita.", e);
        }
    }

    @FXML
    private void modificarCita() {
        Consulta seleccionada = obtenerCitaSeleccionada();
        if (seleccionada == null) {
            mostrarAlerta("Selecciona una cita pendiente para modificarla.");
            return;
        }
        abrirFormularioCita(seleccionada);
    }

    @FXML private void abrirCitaNueva() { abrirFormularioCita(null); }

    private void abrirFormularioCita(Consulta consulta) {
        if (clienteActual == null) {
            mostrarAlerta("No hay ningún cliente identificado.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/vista/CitaNueva.fxml"));
            Parent root = loader.load();
            CitaNuevaController controller = loader.getController();
            controller.configurarContexto(clienteActual, consulta);
            Stage stage = (Stage) tablaCitasPendientes.getScene().getWindow();
            stage.setTitle(consulta == null ? "Nueva cita" : "Modificar cita");
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            mostrarError("No se ha podido abrir el formulario de cita.", e);
        }
    }

    private Consulta obtenerCitaSeleccionada() {
        Consulta seleccionada = tablaCitasPendientes.getSelectionModel().getSelectedItem();
        return seleccionada != null ? seleccionada : tablaHistorialCitas.getSelectionModel().getSelectedItem();
    }

    @FXML private void volverInicio() { cambiarPantalla("/vista/Identificacion.fxml", "Identificación"); }

    public void volverACitas() { cargarCitas(); }

    private void cambiarPantalla(String recurso, String titulo) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(recurso));
            Stage stage = (Stage) tablaCitasPendientes.getScene().getWindow();
            stage.setTitle(titulo);
            stage.setScene(new Scene(root));
        } catch (IOException | NullPointerException e) {
            mostrarError("No se ha podido cambiar de pantalla.", e);
        }
    }

    private String nombresTratamientos(Consulta consulta) {
        StringBuilder texto = new StringBuilder();
        consulta.getTratamientos().forEach(t -> {
            if (texto.length() > 0) texto.append(", ");
            texto.append(t.getNombre());
        });
        return texto.toString();
    }

    private String nombreVeterinario(Consulta consulta) {
        return (valor(consulta.getNombreVeterinario()) + " " +
                valor(consulta.getApellidosVeterinario())).trim();
    }
    private String valor(String texto) { return texto == null ? "" : texto; }
    private void mostrarAlerta(String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle("Citas"); a.setHeaderText(null); a.setContentText(mensaje); a.showAndWait();
    }
    private void mostrarError(String mensaje, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle("Error"); a.setHeaderText(mensaje); a.setContentText(e.getMessage()); a.showAndWait();
    }
}