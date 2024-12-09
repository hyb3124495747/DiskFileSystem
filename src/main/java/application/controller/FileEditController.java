package application.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import application.Service.FileSystem;

/**
 * 文件编辑(图像交互)
 */

public class FileEditController {
    @FXML
    private TextArea contentArea;
    
    @FXML
    private Button saveButton;
    
    private boolean readOnly;
    private String fileName;
    private String fullPath;
    private FileSystem fileSystem;

    public FileEditController() {
        this.fileSystem = new FileSystem();
    }

    @FXML
    public void initialize() {
        saveButton.setOnAction(event -> handleSave());
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        contentArea.setEditable(!readOnly);
        saveButton.setVisible(!readOnly);
        
        if (readOnly) {
            contentArea.setStyle(contentArea.getStyle() + 
                               "-fx-control-inner-background: #f8f9fa;");
        }
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public void setContent(String content,String fullPath) {
        contentArea.setText(content);
        this.fullPath = fullPath;
    }
    
    private void handleSave() {
        String content = contentArea.getText();
        byte[] contentBytes = content.getBytes();
        
        String result = fileSystem.writeFile(fullPath, contentBytes, contentBytes.length, true);
        
        if (result.equals("1")) {
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存失败");
            alert.setHeaderText(null);
            alert.setContentText("文件保存失败: " + result);
            alert.showAndWait();
        }
    }
} 