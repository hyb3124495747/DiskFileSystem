package application.Entity;

import application.Enum.EntryAttribute;

/**
 * 文件/目录目录项
 */
public class Entry {
    private byte[] name;        // 文件名 / 目录名
    private byte[] type;        // 文件类型名 / 目录未使用，填充空格
    private byte attribute;     // 文件属性 / 目录属性
    private byte startNum;      // 文件起始盘块号 / 目录起始盘块号
    private byte diskBlockLength; // 文件长度(盘块数) / 目录未使用，填充0

    /**
     * 用于创建目录项的构造函数
     *
     * @param name
     * @param attribute
     * @param startNum
     */
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

    /**
     * 用于创建文件目录项的构造函数
     *
     * @param name
     * @param type
     * @param attribute
     * @param startNum
     * @param diskBlockLength
     */
    public Entry(byte[] name, byte[] type, byte attribute, byte startNum, byte diskBlockLength) {
        this.name = name;
        this.type = type;
        this.attribute = attribute;
        this.startNum = startNum;
        this.diskBlockLength = diskBlockLength;
    }

    public byte[] getName() {
        return name;
    }

    public void setName(byte[] name) {
        this.name = name;
    }

    public void setType(byte[] type) {
        this.type = type;
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
        return EntryAttribute.DIRECTORY.isEqual(this.attribute);
    }

    public boolean isReadOnly() {
        return EntryAttribute.READ_ONLY.isEqual(this.attribute);
    }

    public boolean isSystemFile() {
        return EntryAttribute.SYSTEM_FILE.isEqual(this.attribute);
    }

    public boolean isNormalFile() {
        return EntryAttribute.NORMAL_FILE.isEqual(this.attribute);
    }

    public void setAttribute(byte newAttribute) {
        this.attribute = newAttribute;
    }
}