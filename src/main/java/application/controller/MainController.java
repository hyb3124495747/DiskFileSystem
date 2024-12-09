package application.controller;

import application.Enum.EntryAttribute;
import application.Service.FileSystem;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

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
    
    private String currentPath = "/";
    
    private ContextMenu contextMenu;
    private ContextMenu fileContextMenu;
    private ContextMenu directoryContextMenu;

    // 初始化
    @FXML
    public void initialize() {
        fileSystem = new FileSystem();
        updatePathField();
        setupContextMenu();
        setupFileContextMenu();
        setupDirectoryContextMenu();
        
        // 添加这行，直接刷新显示实际文件系统内容
        refreshFileView();
        
        Platform.runLater(() -> {
            // 获取窗口并设置标题
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setTitle("文件系统");
            
            // 固定窗口大小
            stage.setResizable(false);

            // 设置窗口在屏幕中央
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
            
            // 添加样式表
            stage.getScene().getStylesheets().add(
                getClass().getResource("/css/fileSystem.css").toExternalForm()
            );
            
            contentArea.requestFocus();
        });
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
     * 文件右键菜单初始化
     */
    private void setupFileContextMenu() {
        fileContextMenu = new ContextMenu();
        
        MenuItem openItem = new MenuItem("打开");
        openItem.setStyle("-fx-padding: 5 15;");
        
        MenuItem readOnlyItem = new MenuItem("只读打开");
        readOnlyItem.setStyle("-fx-padding: 5 15;");
        
        MenuItem propertiesItem = new MenuItem("修改属性");
        propertiesItem.setStyle("-fx-padding: 5 15;");
        
        MenuItem renameItem = new MenuItem("重命名");
        renameItem.setStyle("-fx-padding: 5 15;");
        
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setStyle("-fx-padding: 5 15;");
        
        // 添加分隔线
        SeparatorMenuItem separator1 = new SeparatorMenuItem();
        SeparatorMenuItem separator2 = new SeparatorMenuItem();
        SeparatorMenuItem separator3 = new SeparatorMenuItem();
        
        fileContextMenu.getItems().addAll(
            openItem, readOnlyItem, separator1,
            propertiesItem, separator2,
            renameItem, separator3,
            deleteItem
        );
        
        // 设置菜单样式
        fileContextMenu.setStyle("-fx-background-color: white; " +
                               "-fx-background-radius: 5; " +
                               "-fx-padding: 5; " +
                               "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        
        // 设置菜单项样式
        for (MenuItem item : fileContextMenu.getItems()) {
            if (!(item instanceof SeparatorMenuItem)) {
                item.setStyle("-fx-text-fill: #333333; -fx-padding: 5 15;");
            }
        }
    }

    /**
     * 文件夹右键菜单初始化
     */
    private void setupDirectoryContextMenu() {
        directoryContextMenu = new ContextMenu();
        
        MenuItem deleteItem = new MenuItem("删除");
        deleteItem.setStyle("-fx-padding: 5 15;");
        
        directoryContextMenu.getItems().add(deleteItem);
        
        // 设置菜单样式
        directoryContextMenu.setStyle("-fx-background-color: white; " +
                                   "-fx-background-radius: 5; " +
                                   "-fx-padding: 5; " +
                                   "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        
        // 设置菜单项样式
        for (MenuItem item : directoryContextMenu.getItems()) {
            item.setStyle("-fx-text-fill: #333333; -fx-padding: 5 15;");
        }
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
        if (!currentPath.equals("/")) {
            currentPath = currentPath.substring(0, currentPath.length() - 1);
            // 获取最后一个斜杠的位置
            int lastSlash = currentPath.lastIndexOf('/');

            // 如果当前路径是根目录的子目录
            if (lastSlash > 0) {
                // 新路径为上级目录
                currentPath = currentPath.substring(0, lastSlash+1);
                System.out.println(currentPath);
            } else {
                // 如果没有找到斜杠，说明当前路径是根目录
                currentPath = "/";
            }
            
            // 确保返回的路径是根目录
            if (currentPath.isEmpty()) {
                currentPath = "/";
            }
            
            updatePathField();
            // 刷新界面
            refreshFileView();
        }
    }

    /**
     * 创��文件
     */
    private void handleCreateFile() {
        Dialog<String> dialog = createCustomDialog("创建文件", "请输入文件名");
        dialog.showAndWait().ifPresent(fileName -> {
            String s = currentPath;
            // 构建完整路径，使用 Unix 风格的路径
            String fullPath = s +fileName; // 直接使用 Unix 风格的路径

            // 直接使用完整路径创建文件
            String result = fileSystem.createFile(fullPath, EntryAttribute.NORMAL_FILE.getValue());

            // 检查返回值是否为错误消息
            if (result.startsWith("ERROR:")) {
                showCustomAlert("错误", "文件创建失败", result, Alert.AlertType.ERROR);
            } else {
                try {
                    int success = Integer.parseInt(result);
                    showCustomAlert(success == 1 ? "成功" : "错误",
                                  success == 1 ? "文件创建成功!" : "文件创建失败!",
                                  success == 1 ? null : "错误代码: " + success,
                                  success == 1 ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    
                    // 通知知磁盘状态更新并刷新视图
                    if (success == 1) {
                        if (diskStatusObserver != null) {
                            diskStatusObserver.onDiskStatusChanged();
                        }
                        refreshFileView();
                    }
                } catch (NumberFormatException e) {
                    showCustomAlert("错误", "文件创建失败", "未知错误: " + result, Alert.AlertType.ERROR);
                }
            }
        });
    }

    /**
     * 创建文件夹
     */
    private void handleCreateDirectory() {
        Dialog<String> dialog = createCustomDialog("创建文件夹", "请输入文件夹名称:");
        dialog.showAndWait().ifPresent(dirName -> {
            // 构建完整路径，使用 Unix 风格的路径
            String fullPath = currentPath + dirName; // 直接使用 Unix 风格的路径

            // 直接使用完整路径创建目录
            String result = fileSystem.createDir(fullPath, EntryAttribute.DIRECTORY.getValue());
            
            // 检查返回值是否为错误消息
            if (result.startsWith("ERROR:")) {
                showCustomAlert("错误", "文件夹创建失败", result, Alert.AlertType.ERROR);
            } else {
                try {
                    int success = Integer.parseInt(result);
                    showCustomAlert(success == 1 ? "成功" : "错误",
                                  success == 1 ? "文件夹创建成功!" : "文件夹创建失败!",
                                  success == 1 ? null : "错误代码: " + success,
                                  success == 1 ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR);
                    
                    // 通知磁盘状态更新并刷新视图
                    if (success == 1) {
                        if (diskStatusObserver != null) {
                            diskStatusObserver.onDiskStatusChanged();
                        }
                        refreshFileView();
                    }
                } catch (NumberFormatException e) {
                    showCustomAlert("错误", "文件夹创建失败", "未知错误: " + result, Alert.AlertType.ERROR);
                }
            }
        });
    }

    /**
     * 提示对话框
     * @param title 标签
     * @param headerText 头部标题
     * @return 对话框
     */
    private Dialog<String> createCustomDialog(String title, String headerText) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        
        // 创建自定义内容面板，减小间距
        VBox content = new VBox(8);  // 减小间距从10到8
        content.setPadding(new Insets(15, 15, 10, 15));  // 减内边距
        
        // 添加题标签
        Label titleLabel = new Label(headerText);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // 创建和设置输入框，减小宽度
        TextField input = new TextField();
        input.setPromptText("请输入名称");
        input.setPrefWidth(250);  // 300减小到250
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
                              "-fx-padding: 6 15; -fx-background-radius: 3;");
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setStyle("-fx-padding: 6 15; -fx-background-radius: 3;");
        
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
        
        // 设置对话框小
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
     * @param title 标题
     * @param header 头
     * @param content 内容
     * @param type 类型
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
        
        // 如果有详细内容，添加容文本
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

    // 进入新目录
    private void refreshFileView() {
        System.out.println("刷新");

        // 清除当前内容
        contentArea.getChildren().clear();
        
        // 创建新的TilePane
        TilePane filePane = new TilePane();
        filePane.setHgap(15);
        filePane.setVgap(15);
        filePane.setPrefColumns(5);
        filePane.setPadding(new Insets(10));
        
        // 为filePane添加右键菜单
        setupFilePaneContextMenu(filePane);
        
        // 获取当前目录的内容
        String path = currentPath.equals("/") ? "/" : currentPath; // 使用 Unix 风格的路径

        String[][] dirContent = fileSystem.listDir(path);

        System.out.println(Arrays.deepToString(dirContent));

        if (dirContent != null) {
            for (String[] entry : dirContent) {
                if (entry != null && entry.length >= 2) {
                    String name = entry[0];
                    boolean isDirectory = entry[1].equals(String.valueOf(EntryAttribute.DIRECTORY.getValue()));
                    
                    VBox fileBox = createFileIcon(new TestFile(name, isDirectory));
                    filePane.getChildren().add(fileBox);
                }
            }
        }

        // 设置TilePane在AnchorPane中的位置
        AnchorPane.setTopAnchor(filePane, 0.0);
        AnchorPane.setLeftAnchor(filePane, 0.0);
        AnchorPane.setRightAnchor(filePane, 0.0);
        AnchorPane.setBottomAnchor(filePane, 0.0);
        
        contentArea.getChildren().add(filePane);
    }

    private void setupFilePaneContextMenu(TilePane filePane) {
        filePane.setOnContextMenuRequested(event -> {
            Node target = (Node) event.getTarget();
            if (target == filePane) {
                contextMenu.show(filePane, event.getScreenX(), event.getScreenY());
            }
            event.consume();
        });
        
        filePane.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (contextMenu != null) contextMenu.hide();
                if (fileContextMenu != null) fileContextMenu.hide();
            }
        });
    }

    // 测试文件类
    private static class TestFile {
        String name;
        boolean isDirectory;

        TestFile(String name, boolean isDirectory) {
            this.name = name;
            this.isDirectory = isDirectory;
        }
    }

    private VBox createFileIcon(TestFile file) {
        VBox fileBox = new VBox();
        fileBox.setAlignment(Pos.CENTER);
        fileBox.setSpacing(5);
        fileBox.setPadding(new Insets(10));
        fileBox.getStyleClass().add("file-icon");

        // 创建图标
        ImageView icon = new ImageView(new Image(
            getClass().getResourceAsStream(file.isDirectory ? "/image/floder.png" : "/image/file.png")
        ));
        icon.setFitWidth(48);
        icon.setFitHeight(48);

        // 创建标签
        Label nameLabel = new Label(file.name);
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(TextAlignment.CENTER);
        nameLabel.setMaxWidth(80);
        nameLabel.setStyle("-fx-font-size: 12px;");

        fileBox.getChildren().addAll(icon, nameLabel);

        // 添加点击事件处理
        fileBox.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (event.getClickCount() == 2) {
                    if (file.isDirectory) {
                        currentPath = currentPath + file.name + "/";
                        updatePathField();
                        refreshFileView();
                    } else {
                        handleFileOpen(file, false);
                    }
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                if (file.isDirectory) {
                    directoryContextMenu.getItems().get(0).setOnAction(e -> handleDeleteDirectory(file));
                    directoryContextMenu.show(fileBox, event.getScreenX(), event.getScreenY());
                } else {
                    fileContextMenu.getItems().get(0).setOnAction(e -> handleFileOpen(file, false));
                    fileContextMenu.getItems().get(1).setOnAction(e -> handleFileOpen(file, true));
                    fileContextMenu.getItems().get(3).setOnAction(e -> handleFileProperties(file));
                    fileContextMenu.getItems().get(5).setOnAction(e -> handleRename(file));
                    fileContextMenu.getItems().get(7).setOnAction(e -> handleDeleteFile(file));
                    fileContextMenu.show(fileBox, event.getScreenX(), event.getScreenY());
                }
                event.consume();
            }
        });

        return fileBox;
    }

    /**
     * 处理文件打开
     * @param file 文件
     * @param readOnly 是否只读
     */
    private void handleFileOpen(TestFile file, boolean readOnly) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/FileEditView.fxml"));
            Parent root = loader.load();

            String fullPath = currentPath + file.name;

            String[] fileInfo = fileSystem.getFileInfo(fullPath);
            String s = fileInfo[6];
            System.out.println(s);

            if(s.equals("1") && !readOnly){
                System.out.println("文件为只读，只能以只读方式打开");
                return;
            }

            FileEditController controller = loader.getController();
            controller.setReadOnly(readOnly);
            controller.setFileName(file.name);
            
            String content = fileSystem.readFile(fullPath,EntryAttribute.NORMAL_FILE.getValue());

//            if(content.contains("File open failed, and ")){
//                return;
//            }

            controller.setContent(content,fullPath);
            
            Stage stage = new Stage();
            stage.setTitle((readOnly ? "只读 - " : "") + file.name);
            stage.setScene(new Scene(root, 600, 400));
            
            // 将模态类型改为 WINDOW_MODAL，这样只会阻止与父窗口的交互
            // 而不会阻止与其他窗口的交互
            stage.initModality(Modality.NONE);
            
            // 设置窗口在屏幕中央
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((screenBounds.getWidth() - 600) / 2);
            stage.setY((screenBounds.getHeight() - 400) / 2);
            
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            showCustomAlert("错误", "无法打开文件", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // 添加处理文件删除的方法
    private void handleDeleteFile(TestFile file) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("确定要删除 " + file.name + " 吗？");
        
        // 自定义确认对话框按钮
        Button okButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("删除");
        okButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        
        Button cancelButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("取消");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String s = "/";
            if(!currentPath.equals("/")){
                s = currentPath;
            }
            String fullPath = s + file.name;
            System.out.println(fullPath);
            String deleteResult = fileSystem.deleteFile(fullPath);
            
            if (deleteResult.equals("1")) {
                showCustomAlert("成功", "文件删除成功!", null, Alert.AlertType.INFORMATION);
                if (diskStatusObserver != null) {
                    diskStatusObserver.onDiskStatusChanged();
                }
                refreshFileView();
            } else {
                showCustomAlert("错误", "文件删除失败", "错误信息: " + deleteResult, Alert.AlertType.ERROR);
            }
        }
    }

    // 修改handleFileProperties方法
    private void handleFileProperties(TestFile file) {
        Dialog<Byte> dialog = new Dialog<>();  // 改为Byte类型
        dialog.setTitle("修改文件属性");
        dialog.setHeaderText(null);
        
        // 创建自定义内容面板
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        Label titleLabel = new Label("选择文件属性:");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        ToggleGroup group = new ToggleGroup();
        RadioButton readWriteBtn = new RadioButton("读写");
        readWriteBtn.setToggleGroup(group);
        readWriteBtn.setSelected(true);
        
        RadioButton readOnlyBtn = new RadioButton("只读");
        readOnlyBtn.setToggleGroup(group);
        
        content.getChildren().addAll(titleLabel, readWriteBtn, readOnlyBtn);
        dialog.getDialogPane().setContent(content);
        
        // 添加按钮
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);
        
        // 设置按钮样式
        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                              "-fx-padding: 6 15; -fx-background-radius: 3;");
        
        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // 直接返回对应的属性值
                return readOnlyBtn.isSelected() ? 
                       EntryAttribute.READ_ONLY.getValue() : 
                       EntryAttribute.NORMAL_FILE.getValue();
            }
            return null;
        });
        
        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(attribute -> {

            String s = currentPath;
            String fullPath = s + file.name;
            
            // 直接使用byte属性值
            String result = fileSystem.changeFileAttribute(fullPath, attribute);
            
            if (result.equals("1")) {
                showCustomAlert("成功", "文件属性修改成功!", null, Alert.AlertType.INFORMATION);
                refreshFileView();
            } else {
                showCustomAlert("错误", "文件属性修改失败", "错误信息: " + result, Alert.AlertType.ERROR);
            }
        });
    }

    // 添加处理文件夹删除的方法
    private void handleDeleteDirectory(TestFile file) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("确认删除");
        confirmDialog.setHeaderText(null);
        confirmDialog.setContentText("确定要删除文件夹 " + file.name + " 及其所有内容吗？");
        
        // 自定义确认对话框按钮
        Button okButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText("删除");
        okButton.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        
        Button cancelButton = (Button) confirmDialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("取消");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String fullPath = currentPath + file.name;
            String deleteResult = fileSystem.removeDir(fullPath);
            
            if (deleteResult.equals("1")) {
                showCustomAlert("成功", "文件夹删除成功!", null, Alert.AlertType.INFORMATION);
                if (diskStatusObserver != null) {
                    diskStatusObserver.onDiskStatusChanged();
                }
                refreshFileView();
            } else {
                showCustomAlert("错误", "文件夹删除失败", "错误信息: " + deleteResult, Alert.AlertType.ERROR);
            }
        }
    }

    private void handleRename(TestFile file) {
        Dialog<String> dialog = createCustomDialog("重命名", "请输入新的名称:");
        dialog.getDialogPane().setPrefWidth(300);
        
        // 获取输入框并设置当前文件名
        TextField input = (TextField) ((VBox) dialog.getDialogPane().getContent()).getChildren().get(1);
        input.setText(file.name);
        input.selectAll(); // 选中当前文件名以方便修改
        
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.equals(file.name)) { // 只在名称确实改变时才进行重命名
                String oldPath = currentPath + file.name;
                String newPath = currentPath + newName;
                
                // 调用文件系统的重命名方法
                String result = fileSystem.changeFileName(oldPath, newName);
                
                if (result.equals("1")) {
                    showCustomAlert("成功", "重命名成功!", null, Alert.AlertType.INFORMATION);
                    refreshFileView();
                } else {
                    showCustomAlert("错误", "重命名失败", "错误信息: " + result, Alert.AlertType.ERROR);
                }
            }
        });
    }
} 