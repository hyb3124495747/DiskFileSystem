package application.Manager;

import application.Entity.OFTLE;

import java.util.ArrayList;

/**
 * 已打开文件（登记表）管理器
 */
public class OFTableManager {
    private final ArrayList<OFTLE> oftleList; // 已打开文件表
    private static final int maxLength = 5; // 最大长度

    public OFTableManager() {
        this.oftleList = new ArrayList<>();
    }


    /**
     * 在已打开文件表中添加一个OFTLE项
     *
     * @param newOftle 要添加的OFTLE
     * @return 1：成功添加，-6：该oftle已存在于这个OpenFile表，-9：表已满
     */
    public int add(OFTLE newOftle) {
        if (this.oftleList.size() >= maxLength) return -9;
        if (find(newOftle.getStartNum()) != null) return -6;
        else {
            System.out.println(newOftle.getFilePath()+"添加到已打开文件表中");
            oftleList.add(newOftle);
            return 1;
        }
    }

    /**
     * 通过startNum在已打开文件表中查找一个OFTLE项
     *
     * @param startNum 文件起始块号（startNum是唯一的）
     * @return 找到的OFTLE对象，未找到返回null
     */
    public OFTLE find(int startNum) {
        for (OFTLE oftle : oftleList) {
            if (oftle != null && oftle.getStartNum() == startNum)
                return oftle;
        }
        return null;
    }

    /**
     * 提供根据路径寻找的版本，作用和上面一样
     * @param filePath
     * @return
     */
    public OFTLE find(String filePath) {
        System.out.println(oftleList.size());
        for (OFTLE oftle : oftleList) {
            if (oftle.getFilePath().equals(filePath)) {
                return oftle;
            }
        }
        return null;
    }

    /**
     * 在已打开文件表中删除一个OFTLE项
     *
     * @param targetOftle 要删除的OFTLE
     */
    public void remove(OFTLE targetOftle) {
        System.out.println(targetOftle.getFilePath()+"从已打开文件表中移除");
        this.oftleList.remove(targetOftle);
    }

    public boolean isOFTLEFull(){
        return this.oftleList.size()>=maxLength;
    }

    public static int getMaxLength() {
        return maxLength;
    }

    public ArrayList<OFTLE> getOftleList() {
        System.out.println(oftleList.size());
        return oftleList;
    }
}
