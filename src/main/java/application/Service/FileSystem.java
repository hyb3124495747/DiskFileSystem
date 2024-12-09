package application.Service;

import application.Manager.DiskManager;
import application.Manager.OFTableManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 文件系统，处理文件和目录的操作
 */
public class FileSystem {// 目录项操作类
    private final FileOperator fileOperator; //文件操作类
    private final DirOperator dirOperator; // 目录操作类
    private final String LOG_FILE_NAME; //日志文件

    private final ArrayList<String> currentPath; // 用于记录当前路径

    private final DiskManager debug_diskManager;// 删

    // 初始化文件目录操作类
    public FileSystem() {
        //文件系统初始化，包括建立文件模拟磁盘、初始化磁盘、初始化根目录为空目录项
        DiskManager disk = new DiskManager();
        this.debug_diskManager = disk; //模拟磁盘管理器
        EntryOperator entryOperator = new EntryOperator(disk); // 目录项操作类
        OFTableManager ofTableManager = new OFTableManager(); // 打开文件表管理类
        this.fileOperator = new FileOperator(entryOperator, ofTableManager); // 文件操作类
        this.dirOperator = new DirOperator(entryOperator); // 目录操作类

        this.currentPath = new ArrayList<>(); // 默认从根目录开始
        this.LOG_FILE_NAME = "log.txt"; // 日志文件名
        try { // 若不存在日志文件，则创建一个
            BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME));
            writer.close();
        } catch (IOException ignored) {
        }
    }

    public void debug_printDisk() {
        debug_diskManager.debug_printDisk();
    }

    public void debug_rootDir() {
        debug_diskManager.debug_rootDir();
    }

    /**
     * 创建新文件
     *
     * @param fileAbsolutePath 文件完整路径
     * @param attribute        文件属性
     * @return 错误信息
     */
    public String createFile(String fileAbsolutePath, byte attribute) {
        try {
            // 调用文件操作类创建文件，并检查结果
            int result = fileOperator.create_file(fileAbsolutePath, attribute);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error creating file: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 读取文件内容，先调用打开文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @param readLength       需要读取的长度（以字节为单位）
     * @return 读取的内容，如果文件不存在或无法读取，则返回错误信息
     */
    public String readFile(String fileAbsolutePath, int readLength) {
        try {
            // 调用文件操作类读取文件，并检查结果
            return fileOperator.readFile(fileAbsolutePath, readLength);
        } catch (Exception e) {
            Tools.logError("Error reading file: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 写入文件内容，写完自动调用closeFile
     *
     * @param fileAbsolutePath 文件完整路径
     * @param writeData        存放准备写入磁盘的数据
     * @param writeLength      写长度
     */
    public String writeFile(String fileAbsolutePath, byte[] writeData, int writeLength) {
        try {
            // 调用文件操作类写入文件，并检查结果
            int result = fileOperator.writeFile(fileAbsolutePath, writeData, writeLength);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error writing file: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 关闭文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 关闭成功返回 "1"
     */
    public String closeFile(String fileAbsolutePath) {
        try {
            // 调用文件操作类关闭文件，并检查结果
            int result = fileOperator.closeFile(fileAbsolutePath);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error closing file: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 删除文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 错误信息
     */
    public String deleteFile(String fileAbsolutePath) {
        try {
            // 调用文件操作类删除文件，并检查结果
            int result = fileOperator.deleteFile(fileAbsolutePath);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error deleting file: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 显示文件内容
     *
     * @param fileAbsolutePath 文件完整路径
     * @return 文件内容的字符串表示，如果文件不存在或无法读取，则返回错误信息
     */
    public String typeFile(String fileAbsolutePath) {
        try {
            // 调用文件操作类显示文件内容，并返回结果
            return fileOperator.typeFile(fileAbsolutePath);
        } catch (Exception e) {
            Tools.logError("Error displaying file content: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 修改文件属性
     *
     * @param fileAbsolutePath 文件完整路径
     * @param newAttribute     新属性
     * @return 成功返回"1"
     */
    public String changeFileAttribute(String fileAbsolutePath, byte newAttribute) {
        try {
            // 调用文件操作类修改文件属性，并检查结果
            int result = fileOperator.changeFileAttribute(fileAbsolutePath, newAttribute);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error changing file attribute: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 改变文件名
     *
     * @param fileAbsolutePath 文件完整路径
     * @param newNameAndType   新的文件名
     * @return 成功返回"1"
     */
    public String changeFileName(String fileAbsolutePath, String newNameAndType) {
        try {
            // 调用文件操作类修改文件名，并检查结果
            int result = fileOperator.changeFileName(fileAbsolutePath, newNameAndType);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error changing file name: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 创建新目录
     *
     * @param dirAbsolutePath 目标目录名
     * @param attribute       属性
     * @return 信息
     */
    public String createDir(String dirAbsolutePath, byte attribute) {
        try {
            // 调用目录操作类创建目录，并检查结果
            int result = dirOperator.createDir(dirAbsolutePath, attribute);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error creating directory: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 列出目录下的内容
     *
     * @param dirAbsolutePath 目标目录名
     * @return 内容，null为目录不存在
     */
    public String[][] listDir(String dirAbsolutePath) {
        try {
            return dirOperator.listDir(dirAbsolutePath);
        } catch (Exception e) {
            Tools.logError("Error listing directory: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return new String[][]{{"ERROR: Unknown error occurred.", "ERROR: Unknown error occurred."}};
        }
    }

    /**
     * 删除目录
     *
     * @param dirAbsolutePath 目录名（路径）
     * @return 结果信息
     */
    public String removeDir(String dirAbsolutePath) {
        try {
            int res = dirOperator.removeDir(dirAbsolutePath);
            return Tools.checkResult(res);
        } catch (Exception e) {
            Tools.logError("Error removing directory: " + e.getMessage(), this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 进入下一级目录
     *
     * @param dirName 目录名（含路径）
     */
    public void enterDir(String dirName) {
        this.currentPath.add(dirName);
    }

    /**
     * 返回上一级目录
     */
    public void goBack() {
        if (this.currentPath.size() > 1) { // 确保不是在根目录
            this.currentPath.remove(this.currentPath.size() - 1);
        }
    }

    /**
     * 获取当前路径
     *
     * @return 当前路径
     */
    public String getCurrentPath() {
        return String.join("/", this.currentPath);
    }

    /**
     * 进入任意目录
     *
     * @param dirAbsolutePath 目录完整路径
     * @return 进入成功返回 "1"，失败返回错误信息
     */
    public String enterAnyDir(String dirAbsolutePath) {
        String[] pathComponents = dirAbsolutePath.split("/");
        String curPath = "/";
        // 判断合法性
        for (String dirName : pathComponents) {
            if (!dirName.isEmpty()) {
                curPath += dirName + "/";
                // 检查目录是否存在
                try {
                    if (dirOperator.isDirExist(curPath)) {
                        return "ERROR: Invalid directory path.";
                    }
                } catch (Exception e) {
                    return "ERROR: Unknown error occurred.";
                }
            }
        }
        // 重置路径
        this.currentPath.clear();
        for (String dirName : pathComponents)
            if (!dirName.isEmpty())
                this.currentPath.add(dirName);
        return null;
    }

    /**
     * 搜索文件或目录并返回父目录路径
     *
     * @param absolutePath 要搜索的路径
     * @return 父目录路径，或者错误信息
     */
    public String searchPath(String absolutePath) {
        String[] pathComponents = absolutePath.split("/");
        String parentDirName = String.join(
                "/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1)
        );
        String nameOnly = pathComponents[pathComponents.length - 1];
        try {
            if (dirOperator.isDirExist(parentDirName)) {
                String[][] nameResult = dirOperator.listDir(parentDirName);
                for (String[] entry : nameResult) {
                    if (entry[0].equals(nameOnly))
                        return parentDirName;
                }
            }
            return "ERROR: Invalid directory path.";
        } catch (Exception e) {
            return "ERROR: Unknown error occurred.";
        }
    }
}

