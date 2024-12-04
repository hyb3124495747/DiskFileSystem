package application.Manager;

import application.Service.FileDirOperator;

/**
 * 文件系统，处理文件和目录的操作
 *
 */
public class FileSystem {
    private final FileDirOperator fileDirOperator;

    // 文件目录操作类

    public FileSystem(DiskManager diskManager) {
        fileDirOperator = new FileDirOperator(diskManager);
        diskManager.debug_printDisk();
    }

     /**
     * 创建新文件
     *
     * @param fileNameAndType 文件名（含Type）
     * @param attribute       文件属性
     * @return 1:成功； 0:只读属性无法创建； -1:父目录不存在； -2:文件已存在； -3:无空闲磁盘块；-4：根目录已满；-5:文件名不合法
     */
    public int createFile(String fileNameAndType, byte attribute) {

        int result = fileDirOperator.create_file(fileNameAndType, attribute);
        return result;
    }
}

