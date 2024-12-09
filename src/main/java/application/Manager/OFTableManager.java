package application.Manager;

import application.Entity.OFTLE;
import application.Entity.Pointer;

import java.util.ArrayList;

/**
 * 已打开文件（登记表）管理器
 */
public class OFTableManager {
    private final ArrayList<OFTLE> oftleList;
    private static final int maxLength = 5;

    public OFTableManager() {
        this.oftleList = new ArrayList<>();
        for (int i = 0; i < maxLength; i++) {
            oftleList.add(null);
        }
    }


    /**
     * 在已打开文件表中添加一个OFTLE项
     *
     * @param newOftle 要添加的OFTLE
     * @return 1：成功添加，0：该oftle已存在于这个OpenFile表，-1：表已满
     */
    public int add(OFTLE newOftle) {
        if (this.oftleList.size() >= maxLength) return -1;
        if (find(newOftle.getStartNum()) != null) return 0;
        else {
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
     * 在已打开文件表中删除一个OFTLE项
     *
     * @param targetOftle 要删除的OFTLE
     */
    public void remove(OFTLE targetOftle) {
        this.oftleList.remove(targetOftle);
    }

    public static int getMaxLength() {
        return maxLength;
    }

    public ArrayList<OFTLE> getOftleList() {
        return oftleList;
    }
}
