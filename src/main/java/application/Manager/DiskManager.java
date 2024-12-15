package application.Manager;

import application.Enum.BlockStatus;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


/**
 * 模拟磁盘，管理磁盘块和提供FAT
 */
public class DiskManager {

    public static final int DISK_SIZE = 128; // 128 个磁盘块
    public static final int BLOCK_SIZE = 64; // 每个磁盘块64字节
    public static final int ROOT_DIR_POS = 2; // 根目录磁盘号为2
    public static final int USER_AREA_START = 5; // 用户区域起始块号，0到4号磁盘包括了FAT（0、1）、根目录项、系统文件区

    private static final File diskFile = new File("disk.dat"); // 磁盘文件
    private byte[] FAT; //内存中保存的FAT表

    public void debug_printDisk() {
        try (FileInputStream fis = new FileInputStream(diskFile)) {
            for (int i = 0; i < DISK_SIZE; i++) {
                System.out.print((byte) fis.read() + "\t");
                if ((i + 1) % 16 == 0)
                    System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void debug_rootDir() {
        byte[] rootDirBlock = readBlock(ROOT_DIR_POS);
        for (int i = 0; i < BLOCK_SIZE; i += 8) {
            byte[] entry = Arrays.copyOfRange(rootDirBlock, i, i + 8);
            System.out.print((char) entry[0] + " ");
        }
        System.out.println();
    }

    public List<Boolean> disk_status() {
        System.out.println("获取磁盘状态");
        List<Boolean> list = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(diskFile)) {
            for (int i = 0; i < DISK_SIZE; i++) {
                byte read = (byte)fis.read();
                if (BlockStatus.END_OF_FILE.isEqual(read) ||  BlockStatus.BAD_BLOCK.isEqual(read)) {
                    list.add(true);
                } else if (BlockStatus.FREE.isEqual(read) ) {
                    list.add(false);
                }
            }
            debug_printDisk();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 初始化磁盘和文件分配表
     */
    public DiskManager() {
        // 检查磁盘文件是否存在，如果不存在则创建
        if (!diskFile.exists()) {
            boolean res = false;
            try {
                res = diskFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!res) {
                throw new RuntimeException("创建文件失败");
            }
            // 格式化磁盘
            formatDisk();
            // 保存当前FAT在内存中
            this.FAT = readFAT();
        } else {
            // 读取FAT
            this.FAT = readFAT();
        }
    }

    /**
     * 格式化磁盘
     */
    public void formatDisk() {
        // 在磁盘文件开头写入FAT和根目录项
        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {

            // 初始化FAT
            for (int i = 0; i < DISK_SIZE; i++) {
                // 设置FAT中表示的各磁盘块的状态
                if (i < USER_AREA_START) {
                    raf.seek(raf.length());
                    raf.write(BlockStatus.END_OF_FILE.getValue());
                } else {
                    raf.seek(raf.length());
                    raf.write(BlockStatus.FREE.getValue());
                }
            }

            // 初始化根目录项
            byte[] rootDirBlock = new byte[BLOCK_SIZE];
            Arrays.fill(rootDirBlock, (byte) 0);
            for (int i = 0; i < BLOCK_SIZE; i += 8) {
                rootDirBlock[i] = BlockStatus.EMPTY_ENTRY.getValue();
            }
            raf.seek(raf.length());
            raf.write(rootDirBlock);

            // 初始化其余磁盘存储
            for (int i = USER_AREA_START * BLOCK_SIZE; i < DISK_SIZE * BLOCK_SIZE; i++) {
                raf.seek(raf.length());
                raf.write(0);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从磁盘读取FAT
     *
     * @return 文件分配表的字节数组
     */
    private byte[] readFAT() {
        byte[] FAT = new byte[DISK_SIZE];
        try (FileInputStream fis = new FileInputStream(diskFile)) {
            // 读取文件
            if (fis.read(FAT) != -1) {
                return FAT;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("读取FAT失败");
    }

    /**
     * 将FAT写回磁盘
     *
     * @param FAT 文件分配表的字节数组
     */
    private void writeFAT(byte[] FAT) {
        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            raf.write(FAT);
            // 更新内存中的FAT
            this.FAT = readFAT();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置磁盘块的数据
     *
     * @param index 磁盘块索引
     * @param data  要写入的数据
     */
    public void writeBlock(int index, byte[] data) {
        if (index >= 0 && index < DISK_SIZE) {
            try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
                raf.seek((long) index * BLOCK_SIZE);
                raf.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new IllegalArgumentException("Invalid block index");
        }
    }


    /**
     * 读取磁盘块的数据
     *
     * @param index 磁盘块索引
     * @return 读取的数据
     */
    public byte[] readBlock(int index) {
        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            // 检查索引是否合法
            if (index == ROOT_DIR_POS || index >= USER_AREA_START && index < DISK_SIZE) {
                byte[] data = new byte[BLOCK_SIZE];
                raf.skipBytes(index * BLOCK_SIZE);
                raf.read(data);
                return data;
            } else {
                throw new IllegalArgumentException("Invalid block index");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置FAT的条目
     *
     * @param index 文件分配表的索引
     * @param value 要设置的值
     */
    public void setFatEntry(int index, byte value) {
        byte[] FAT = readFAT();
        if (index >= 0 && index < DISK_SIZE) {
            FAT[index] = value;
            writeFAT(FAT);
            // 更新内存中的FAT
            this.FAT = readFAT();
        } else {
            throw new IllegalArgumentException("Invalid FAT index");
        }
    }

    /**
     * 获取FAT的条目
     *
     * @param index 文件分配表的索引
     * @return 文件分配表的条目值
     */
    public byte getFatEntry(int index) {
        byte[] FAT = this.FAT;
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
        byte[] FAT = readFAT();
        for (int i = USER_AREA_START; i < DISK_SIZE; i++) { // 从用户区域开始查找
            if (BlockStatus.FREE.isEqual(FAT[i])) {
                FAT[i] = BlockStatus.END_OF_FILE.getValue(); // 标记为文件结束
                writeFAT(FAT);
                // 更新内存中的FAT
                this.FAT = readFAT();
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
    public void deallocateBlock(int blockIndex) throws IOException {
        byte[] FAT = readFAT();
        if (blockIndex >= USER_AREA_START && blockIndex < DISK_SIZE) { // 系统区域块不回收
            FAT[blockIndex] = BlockStatus.FREE.getValue();
            writeFAT(FAT);
            // 更新内存中的FAT
            this.FAT = readFAT();
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
        byte[] FAT = this.FAT;
        return BlockStatus.EMPTY_ENTRY.isEqual(FAT[blockIndex]);
    }

    /**
     * 检查磁盘块是否为坏块
     *
     * @param blockIndex 磁盘块索引
     * @return 如果磁盘块是坏块返回true，否则返回false
     */
    public boolean isBlockBad(int blockIndex) {
        byte[] FAT = this.FAT;
        return BlockStatus.BAD_BLOCK.isEqual(FAT[blockIndex]);
    }

    /**
     * 检查磁盘块是否已被分配
     *
     * @param blockIndex 磁盘块索引
     * @return 如果磁盘块已被分配返回true，否则返回false
     */
    public boolean isBlockAllocated(int blockIndex) {
        byte[] FAT = this.FAT;
        return !BlockStatus.FREE.isEqual(FAT[blockIndex])
                && !BlockStatus.BAD_BLOCK.isEqual(FAT[blockIndex]);
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
            System.out.println("blockIndex:" + blockIndex);
        } while (isBlockBad(blockIndex)); // 如果已经是坏块，则重新选择

        // 将选中的磁盘块标记为坏块
        setFatEntry(blockIndex, BlockStatus.BAD_BLOCK.getValue());

        // 模拟磁盘块数据损坏，corruptedData里都是0
        byte[] corruptedData = new byte[BLOCK_SIZE];
        // 模拟填充损坏数据，简单地用0填充
        writeBlock(blockIndex, corruptedData);
    }

    /**
     * 修复所有损坏的非系统区磁盘块
     */
    public void fixDisk() {
        for (int i = USER_AREA_START; i < DISK_SIZE; i++) {
            // 检查磁盘块是否为坏块
            if (isBlockBad(i)) {
                // 将坏块标记为空闲状态
                setFatEntry(i, BlockStatus.FREE.getValue());
            }
        }
    }

    /**
     * 初始化目录磁盘块
     *
     * @param newBlockIndex 新的磁盘块索引
     */
    public void initDirBlock(int newBlockIndex) {
        byte[] data = new byte[BLOCK_SIZE];
        Arrays.fill(data, (byte) 0);
        for (int i = 0; i < BLOCK_SIZE; i += 8) {
            data[i] = BlockStatus.EMPTY_ENTRY.getValue();
        }
        writeBlock(newBlockIndex, data);
    }

    /**
     * 格式化普通磁盘块
     *
     * @param newBlockIndex 新的磁盘块索引
     */
    public void initBlock(int newBlockIndex) {
        byte[] data = new byte[BLOCK_SIZE];
        Arrays.fill(data, (byte) 0);
        writeBlock(newBlockIndex, data);
    }

    /**
     * 获取FAT
     *
     * @return FAT
     */
    public byte[] getFAT() {
        return FAT;
    }

}