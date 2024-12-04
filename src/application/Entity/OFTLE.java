package application.Entity;

/**
 * 已打开文件表项类型定义
 */
public class OFTLE {
    private String filePath; // 文件绝对路径名
    private char attribute; // 文件的属性
    private int number; // 文件起始盘块号
    private int length; // 文件长度，文件占用的字节数
    private int operateFlag; // 操作类型，0 表示读操作，1 表示写操作
    private Pointer read; // 读文件的位置，文件打开时 dnum 为文件起始盘块号，bnum 为“0”
    private Pointer write; // 写文件的位置，文件刚建立时 dnum 为文件起始盘块号，bnum 为“0 ，打开文件时 dnum 和 bnum 为文件的末尾位置

    /**
     * 构造函数
     * @param filePath 文件绝对路径名
     * @param attribute 文件属性
     * @param number 文件起始盘块号
     * @param length 文件长度
     * @param read 读指针
     * @param write 写指针
     * @param operateFlag 操作类型
     */
    public OFTLE(String filePath, char attribute, int number, int length, Pointer read, Pointer write, String operateFlag) {
        this.filePath = filePath;
        this.attribute = attribute;
        this.number = number;
        this.length = length;
        this.read = read;
        this.write = write;
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

    public char getAttribute() {
        return attribute;
    }

    public int getNumber() {
        return number;
    }

    public int getLength() {
        return length;
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

    public void setAttribute(char attribute) {
        this.attribute = attribute;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public void setLength(int length) {
        this.length = length;
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
