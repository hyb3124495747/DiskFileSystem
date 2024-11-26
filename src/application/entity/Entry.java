package application.entity;

import application.Manager.DiskManager;

/**
 * 文件/目录登记项
 */
public class Entry {
    private byte[] name;        // 文件名 / 目录名
    private byte[] type;        // 文件类型名 / 目录未使用，填充空格
    private byte attribute;     // 文件属性 / 目录属性
    private byte startNum;      // 文件起始盘块号 / 目录起始盘块号
    private byte diskBlockLength; // 文件长度(盘块数) / 目录未使用，填充0

    // 属性位常量
    public static final int READ_ONLY = 0x01; // 第0位
    public static final int SYSTEM_FILE = 0x02; // 第1位
    public static final int NORMAL_FILE = 0x04; // 第2位
    public static final int DIRECTORY = 0x08; // 第3位

    public byte[] getName() {
        return name;
    }

    public byte[] getType() {
        return type;
    }

    public byte getAttribute() {
        return attribute;
    }

    public byte getStartNum() {
        return startNum;
    }

    public byte getDiskBlockLength() {
        return diskBlockLength;
    }

    public boolean isDirectory() {
        return (attribute & DIRECTORY) == DIRECTORY;
    }

    public boolean isReadOnly() {
        return (attribute & READ_ONLY) == READ_ONLY;
    }

    public boolean isSystemFile() {
        return (attribute & SYSTEM_FILE) == SYSTEM_FILE;
    }

    public boolean isNormalFile() {
        return (attribute & NORMAL_FILE) == NORMAL_FILE;
    }

    // 用于创建目录登记项的构造函数
    public Entry(byte[] name, byte attribute, byte startNum) {
        this.name = name;
        // 目录类型未使用，填充空格
        this.type = new byte[2];
        this.type[0] = (byte) ' ';
        this.type[1] = (byte) ' ';
        this.attribute = attribute;
        this.startNum = startNum;
        // 目录长度未使用，填充0
        this.diskBlockLength = 0;
    }

    // 用于创建文件登记项的构造函数
    public Entry(byte[] name, byte[] type, byte attribute, byte startNum, byte diskBlockLength) {
        this.name = name;
        this.type = type;
        this.attribute = attribute;
        this.startNum = startNum;
        this.diskBlockLength = diskBlockLength;
    }
}