package application.controller;


import application.Manager.DiskManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Tooltip;

import java.util.List;

/**
 * 磁盘状态管理(界面交互)
 */

public class DiskStatusController implements DiskStatusObserver {
    
    @FXML
    private Label totalCapacityLabel;
    
    @FXML
    private Label usedCapacityLabel;
    
    @FXML
    private Label freeSpaceLabel;
    
    @FXML
    private ProgressBar usageProgressBar;
    
    @FXML
    private TilePane blockPane;
    
    private DiskManager diskManager;

    // 初始化
    public void initialize() {
        diskManager = new DiskManager();
        updateDiskStatus();
    }

    // 更新磁盘状态
    public void updateDiskStatus() {
        List<Boolean> blockStatus = diskManager.disk_status();
        
        // 计算使用情况
        int totalBlocks = blockStatus.size();
        int usedBlocks = 0;
        for (Boolean status : blockStatus) {
            if (status) usedBlocks++;
        }
        int freeBlocks = totalBlocks - usedBlocks;
        
        // 更新标签
        totalCapacityLabel.setText(totalBlocks + " 块");
        usedCapacityLabel.setText(usedBlocks + " 块");
        freeSpaceLabel.setText(freeBlocks + " 块");
        
        // 更新进度条
        double usage = (double) usedBlocks / totalBlocks;
        usageProgressBar.setProgress(usage);
        
        // 根据使用率设置颜色
        if (usage > 0.8) {
            usageProgressBar.setStyle("-fx-accent: #F44336; -fx-control-inner-background: #E0E0E0;"); // 红色警告
        } else if (usage > 0.6) {
            usageProgressBar.setStyle("-fx-accent: #FFA726; -fx-control-inner-background: #E0E0E0;"); // 橙色警告
        } else {
            usageProgressBar.setStyle("-fx-accent: #4CAF50; -fx-control-inner-background: #E0E0E0;"); // 正常绿色
        }
        
        // 清除旧的块显示
        blockPane.getChildren().clear();
        
        // 创建新的块显示
        for (int i = 0; i < blockStatus.size(); i++) {
            boolean isUsed = blockStatus.get(i);
            
            // 创建一个小方块
            Rectangle block = new Rectangle(20, 20);
            block.setFill(isUsed ? Color.valueOf("#F44336") : Color.valueOf("#4CAF50"));
            block.setArcWidth(4);
            block.setArcHeight(4);
            
            // 创建一个容器来使方块居中
            StackPane blockContainer = new StackPane(block);
            blockContainer.setPrefSize(22, 22);
            
            // 添加提示信息
            Tooltip tooltip = new Tooltip("块 " + i + ": " + (isUsed ? "已使用" : "未使用"));
            Tooltip.install(blockContainer, tooltip);
            
            // 添加到网格中
            blockPane.getChildren().add(blockContainer);
        }
    }

    @Override
    public void onDiskStatusChanged() {
        updateDiskStatus();
    }
} 