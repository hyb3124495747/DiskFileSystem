package application.Service;

import application.Entity.Entry;
import application.Entity.OFTLE;
import application.Entity.Pointer;
import application.Enum.BlockStatus;
import application.Enum.EntryAttribute;
import application.Enum.EntryStructure;
import application.Manager.DiskManager;
import application.Manager.OFTableManager;

import java.util.Arrays;
import java.util.Objects;

/**
 * 文件操作，提供文件的操作接口：创建文件、读文件、写文件、关闭文件、删除文件等
 */
public class FileOperator {
    private final EntryOperator entryOperator; // 目录项操作
    private final OFTableManager ofTableManager;// 打开文件表管理器

    public FileOperator(EntryOperator entryOperator, OFTableManager ofTableManager) {
        this.entryOperator = entryOperator;
        this.ofTableManager = ofTableManager;
    }

    /**
     * 创建新文件
     *
     * @param fileAbsolutePath 文件完整路径
     * @param attribute        文件属性
     * @return 1:成功； 0:只读属性无法创建； -1:路径不存在； -2:文件已存在； -3:无空闲磁盘块；-4：根目录已满；-5:文件名不合法
     */
    public int create_file(String fileAbsolutePath, byte attribute) throws Exception {
        // 文件属性如果是只读性质则不能建立
        if (EntryAttribute.READ_ONLY.isEqual(attribute)) {
            return 0;
        }

        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        String fileNameOnly = pathComponents[pathComponents.length - 1];
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        if (parentDirBlockIndex == -1) {
            return -1;
        }

        // 检查文件名（含type）
        byte[][] nameAndType = Tools.checkNameAndType(fileNameOnly, EntryAttribute.DIRECTORY.isEqual(attribute));
        if (nameAndType == null) {
            return -5;
        }
        // 分离文件名和类型
        byte[] fileNameBytes = new byte[EntryStructure.NAME_LENGTH.getValue()];
        byte[] fileType = new byte[EntryStructure.TYPE_LENGTH.getValue()];
        System.arraycopy(nameAndType[0], 0, fileNameBytes, 0, fileNameBytes.length);
        System.arraycopy(nameAndType[1], 0, fileType, 0, fileType.length);

        // 检查是否有重名文件
        if (this.entryOperator.findEntryInDirectory(parentDirBlockIndex, fileNameOnly, attribute) != null) {
            return -2;
        }

        // 寻找空闲目录项，支持子目录任意长（但根目录不支持）
        int[] result = this.entryOperator.findFreeEntry(parentDirBlockIndex);
        parentDirBlockIndex = result[0];
        int freeEntryIndex = result[1];
        if (parentDirBlockIndex == 0) {
            return -4; // 根目录不支持任意长
        }
        if (parentDirBlockIndex == -1) {
            return -3; // 没有多余的磁盘块来添加目录项，所以实际上和没有多余磁盘块给文件一样，返回-3
        }

        // 分配一个新的磁盘块给文件
        int fileBlockIndex = this.entryOperator.allocateDiskBlock();
        if (fileBlockIndex == -1) {
            return -3;
        }
        // 在这个新文件中写入文件结束符
        this.entryOperator.setContentToEntry(fileBlockIndex, new byte[]{BlockStatus.EOF.getValue()});

        // 创建新的目录项并添加到父目录中
        Entry newFileEntry = new Entry(fileNameBytes, fileType, attribute, (byte) fileBlockIndex, (byte) 1);
        this.entryOperator.addEntryToDirectory(parentDirBlockIndex, freeEntryIndex, newFileEntry);

        // 最后填写已打开文件表
        //OFTLE newOftle = new OFTLE(fileAbsolutePath, attribute, fileBlockIndex, fileBlockIndex, 0, "rw");
        //ofTableManager.add(newOftle);
        return 1;
    }

    /**
     * 打开文件
     * 若文件已存在于OpenFile表中，则不需要填写已打开文件表, 否则填写已打开文件表
     *
     * @param fileAbsolutePath 文件完整路径
     * @param operateFlag      文件操作类型（r、w、rw、其他默认rw）
     * @return 成功返回 1，只读文件以写方式打开返回0，文件路径不存在返回 -1，文件已打开-6，打开文件表已满-9
     */
    public int openFile(String fileAbsolutePath, String operateFlag) throws Exception {
        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);
        int entryStartNum = Integer.parseInt(fileInfo[2]);
        int entryEndNum = Integer.parseInt(fileInfo[3]);
        int bytesLength = Integer.parseInt(fileInfo[4]);
        // 寻找该登记项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(parentDirBlockIndex, fileNameOnly, EntryAttribute.NORMAL_FILE.getValue());
        if (fileEntry == null) return -1; // 文件不存在

        if (!Objects.equals(operateFlag, "r") && fileEntry.isReadOnly()) {
            // 文件是只读文件，无法以写方式打开
            return 0;
        }
        OFTLE ofTle = new OFTLE(fileAbsolutePath, EntryAttribute.NORMAL_FILE.getValue(),
                entryStartNum, entryEndNum, bytesLength, operateFlag);
        return this.ofTableManager.add(ofTle);
    }


    /**
     * 读取文件内容，先调用打开文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @param readLength       需要读取的长度（以字节为单位）
     * @return 读取的内容，如果文件不存在或无法读取，则返回错误信息
     */
    public String readFile(String fileAbsolutePath, int readLength) throws Exception {
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        int entryStartNum = Integer.parseInt(fileInfo[2]);

        int result = openFile(fileAbsolutePath, "r");
        if (result != 1) {
            return "File open failed, and " + Tools.checkResult(result);
        }
        OFTLE ofTle = ofTableManager.find(entryStartNum);

        // 检查文件是否以读方式打开
        if (ofTle.getOperateFlag() != 0) {
            return "File is not opened in read mode.";
        }

        // 从已打开文件表中读出读指针，并从这个位置上读出所需长度
        Pointer readPointer = ofTle.getRead();
        byte[] fileContent = new byte[readLength];
        int bytesRead = 0;
        int curBlockIndex = readPointer.getdNum();
        // 读取文件内容
        while (bytesRead < readLength) {
            byte[] blockData = this.entryOperator.getContentFromBlock(curBlockIndex);
            for (; readPointer.getbNum() < blockData.length; readPointer.setbNum(readPointer.getbNum() + 1)) {
                if (blockData[readPointer.getbNum()] == BlockStatus.EOF.getValue()) {
                    return new String(fileContent).trim();
                }
                fileContent[bytesRead++] = blockData[readPointer.getbNum()];
            }
            curBlockIndex = this.entryOperator.getNextBlockIndex(curBlockIndex);
            if (curBlockIndex == -1) {
                return new String(fileContent).trim();
            }
            readPointer.setdNum(curBlockIndex);
            readPointer.setbNum(0);
        }
        return "1";
    }


    /**
     * 写入文件内容
     *
     * @param fileAbsolutePath 文件完整路径
     * @param writeData        存放准备写入磁盘的数据
     * @param writeLength      写长度
     * @param isTotalFile      是否是覆盖写，true表示是，false表示否
     * @return 写入成功返回 1，失败返回对应错误码
     */
    public int writeFile(String fileAbsolutePath, byte[] writeData, int writeLength, boolean isTotalFile) throws Exception {
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        int entryStartNum = Integer.parseInt(fileInfo[2]);

        int result = openFile(fileAbsolutePath, "rw");
        if (result != 1) {
            return result;
        }
        OFTLE ofTle = ofTableManager.find(entryStartNum);

        // 检查文件是否以写方式打开
        if (ofTle.getOperateFlag() != 1 && ofTle.getOperateFlag() != 2) {
            return 0;
        }

        // 获取写指针
        Pointer writePointer = ofTle.getWrite();
        int writePointerBNum = writePointer.getbNum();
        int curBlockIndex = writePointer.getdNum();
        if (isTotalFile) {
            writePointerBNum = 0;
            curBlockIndex = entryStartNum;
        }
        int bytesWritten = 0;
        // 写入文件内容
        while (bytesWritten < writeLength) {
            byte[] blockData = this.entryOperator.getContentFromBlock(curBlockIndex);
            // 检查当前块是否有足够的空间写入数据
            while (writePointerBNum < blockData.length && bytesWritten < writeLength) {
                blockData[writePointerBNum] = writeData[bytesWritten];
                writePointerBNum++;
                bytesWritten++;
            }
            // 将修改后的数据写回块中
            this.entryOperator.setContentToEntry(curBlockIndex, blockData);

            // 检查是否需要移动到下一个块
            if (writePointerBNum >= blockData.length) {
                curBlockIndex = this.entryOperator.getNextBlockIndex(curBlockIndex);
                if (curBlockIndex == -1) {
                    // 如果没有下一个块，需要请求新的块
                    curBlockIndex = this.entryOperator.allocateDiskBlock();
                }
                writePointerBNum = 0; // 重置为新块的开始位置
            }
        }
        // 最后更新写指针的位置
        writePointer.setdNum(curBlockIndex);
        writePointer.setbNum(writePointerBNum);
        ofTle.setWrite(writePointer);
        // 关闭文件
        closeFile(fileAbsolutePath);

        return 1;
    }

    /**
     * 关闭文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 关闭成功返回 1
     */
    public int closeFile(String fileAbsolutePath) throws Exception {
        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);

        // 寻找该登记项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) return -1; // 文件不存在

        // 文件未打开
        OFTLE targetOftle = ofTableManager.find(fileEntry.getStartNum());
        if (targetOftle == null) return 1;

        // 如果是以写方式打开的，追加文件结束符并更新目录项
        if (targetOftle.getOperateFlag() == 1 || targetOftle.getOperateFlag() == 2) {
            // 追加文件结束符
            int dNum = targetOftle.getWrite().getdNum();
            int bNum = targetOftle.getWrite().getbNum();
            // 在第dNum块的第bNum个字节处中追加文件结束符
            byte[] content = this.entryOperator.getContentFromBlock(dNum);
            content[bNum] = BlockStatus.EOF.getValue();
            // 更新写指针
            targetOftle.setWrite(new Pointer(dNum, bNum));
            this.entryOperator.setContentToEntry(dNum, content);
        }
        // 从已打开文件表中删除对应项
        this.ofTableManager.remove(targetOftle);
        return 1;
    }

    /**
     * 删除文件
     *
     * @param fileAbsolutePath 文件名（含路径）
     * @return 删除成功返回 1， 文件不存在返回 -1， 文件已打开返回 -6
     */
    public int deleteFile(String fileAbsolutePath) throws Exception {
        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);
        // 寻找该登记项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) return -1; // 文件不存在

        // 检查文件是否已打开
        if (ofTableManager.find(fileEntry.getStartNum()) != null) {
            return -6;
        }

        // 删除文件目录项
        int fileBlockIndex = fileEntry.getStartNum();
        String fileNameAndType = new String(fileEntry.getName()).trim() + new String(fileEntry.getType()).trim();
        this.entryOperator.dealEntry(fileNameAndType, parentDirBlockIndex, fileBlockIndex);

        return 1;
    }


    /**
     * 显示文件内容
     *
     * @param fileAbsolutePath 文件完整路径
     * @return 文件内容的字符串表示，如果文件不存在或无法读取，则返回错误信息
     */
    public String typeFile(String fileAbsolutePath) throws Exception {
        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);
        // 找到文件目录项
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (fileEntry == null) {
            return "File does not exist.";
        }

        // 检查文件是否已打开
        if (ofTableManager.find(fileEntry.getStartNum()) != null) {
            return "Cannot display content of an open file.";
        }

        // 从目录中取出文件的起始盘块号
        byte startBlockIndex = fileEntry.getStartNum();
        return this.entryOperator.getAllFromEntry(startBlockIndex);
    }


    /**
     * 改变文件属性
     *
     * @param fileAbsolutePath 文件完整路径
     * @param newAttribute     新的文件属性
     * @return 1:成功， -1:文件不存在 ，-6:文件已打开
     */
    public int changeFileAttribute(String fileAbsolutePath, byte newAttribute) throws Exception {
        // 获取父目录盘块号和文件名，以检查父目录是否存在
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);
        // 寻找该登记项
        Entry existingEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (existingEntry == null) return -1; // 文件不存在

        // 检查文件是否已打开
        if (ofTableManager.find(existingEntry.getStartNum()) != null) {
            return -6;
        }

        // 改变文件属性
        existingEntry.setAttribute(newAttribute);
        this.entryOperator.setEntryToDirectory(parentDirBlockIndex, existingEntry);
        return 1;
    }

    /**
     * 改变文件名
     *
     * @param fileAbsolutePath 文件完整路径
     * @param newNameAndType   新的文件名
     * @return 1:成功， -1:文件不存在，-2：文件重名， -5：文件名不合法 ，-6:文件已打开
     */
    public int changeFileName(String fileAbsolutePath, String newNameAndType) throws Exception {
        String[] fileInfo = getFileInfo(fileAbsolutePath);
        String fileNameOnly = fileInfo[0];
        int parentDirBlockIndex = Integer.parseInt(fileInfo[1]);
        // 寻找该登记项
        Entry existingEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );
        if (existingEntry == null) return -1; // 文件不存在

        // 检查文件名（含type）
        byte[][] nameAndType = Tools.checkNameAndType(newNameAndType, false);
        if (nameAndType == null) {
            return -5;
        }
        // 分离文件名和类型
        byte[] newFileNameBytes = new byte[EntryStructure.NAME_LENGTH.getValue()];
        byte[] newFileType = new byte[EntryStructure.TYPE_LENGTH.getValue()];
        System.arraycopy(nameAndType[0], 0, newFileNameBytes, 0, newFileNameBytes.length);
        System.arraycopy(nameAndType[1], 0, newFileType, 0, newFileType.length);

        // 检查文件是否已打开
        if (ofTableManager.find(existingEntry.getStartNum()) != null) {
            return -6;
        }

        // 改变文件属性
        existingEntry.setName(newFileNameBytes);
        existingEntry.setType(newFileType);
        this.entryOperator.setEntryToDirectory(parentDirBlockIndex, existingEntry);
        return 1;
    }


    /**
     * 获取文件的各种信息
     *
     * @param fileAbsolutePath 文件绝对路径
     * @return [0]文件名（带type）、[1]文件父目录磁盘块号、[2]文件起始块号、[3]文件结束块号、[4]文件总字节数、[5]文件总磁盘块数、[6]文件属性
     */
    public String[] getFileInfo(String fileAbsolutePath) throws Exception {
        // 解析文件路径，获取父目录盘块号
        String[] pathComponents = fileAbsolutePath.split("/");
        String parentDirName = String.join("/", Arrays.copyOfRange(pathComponents, 0, pathComponents.length - 1));
        String fileNameOnly = pathComponents[pathComponents.length - 1];
        int parentDirBlockIndex = this.entryOperator.findDirBlockIndex(parentDirName);
        Entry fileEntry = this.entryOperator.findEntryInDirectory(
                parentDirBlockIndex,
                fileNameOnly,
                EntryAttribute.NORMAL_FILE.getValue()
        );

        byte attribute = fileEntry.getAttribute();
        int blockNum = fileEntry.getStartNum();
        int endNum = blockNum;
        int byteLength;
        int diskBlockLength = 0;
        // 遍历所有的磁盘块，记录总盘块数，找出最后一个磁盘块
        while (blockNum != -1) {
            diskBlockLength++;
            endNum = blockNum;      // 最后一个盘块号
            blockNum = this.entryOperator.getNextBlockIndex(blockNum);
        }

        // 计算最后一个盘块的实际字节长度，总字节数 = (总盘块数 - 1) * 64 + 最后一个盘块的字节数
        int lastDiskByteLength = 0;
        byte[] blockData = this.entryOperator.getContentFromBlock(endNum);
        for (int i = 0; i < DiskManager.BLOCK_SIZE; i++)
            if (blockData[i] == BlockStatus.EOF.getValue()) break;
            else lastDiskByteLength++;
        byteLength = (diskBlockLength - 1) * DiskManager.BLOCK_SIZE + lastDiskByteLength;

        if (parentDirName.isEmpty()) {
            parentDirName = "/";
        }

        return new String[]{
                fileNameOnly,
                String.valueOf(parentDirBlockIndex),
                String.valueOf(fileEntry.getStartNum()),
                String.valueOf(endNum),
                String.valueOf(byteLength),
                String.valueOf(diskBlockLength),
                String.valueOf(attribute)
        };
    }

}
