package application.Service;

import application.Enum.BlockStatus;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;
import application.Manager.DiskManager;
import application.Entity.Entry;

import java.util.Arrays;

/**
 * 检索文件或目录登记项
 */
public class EntryOperator {
    private final DiskManager diskManager; // 磁盘管理器
    private final int entrySize = 8; // 每个目录项的大小

    private byte[] readBuffer; // 读缓冲
    private byte[] writeBuffer; // 写缓冲

    public EntryOperator(DiskManager diskManager) {
        this.diskManager = diskManager;
        this.readBuffer = new byte[DiskManager.BLOCK_SIZE];
        this.writeBuffer = new byte[DiskManager.BLOCK_SIZE];
    }

    /**
     * 在磁盘管理器中查找目录的盘块号（要改，还没支持任意长目录）
     *
     * @param parentDirName 父目录名
     * @return 找到的目录项的起始盘块号
     */
    public int findDirBlockIndex(String parentDirName) {
        // 从根目录开始查找
        int currentBlockIndex = DiskManager.ROOT_DIR_START; // 假设根目录起始盘块号是常量

        // 按路径分割父目录名
        String[] dirComponents = parentDirName.split("/");

        // 遍历父目录路径的每个部分
        for (String dirName : dirComponents) {
            if (dirName.isEmpty()) continue; // 跳过空字符串，如路径以 '/' 开头

            // 读取当前目录块的数据
            byte[] dirBlockData = diskManager.readBlock(currentBlockIndex);
            int entryIndex = -1; // 初始化为 -1，表示未找到

            // 遍历目录块中的每个目录项
            for (int j = 0; j < DiskManager.BLOCK_SIZE / this.entrySize; j++) {
                int offset = j * this.entrySize; // 目录项的偏移量
                byte[] entryNameBytes = Arrays.copyOfRange(dirBlockData, offset + EntryStructure.NAME_POS.getValue(), offset + EntryStructure.NAME_END.getValue());
                byte[] entryTypeBytes = Arrays.copyOfRange(dirBlockData, offset + EntryStructure.TYPE_POS.getValue(), offset + EntryStructure.TYPE_END.getValue());

                // 构造目录项的文件名
                String entryName = new String(entryNameBytes) + "." + new String(entryTypeBytes);
                if (entryName.equals(dirName)) {
                    entryIndex = j; // 找到匹配的目录项，记录索引
                    currentBlockIndex = dirBlockData[offset + EntryStructure.START_NUM_POS.getValue()]; // 获取路径中下一个目录的块号
                    break;
                }
            }

            if (entryIndex == -1) { // 如果未找到目录项且该目录已经找完，返回 -1
                if (diskManager.getFatEntry(currentBlockIndex) == BlockStatus.END_OF_FILE.getValue()) {
                    return -1;
                }
                else { // 否则，查找后续目录块号
                    currentBlockIndex = diskManager.getFatEntry(currentBlockIndex);
                }
            }
        }
        return currentBlockIndex;
    }

    /**
     * 在目录中查找特定文件的目录项
     *
     * @param dirBlockIndex   目录盘块号
     * @param fileNameAndType 文件名（含Type）
     * @return 找到的Entry对象，如果没有找到则返回null
     */
    public Entry findEntryInDirectory(int dirBlockIndex, String fileNameAndType) {
        byte[] dirBlockData = diskManager.readBlock(dirBlockIndex);
        for (int i = 0; i < DiskManager.BLOCK_SIZE; i += this.entrySize) {
            byte[] entry = Arrays.copyOfRange(dirBlockData, i, i + this.entrySize);
            // 跳过空目录项
            if (entry[0]==BlockStatus.EMPTY_ENTRY.getValue()){
                continue;
            }
            // 完整文件名为name + type
            String entryName = new String(new byte[]{entry[0], entry[1], entry[2]}) + "." + new String(new byte[]{entry[3], entry[4]});
//            System.out.print("entryName: " + entryName);
//            System.out.println(" and  "+fileNameAndType);

            if (entryName.equals(fileNameAndType)) {
                byte[] entryNameBytes = new byte[]{entry[0], entry[1], entry[2]};
                byte[] entryTypeBytes = new byte[2];
                entryTypeBytes[0] = entry[3];
                entryTypeBytes[1] = entry[4];
                return new Entry(entryNameBytes, entryTypeBytes, entry[5], entry[6], entry[7]);
            }
        }
        return null; // 未找到
    }

    /**
     * 将新登记项添加到目录
     *
     * @param dirBlockIndex 目录盘块号
     * @param newEntry      新登记项
     */
    public void addEntryToDirectory(int dirBlockIndex, int freeEntryIndex, Entry newEntry) {
        // 记录新登记项数据
        byte[] newEntryData = new byte[this.entrySize];
        System.arraycopy(newEntry.getName(), 0, newEntryData, EntryStructure.NAME_POS.getValue(), EntryStructure.NAME_LENGTH.getValue());
        System.arraycopy(newEntry.getType(), 0, newEntryData, EntryStructure.TYPE_POS.getValue(), EntryStructure.TYPE_LENGTH.getValue());
        newEntryData[EntryStructure.ATTRIBUTE_POS.getValue()] = newEntry.getAttribute(); // 文件属性
        newEntryData[EntryStructure.START_NUM_POS.getValue()] = newEntry.getStartNum(); // 起始盘块号
        newEntryData[EntryStructure.DISK_BLOCK_LENGTH_POS.getValue()] = newEntry.getDiskBlockLength(); // 长度

        // 获取目录项磁盘块的原数据
        this.readBuffer = diskManager.readBlock(dirBlockIndex);
        System.arraycopy(this.readBuffer, 0, this.writeBuffer, 0, DiskManager.BLOCK_SIZE);

        // 把需要登记的数据写到写缓冲区对应freeEntryIndex的位置
        int entryOffset = freeEntryIndex * this.entrySize;
        System.arraycopy(newEntryData, 0, this.writeBuffer, entryOffset, newEntryData.length);
        diskManager.writeBlock(dirBlockIndex, this.writeBuffer); // 将更新后的数据写回磁盘
    }

    /**
     * 寻找空闲目录项
     *
     * @param curDirBlockIndex 当前目录盘块号
     * @return 找到的父目录磁盘块号和空闲目录项的索引；如果是根目录没有空闲项，则返回0，0； 如果是没有多余的磁盘块来分配，则返回-1，-1
     */
    public int[] findFreeEntry(int curDirBlockIndex) {
        // 读缓冲区读取目录盘块的数据
        byte[] dirBlockData = diskManager.readBlock(curDirBlockIndex);

        // 遍历目录项以找到第一个空闲项
        for (int index = 0; index < DiskManager.BLOCK_SIZE / this.entrySize; index++) {
            int offset = index * this.entrySize; // 计算目录项的偏移量
            byte[] entry = Arrays.copyOfRange(dirBlockData, offset, offset + this.entrySize);

            // 检查是否为空闲项
            if (entry[0] == BlockStatus.EMPTY_ENTRY.getValue()) {
                return new int[]{curDirBlockIndex, index}; // 返回空闲目录项的索引
            }
        }

        // 如果是根目录,则不支持另外分配磁盘块来存储新目录项
        if (curDirBlockIndex == DiskManager.ROOT_DIR_START) {
            return new int[]{0, 0};
        }

        // 如果未找到空闲项，检查是否需要为父目录分配新盘块
        if (diskManager.getFatEntry(curDirBlockIndex) == BlockStatus.END_OF_FILE.getValue()) {
            // 分配新盘块
            int newBlockIndex = diskManager.allocateBlock();
            if (newBlockIndex == -1) {
                return new int[]{-1, -1}; // 没有空闲的位置添加新目录项
            }

            // 更新FAT表，指向新分配的盘块
            diskManager.setFatEntry(curDirBlockIndex, (byte) newBlockIndex);
            // 初始化新目录盘块
            diskManager.initDirBlock(newBlockIndex);

            // 更新父目录的盘块号为新盘块号
            return new int[]{newBlockIndex, 0};
        }
        // 如果该目录项磁盘块已满并存在后继磁盘块，递归调用以在后继磁盘块中查找空闲项
        else {
            return findFreeEntry(diskManager.getFatEntry(curDirBlockIndex));
        }
    }
}
