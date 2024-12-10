package application.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import application.Service.FileSystem;
import javafx.application.Platform;

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
    private Stage stage;

//    public FileEditController() {
//        this.fileSystem = new FileSystem();
//    }
//
//    public FileEditController(FileSystem fileSystem) {
//        this.fileSystem = fileSystem;
//    }

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
    
    public void setContent(String content, String fullPath) {
        this.fullPath = fullPath;
        contentArea.setText(content);
        
        Platform.runLater(() -> {
            if (stage == null) {
                stage = (Stage) saveButton.getScene().getWindow();
            }
            
            stage.setOnCloseRequest(event -> {
                if (this.fullPath != null) {
                    String result = fileSystem.closeFile(this.fullPath);
                    if (!result.equals("1")) {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("关闭失败");
                        alert.setHeaderText(null);
                        alert.setContentText("文件关闭失败: " + result);
                        alert.showAndWait();
                    }
                }
            });
        });
    }
    
    private void handleSave() {
        String content = contentArea.getText();
        byte[] contentBytes = content.getBytes();
        
        String result = fileSystem.writeFile(fullPath, contentBytes, contentBytes.length, true);
        
        if (result.equals("1")) {
            if (fullPath != null) {
                String closeResult = fileSystem.closeFile(fullPath);
                if (!closeResult.equals("1")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("关闭失败");
                    alert.setHeaderText(null);
                    alert.setContentText("文件关闭失败: " + closeResult);
                    alert.showAndWait();
                }
            }
            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存失败");
            alert.setHeaderText(null);
            alert.setContentText("文件保��失败: " + result);
            alert.showAndWait();
        }
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
} 