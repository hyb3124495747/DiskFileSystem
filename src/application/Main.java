package application; /**
 * JavaFX 界面主类
 */
import application.Enum.EntryAttribute;
import application.Manager.DiskManager;
import application.Manager.FileSystem;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class Main extends Application {
    private FileSystem fileSystem;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        DiskManager disk = new DiskManager();
        fileSystem = new FileSystem(disk);

        // 创建 UI 元素
        Button createFileButton = new Button("Create application.entity.File");
        createFileButton.setOnAction(e -> createFileDialog());

        VBox root = new VBox(10, createFileButton);
        Scene scene = new Scene(root, 400, 200);
        primaryStage.setTitle("application.entity.File System Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void createFileDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create application.entity.File");
        dialog.setHeaderText("Enter file name:");
        dialog.showAndWait().ifPresent(fileName -> {
            // 调用文件系统的 createFile 方法
            int success = fileSystem.createFile(fileName, EntryAttribute.NORMAL_FILE.getValue());
            if (success==1) {
                showAlert("application.entity.File created successfully!");
            } else {
                showAlert("application.entity.File creation failed!"+"Error code: " + success);
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

