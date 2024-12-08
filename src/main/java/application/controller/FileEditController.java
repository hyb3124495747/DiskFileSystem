package application.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

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
    
    public void setContent(String content) {
        contentArea.setText(content);
    }
    
    private void handleSave() {
        // TODO: 实现保存逻辑
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
} 