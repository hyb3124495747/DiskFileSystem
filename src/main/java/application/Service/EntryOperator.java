package application.Service;

import application.Enum.BlockStatus;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;
import application.Manager.DiskManager;
import application.Entity.Entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 目录项操作项，使用到读写缓冲，为文件操作类和目录操作类提供接口
 */
public class EntryOperator {
    private final DiskManager diskManager; // 磁盘管理器
    private final int entrySize; // 每个目录项的大小

    private byte[] readBuffer; // 读缓冲
    private byte[] writeBuffer; // 写缓冲

    public EntryOperator(DiskManager diskManager) {
        this.diskManager = diskManager;
        this.entrySize = EntryStructure.ENTRY_LENGTH.getValue();
        this.readBuffer = new byte[DiskManager.BLOCK_SIZE];
        this.writeBuffer = new byte[DiskManager.BLOCK_SIZE];
    }

    /**
     * 分配一个新的磁盘块
     *
     * @return 分配的磁盘块索引，如果没有空闲块则返回 -1
     */
    public int allocateDiskBlock() throws Exception {
        return diskManager.allocateBlock();
    }

    /**
     * 释放磁盘块
     *
     * @param blockIndex 要释放的磁盘块索引
     */
    public void deallocateBlock(int blockIndex) {
        try {
            diskManager.deallocateBlock(blockIndex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用读缓冲区读取指定磁盘块的内容
     *
     * @param blockIndex 磁盘块索引
     * @return 读取结果
     */
    public byte[] getContentFromBlock(int blockIndex) {
        this.readBuffer = diskManager.readBlock(blockIndex);
        byte[] data = new byte[DiskManager.BLOCK_SIZE];
        System.arraycopy(this.readBuffer, 0, data, 0, DiskManager.BLOCK_SIZE);
        return data;
    }

    /**
     * 将登记项的所有内容一块一块的读取转换为字符串
     *
     * @param blockIndex 登记项起始盘块
     * @return 内容字符串
     */
    public String getAllFromEntry(byte blockIndex) {
        byte curBlockIndex = blockIndex;
        StringBuilder sb = new StringBuilder();
        while (curBlockIndex != BlockStatus.END_OF_FILE.getValue()) {
            byte[] blockData = getContentFromBlock(curBlockIndex);
            for (byte b : blockData) {
                if (b == (byte) '#') {
                    break;
                }
                sb.append((char) b);
            }
            curBlockIndex = diskManager.getFatEntry(curBlockIndex);
        }
        return sb.toString();
    }


    /**
     * 使用写缓冲区将内容写入该文件占有的磁盘块
     *
     * @param blockIndex 磁盘块索引
     * @param data       要写入的数据
     */
    public void setContentToEntry(int blockIndex, byte[] data) throws Exception {
        // 确保数据长度不超过磁盘块大小
        if (data.length > DiskManager.BLOCK_SIZE) {
            throw new IllegalArgumentException("Data exceeds block size.");
        }

        // 读取磁盘块的当前内容到写缓冲区
        this.readBuffer = getContentFromBlock(blockIndex);
        System.arraycopy(this.readBuffer, 0, this.writeBuffer, 0, DiskManager.BLOCK_SIZE);

        // 将新数据写入写缓冲区的起始位置
        System.arraycopy(data, 0, writeBuffer, 0, data.length);

        // 将写缓冲区的内容写回磁盘块
        diskManager.writeBlock(blockIndex, writeBuffer);
    }

    /**
     * 在磁盘管理器中查找目录的盘块号（已经存在的），支持任意长
     *
     * @param dirAbsolutePath 目录名
     * @return 找到的目录盘块号
     */
    public int findDirBlockIndex(String dirAbsolutePath) throws Exception {
        // 从根目录开始查找
        int currentBlockIndex = DiskManager.ROOT_DIR_POS;

        // 按路径分割父目录名
        String[] dirComponents = dirAbsolutePath.split("/");

        // 遍历父目录路径的每个部分
        for (String eachName : dirComponents) {
            if (eachName.isEmpty()) continue; // 跳过空字符串，如路径以 '/' 开头

            // 遍历目录块中的每个目录项
            while (currentBlockIndex != BlockStatus.END_OF_FILE.getValue()) {
                // 读取当前目录块的数据
                byte[] dirBlockData = getContentFromBlock(currentBlockIndex);
                int entryIndex = -1; // 初始化为 -1，表示未找到

                for (int j = 0; j < DiskManager.BLOCK_SIZE / this.entrySize; j++) {
                    int offset = j * this.entrySize; // 目录项的偏移量
                    byte[] entryNameBytes = Arrays.copyOfRange(dirBlockData, offset + EntryStructure.NAME_POS.getValue(), offset + EntryStructure.NAME_END.getValue());

                    // 构造目录项的文件名
                    String entryName = new String(entryNameBytes); // + "." + new String(entryTypeBytes);
                    if (entryName.trim().equals(eachName)) {
                        entryIndex = j; // 找到匹配的目录项，记录索引
                        currentBlockIndex = dirBlockData[offset + EntryStructure.START_NUM_POS.getValue()];
                        break;
                    }
                }
                if (entryIndex == -1) currentBlockIndex = diskManager.getFatEntry(currentBlockIndex);
                else break;
            }
        }
        return currentBlockIndex;
    }

    /**
     * 在目录中查找特定文件的目录项（已经存在的目录项），支持任意长
     *
     * @param dirBlockIndex   目录盘块号
     * @param fileNameAndType 文件名（含Type）
     * @param attribute       文件属性
     * @return 找到的Entry对象，如果没有找到则返回null
     */
    public Entry findEntryInDirectory(int dirBlockIndex, String fileNameAndType, byte attribute) throws Exception {
        // 读取
        byte[] dirBlockData = getContentFromBlock(dirBlockIndex);

        // 遍历目录块中的每个目录项
        for (int i = 0; i < DiskManager.BLOCK_SIZE; i += this.entrySize) {
            byte[] entry = Arrays.copyOfRange(dirBlockData, i, i + this.entrySize);
            // 跳过空目录项
            if (entry[0] == BlockStatus.EMPTY_ENTRY.getValue()) continue;

            // 目录没有type，完整文件名为name + type
            String entryName = "";
            if (EntryAttribute.DIRECTORY.isEqual(entry[EntryStructure.ATTRIBUTE_POS.getValue()]))
                entryName = new String(new byte[]{entry[0], entry[1], entry[2]}).trim();
            else
                entryName = new String(new byte[]{entry[0], entry[1], entry[2]}).trim() + "." + new String(new byte[]{entry[3], entry[4]}).trim();

//            System.out.print("entryName: " + entryName);
//            System.out.println(" and  "+fileNameAndType);

            // 比较
            if (entryName.equals(fileNameAndType)) {
                byte[] entryNameBytes = new byte[]{entry[0], entry[1], entry[2]};
                byte[] entryTypeBytes = new byte[2];
                entryTypeBytes[0] = entry[3];
                entryTypeBytes[1] = entry[4];
                if (EntryAttribute.DIRECTORY.isEqual(attribute))
                    return new Entry(entryNameBytes, entry[5], entry[6]);
                else
                    return new Entry(entryNameBytes, entryTypeBytes, entry[5], entry[6], entry[7]);
            }
        }

        // 如果未找到目录项且该目录已经找完，返回 null
        if (diskManager.getFatEntry(dirBlockIndex) == BlockStatus.END_OF_FILE.getValue())
            return null;

            // 否则，获取记录该目录的下一个磁盘块号，查找后续目录块号
        else {
            dirBlockIndex = diskManager.getFatEntry(dirBlockIndex);
            return findEntryInDirectory(dirBlockIndex, fileNameAndType, attribute);
        }
    }

    /**
     * 将新目录项添加到目录
     *
     * @param dirBlockIndex 目录盘块号
     * @param newEntry      新目录项
     */
    public void addEntryToDirectory(int dirBlockIndex, int freeEntryIndex, Entry newEntry) throws Exception {
        // 读取目录项磁盘块的原数据到写缓冲
        this.writeBuffer = getContentFromBlock(dirBlockIndex);

        // 把需要登记的数据写到写缓冲区对应freeEntryIndex的位置
        int entryOffset = freeEntryIndex * this.entrySize;
        System.arraycopy(newEntry.getName(), 0, this.writeBuffer, entryOffset + EntryStructure.NAME_POS.getValue(), EntryStructure.NAME_LENGTH.getValue());
        System.arraycopy(newEntry.getType(), 0, this.writeBuffer, entryOffset + EntryStructure.TYPE_POS.getValue(), EntryStructure.TYPE_LENGTH.getValue());
        this.writeBuffer[entryOffset + EntryStructure.ATTRIBUTE_POS.getValue()] = newEntry.getAttribute(); // 文件属性
        this.writeBuffer[entryOffset + EntryStructure.START_NUM_POS.getValue()] = newEntry.getStartNum(); // 起始盘块号
        this.writeBuffer[entryOffset + EntryStructure.DISK_BLOCK_LENGTH_POS.getValue()] = newEntry.getDiskBlockLength(); // 长度

        // 将更新后的数据写回磁盘
        diskManager.writeBlock(dirBlockIndex, this.writeBuffer);
    }

    /**
     * 寻找第一个空闲目录项
     *
     * @param curDirBlockIndex 当前目录盘块号
     * @return 找到的父目录磁盘块号和空闲目录项的索引；如果是根目录没有空闲项，则返回0，0； 如果是没有多余的磁盘块来分配，则返回-1，-1
     */
    public int[] findFreeEntry(int curDirBlockIndex) throws Exception {
        // 读取目录盘块的数据
        byte[] dirBlockData = getContentFromBlock(curDirBlockIndex);

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
        if (curDirBlockIndex == DiskManager.ROOT_DIR_POS) {
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

    /**
     * 获取指定目录下所有的登记项
     *
     * @param dirName 目录名（路径）
     * @return 目录内容的 Entry 数组
     */
    public Entry[] listDir(String dirName) throws Exception {
        int dirBlockIndex = findDirBlockIndex(dirName);
        if (dirBlockIndex == -1) return null; // 目录不存在

        ArrayList<Entry> entriesList = new ArrayList<>();
        int currentBlockIndex = dirBlockIndex;

        // 循环处理每个磁盘块，直到没有后续块
        while (currentBlockIndex != BlockStatus.END_OF_FILE.getValue()) {
            byte[] dirBlockData = getContentFromBlock(currentBlockIndex);
            // 遍历该磁盘块下的所有登记项
            for (int i = 0; i < DiskManager.BLOCK_SIZE; i += EntryStructure.ENTRY_LENGTH.getValue()) {
                // 取得磁盘块内容
                byte[] entry = Arrays.copyOfRange(dirBlockData, i, i + EntryStructure.ENTRY_LENGTH.getValue());
                if (entry[0] != BlockStatus.EMPTY_ENTRY.getValue()) {
                    // 获取各项信息
                    byte[] name = Arrays.copyOfRange(entry, EntryStructure.NAME_POS.getValue(), EntryStructure.NAME_END.getValue());
                    byte[] type = Arrays.copyOfRange(entry, EntryStructure.TYPE_POS.getValue(), EntryStructure.TYPE_END.getValue());
                    byte attribute = entry[EntryStructure.ATTRIBUTE_POS.getValue()];
                    byte startNum = entry[EntryStructure.START_NUM_POS.getValue()];
                    byte diskBlockLength = entry[EntryStructure.DISK_BLOCK_LENGTH_POS.getValue()];

                    // 构建登记项对象，并加入到list
                    Entry newEntry = null;
                    if (EntryAttribute.DIRECTORY.isEqual(attribute))
                        newEntry = new Entry(name, attribute, startNum);
                    else
                        newEntry = new Entry(name, type, attribute, startNum, diskBlockLength);
                    entriesList.add(newEntry);
                }
            }
            // 获取下一个磁盘块号
            currentBlockIndex = diskManager.getFatEntry(currentBlockIndex);
        }
        // 返回登记项数组
        return entriesList.toArray(new Entry[0]);
    }


    /**
     * 删除登记项以及其占用的磁盘块
     *
     * @param name                要删除的登记项名称
     * @param parentDirBlockIndex 父目录盘块号
     * @param dirBlockIndex       要删除的登记项所在盘块号
     */
    public void dealEntry(String name, int parentDirBlockIndex, int dirBlockIndex) throws Exception {
        //从父目录中删除目录项
        byte[] dirBlockData = getContentFromBlock(parentDirBlockIndex);
        int entryIndex = -1; // 初始化为-1，表示未找到
        for (int i = 0; i < DiskManager.BLOCK_SIZE; i += EntryStructure.ENTRY_LENGTH.getValue()) {
            byte[] entry = Arrays.copyOfRange(dirBlockData, i, i + EntryStructure.ENTRY_LENGTH.getValue());

            // 获取目录项名称,需要判断是目录还是文件
            String judgeName = new String(entry, EntryStructure.NAME_POS.getValue(), EntryStructure.NAME_LENGTH.getValue()).trim();
            if (!EntryAttribute.DIRECTORY.isEqual(entry[EntryStructure.ATTRIBUTE_POS.getValue()]))
                judgeName = judgeName + new String(entry, EntryStructure.TYPE_POS.getValue(), EntryStructure.TYPE_LENGTH.getValue()).trim();

            if (judgeName.equals(name)) {
                entryIndex = i / EntryStructure.ENTRY_LENGTH.getValue(); // 计算目录项索引
                break;
            }
        }

        if (entryIndex != -1) {
            // 删除目录项，将其置为空闲
            System.arraycopy(new byte[]{BlockStatus.EMPTY_ENTRY.getValue()}, 0, dirBlockData, entryIndex * EntryStructure.ENTRY_LENGTH.getValue(), 1);
            setContentToEntry(parentDirBlockIndex, dirBlockData);

            // 回收磁盘块
            while (diskManager.getFatEntry(dirBlockIndex) != BlockStatus.END_OF_FILE.getValue()) {
                int nextBlockIndex = diskManager.getFatEntry(dirBlockIndex);
                diskManager.setFatEntry(dirBlockIndex, BlockStatus.FREE.getValue());
                dirBlockIndex = nextBlockIndex;
            }
        } else {
            // 其实是不会到这里的，因为删除目录项的时候，已经判断了该登记项是否存在
            System.out.println("Entry not found: " + name);
        }
    }

    /**
     * 获取登记项的起始盘块号
     *
     * @param name      文件名
     * @param attribute 属性
     * @return 登记项的起始盘块号
     */
    public int getEntryStartNum(int parentDirBlockIndex, String name, byte attribute) throws Exception {
        //获取父目录的盘块号
        Entry entry = findEntryInDirectory(parentDirBlockIndex, name, attribute);
        // 找不到目录项就会返回-1
        if (entry == null) return -1;
        else return entry.getStartNum();
    }

    /**
     * 获取下一个盘块号
     *
     * @param currentBlockIndex 当前盘块号
     * @return 下一个
     */
    public int getNextBlockIndex(int currentBlockIndex) {
        return diskManager.getFatEntry(currentBlockIndex);
    }

    /**
     * 设置登记项
     *
     * @param parentDirBlockIndex
     * @param targetEntry
     */
    public void setEntryToDirectory(int parentDirBlockIndex, Entry targetEntry) throws Exception {
        // 读取
        byte[] dirBlockData = getContentFromBlock(parentDirBlockIndex);
        String fileNameAndType = new String(targetEntry.getName()).trim() + new String(targetEntry.getType()).trim();

        // 遍历目录块中的每个目录项
        for (int entryOffset = 0; entryOffset < DiskManager.BLOCK_SIZE; entryOffset += this.entrySize) {
            byte[] entry = Arrays.copyOfRange(dirBlockData, entryOffset, entryOffset + this.entrySize);
            // 跳过空目录项
            if (entry[0] == BlockStatus.EMPTY_ENTRY.getValue()) continue;

            // 目录没有type，完整文件名为name + type
            String entryName = "";
            if (EntryAttribute.DIRECTORY.isEqual(entry[EntryStructure.ATTRIBUTE_POS.getValue()]))
                entryName = new String(new byte[]{entry[0], entry[1], entry[2]}).trim();
            else
                entryName = new String(new byte[]{entry[0], entry[1], entry[2]}).trim() + "." + new String(new byte[]{entry[3], entry[4]}).trim();

            if (entryName.equals(fileNameAndType)) {
                System.arraycopy(targetEntry.getName(), 0, this.writeBuffer, entryOffset + EntryStructure.NAME_POS.getValue(), EntryStructure.NAME_LENGTH.getValue());
                System.arraycopy(targetEntry.getType(), 0, this.writeBuffer, entryOffset + EntryStructure.TYPE_POS.getValue(), EntryStructure.TYPE_LENGTH.getValue());
                this.writeBuffer[entryOffset + EntryStructure.ATTRIBUTE_POS.getValue()] = targetEntry.getAttribute(); // 文件属性
                this.writeBuffer[entryOffset + EntryStructure.START_NUM_POS.getValue()] = targetEntry.getStartNum(); // 起始盘块号
                this.writeBuffer[entryOffset + EntryStructure.DISK_BLOCK_LENGTH_POS.getValue()] = targetEntry.getDiskBlockLength(); // 长度

                // 将更新后的数据写回磁盘
                setContentToEntry(parentDirBlockIndex, this.writeBuffer);
                break;
            }
        }
    }
}

