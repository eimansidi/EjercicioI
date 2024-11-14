package com.example.eji;

import com.example.eji.model.Persona;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgregarController {

    @FXML
    private TextField txtNombre;

    @FXML
    private TextField txtApellidos;

    @FXML
    private TextField txtEdad;

    @FXML
    private Button btnGuardar;

    @FXML
    private Button btnCancelar;

    private PersonasController helloController;
    private Persona personaOriginal;
    private boolean modoModificar;

    /**
     * Establece el controlador principal para la comunicacion con la ventana principal.
     *
     * @param helloController El controlador principal de la aplicacion.
     */
    public void setMainController(PersonasController helloController) {
        this.helloController = helloController;
    }

    /**
     * Establece el modo de modificacion o agregado de persona.
     *
     * @param modificar Si es verdadero, activa el modo modificar, si es falso, activa el modo agregar.
     */
    public void setModoModificar(boolean modificar) {
        this.modoModificar = modificar;
        btnGuardar.setText(modificar ? "Modificar" : "Agregar");
    }

    /**
     * Rellena los campos con los datos de la persona proporcionada.
     *
     * @param persona La persona cuyos datos se deben mostrar en los campos.
     */
    public void llenarCampos(Persona persona) {
        this.personaOriginal = persona;
        txtNombre.setText(persona.getNombre());
        txtApellidos.setText(persona.getApellidos());
        txtEdad.setText(String.valueOf(persona.getEdad()));
    }

    /**
     * Metodo que guarda o modifica los datos de la persona. Valida los datos antes de
     * guardarlos o modificarlos en la base de datos.
     *
     * @param event El evento generado por el clic en el boton guardar.
     */
    @FXML
    void guardar(ActionEvent event) {
        String nombre = txtNombre.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String edadStr = txtEdad.getText().trim();

        StringBuilder errores = new StringBuilder();
        if (nombre.isEmpty()) {
            errores.append("El campo nombre es obligatorio.\n");
        }
        if (apellidos.isEmpty()) {
            errores.append("El campo apellidos es obligatorio.\n");
        }

        int edad = -1;
        if (edadStr.isEmpty()) {
            errores.append("El campo edad es obligatorio.\n");
        } else {
            try {
                edad = Integer.parseInt(edadStr);
                if (edad < 0) {
                    errores.append("La edad debe ser un número positivo.\n");
                }
            } catch (NumberFormatException e) {
                errores.append("La edad debe ser un número entero.\n");
            }
        }

        if (errores.length() > 0) {
            mostrarAlertaError("Datos inválidos", errores.toString());
            return;
        }

        Connection connection = helloController.conectarBaseDatos("personas"); // Obtener conexión a la base de datos

        if (modoModificar && personaOriginal != null) {
            // Modificar persona existente en la base de datos
            String sql = "UPDATE Persona SET nombre = ?, apellidos = ?, edad = ? WHERE id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, nombre);
                pstmt.setString(2, apellidos);
                pstmt.setInt(3, edad);
                pstmt.setInt(4, personaOriginal.getId());
                pstmt.executeUpdate();

                Persona personaModificada = new Persona(personaOriginal.getId(), nombre, apellidos, edad);
                helloController.modificarPersonaTabla(personaOriginal, personaModificada);
                mostrarAlertaExito("Info", "Persona modificada correctamente");
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAlertaError("Error al modificar", "No se pudo modificar la persona en la base de datos.");
                return;
            }
        } else {
            // Agregar nueva persona a la base de datos
            String sql = "INSERT INTO Persona (nombre, apellidos, edad) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, nombre);
                pstmt.setString(2, apellidos);
                pstmt.setInt(3, edad);
                pstmt.executeUpdate();

                ResultSet generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    Persona nuevaPersona = new Persona(id, nombre, apellidos, edad);

                    helloController.agregarPersonaTabla(nuevaPersona);
                    mostrarAlertaExito("Info", "Persona añadida correctamente");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                mostrarAlertaError("Error al agregar", "No se pudo agregar la persona a la base de datos.");
                return;
            }
        }

        Stage stage = (Stage) btnGuardar.getScene().getWindow();
        stage.close();
    }

    /**
     * Metodo que cancela la operacion y cierra la ventana actual.
     *
     * @param event El evento generado por el clic en el boton cancelar.
     */
    @FXML
    void cancelar(ActionEvent event) {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    /**
     * Muestra una alerta de éxito con el mensaje proporcionado.
     *
     * @param titulo El titulo de la alerta.
     * @param mensaje El mensaje que se mostrará en la alerta.
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
     * @param mensaje El mensaje que se mostrará en la alerta.
     */
    private void mostrarAlertaError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
