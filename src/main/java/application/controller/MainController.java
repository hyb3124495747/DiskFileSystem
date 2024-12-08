package application.controller;

import application.Enum.EntryAttribute;
import application.Service.FileSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.image.ImageView;

/**
 * 磁盘模块展示(界面交互)
 */

public class MainController {

    private FileSystem fileSystem;

    private DiskStatusObserver diskStatusObserver;

    @FXML
    private TextField pathField;

    @FXML
    private AnchorPane contentArea;

    private String currentPath = "C:";

    private ContextMenu contextMenu;

    // 初始化
    public void initialize() {
        fileSystem = new FileSystem();
        updatePathField();
        setupContextMenu();

        contentArea.setOnContextMenuRequested(this::showContextMenu);

        Platform.runLater(() -> contentArea.requestFocus());
    }

    /**
     * 菜单样式
     */
    private void setupContextMenu() {
        contextMenu = new ContextMenu();

        MenuItem createFileItem = new MenuItem("创建文件");
        createFileItem.setOnAction(event -> handleCreateFile());
        createFileItem.setStyle("-fx-padding: 5 15;");

        MenuItem createDirItem = new MenuItem("创建文件夹");
        createDirItem.setOnAction(event -> handleCreateDirectory());
        createDirItem.setStyle("-fx-padding: 5 15;");

        // 添加分隔线
        SeparatorMenuItem separator = new SeparatorMenuItem();

        contextMenu.getItems().addAll(createFileItem, separator, createDirItem);

        // 设置菜单样式
        String style = "-fx-background-color: white; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 5; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);";

        contextMenu.setStyle(style);

        // 设置菜单项样式
        for (MenuItem item : contextMenu.getItems()) {
            if (!(item instanceof SeparatorMenuItem)) {
                item.setStyle("-fx-text-fill: #333333; -fx-padding: 5 15;");
            }
        }
    }

    /**
     * 右键菜单
     *
     * @param event event
     */
    private void showContextMenu(ContextMenuEvent event) {
        contextMenu.show(contentArea, event.getScreenX(), event.getScreenY());
    }

    /**
     * 修改目录路径
     */
    private void updatePathField() {
        pathField.setText(currentPath);
    }

    /**
     * 目录位置
     */
    @FXML
    private void handleUpDirectory() {
        if (!currentPath.equals("C:")) {
            int lastBackslash = currentPath.lastIndexOf('\\');
            if (lastBackslash > 0) {
                currentPath = currentPath.substring(0, lastBackslash);
            } else {
                currentPath = "C:";
            }
            updatePathField();
        }
    }

    /**
     * 创建文件
     */
    private void handleCreateFile() {
        Dialog<String> dialog = createCustomDialog("创建文件", "请输入文件名:");
        dialog.showAndWait().ifPresent(fileName -> {

            String result = fileSystem.createFile(fileName, EntryAttribute.NORMAL_FILE.getValue());
            showCustomAlert(result == null ? "成功" : "错误",
                    result == null ? "文件夹创建成功!" : "文件夹创建失败!",
                    result == null ? null : "错误代码: " + result,
                    result == null ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);

            // 通知磁盘状态更新
            if (result == null && diskStatusObserver != null) {
                diskStatusObserver.onDiskStatusChanged();
            }
        });
    }


    /**
     * 创建文件夹
     */
    private void handleCreateDirectory() {
        Dialog<String> dialog = createCustomDialog("创建文件夹", "请输入文件夹名称:");
        dialog.showAndWait().ifPresent(dirName -> {
            String result = fileSystem.createDir(dirName, EntryAttribute.DIRECTORY.getValue());
            showCustomAlert(result == null ? "成功" : "错误",
                    result == null ? "文件夹创建成功!" : "文件夹创建失败!",
                    result == null ? null : "错误代码: " + result,
                    result == null ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);

            // 通知磁盘状态更新
            if (result == null && diskStatusObserver != null) {
                diskStatusObserver.onDiskStatusChanged();
            }
        });
    }

    /**
     * 提示对话框
     *
     * @param title      标签
     * @param headerText 头部标题
     * @return 对话框
     */
    private Dialog<String> createCustomDialog(String title, String headerText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        // 创建自定义内容面板，减小间距
        VBox content = new VBox(8);  // 减小间距从10到8
        content.setPadding(new Insets(15, 15, 10, 15));  // 减小内边距

        // 添加标题标签
        Label titleLabel = new Label(headerText);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 创建和设置输入框，减小宽度
        TextField input = new TextField();
        input.setPromptText("请输入名称");
        input.setPrefWidth(250);  // 从300减小到250
        input.setStyle("-fx-padding: 6; -fx-background-radius: 3;");  // 减小padding从8到6

        content.getChildren().addAll(titleLabel, input);
        dialog.getDialogPane().setContent(content);

        // 设置按钮
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

        // 获取并设置按钮样式，减小padding
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-padding: 6 15; -fx-background-radius: 3;");  // 减小padding

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-padding: 6 15; -fx-background-radius: 3;");  // 减小padding

        // 设置对话框样式
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0;");

        // 输入验证
        Node confirmButtonNode = dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButtonNode.setDisable(true);

        input.textProperty().addListener((observable, oldValue, newValue) -> {
            confirmButtonNode.setDisable(newValue.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return input.getText().trim();
            }
            return null;
        });

        // 设置对话框大小
        dialog.getDialogPane().setPrefWidth(250);

        // 添加hover效果，同步修改padding
        confirmButton.setOnMouseEntered(e ->
                confirmButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; " +
                        "-fx-padding: 6 15; -fx-background-radius: 3;"));

        confirmButton.setOnMouseExited(e ->
                confirmButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                        "-fx-padding: 6 15; -fx-background-radius: 3;"));

        return dialog;
    }

    /**
     * 成功或失败响应
     *
     * @param title   标题
     * @param header  头
     * @param content 内容
     * @param type    类型
     */
    private void showCustomAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);  // 移除默认header

        // 创建自定义内容面板
        VBox contentPane = new VBox(10);
        contentPane.setPadding(new Insets(15, 15, 5, 15));

        // 添加图标
        ImageView icon = new ImageView();
        if (type == Alert.AlertType.INFORMATION) {
            icon.setStyle("-fx-text-fill: #4CAF50;");  // 成功时使用绿色
        } else {
            icon.setStyle("-fx-text-fill: #F44336;");  // 错误时使用红色
        }

        // 添加标题文本
        Label headerLabel = new Label(header);
        headerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 如果有详细内容，添加内容文本
        if (content != null) {
            Label contentLabel = new Label(content);
            contentLabel.setStyle("-fx-text-fill: #666666;");
            contentPane.getChildren().addAll(headerLabel, contentLabel);
        } else {
            contentPane.getChildren().add(headerLabel);
        }

        alert.getDialogPane().setContent(contentPane);

        // 设置对话框样式
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        alert.getDialogPane().setPrefWidth(250);

        // 获取并设置确定按钮样式
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: " + (type == Alert.AlertType.INFORMATION ? "#4CAF50" : "#F44336") + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 6 15;" +
                "-fx-background-radius: 3;");

        // 添加hover效果
        String baseColor = type == Alert.AlertType.INFORMATION ? "#4CAF50" : "#F44336";
        String darkerColor = type == Alert.AlertType.INFORMATION ? "#388E3C" : "#D32F2F";

        okButton.setOnMouseEntered(e ->
                okButton.setStyle("-fx-background-color: " + darkerColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 6 15;" +
                        "-fx-background-radius: 3;"));

        okButton.setOnMouseExited(e ->
                okButton.setStyle("-fx-background-color: " + baseColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 6 15;" +
                        "-fx-background-radius: 3;"));

        alert.showAndWait();
    }

    // 设置观察者的方法
    public void setDiskStatusObserver(DiskStatusObserver observer) {
        this.diskStatusObserver = observer;
    }
} 