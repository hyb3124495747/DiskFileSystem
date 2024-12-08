package application.Service;

import application.Manager.DiskManager;
import application.Manager.OFTableManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 文件系统，处理文件和目录的操作
 */
public class FileSystem {
    private final FileOperator fileOperator; //文件操作类
    private final DirOperator dirOperator; // 目录操作类
    private final OFTableManager ofTableManager; // 打开文件表管理类
    private final String LOG_FILE_NAME; //日志文件

    private final DiskManager debug_diskManager;// 删

    // 初始化文件目录操作类
    public FileSystem() {
        DiskManager disk = new DiskManager(); this.debug_diskManager = disk;
        EntryOperator entryOperator = new EntryOperator(disk);
        this.fileOperator = new FileOperator(entryOperator);
        this.dirOperator = new DirOperator(entryOperator);
        this.ofTableManager = new OFTableManager();

        this.LOG_FILE_NAME = "log.txt";
        try { // 若不存在日志文件，则创建一个
            BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE_NAME));
            writer.close();
        } catch (IOException ignored){}
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
     * @param fileNameAndType 文件名（含Type）
     * @param attribute       文件属性
     * @return 信息
     */
    public String createFile(String fileNameAndType, byte attribute) {
        try {
            // 调用文件操作类创建文件，并检查结果
            int result = fileOperator.create_file(fileNameAndType, attribute);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error creating file: " + e.getMessage(),this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 创建新目录
     * @param dirName 目标目录名
     * @param attribute 属性
     * @return 信息
     */
    public String createDir(String dirName, byte attribute){
        try {
            // 调用目录操作类创建目录，并检查结果
            int result = dirOperator.createDir(dirName, attribute);
            return Tools.checkResult(result);
        } catch (Exception e) {
            Tools.logError("Error creating directory: " + e.getMessage(),this.LOG_FILE_NAME); //输出错误信息到日志文件
            return "ERROR: Unknown error occurred.";
        }
    }

    /**
     * 列出目录下的内容
     * @param dirName 目标目录名
     * @return 内容，null为目录不存在
     */
    public String[][] listDir(String dirName) {
        try {
            return dirOperator.listDir(dirName);
        } catch (Exception e) {
            Tools.logError("Error listing directory: " + e.getMessage(),this.LOG_FILE_NAME); //输出错误信息到日志文件
            return null;
        }
    }

    /**
     * 删除目录
     * @param dirName 目录名（路径）
     * @return 1成功，-1失败
     */
    public int removeDir(String dirName) {
        try {
            return dirOperator.removeDir(dirName);
        } catch (Exception e) {
            Tools.logError("Error removing directory: " + e.getMessage(),this.LOG_FILE_NAME); //输出错误信息到日志文件
            return -1;
        }
    }
}

