package controlador;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import modelo.dao.ConsultaDAO;
import modelo.dao.MascotaDAO;
import modelo.dao.TratamientoDAO;
import modelo.entidades.Cliente;
import modelo.entidades.Consulta;
import modelo.entidades.Mascota;
import modelo.entidades.Tratamiento;

public class CitaNuevaController {
    @FXML private Label lblCliente, lblDisponibilidad, lblPrecioTotal, lblDuracionTotal;
    @FXML private ComboBox<Mascota> cmbMascota;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cmbHora;
    @FXML private TableView<Tratamiento> tablaTratamientos, tablaTratamientosSeleccionados;
    @FXML private TableColumn<Tratamiento,String> colTratamiento,colCategoria,colDuracion,colPrecio;
    @FXML private TableColumn<Tratamiento,String> colSeleccionadoNombre,colSeleccionadoPrecio,colSeleccionadoDuracion;
    @FXML private TextArea txtNota;
    private final ConsultaDAO consultaDAO=new ConsultaDAO();
    private final MascotaDAO mascotaDAO=new MascotaDAO();
    private final TratamientoDAO tratamientoDAO=new TratamientoDAO();
    private final ObservableList<Tratamiento> disponibles=FXCollections.observableArrayList();
    private final ObservableList<Tratamiento> seleccionados=FXCollections.observableArrayList();
    private Cliente clienteActual;
    private Consulta consultaActual;

    @FXML private void initialize() {
        colTratamiento.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNombre()));
        colCategoria.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getCategoria()));
        colDuracion.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDuracion()+" min"));
        colPrecio.setCellValueFactory(d->new SimpleStringProperty(precio(d.getValue().getPrecio())));
        colSeleccionadoNombre.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getNombre()));
        colSeleccionadoPrecio.setCellValueFactory(d->new SimpleStringProperty(precio(d.getValue().getPrecio())));
        colSeleccionadoDuracion.setCellValueFactory(d->new SimpleStringProperty(d.getValue().getDuracion()+" min"));
        tablaTratamientos.setItems(disponibles); tablaTratamientosSeleccionados.setItems(seleccionados);
        cmbMascota.setConverter(new StringConverter<Mascota>() {
            public String toString(Mascota m){return m==null?"":m.getNombre();}
            public Mascota fromString(String s){return null;}
        });
        dpFecha.valueProperty().addListener((o,a,n)->cargarHorarios());
        cmbMascota.valueProperty().addListener((o,a,n)->cargarHorarios());
    }

    public void configurarContexto(Cliente cliente, Consulta consulta) {
        clienteActual=cliente; consultaActual=consulta;
        if(cliente==null){alerta("No se ha recibido el cliente identificado.");return;}
        lblCliente.setText("Cliente: "+cliente.getNombre()+" "+cliente.getApellidos());
        try {
            ArrayList<Mascota> mascotas=mascotaDAO.listarPorCliente(cliente.getIdCliente());
            mascotas.removeIf(m->m.getFechaDefuncion()!=null);
            cmbMascota.setItems(FXCollections.observableArrayList(mascotas));
            disponibles.setAll(tratamientoDAO.listarTodos());
            if(consulta!=null){
                for(Mascota m:mascotas)if(m.getIdMascota()==consulta.getIdMascota())cmbMascota.setValue(m);
                dpFecha.setValue(consulta.getFecha().toLocalDate());
                txtNota.setText(consulta.getNota()); seleccionados.setAll(consulta.getTratamientos());
            }
            actualizarTotales(); cargarHorarios();
        }catch(Exception e){error("No se han podido cargar los datos de la cita.",e);}
    }

    @FXML private void cargarHorarios() {
        cmbHora.getItems().clear();
        if(dpFecha.getValue()==null||cmbMascota.getValue()==null){lblDisponibilidad.setText("Selecciona una mascota y una fecha.");return;}
        LocalDate fecha=dpFecha.getValue();
        if(fecha.isBefore(LocalDate.now())){lblDisponibilidad.setText("No se pueden reservar fechas anteriores.");return;}
        if(seleccionados.isEmpty()){lblDisponibilidad.setText("Selecciona tratamientos para calcular la duración.");return;}
        if(fecha.getDayOfWeek()==DayOfWeek.SUNDAY){lblDisponibilidad.setText("La clínica permanece cerrada los domingos.");return;}
        ArrayList<LocalTime> horas=new ArrayList<>();
        agregarHoras(horas,LocalTime.of(9,0),LocalTime.of(14,0),duracion());
        if(fecha.getDayOfWeek()!=DayOfWeek.SATURDAY)agregarHoras(horas,LocalTime.of(16,0),LocalTime.of(20,0),duracion());
        try{
            for(LocalTime h:horas){
                LocalDateTime inicio=LocalDateTime.of(fecha,h);
                if(inicio.isBefore(LocalDateTime.now()))continue;
                int excluir=consultaActual==null?0:consultaActual.getIdConsulta();
                if(consultaDAO.buscarVeterinarioDisponible(inicio,duracion(),excluir)>0)cmbHora.getItems().add(h.toString());
            }
            lblDisponibilidad.setText(cmbHora.getItems().isEmpty()?"No hay horarios disponibles.":"Horarios disponibles: "+cmbHora.getItems().size());
        }catch(Exception e){error("Error al consultar la disponibilidad.",e);}
    }

    private void agregarHoras(ArrayList<LocalTime> horas,LocalTime inicio,LocalTime cierre,int minutos){
        for(LocalTime h=inicio;!h.plusMinutes(minutos).isAfter(cierre);h=h.plusMinutes(30))horas.add(h);
    }

    @FXML private void agregarTratamiento(){
        Tratamiento t=tablaTratamientos.getSelectionModel().getSelectedItem();
        if(t==null){alerta("Selecciona un tratamiento.");return;}
        if(seleccionados.stream().anyMatch(x->x.getIdTratamiento()==t.getIdTratamiento())){alerta("Ese tratamiento ya está incluido.");return;}
        seleccionados.add(t);actualizarTotales();cargarHorarios();
    }
    @FXML private void eliminarTratamiento(){
        Tratamiento t=tablaTratamientosSeleccionados.getSelectionModel().getSelectedItem();
        if(t==null){alerta("Selecciona un tratamiento para eliminarlo.");return;}
        seleccionados.remove(t);actualizarTotales();cargarHorarios();
    }
    private int duracion(){int n=0;for(Tratamiento t:seleccionados)n+=t.getDuracion();return n;}
    private void actualizarTotales(){
        BigDecimal total=BigDecimal.ZERO;int minutos=0;
        for(Tratamiento t:seleccionados){if(t.getPrecio()!=null)total=total.add(t.getPrecio());minutos+=t.getDuracion();}
        lblPrecioTotal.setText(precio(total));lblDuracionTotal.setText(minutos+" minutos");
    }

    @FXML private void guardarCita(){
        Mascota mascota=cmbMascota.getValue();
        if(clienteActual==null||mascota==null||dpFecha.getValue()==null||cmbHora.getValue()==null||seleccionados.isEmpty()){
            alerta("Completa la mascota, tratamientos, fecha y hora.");return;
        }
        try{
            LocalDateTime fecha=LocalDateTime.of(dpFecha.getValue(),LocalTime.parse(cmbHora.getValue()));
            int excluir=consultaActual==null?0:consultaActual.getIdConsulta();
            int veterinario=consultaDAO.buscarVeterinarioDisponible(fecha,duracion(),excluir);
            if(veterinario<=0){alerta("El horario ya no está disponible. Vuelve a consultar.");cargarHorarios();return;}
            Consulta c=consultaActual==null?new Consulta():consultaActual;
            c.setIdCliente(clienteActual.getIdCliente());c.setIdMascota(mascota.getIdMascota());
            c.setIdVeterinario(veterinario);c.setFecha(fecha);
            c.setNota(txtNota.getText()==null||txtNota.getText().isBlank()?null:txtNota.getText().trim());
            c.setEstado("Pendiente");c.setTratamientos(new ArrayList<>(seleccionados));
            if(consultaActual==null)consultaDAO.insertar(c);
            else if(consultaDAO.actualizar(c)==0){alerta("La cita ya no está pendiente y no se puede modificar.");return;}
            informacion(consultaActual==null?"Cita guardada correctamente.":"Cita modificada correctamente.");
            volverACitas();
        }catch(Exception e){error("No se ha podido guardar la cita.",e);}
    }
    @FXML private void cancelar(){volverACitas();}
    private void volverACitas(){
        try{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/vista/Citas.fxml"));Parent root=loader.load();
            CitasController controller=loader.getController();controller.configurarCliente(clienteActual);
            Stage stage=(Stage)tablaTratamientos.getScene().getWindow();stage.setTitle("Citas");stage.setScene(new Scene(root));
        }catch(IOException|NullPointerException e){error("No se ha podido volver a las citas.",e);}
    }
    private String precio(BigDecimal p){return p==null?"":p.toPlainString()+" €";}
    private void alerta(String m){Alert a=new Alert(Alert.AlertType.WARNING);a.setTitle("Nueva cita");a.setHeaderText(null);a.setContentText(m);a.showAndWait();}
    private void informacion(String m){Alert a=new Alert(Alert.AlertType.INFORMATION);a.setTitle("Cita guardada");a.setHeaderText(null);a.setContentText(m);a.showAndWait();}
    private void error(String m,Exception e){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle("Error");a.setHeaderText(m);a.setContentText(e.getMessage());a.showAndWait();}
}