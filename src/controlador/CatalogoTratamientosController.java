package controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import modelo.dao.TratamientoDAO;
import modelo.entidades.Tratamiento;

/**
 * Controla la consulta y el filtrado del catalogo de tratamientos.
 */
public class CatalogoTratamientosController {

    @FXML
    private ComboBox<String> cmbCategoria;

    @FXML
    private TableView<Tratamiento> tablaTratamientos;

    @FXML
    private TableColumn<Tratamiento, String> colCategoria;

    @FXML
    private TableColumn<Tratamiento, String> colNombre;

    @FXML
    private TableColumn<Tratamiento, String> colDescripcion;

    @FXML
    private TableColumn<Tratamiento, String> colDuracion;

    @FXML
    private TableColumn<Tratamiento, String> colPrecio;

    private final TratamientoDAO tratamientoDAO = new TratamientoDAO();

    private final ObservableList<Tratamiento> tratamientos =
            FXCollections.observableArrayList();

    /**
     * Configura las columnas y carga el catalogo al abrir la pantalla.
     */
    @FXML
    private void initialize() {

        colCategoria.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getCategoria()));

        colNombre.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getNombre()));

        colDescripcion.setCellValueFactory(datos ->
                new SimpleStringProperty(datos.getValue().getDescripcion()));

        colDuracion.setCellValueFactory(datos ->
                new SimpleStringProperty(
                        datos.getValue().getDuracion() + " min"));

        colPrecio.setCellValueFactory(datos -> {
            BigDecimal precio = datos.getValue().getPrecio();
            return new SimpleStringProperty(
                    precio == null ? "" : precio.toPlainString() + " €");
        });

        cargarTratamientos();
    }

    /**
     * Obtiene todos los tratamientos y prepara las categorias disponibles.
     */
    private void cargarTratamientos() {

        try {
            ArrayList<Tratamiento> resultado = tratamientoDAO.listarTodos();
            tratamientos.setAll(resultado);
            tablaTratamientos.setItems(tratamientos);

            Set<String> categorias = new LinkedHashSet<>();
            for (Tratamiento tratamiento : tratamientos) {
                if (tratamiento.getCategoria() != null
                        && !tratamiento.getCategoria().isBlank()) {
                    categorias.add(tratamiento.getCategoria());
                }
            }

            cmbCategoria.setItems(
                    FXCollections.observableArrayList(categorias));
            cmbCategoria.getSelectionModel().clearSelection();

        } catch (SQLException e) {
            mostrarError("No se ha podido cargar el catálogo de tratamientos.", e);
        }
    }

    /**
     * Muestra los tratamientos de la categoria seleccionada.
     */
    @FXML
    private void filtrarCategoria() {

        String categoria = cmbCategoria.getValue();

        if (categoria == null || categoria.isBlank()) {
            tablaTratamientos.setItems(tratamientos);
            return;
        }

        ObservableList<Tratamiento> filtrados =
                FXCollections.observableArrayList();

        for (Tratamiento tratamiento : tratamientos) {
            if (categoria.equals(tratamiento.getCategoria())) {
                filtrados.add(tratamiento);
            }
        }

        tablaTratamientos.setItems(filtrados);
    }

    /**
     * Vuelve a mostrar todos los tratamientos del catalogo.
     */
    @FXML
    private void mostrarTodos() {
        cmbCategoria.getSelectionModel().clearSelection();
        tablaTratamientos.setItems(tratamientos);
    }

    /**
     * Regresa a la pantalla principal de identificacion.
     */
    @FXML
    private void volverInicio() {

        try {
            FXMLLoader cargador = new FXMLLoader(
                    getClass().getResource("/vista/Identificacion.fxml"));
            Parent contenido = cargador.load();

            Stage ventana = (Stage) tablaTratamientos.getScene().getWindow();
            ventana.setTitle("Identificación");
            ventana.setScene(new Scene(contenido));

        } catch (IOException | NullPointerException e) {
            mostrarError("No se ha podido volver a la pantalla de identificación.", e);
        }
    }

    /**
     * Muestra un mensaje cuando se produce un error al consultar los datos.
     */
    private void mostrarError(String mensaje, Exception e) {

        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(e.getMessage());
        alerta.showAndWait();
    }
}
