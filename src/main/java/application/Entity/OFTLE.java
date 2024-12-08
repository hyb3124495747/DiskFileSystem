package application.Entity;

import application.Manager.DiskManager;

/**
 * 已打开文件表项类型定义
 */
public class OFTLE {
    private String filePath; // 文件绝对路径名
    private byte attribute; // 文件的属性
    private int startNum; // 文件起始盘块号
    private int bytesLength; // 文件长度，文件占用的字节数
    private int operateFlag; // 操作类型，0 表示读操作，1 表示写操作
    private Pointer read; // 读文件的位置，文件打开时 dnum 为文件起始盘块号，bnum 为“0”
    private Pointer write; // 写文件的位置，文件刚建立时 dnum 为文件起始盘块号，bnum 为“0 ，打开文件时 dnum 和 bnum 为文件的末尾位置

    /**
     * 构造函数
     * @param filePath 文件绝对路径名
     * @param attribute 文件属性
     * @param startNum 文件起始盘块号
     * @param bytesLength 文件长度
     * @param operateFlag 操作类型
     */
    public OFTLE(String filePath, byte attribute, int startNum, int endNum, int bytesLength, String operateFlag) {
        this.filePath = filePath;
        this.attribute = attribute;
        this.startNum = startNum;
        this.bytesLength = bytesLength;
        this.read = new Pointer(startNum, 0);
        this.write = new Pointer(endNum, bytesLength % DiskManager.BLOCK_SIZE);
        switch (operateFlag) {
            case "r":
                this.operateFlag = 0; break;
            case "w":
                this.operateFlag = 1; break;
            case "rw":
                // 读写类型的赋值逻辑和默认值一致
            default: this.operateFlag = 2;
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public byte getAttribute() {
        return attribute;
    }

    public int getStartNum() {
        return startNum;
    }

    public int getBytesLength() {
        return bytesLength;
    }

    public int getOperateFlag() {
        return operateFlag;
    }

    public Pointer getRead() {
        return read;
    }

    public Pointer getWrite() {
        return write;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setAttribute(byte attribute) {
        this.attribute = attribute;
    }

    public void setStartNum(int startNum) {
        this.startNum = startNum;
    }

    public void setBytesLength(int bytesLength) {
        this.bytesLength = bytesLength;
    }

    public void setOperateFlag(int operateFlag) {
        this.operateFlag = operateFlag;
    }

    public void setRead(Pointer read) {
        this.read = read;
    }

    public void setWrite(Pointer write) {
        this.write = write;
    }
}
