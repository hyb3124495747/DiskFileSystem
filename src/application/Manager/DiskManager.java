package application.Manager;

import java.util.Arrays;
import java.util.Random;

/**
 * 模拟磁盘，管理磁盘块、文件分配表和空闲空间
 */
public class DiskManager {
    public static final int DISK_SIZE = 128; // 128 个磁盘块
    public static final int BLOCK_SIZE = 64; // 每个磁盘块64字节
    public static final byte FREE = 0; // 空闲块
    public static final byte END_OF_FILE = (byte) 255; // 文件结束标志
    public static final byte BAD_BLOCK = (byte) 254; // 坏块标志
    public static final byte EMPTY_DIR_ENTRY = (byte) '$'; // 表示目录为空目录项
    public static final int ROOT_DIR_START = 2; // 根目录磁盘号为2
    public static final int USER_AREA_START = 5; // 用户区域起始块号，0到4号磁盘包括了FAT（0、1）、根目录项、系统文件区

    private static byte[] disk = null; // 模拟磁盘的存储区域

    /**
     * 初始化磁盘和文件分配表
     */
    public DiskManager() {
        disk = new byte[DISK_SIZE * BLOCK_SIZE]; // 初始化磁盘
        formatDisk();
    }

    /**
     * 通过文件分配表格式化磁盘
     */
    private void formatDisk() {
        // 在可用区域填充0格式化
        Arrays.fill(disk, USER_AREA_START * BLOCK_SIZE, DISK_SIZE * BLOCK_SIZE, (byte) 0);

        // 从磁盘获取FAT
        byte[] FAT = getFAT();
        // 初始化FAT
        for (int i = 0; i < USER_AREA_START; i++)
            FAT[i] = END_OF_FILE;
        for (int i = USER_AREA_START; i < DISK_SIZE; i++) {
            FAT[i] = FREE;
        }
        // 将FAT写回磁盘
        setFAT(FAT);

        // 初始化根目录项（根目录只占一个磁盘块）
        for (int i = DISK_SIZE; i < DISK_SIZE + BLOCK_SIZE; i++) {
            if (i % 8 == 0)
                disk[i] = EMPTY_DIR_ENTRY;
            else
                disk[i] = (byte) 0;
        }
    }

    /**
     * 获取模拟磁盘的存储区域
     *
     * @return 磁盘存储区域的字节数组
     */
    public byte[] getDisk() {
        return disk;
    }

    /**
     * 从模拟磁盘前两块中获取文件分配表 (FAT)
     *
     * @return 文件分配表的字节数组
     */
    public byte[] getFAT() {
        return new byte[]{disk[DISK_SIZE]};
    }

    /**
     * 把FAT写回磁盘
     */
    private void setFAT(byte[] FAT) {
        System.arraycopy(FAT, 0, disk, 0, FAT.length);
    }

    /**
     * 设置磁盘块的数据
     *
     * @param index 磁盘块索引
     * @param data  要写入的数据
     */
    public void setBlock(int index, byte[] data) {
        if (index >= 0 && index < DISK_SIZE && data.length == BLOCK_SIZE) {
            // 将数据写入对应磁盘块
            System.arraycopy(data, 0, disk, index * BLOCK_SIZE, BLOCK_SIZE);
        } else {
            throw new IllegalArgumentException("Invalid block index or data size");
        }
    }

    /**
     * 读取磁盘块的数据
     *
     * @param index 磁盘块索引
     * @return 读取的数据
     */
    public byte[] readBlock(int index) {
        if (index >= 0 && index < DISK_SIZE) {
            byte[] data = new byte[BLOCK_SIZE];
            System.arraycopy(disk, index * BLOCK_SIZE, data, 0, BLOCK_SIZE);
            return data;
        } else {
            throw new IllegalArgumentException("Invalid block index");
        }
    }

    /**
     * 设置文件分配表的条目
     *
     * @param index 文件分配表的索引
     * @param value 要设置的值
     */
    public void setFatEntry(int index, byte value) {
        byte[] FAT = getFAT();
        if (index >= 0 && index < DISK_SIZE) {
            FAT[index] = value;
            setFAT(FAT);
        } else {
            throw new IllegalArgumentException("Invalid FAT index");
        }
    }

    /**
     * 获取文件分配表的条目
     *
     * @param index 文件分配表的索引
     * @return 文件分配表的条目值
     */
    public byte getFatEntry(int index) {
        byte[] FAT = getFAT();
        if (index >= 0 && index < DISK_SIZE) {
            return FAT[index];
        } else {
            throw new IllegalArgumentException("Invalid FAT index");
        }
    }

    /**
     * 分配一个新的磁盘块
     *
     * @return 分配的磁盘块索引，如果没有空闲块则返回-1
     */
    public int allocateBlock() {
        byte[] FAT = getFAT();
        for (int i = USER_AREA_START; i < DISK_SIZE; i++) { // 从用户区域开始查找
            if (FAT[i] == FREE) {
                FAT[i] = END_OF_FILE; // 标记为文件结束
                setFAT(FAT);
                return i;
            }
        }
        return -1; // 没有空闲块
    }

    /**
     * 回收磁盘块
     *
     * @param blockIndex 要回收的磁盘块索引
     */
    public void deallocateBlock(int blockIndex) {
        byte[] FAT = getFAT();
        if (blockIndex >= USER_AREA_START && blockIndex < DISK_SIZE) { // 系统区域块不回收
            FAT[blockIndex] = FREE;
            setFAT(FAT);
        } else {
            throw new IllegalArgumentException("Invalid block index for deallocation");
        }
    }

    /**
     * 检查磁盘块是否空闲
     *
     * @param blockIndex 磁盘块索引
     * @return 如果磁盘块空闲返回true，否则返回false
     */
    public boolean isBlockFree(int blockIndex) {
        byte[] FAT = getFAT();
        return FAT[blockIndex] == FREE;
    }

    /**
     * 检查磁盘块是否为坏块
     *
     * @param blockIndex 磁盘块索引
     * @return 如果磁盘块是坏块返回true，否则返回false
     */
    public boolean isBlockBad(int blockIndex) {
        byte[] FAT = getFAT();
        return FAT[blockIndex] == BAD_BLOCK;
    }

    /**
     * 检查磁盘块是否已被分配
     *
     * @param blockIndex 磁盘块索引
     * @return 如果磁盘块已被分配返回true，否则返回false
     */
    public boolean isBlockAllocated(int blockIndex) {
        byte[] FAT = getFAT();
        return FAT[blockIndex] != FREE && FAT[blockIndex] != BAD_BLOCK;
    }

    /**
     * 随机损坏一个非系统区磁盘块
     */
    public void crippleBlock() {
        Random rand = new Random();
        int blockIndex;

        // 确保只损坏用户区域的完整磁盘块
        do {
            blockIndex = rand.nextInt(DISK_SIZE - USER_AREA_START) + USER_AREA_START;
        } while (isBlockBad(blockIndex)); // 如果已经是坏块，则重新选择

        // 将选中的磁盘块标记为坏块
        setFatEntry(blockIndex, BAD_BLOCK);

        // 模拟磁盘块数据损坏，corruptedData里都是0
        byte[] corruptedData = new byte[BLOCK_SIZE];
        // 模拟填充损坏数据，简单地用0填充
        setBlock(blockIndex, corruptedData);
    }

    /**
     * 修复所有损坏的非系统区磁盘块
     */
    public void fixDisk() {
        for (int i = USER_AREA_START; i < DISK_SIZE; i++) {
            // 检查磁盘块是否为坏块
            if (isBlockBad(i)) {
                // 将坏块标记为空闲状态
                setFatEntry(i, FREE);
            }
        }
    }
}