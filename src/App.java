import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import modelo.sesion.Sesion;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Sesion.limpiar();

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/vista/Identificacion.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("Gestión Veterinaria");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}