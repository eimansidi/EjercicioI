package com.example.eji;

import com.example.eji.model.Persona;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;

import java.util.Locale;

public class PersonasController implements Initializable {

    private ContextMenu contextMenu;

    private ResourceBundle resourceBundle;

    @FXML
    private Button btnAgregar;

    @FXML
    private Button btnModificar;

    @FXML
    private Button btnEliminar;

    @FXML
    private TableView<Persona> tableView;

    @FXML
    private TableColumn<Persona, String> nombre;

    @FXML
    private TableColumn<Persona, String> apellidos;

    @FXML
    private TableColumn<Persona, Integer> edad;

    @FXML
    private TextField txtFiltro;

    private ObservableList<Persona> listaPersonas = FXCollections.observableArrayList();

    private Connection connection;
    private final String db_url = "jdbc:mysql://database-1.cr60ewocg533.us-east-1.rds.amazonaws.com:3306/";
    private final String user = "admin";
    private final String password = "12345678";

    /**
     * Metodo que se ejecuta al iniciar la aplicacion, cargando el idioma, configurando las columnas
     * de la tabla y realizando la conexion con la base de datos.
     *
     * @param location La URL de la ubicacion del archivo FXML.
     * @param resources El recurso utilizado por la vista FXML.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cargarIdioma("es");

        // Configura los botones y columnas con texto desde el archivo de recursos
        btnAgregar.setText(resourceBundle.getString("agregar"));
        btnModificar.setText(resourceBundle.getString("modificar"));
        btnEliminar.setText(resourceBundle.getString("eliminar"));

        nombre.setText(resourceBundle.getString("nombre"));
        apellidos.setText(resourceBundle.getString("apellidos"));
        edad.setText(resourceBundle.getString("edad"));

        // Conexion a la base de datos y carga de los datos
        connection = conectarBaseDatos("personas");
        if (connection != null) {
            crearTablaPersonas();
            cargarDatosDesdeBaseDeDatos();
        }

        // Configuracion de las columnas de la tabla
        nombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        apellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        edad.setCellValueFactory(new PropertyValueFactory<>("edad"));

        tableView.setItems(listaPersonas);

        // Configuracion del menu contextual para las acciones de la tabla
        contextMenu = new ContextMenu();

        MenuItem modificarItem = new MenuItem("Modificar");
        modificarItem.setOnAction(event -> modificar(null));
        contextMenu.getItems().add(modificarItem);

        MenuItem eliminarItem = new MenuItem("Eliminar");
        eliminarItem.setOnAction(event -> eliminar(null));
        contextMenu.getItems().add(eliminarItem);

        tableView.setContextMenu(contextMenu);

        // Mostrar el menu contextual al hacer clic derecho en la tabla
        tableView.setOnMouseClicked(event -> {
            if (event.isPopupTrigger() || event.getButton() == MouseButton.SECONDARY) {
                Persona personaSeleccionada = tableView.getSelectionModel().getSelectedItem();
                if (personaSeleccionada != null) {
                    contextMenu.show(tableView, event.getScreenX(), event.getScreenY());
                }
            }
        });

    }

    /**
     * Metodo para conectar con la base de datos.
     *
     * @param dbName El nombre de la base de datos a conectar.
     * @return La conexion a la base de datos.
     */
    Connection conectarBaseDatos(String dbName) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(db_url + dbName, user, password);
            mostrarAlertaExito("Info", "Conexión exitosa a la base de datos: " + dbName);
        } catch (SQLException e) {
            if (e.getErrorCode() == 1049) {
                crearBaseDatos();
                conn = conectarBaseDatos(dbName);
            } else {
                mostrarAlertaError("Error de conexión", "No se pudo conectar a la base de datos.");
            }
        }
        return conn;
    }

    /**
     * Metodo para crear la base de datos si no existe.
     */
    private void crearBaseDatos() {
        try (Connection conn = DriverManager.getConnection(db_url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE DATABASE IF NOT EXISTS personas";
            stmt.executeUpdate(sql);
            System.out.println("Base de datos 'personas' creada o ya existía.");
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlertaError("Error de creación", "No se pudo crear la base de datos.");
        }
    }

    /**
     * Metodo para crear la tabla 'Persona' en la base de datos si no existe.
     */
    private void crearTablaPersonas() {
        if (!tablaExiste("Persona")) {
            String sqlCrearTabla = "CREATE TABLE IF NOT EXISTS Persona ("
                    + "id INT NOT NULL AUTO_INCREMENT, "
                    + "nombre VARCHAR(250) NULL DEFAULT NULL, "
                    + "apellidos VARCHAR(250) NULL DEFAULT NULL, "
                    + "edad INT NULL DEFAULT NULL, "
                    + "PRIMARY KEY (id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=latin1;";

            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(sqlCrearTabla);
                System.out.println("Tabla 'Persona' creada.");
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAlertaError("Error al crear la tabla", "No se pudo crear la tabla Persona.");
            }
        } else {
            System.out.println("La tabla 'Persona' ya existe.");
        }
    }

    /**
     * Verifica si una tabla existe en la base de datos.
     *
     * @param nombreTabla El nombre de la tabla a verificar.
     * @return true si la tabla existe, false si no.
     */
    private boolean tablaExiste(String nombreTabla) {
        try (ResultSet rs = connection.getMetaData().getTables(null, null, nombreTabla, null)) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlertaError("Error", "No se pudo verificar si la tabla " + nombreTabla + " existe.");
            return false;
        }
    }

    /**
     * Metodo para cargar los datos de la tabla Persona desde la base de datos.
     */
    private void cargarDatosDesdeBaseDeDatos() {
        String sql = "SELECT * FROM Persona";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                String apellidos = rs.getString("apellidos");
                int edad = rs.getInt("edad");
                Persona persona = new Persona(id, nombre, apellidos, edad);
                listaPersonas.add(persona);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlertaError("Error de carga", "No se pudieron cargar los datos de la base de datos.");
        }
    }

    /**
     * Metodo para filtrar los datos de la tabla por nombre.
     */
    @FXML
    public void filtrarPorNombre() {
        String filtro = txtFiltro.getText().toLowerCase();

        if (filtro.isEmpty()) {
            tableView.setItems(listaPersonas);
        } else {
            ObservableList<Persona> listaFiltrada = FXCollections.observableArrayList();
            for (Persona persona : listaPersonas) {
                if (persona.getNombre().toLowerCase().contains(filtro)) {
                    listaFiltrada.add(persona);
                }
            }
            tableView.setItems(listaFiltrada);
        }
    }

    /**
     * Abre una ventana para agregar una nueva persona.
     *
     * @param event El evento de accion.
     */
    @FXML
    void agregar(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("agregar.fxml"));
            Scene scene = new Scene(loader.load());

            AgregarController agregarController = loader.getController();
            agregarController.setMainController(this);
            agregarController.setModoModificar(false);

            Stage stage = new Stage();
            stage.setTitle("Nueva Persona");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Abre una ventana para modificar una persona seleccionada.
     *
     * @param event El evento de accion.
     */
    @FXML
    void modificar(ActionEvent event) {
        Persona personaSeleccionada = tableView.getSelectionModel().getSelectedItem();
        if (personaSeleccionada == null) {
            mostrarAlertaError("Error", "Debes seleccionar una persona para modificarla.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("agregar.fxml"));
            Scene scene = new Scene(loader.load());

            AgregarController agregarController = loader.getController();
            agregarController.setMainController(this);
            agregarController.setModoModificar(true);
            agregarController.llenarCampos(personaSeleccionada);

            Stage stage = new Stage();
            stage.setTitle("Editar persona");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Elimina una persona seleccionada de la base de datos y de la tabla.
     *
     * @param event El evento de accion.
     */
    @FXML
    void eliminar(ActionEvent event) {
        Persona personaSeleccionada = tableView.getSelectionModel().getSelectedItem();
        if (personaSeleccionada == null) {
            mostrarAlertaError("Error", "Debes seleccionar una persona para eliminar.");
            return;
        }

        String sql = "DELETE FROM Persona WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, personaSeleccionada.getId());
            pstmt.executeUpdate();
            tableView.getItems().remove(personaSeleccionada);
            mostrarAlertaExito("Info", "Persona eliminada correctamente");
        } catch (SQLException e) {
            e.printStackTrace();
            mostrarAlertaError("Error al eliminar", "No se pudo eliminar la persona de la base de datos.");
        }
    }

    /**
     * Agrega una persona a la tabla de la interfaz de usuario.
     *
     * @param persona La persona a agregar.
     */
    public void agregarPersonaTabla(Persona persona) {
        listaPersonas.add(persona);
        tableView.getItems().add(persona);
    }

    /**
     * Modifica una persona en la tabla de la interfaz de usuario.
     *
     * @param personaOriginal La persona original.
     * @param personaModificada La persona modificada.
     */
    public void modificarPersonaTabla(Persona personaOriginal, Persona personaModificada) {
        int indice = tableView.getItems().indexOf(personaOriginal);
        tableView.getItems().set(indice, personaModificada);
    }

    /**
     * Carga el idioma de la aplicacion.
     *
     * @param idioma El idioma a cargar.
     */
    private void cargarIdioma(String idioma) {
        Locale locale = new Locale(idioma);
        resourceBundle = ResourceBundle.getBundle("config", locale);
    }

    /**
     * Muestra una alerta de exito con el mensaje proporcionado.
     *
     * @param titulo El titulo de la alerta.
     * @param mensaje El mensaje que se mostrara en la alerta.
     */
    private void mostrarAlertaExito(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra una alerta de error con el mensaje proporcionado.
     *
     * @param titulo El titulo de la alerta.
     * @param mensaje El mensaje que se mostrara en la alerta.
     */
    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
