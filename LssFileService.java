/**
 * 
 */
package com.huawei.bes.om.ctz.order1.batch.common.utils;

import java.util.List;

/**
 * lss 服务器文件处理接口
 * @author wWX377030
 *
 */
public interface LssFileService
{

    /**
     * 连接服务器
     * @param serverIp 服务器IP
     * @param serverPort 服务器端口
     * @param userName 用户名
     * @param passWd 密码
     */
    void connect(String serverIp, int serverPort, String userName, String passWd);
    
    /**
     * 断开连接
     */
    void close();
    
    List<String> listFileNames(String path);
    
    void uploadFile(String localFile, String remoteFile);
    void uploadFile(String localPath, String srcName, String remotePath, String destName);
    
    void downLoadFileFromLss(String remoteFile, String localFile);
    void downLoadFile(String remotePath, String srcName, String localPath, String destName);
    
    void moveFile(String oldFile, String newFile);
    void moveFile(String oldPath, String oldName, String newPath, String newName);
    
    long getFileSize(String file);
    long getFileSize(String path, String name);
    
    boolean mkdirIfNotExist(String path);
    boolean mkdirIfNotExist(String... path);
    
    void writeStrToNewFile(String msg, String dstFile);
    void writeStrToNewFile(String msg, String path, String name);
    
    void deleteFile(String file);
    void deleteFile(String path, String name);
}
