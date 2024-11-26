package application.contorller;

import application.Manager.DiskManager;
import application.entity.Entry;

import java.util.Arrays;

public class EntrySearcher {
    private final DiskManager diskManager;

    public EntrySearcher(DiskManager diskManager) {
        this.diskManager = diskManager;
    }

    /**
     * 根据绝对路径查找文件或目录
     *
     * @param absolutePath 绝对路径名
     * @return 找到的Entry对象，如果没有找到则返回null
     */
    public Entry searchByAbsolutePath(String absolutePath) {
        String[] pathComponents = absolutePath.split("/");
        byte currentBlockIndex = DiskManager.ROOT_DIR_START; // 根目录起始盘块号

        // 遍历路径组件，看有没有目标目录项
        for (String component : pathComponents) {
            if (component.isEmpty()) continue; // 跳过空字符串，如路径以/开头

            Entry[] entries = readDirectoryEntries(currentBlockIndex);
            Entry targetEntry = null;

            for (Entry entry : entries) {
                if (entry.getName() != null && Arrays.equals(entry.getName(), component.getBytes())) {
                    targetEntry = entry;
                    break;
                }
            }

            if (targetEntry == null) {
                return null; // 未找到组件，查找失败
            }

            if (!targetEntry.isDirectory()) {
                return targetEntry; // 找到文件，结束查找
            }

            currentBlockIndex = targetEntry.getStartNum(); // 目录，更新当前盘块号为目录的起始盘块号
        }

        return null; // 未找到或路径错误
    }

    /**
     * 从指定盘块号读取目录项
     *
     * @param blockIndex 盘块号
     * @return 目录项数组
     */
    private Entry[] readDirectoryEntries(byte blockIndex) {
        byte[] blockData = diskManager.readBlock(blockIndex);
        Entry[] entries = new Entry[8]; // 每个盘块有8个目录项

        // 遍历8个目录项
        for (int i = 0; i < 8; i++) {
            int offset = i * 8; // 每个目录项8字节
            byte[] name = new byte[3];
            byte[] type = new byte[2];

            // 获取每个登记项的信息
            byte attribute = blockData[offset + 5];
            byte startNum = blockData[offset + 6];
            byte length = blockData[offset + 7];
            System.arraycopy(blockData, offset, name, 0, 3);
            System.arraycopy(blockData, offset + 3, type, 0, 2);

            entries[i] = new Entry(name, type, attribute, startNum, length);
        }
        return entries;
    }
}
