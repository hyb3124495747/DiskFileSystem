package application.Manager;

import application.entity.File;

/**
 * 文件系统管理
 * 文件系统管理类，处理文件和目录的操作
 */
public class FileSystem {
    // 磁盘管理器
    private final DiskManager disk;

    public FileSystem(DiskManager disk) {
        this.disk = disk;
    }

    public boolean createFile(String fileName, byte fileType, byte fileAttributes) {
        // 检查文件是否已存在
        if (findFile(fileName) != null) {
            return false; // 文件已存在
        }

        // 寻找一个空闲块来存储文件数据
        int freeBlock = findFreeBlock();
        if (freeBlock == -1) {
            return false; // 没有空闲空间
        }

        // 创建文件并分配磁盘块
//        application.entity.File newFile = new application.entity.File(fileName, fileType, fileAttributes, freeBlock, 1);
        File newFile = null;

        // 将文件信息写入文件目录
        addFileToDirectory(newFile);
        return true;
    }

    public File findFile(String fileName) {
        // 查找文件，返回文件对象
        // 这里可以实现文件查找逻辑
        return null;
    }

    private int findFreeBlock() {
        // 查找空闲磁盘块
        for (int i = DiskManager.USER_AREA_START; i < disk.getFAT().length; i++) {
            if (disk.getFAT()[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private void addFileToDirectory(File file) {
        // 将文件信息添加到文件目录
        // 目录添加逻辑
    }
}

