package controlador;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import modelo.dao.ClienteDAO;
import modelo.dao.MascotaDAO;
import modelo.entidades.Cliente;
import modelo.entidades.Mascota;

/**
 * Controla la pantalla de identificacion del cliente.
 */
public class IdentificacionController {

    @FXML
    private TextField txtDniNif;

    @FXML
    private TextField txtDniCliente;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtApellidos;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtDireccion;

    @FXML
    private TableView<Mascota> tablaMascotas;

    @FXML
    private TableColumn<Mascota, String> colNombreMascota;

    @FXML
    private TableColumn<Mascota, String> colRaza;

    @FXML
    private TableColumn<Mascota, Number> colEdad;

    @FXML
    private TableColumn<Mascota, String> colChip;

    @FXML
    private TableColumn<Mascota, String> colPeso;

    @FXML
    private TableColumn<Mascota, String> colEstadoMascota;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MascotaDAO mascotaDAO = new MascotaDAO();

    private Cliente clienteActual;

    /**
     * Configura las columnas de la tabla cuando JavaFX carga el FXML.
     */
    @FXML
    private void initialize() {

        colNombreMascota.setCellValueFactory(
                datos -> new SimpleStringProperty(datos.getValue().getNombre()));

        colRaza.setCellValueFactory(
                datos -> new SimpleStringProperty(datos.getValue().getRaza()));

        colEdad.setCellValueFactory(
                datos -> new SimpleIntegerProperty(datos.getValue().calcularEdad()));

        colChip.setCellValueFactory(
                datos -> new SimpleStringProperty(datos.getValue().getNumChip()));

        colPeso.setCellValueFactory(datos -> {
            Double peso = datos.getValue().getPeso();
            return new SimpleStringProperty(
                    peso == null ? "" : String.valueOf(peso));
        });

        colEstadoMascota.setCellValueFactory(
                datos -> new SimpleStringProperty(datos.getValue().getEstado()));

        actualizarEstadoPantalla(false);
    }

    /**
     * Busca un cliente por su DNI/NIF y carga sus mascotas.
     */
    @FXML
    private void buscarCliente() {

        String dniNif = txtDniNif.getText() == null
                ? "" : txtDniNif.getText().trim();

        if (dniNif.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Identificacion",
                    "Introduce el DNI/NIF del cliente.");
            txtDniNif.requestFocus();
            return;
        }

        try {
            Cliente cliente = clienteDAO.buscarPorDni(dniNif);

            if (cliente == null) {
                clienteActual = null;
                limpiarDatosCliente();
                tablaMascotas.getItems().clear();
                actualizarEstadoPantalla(false);

                mostrarAlerta(Alert.AlertType.INFORMATION, "Cliente no encontrado",
                        "No existe ningún cliente con ese DNI/NIF.");
                return;
            }

            clienteActual = cliente;
            mostrarDatosCliente(cliente);
            cargarMascotas();

        } catch (SQLException e) {
            mostrarError("No se ha podido buscar el cliente.", e);
        }
    }

    /**
     * Guarda los cambios realizados en los datos del cliente identificado.
     */
    @FXML
    private void guardarCliente() {

        if (clienteActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Guardar cliente",
                    "Primero debes buscar e identificar un cliente.");
            return;
        }

        if (!confirmar("Guardar cambios",
                "¿Quieres guardar los cambios realizados en los datos del cliente?")) {
            return;
        }

        String nombre = obtenerTexto(txtNombre);
        String apellidos = obtenerTexto(txtApellidos);
        String telefono = obtenerTexto(txtTelefono);
        String email = obtenerTexto(txtEmail);
        String direccion = obtenerTexto(txtDireccion);

        if (nombre.isEmpty() || apellidos.isEmpty() || telefono.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "El nombre, los apellidos y el teléfono son obligatorios.");
            return;
        }

        clienteActual.setNombre(nombre);
        clienteActual.setApellidos(apellidos);
        clienteActual.setTelefono(telefono);
        clienteActual.setEmail(email.isEmpty() ? null : email);
        clienteActual.setDireccion(direccion);

        try {
            int filasModificadas = clienteDAO.actualizar(clienteActual);

            if (filasModificadas > 0) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Cliente actualizado",
                        "Los datos del cliente se han guardado correctamente.");
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin cambios",
                        "No se ha actualizado ningún registro.");
            }

        } catch (SQLException e) {
            mostrarError("No se han podido guardar los datos del cliente.", e);
        }
    }

    /**
     * Abre el formulario para registrar una nueva mascota.
     */
    @FXML
    private void abrirNuevaMascota() {

        if (!comprobarClienteIdentificado()) {
            return;
        }

        abrirFormularioMascota("Nueva mascota", null);
    }

    /**
     * Abre el formulario para modificar la mascota seleccionada.
     */
    @FXML
    private void modificarMascota() {

        if (!comprobarClienteIdentificado()) {
            return;
        }

        Mascota mascotaSeleccionada = tablaMascotas.getSelectionModel()
                .getSelectedItem();

        if (mascotaSeleccionada == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Modificar mascota",
                    "Selecciona una mascota de la tabla.");
            return;
        }

        abrirFormularioMascota("Modificar mascota", mascotaSeleccionada);
    }

    /**
     * Abre la pantalla de citas.
     */
    @FXML
    private void abrirCitas() {
        if (comprobarClienteIdentificado()) {
            cambiarPantalla("Citas.fxml", "Citas");
        }
    }

    /**
     * Abre la pantalla de consultas y tratamientos de la mascota seleccionada.
     */
    @FXML
    private void abrirConsultasTratamientos() {

        if (!comprobarClienteIdentificado()) {
            return;
        }

        if (obtenerMascotaSeleccionada() == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Consultas y tratamientos",
                    "Selecciona una mascota de la tabla.");
            return;
        }

        cambiarPantalla("ConsultasTratamientos.fxml", "Consultas y tratamientos");
    }

    /**
     * Abre el formulario para crear una cita.
     */
    @FXML
    private void abrirCitaNueva() {
        if (comprobarClienteIdentificado()) {
            cambiarPantalla("CitaNueva.fxml", "Nueva cita");
        }
    }

    /**
     * Abre el catalogo de tratamientos.
     */
    @FXML
    private void abrirCatalogoTratamientos() {
        cambiarPantalla("CatalogoTratamientos.fxml", "Catalogo de tratamientos");
    }

    /**
     * Carga en la tabla las mascotas pertenecientes al cliente actual.
     */
    private void cargarMascotas() throws SQLException {

        ArrayList<Mascota> mascotas =
                mascotaDAO.listarPorCliente(clienteActual.getIdCliente());

        tablaMascotas.getItems().setAll(mascotas);
        actualizarEstadoPantalla(true);
    }

    /**
     * Muestra los datos del cliente en los campos del formulario.
     */
    private void mostrarDatosCliente(Cliente cliente) {

        txtDniCliente.setText(cliente.getDniNif());
        txtNombre.setText(cliente.getNombre());
        txtApellidos.setText(cliente.getApellidos());
        txtTelefono.setText(cliente.getTelefono());
        txtEmail.setText(cliente.getEmail());
        txtDireccion.setText(cliente.getDireccion());
    }

    /**
     * Limpia los campos que muestran los datos del cliente.
     */
    private void limpiarDatosCliente() {

        txtDniCliente.clear();
        txtNombre.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtDireccion.clear();
    }

    /**
     * Activa o desactiva los controles que necesitan un cliente identificado.
     */
    private void actualizarEstadoPantalla(boolean clienteIdentificado) {

        txtNombre.setDisable(!clienteIdentificado);
        txtApellidos.setDisable(!clienteIdentificado);
        txtTelefono.setDisable(!clienteIdentificado);
        txtEmail.setDisable(!clienteIdentificado);
        txtDireccion.setDisable(!clienteIdentificado);

        tablaMascotas.setDisable(!clienteIdentificado);
    }

    /**
     * Comprueba que exista un cliente identificado antes de continuar.
     */
    private boolean comprobarClienteIdentificado() {

        if (clienteActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Cliente no identificado",
                    "Primero debes buscar e identificar un cliente.");
            return false;
        }

        return true;
    }

    /**
     * Devuelve la mascota seleccionada en la tabla.
     */
    private Mascota obtenerMascotaSeleccionada() {
        return tablaMascotas.getSelectionModel().getSelectedItem();
    }

    /**
     * Abre el formulario de mascota como ventana modal.
     */
    private void abrirFormularioMascota(String titulo, Mascota mascota) {

        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource("/vista/Mascota.fxml"));
            Parent contenido = cargador.load();

            MascotaController controlador = cargador.getController();

            if (mascota == null) {
                controlador.configurarNuevaMascota(clienteActual);
            } else {
                controlador.configurarEdicionMascota(clienteActual, mascota);
            }

            Stage ventana = new Stage();
            ventana.setTitle(titulo);
            ventana.initModality(Modality.APPLICATION_MODAL);

            Window ventanaActual = tablaMascotas.getScene().getWindow();
            ventana.initOwner(ventanaActual);
            ventana.setScene(new Scene(contenido));
            ventana.showAndWait();

            cargarMascotas();

        } catch (IOException | SQLException e) {
            mostrarError("No se ha podido abrir el formulario de mascota.", e);
        }
    }

    /**
     * Carga otra pantalla FXML en la ventana actual.
     */
    private void cambiarPantalla(String archivoFXML, String titulo) {

        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource("/vista/" + archivoFXML));
            Parent contenido = cargador.load();

            Stage ventana = (Stage) tablaMascotas.getScene().getWindow();
            ventana.setTitle(titulo);
            ventana.setScene(new Scene(contenido));

        } catch (IOException | NullPointerException e) {
            mostrarError("No se ha podido abrir la pantalla " + archivoFXML + ".", e);
        }
    }

    /**
     * Obtiene el contenido de un campo de texto sin espacios exteriores.
     */
    private String obtenerTexto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    /**
     * Solicita confirmacion antes de realizar una operacion.
     */
    private boolean confirmar(String titulo, String mensaje) {

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);

        return alerta.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Muestra una alerta informativa o de validacion.
     */
    private void mostrarAlerta(Alert.AlertType tipo, String titulo,
            String mensaje) {

        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    /**
     * Muestra un error de acceso a datos o de carga de pantalla.
     */
    private void mostrarError(String mensaje, Exception e) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(e.getMessage());
        alerta.showAndWait();
    }
}
