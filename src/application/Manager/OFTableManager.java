package application.Manager;

import application.Entity.OFTLE;

import java.util.ArrayList;

/**
 * 已打开文件（登记表）管理器
 */
public class OFTableManager {
    private final ArrayList<OFTLE> oftleList = new ArrayList<>();
    private static final int maxLength = 5;

    public OFTLE find(long posOfEntry) {
        return null;
    }

    public static int getMaxLength() {
        return maxLength;
    }

    public ArrayList<OFTLE> getOftleList() {
        return oftleList;
    }
}
