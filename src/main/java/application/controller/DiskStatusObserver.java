package application.controller;

/**
 * 外部调用磁盘更新 (方便分类功能)
 */
public interface DiskStatusObserver {
    void onDiskStatusChanged();
}