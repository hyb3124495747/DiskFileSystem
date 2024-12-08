package application.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * 模拟磁盘(界面交互)
 */

public class LaunchController {
    
    private Stage fileSystemStage;
    private Stage diskStatusStage;
    private DiskStatusController diskStatusController;
    
    @FXML
    private Button diskButton;
    
    @FXML
    private Button statusButton;

    /**
     * 进入磁盘界面
     */
    @FXML
    private void handleDiskButton() {
        if (fileSystemStage == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
                Parent root = loader.load();
                
                // 获取MainController并设置观察者
                MainController mainController = loader.getController();
                if (diskStatusController != null) {
                    mainController.setDiskStatusObserver(diskStatusController);
                }
                
                fileSystemStage = new Stage();
                fileSystemStage.setTitle("文件系统");
                fileSystemStage.setScene(new Scene(root, 600, 500));
                fileSystemStage.initModality(Modality.NONE);
                
                // 当窗口关闭时，将stage设为null以允许再次打开
                fileSystemStage.setOnCloseRequest(event -> fileSystemStage = null);
                
                fileSystemStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fileSystemStage.requestFocus();
        }
    }

    /**
     * 进入磁盘状态界面
     */
    @FXML
    private void handleStatusButton() {
        if (diskStatusStage == null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DiskStatusView.fxml"));
                Parent root = loader.load();
                
                diskStatusController = loader.getController();
                
                diskStatusStage = new Stage();
                diskStatusStage.setTitle("磁盘状态");
                diskStatusStage.setScene(new Scene(root, 385, 390));
                diskStatusStage.setResizable(false);
                diskStatusStage.initModality(Modality.NONE);
                
                diskStatusStage.setOnCloseRequest(event -> diskStatusStage = null);
                
                diskStatusStage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            diskStatusStage.requestFocus();
        }
    }
    
    // 添加鼠标悬停效果
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // 获取窗口并设置标题
            Stage stage = (Stage) diskButton.getScene().getWindow();
            stage.setTitle("文件系统管理器");
            
            // 固定窗口大小
            stage.setResizable(false);
            
            // 设置窗口在屏幕中央
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
        });
        
        // 添加按钮悬停效果
        setupButtonHoverEffects(diskButton);
        setupButtonHoverEffects(statusButton);
    }

    // 右键菜单
    private void setupButtonHoverEffects(Button button) {
        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() + 
                "-fx-background-color: #f8f9fa;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 0);");
        });
        
        button.setOnMouseExited(e -> {
            button.setStyle(button.getStyle() +
                "-fx-background-color: white;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        });
    }

    // 添加获取DiskStatusController的方法
    public DiskStatusController getDiskStatusController() {
        return diskStatusController;
    }
} 