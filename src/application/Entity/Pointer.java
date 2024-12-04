package application.Entity;

/**
 * 已打开文件表项中的读写指针类
 */
public class Pointer {
    private int dNum; //块号
    private int bNum; //块内地址

    public Pointer(int dNum, int bNum) {
        this.dNum = dNum;
        this.bNum = bNum;
    }

    public int getdNum() {return dNum;}
    public int getbNum() {return bNum;}

    public void setdNum(int dNum) {this.dNum = dNum;}
    public void setbNum(int bNum) {this.bNum = bNum;}
}
