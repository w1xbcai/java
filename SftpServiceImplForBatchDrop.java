package com.huawei.bes.om.ctz.order1.batch.dropsubs.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.file.ftp.constant.MsgNo;
import com.huawei.bes.common.file.ftp.domain.FtpProperty;
import com.huawei.bes.common.file.ftp.service.FtpService;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.utils.validate.ValidateHelper;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * 批销文件操作帮助类
 * @author wWX377030
 * 
 * @date  2017-6-6
 *
 */
@SuppressWarnings("deprecation")
public class SftpServiceImplForBatchDrop implements FtpService
{
    private static final BesLog LOG = BesLogFactory.getLog(SftpServiceImplForBatchDrop.class);
    
    /**
     * 连接Session
     */
    private Session sftpSession = null;
    
    /**
     * SFTP功能类
     */
    private JSch jsch = null;
    
    /**
     * SFTP传输渠道
     */
    private Channel sftpChannel = null;
    
    public void uploadStrToFile(InputStream inputStr, String dstFile)
    {
        ValidateHelper.throwEmptyStringException(dstFile);
        ValidateHelper.throwNullObjException(inputStr);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.put(inputStr, dstFile);
        } 
        catch (SftpException ex)
        {
            throw new BESException(MsgNo.ERR_SFTP_UPLOAD_FILE, ex);
        }
        finally
        {
            //关闭输入流
            if (null != inputStr)
            {
                try
                {
                    inputStr.close();
                }
                catch (IOException ex)
                {
                    LOG.error(ex, "IO error!");
                }
            }
        }
    }
    
    
    /**
     * 连接服务器
     * @param serverIp 服务器IP
     * @param serverPort 服务器端口
     * @param userName 用户名
     * @param passWd 密码
     */
    @Override
    public void connect(String serverIp, int serverPort, String userName,
            String passWd)
    {
        ValidateHelper.throwEmptyStringException(serverIp);
        ValidateHelper.throwEmptyStringException(userName);
        ValidateHelper.throwEmptyStringException(passWd);
        
        boolean success = false;
        try
        {
            jsch = new JSch();
            
            //获得连接的Session
            sftpSession = jsch.getSession(userName, serverIp, serverPort);
            
            //设置Session密码
            sftpSession.setPassword(passWd);
            
            Properties prop = new Properties();
            prop.setProperty("StrictHostKeyChecking", "no");
            
            sftpSession.setConfig(prop);
            
            //session连接
            sftpSession.connect();
            
            //打开数据传输渠道
            sftpChannel = sftpSession.openChannel("sftp");
            
            //传输渠道连接
            sftpChannel.connect();
            success = true;
        }
        catch (JSchException ex)
        {
            throw new BESException(MsgNo.ERR_SFTP_CAN_NOT_CONNECT, ex);
        }
        finally
        {
            //若连接不成功，则关闭连接
            if (!success)
            {
                close();
            }
        }
    }
    
    /**
     * 关闭 void
     */
    private void close()
    {
        //若传输渠道连接没有关闭，则关闭渠道
        if (null != sftpChannel && sftpChannel.isConnected())
        {
            try
            {
                sftpChannel.disconnect();
            }
            catch (Exception ex)
            {
                throw new BESException(MsgNo.ERR_SFTP_RELEASE_CONNECT, ex);
            }
        }
        
        //若Session连接没有关闭，则关闭Session
        if (null != sftpSession && sftpSession.isConnected())
        {
            try
            {
                sftpSession.disconnect();
            }
            catch (Exception ex)
            {
                throw new BESException(MsgNo.ERR_SFTP_RELEASE_CONNECT, ex);
            }
        }
    }
    
    /**
     * 上传文件
     * @param srcFile 源文件
     * @param dstFile 目标文件
     */
    @Override
    public void uploadFile(String srcFile, String dstFile)
    {
        ValidateHelper.throwEmptyStringException(srcFile);
        ValidateHelper.throwEmptyStringException(dstFile);
        
        uploadFile(new File(srcFile), dstFile);
    }
    
    /**
     * 上传File对象对应的文件
     *
     * @param file File对象
     * @param dstFile 文件上传的目的路径(包含文件路径和文件名)
     */
    @Override
    public void uploadFile(File file, String dstFile)
    {
        ValidateHelper.throwEmptyStringException(dstFile);
        
        InputStream input = null;
        
        try
        {
            ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
            
            //打开输入流
            input = new FileInputStream(file);
            
            //使用指定了文件路径和文件名的路径上传
            channelSftp.put(input, dstFile);
            //记录成功上传记录
            LOG.debug("Uploading success, dstFile=", dstFile);
        }
        catch (Exception ex)
        {
            throw new BESException(MsgNo.ERR_SFTP_UPLOAD_FILE, ex);
        }
        finally
        {
            //关闭输入流
            if (null != input)
            {
                try
                {
                    input.close();
                }
                catch (IOException ex)
                {
                    LOG.error(ex, "IO error!");
                }
            }
        }
    }
    
    /**
     * 上传文件
     * 
     * @param property 上传文件时的属性对象
     */
    @Override
    public void uploadFile(FtpProperty property)
    {
        //若通过File上传文件
        File file = property.getFile();
        
        if (null != file)
        {
            //File对象上传文件
            uploadFile(file, property.getRemoteFile());
        }
        else
        {
            //通过本地路径和远程路径上传文件
            uploadFile(property.getLocalFile(), property.getRemoteFile());
        }
    }
    
    /**
     * 下载文件
     * @param srcFile 源文件
     * @param dstFile 目标文件
     */
    @Override
    public void downloadFile(String srcFile, String dstFile)
    {
        ValidateHelper.throwEmptyStringException(srcFile);
        ValidateHelper.throwEmptyStringException(dstFile);
        
        BufferedOutputStream output = null;
        try
        {
            output = new BufferedOutputStream(new FileOutputStream(dstFile));
            ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
            channelSftp.get(srcFile, output);
        }
        catch (Exception ex)
        {
            throw new BESException(MsgNo.ERR_SFTP_DOWNLOAD_FILE, ex);
        }
        finally
        {
            //关闭输入流
            if (null != output)
            {
                try
                {
                    output.close();
                }
                catch (IOException ex)
                {
                    LOG.error(ex, "IO errror!");
                }
            }
        }
    }
    
    /**
     * 下载文件
     * 
     * @param property 下载文件时的属性对象
     */
    @Override
    public void downloadFile(FtpProperty property)
    {
        //下载文件
        if (ValidateHelper.isNotEmptyString(property.getLocalFile())
                && ValidateHelper.isNotEmptyString(property.getRemoteFile()))
        {
            //通过本地路径和远程路径上传文件
            downloadFile(property.getRemoteFile(), property.getLocalFile());
        }
    }
    
    /**
     * 断开连接
     */
    @Override
    public void disconnect()
    {
        close();
    }
    
    /**
     * 新建文件夹
     * 
     * @param dir 文件夹名称
     */
    @Override
    public void makeDir(String dir)
    {
        
    }
    
    /**
     * 删除文件夹
     * 
     * @param pathName 文件夹名称
     */
    @Override
    public void deletePath(String pathName)
    {
        ValidateHelper.throwEmptyStringException(pathName);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.rmdir(pathName);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_403#deletePath()#pathName=" , pathName);
            throw new BESException(MsgNo.ERR_SFTP_DELETEFILE, ex);
        }
    }
    
    /**
     * 删除文件
     * 
     * @param fileName 文件名称
     */
    @Override
    public void deleteFile(String fileName)
    {
        ValidateHelper.throwEmptyStringException(fileName);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.rm(fileName);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_425#deleteFile()#fileName=" , fileName);
            throw new BESException(MsgNo.ERR_SFTP_DELETEFILE, ex);
        }
    }
    
    /**
     * 重命名
     * 
     * @param oldFile 原名
     * @param newFile 新名
     */
    @Override
    public void renameFileName(String oldFile, String newFile)
    {
        ValidateHelper.throwEmptyStringException(oldFile);
        ValidateHelper.throwEmptyStringException(newFile);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        
        //服务器端文件重命名
        try
        {
            channelSftp.rename(oldFile, newFile);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_450#renameFileName#oldFile=" , oldFile
                    , ",newFile=" , newFile);
            throw new BESException(MsgNo.ERR_SFTP_RENAME_FILE, ex);
        }
    }
    
    /**
     * 复制文件
     * 
     * @param srcPathFileName 原名(包含文件路径和文件名)
     * @param targetPath 新名(包含文件路径)
     * @param targetFileName 文件名
     */
    @Override
    public void copyFile(String srcPathFileName, String targetPath,
            String targetFileName)
    {
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        
        // 在服务器目录里 复制文件 
        try
        {
            channelSftp.symlink(srcPathFileName, targetPath + targetFileName);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_475#srcPathFileName=" , srcPathFileName);
            throw new BESException(MsgNo.ERR_SFTP_RENAME_FILE, ex);
        }
    }
    
    /**
     * 列出所有文件名称
     * @param dir 目录名称
     * @return List 文件名称列表
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> listFileNames(String dir)
    {
        ValidateHelper.throwEmptyStringException(dir);
        
        List<String> fileNameList = new ArrayList<String>();
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        //获取文件名列表
        try
        {
            List<LsEntry> fileInfos = channelSftp.ls(dir);
            for (LsEntry fileInfo : fileInfos)
            {
                if (fileInfo.getAttrs().isDir())
                {
                    continue;
                }
                
                fileNameList.add(fileInfo.getFilename());
            }
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_507#listFileNames()#dir=" , dir);
            throw new BESException(MsgNo.ERR_SFTP_LISTFILENAMES, ex);
        }
        
        return fileNameList;
    }
    
    /**
     * 
     * 列出所有路径下的子目录
     *
     * @param dir 目录名称
     * @return List 目录下一级子目录名称
     */
    @Override
    public List<String> listDirNames(String dir)
    {
        ValidateHelper.throwEmptyStringException(dir);
        
        List<String> dirNameList = new ArrayList<String>();
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        //获取文件名列表
        try
        {
            @SuppressWarnings("unchecked")
            List<LsEntry> fileInfos = channelSftp.ls(dir);
            for (LsEntry fileInfo : fileInfos)
            {
                if (fileInfo.getAttrs().isDir())
                {
                    dirNameList.add(fileInfo.getFilename());
                }
            }
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_540#listDirNames()#dir=" , dir);
            throw new BESException(MsgNo.ERR_SFTP_LISTFILENAMES, ex);
        }
        
        return dirNameList;
    }
    
    /**
     * 改变当前目录
     * 
     * @param dir 目录名称
     */
    @Override
    public void changeDir(String dir)
    {
        ValidateHelper.throwEmptyStringException(dir);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.cd(dir);//切换到对应的目录
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_562#changeDir()#dir=" , dir);
            throw new BESException(MsgNo.ERR_SFTP_CHANGEDIR, ex);
        }
    }
    
    /**
     * 检查ftp某文件夹是否存在。
     * 
     * @param dir 需要检查是否存在的文件夹
     */
    @Override
    public boolean isDirExist(String dir)
    {
        ValidateHelper.throwEmptyStringException(dir);
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            channelSftp.cd(dir); //切换到对应的目录
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_584#isDirExist()#dir=" , dir);
            return false;
        }
        return true;
    }
    
    /**
     * 返回文件输入流
     * @param dstFile 包含文件路径和文件名
     * @return InputStream 返回的文件流
     */
    @Override
    public InputStream getInputStream(String dstFile)
    {
        ValidateHelper.throwEmptyStringException(dstFile);
        
        InputStream in = null;
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            in = channelSftp.get(dstFile);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_605#ex=");
        }
        
        return in;
    }
    
    /**
     * 返回文件输出流
     * @param dstFile 包含文件路径和文件名
     * @return OutputStream 返回的文件流
     */
    @Override
    public OutputStream getOutputStream(String dstFile)
    {
        ValidateHelper.throwEmptyStringException(dstFile);
        
        OutputStream out = null;
        
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        try
        {
            out = channelSftp.put(dstFile);
        }
        catch (SftpException ex)
        {
            LOG.error(ex, "ERR_630#ex=");
        }
        
        return out;
    }
    
    /**
     * 获取当前目录
     * @return String
     */
    @Override
    public String currentDir()
    {
        ChannelSftp channelSftp = (ChannelSftp)sftpChannel;
        String retStr = "";
        try
        {
            retStr = channelSftp.pwd();
        }
        catch (SftpException e)
        {
            LOG.error(e, "2001106042L");
        }
        return retStr;
    }
    
    /**
     * 
     * 列出所有路径下的子目录
     *
     * @param dir 目录名称
     * @return List 目录下的文件名
     */
    @Override
    public List<String> listFileNamesOfDir(String dir)
    {
        return null;
    }
    

}
