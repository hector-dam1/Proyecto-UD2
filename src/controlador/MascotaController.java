package controlador;

import java.sql.SQLException;
import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import modelo.dao.MascotaDAO;
import modelo.entidades.Cliente;
import modelo.entidades.Mascota;

/**
 * Controla el formulario de alta y modificacion de mascotas.
 */
public class MascotaController {

    @FXML
    private Label lblTitulo;

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtRaza;

    @FXML
    private DatePicker dpFechaNacimiento;

    @FXML
    private TextField txtNumChip;

    @FXML
    private TextField txtPeso;

    @FXML
    private DatePicker dpFechaDefuncion;

    @FXML
    private TextField txtEstado;

    private final MascotaDAO mascotaDAO = new MascotaDAO();

    private Cliente clienteActual;
    private Mascota mascotaActual;
    private boolean modoEdicion;
    private boolean guardado;

    /**
     * Configura el formulario para registrar una mascota nueva.
     */
    public void configurarNuevaMascota(Cliente cliente) {
        clienteActual = cliente;
        mascotaActual = null;
        modoEdicion = false;
        guardado = false;

        lblTitulo.setText("Nueva mascota");
        txtRaza.setDisable(false);
        dpFechaNacimiento.setDisable(false);
        txtNumChip.setDisable(false);
    }

    /**
     * Configura el formulario para modificar una mascota existente.
     */
    public void configurarEdicionMascota(Cliente cliente, Mascota mascota) {
        clienteActual = cliente;
        mascotaActual = mascota;
        modoEdicion = true;
        guardado = false;

        lblTitulo.setText("Modificar mascota");
        txtNombre.setText(mascota.getNombre());
        txtRaza.setText(mascota.getRaza());
        dpFechaNacimiento.setValue(mascota.getFechaNacimiento());
        txtNumChip.setText(mascota.getNumChip());
        txtPeso.setText(mascota.getPeso() == null
                ? "" : String.valueOf(mascota.getPeso()));
        dpFechaDefuncion.setValue(mascota.getFechaDefuncion());
        txtEstado.setText(mascota.getEstado());

        txtRaza.setDisable(true);
        dpFechaNacimiento.setDisable(true);
        txtNumChip.setDisable(true);
    }

    /**
     * Guarda una mascota nueva o los cambios permitidos de una existente.
     */
    @FXML
    private void guardarMascota() {

        if (clienteActual == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se ha recibido el cliente. Cierra el formulario y vuelve a abrirlo.");
            return;
        }

        String nombre = obtenerTexto(txtNombre);
        String raza = obtenerTexto(txtRaza);
        String numChip = obtenerTexto(txtNumChip);
        String estado = obtenerTexto(txtEstado);
        String textoPeso = obtenerTexto(txtPeso);

        LocalDate fechaNacimiento = dpFechaNacimiento.getValue();
        LocalDate fechaDefuncion = dpFechaDefuncion.getValue();

        if (nombre.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "El nombre de la mascota es obligatorio.");
            txtNombre.requestFocus();
            return;
        }

        if (!modoEdicion && raza.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "La raza de la mascota es obligatoria.");
            txtRaza.requestFocus();
            return;
        }

        if (!modoEdicion && fechaNacimiento == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "La fecha de nacimiento es obligatoria.");
            dpFechaNacimiento.requestFocus();
            return;
        }

        if (!modoEdicion && numChip.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Datos incompletos",
                    "El número de chip es obligatorio.");
            txtNumChip.requestFocus();
            return;
        }

        if (!modoEdicion && fechaNacimiento.isAfter(LocalDate.now())) {
            mostrarAlerta(Alert.AlertType.WARNING, "Fecha no válida",
                    "La fecha de nacimiento no puede ser futura.");
            dpFechaNacimiento.requestFocus();
            return;
        }

        if (fechaDefuncion != null) {
            if (fechaNacimiento != null && fechaDefuncion.isBefore(fechaNacimiento)) {
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha no válida",
                        "La fecha de defunción no puede ser anterior al nacimiento.");
                dpFechaDefuncion.requestFocus();
                return;
            }

            if (fechaDefuncion.isAfter(LocalDate.now())) {
                mostrarAlerta(Alert.AlertType.WARNING, "Fecha no válida",
                        "La fecha de defunción no puede ser futura.");
                dpFechaDefuncion.requestFocus();
                return;
            }
        }

        Double peso = null;
        if (!textoPeso.isEmpty()) {
            try {
                peso = Double.parseDouble(textoPeso.replace(',', '.'));
            } catch (NumberFormatException e) {
                mostrarAlerta(Alert.AlertType.WARNING, "Peso no válido",
                        "Introduce un peso numérico. Puedes utilizar coma o punto decimal.");
                txtPeso.requestFocus();
                return;
            }

            if (!Double.isFinite(peso) || peso <= 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Peso no válido",
                        "El peso debe ser un número mayor que cero.");
                txtPeso.requestFocus();
                return;
            }
        }

        try {
            if (modoEdicion) {
                mascotaActual.setNombre(nombre);
                mascotaActual.setFechaDefuncion(fechaDefuncion);
                mascotaActual.setPeso(peso);
                mascotaActual.setEstado(estado.isEmpty() ? null : estado);

                int filasModificadas = mascotaDAO.actualizar(mascotaActual);

                if (filasModificadas == 0) {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Sin cambios",
                            "No se ha modificado ningún registro.");
                } else {
                    mostrarAlerta(Alert.AlertType.INFORMATION, "Mascota actualizada",
                            "Los datos de la mascota se han guardado correctamente.");
                }
            } else {
                Mascota nuevaMascota = new Mascota(
                        clienteActual.getIdCliente(),
                        nombre,
                        raza,
                        fechaNacimiento,
                        fechaDefuncion,
                        numChip,
                        peso,
                        estado.isEmpty() ? null : estado
                );

                mascotaDAO.insertar(nuevaMascota);

                mostrarAlerta(Alert.AlertType.INFORMATION, "Mascota registrada",
                        "La mascota se ha registrado correctamente.");
            }

            guardado = true;
            cerrarVentana();

        } catch (SQLException e) {
            mostrarError("No se han podido guardar los datos de la mascota. "
                    + "Comprueba que el número de chip no esté registrado.", e);
        }
    }

    /**
     * Cancela la operacion y cierra el formulario.
     */
    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    /**
     * Comprueba si se puede cerrar la ventana sin perder cambios.
     *
     * @return true si se permite cerrar la ventana
     */
    public boolean confirmarCierre() {

        if (!hayCambiosSinGuardar()) {
            return true;
        }

        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
        alerta.setTitle("Descartar cambios");
        alerta.setHeaderText(null);
        alerta.setContentText("Hay cambios sin guardar. ¿Quieres descartarlos?");

        return alerta.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    /**
     * Comprueba si se han introducido o modificado datos en el formulario.
     */
    private boolean hayCambiosSinGuardar() {

        if (guardado) {
            return false;
        }

        if (!modoEdicion) {
            return !obtenerTexto(txtNombre).isEmpty()
                    || !obtenerTexto(txtRaza).isEmpty()
                    || dpFechaNacimiento.getValue() != null
                    || !obtenerTexto(txtNumChip).isEmpty()
                    || !obtenerTexto(txtPeso).isEmpty()
                    || dpFechaDefuncion.getValue() != null
                    || !obtenerTexto(txtEstado).isEmpty();
        }

        return !igual(obtenerTexto(txtNombre), mascotaActual.getNombre())
                || !igual(obtenerTexto(txtPeso), mascotaActual.getPeso() == null
                        ? "" : String.valueOf(mascotaActual.getPeso()))
                || !igual(dpFechaDefuncion.getValue(), mascotaActual.getFechaDefuncion())
                || !igual(obtenerTexto(txtEstado), mascotaActual.getEstado());
    }

    /**
     * Obtiene el texto de un campo sin espacios exteriores.
     */
    private String obtenerTexto(TextField campo) {
        return campo.getText() == null ? "" : campo.getText().trim();
    }

    private boolean igual(Object valor1, Object valor2) {
        if (valor1 == null) {
            return valor2 == null;
        }
        return valor1.equals(valor2 == null ? "" : valor2);
    }

    /**
     * Cierra la ventana del formulario.
     */
    private void cerrarVentana() {
        Stage ventana = (Stage) txtNombre.getScene().getWindow();
        ventana.close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }

    private void mostrarError(String mensaje, SQLException e) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(e.getMessage());
        alerta.showAndWait();
    }
}
