package com.huawei.bes.om.ctz.order1.batch.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.huawei.bes.common.attachment.intf.ISMAttachment;
import com.huawei.bes.common.attachment.intf.impl.SMAttachmentImpl;
import com.huawei.bes.common.batch.helper.FileServerHelper;
import com.huawei.bes.common.batchtask.config.app.model.vo.BatchTaskFileStruEntityDTO;
import com.huawei.bes.common.batchtask.configbatch.app.business.bo.BatchDefineBOService;
import com.huawei.bes.common.batchtask.configbatch.app.model.pojo.BusiParamsMap;
import com.huawei.bes.common.batchtask.configbatch.app.model.vo.BatchDefineDTO;
import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.task.intf.Taskable;
import com.huawei.bes.common.utils.validate.ValidateUtils;
import com.huawei.soa.bdf.integration.util.ServiceHelper;

/**
 * 通过文件拉起批量
 * 该文件可以是第三方传过来需要转换的文件,也可以是直接可执行的批量文件.
 * 处理步骤:
 *  1, 初始化系统参数,包括文件目录,远程连接服务等.
 *  2, 处理文件前,对文件进行简单的校验.
 *  3, 文件转换,转换过程中也可以进行简单的数据判断.
 *  4, 初始化批量信息.
 *  5, 批量开始前进行些其他的归档等.
 *  6, 文件删除,移历史等操作
 * @author wWX377030
 * @date 2017-06-12
 * @since 
 */
@SuppressWarnings("deprecation")
public abstract class ExecBatchByFileBase implements Taskable
{
    private static final BesLog LOG = BesLogFactory.getLog(ExecBatchByFileBase.class);
    
    private List<String> batchFileList = null;
    
    protected String remoteFilePath = null;
    
    protected String ftpType;
    
    /**
     * 批量模板id
     */
    protected static String batchTempId = null;
    
    /**
     * 远程上传的文件后缀
     */
    protected static String remoteFileNameSuffix = ".csv";
    
    /**
     * 上传至批量框架的文件后缀
     */
    protected static String batchFileNameSuffix = ".txt";
    
    protected static BatchDefineDTO defDto;
    protected static List<BatchTaskFileStruEntityDTO> strDto;
    protected static BusiParamsMap mapDto;
    
    @Override
    public boolean execute(TaskItem taskItem)
    {
        initBatchPara();
        if ((null == batchFileList) || (batchFileList.size() < 1))
        {
            return true;
        }
        
        for (String batchFile : batchFileList)
        {
            dealOneFile(batchFile);
        }
        
        return true;
    }
    
    private void dealOneFile(String batchFile)
    {
        StringBuffer batchBuffer = null;
        StringBuffer batchDescBuffer = null;
        String fileId = null;
        if (!checkRemoteFile(batchFile))
        {
            dealRemoteErrorFile(batchFile);
            return;
        }

        if (!formatRemoteFileToBatch(batchFile, batchBuffer, batchDescBuffer))
        {
            if (dealRemoteFormatErrorFile(batchFile))
            {
                return;
            }
        }
        
        //这里应该会使用到一些批量相关的全局变量
        fileId = uploadBatchFile(batchFile, batchBuffer);
        
        addBatchTaskContextInfo(fileId);
    
        startBatchTask();
        
        deleteTempFile(batchFile);
        
        dealOtherThings();
    }

    protected abstract void dealOtherThings();

    protected abstract String getLocalFilePath();

    protected abstract void addBatchTaskExtendedInfo();

    protected abstract boolean dealRemoteFormatErrorFile(String batchFile);

    protected abstract boolean formatRemoteFileToBatch(String batchFile, StringBuffer batchBuffer, StringBuffer batchDescBuffer);
    
    protected abstract void dealRemoteErrorFile(String batchFile);
    
    protected abstract boolean initBatchFileDir();
    
    protected abstract boolean checkRemoteFile(String batchFile);
    
    private boolean initBatchFtpTool()
    {
        String ip = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.IP");
        int port = ConfigHelper.getShareConfig().getInt("OM.BATCH.TLF.PORT");
        String user = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.USERNAME");
        String pwd = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.PASSWORD");
        String ptc = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.PROTOCOL");
        if (ValidateUtils.isEmptyString(ip)
                || ValidateUtils.isEmptyString(user)
                || ValidateUtils.isEmptyString(pwd)
                || ValidateUtils.isEmptyString(ptc)
                || (port == 0))
        {
            return false;
        }
        ftpType = ptc.equals("SFTP") ? "defaultSftp" : "noDefine";
        if (FtpServicePool.addFtpConnect(ftpType, ip, port, user, pwd))
        {
            return true;
        }
        return false;
    }
    
    private void addBatchTaskContextInfo(String fileId){
        ISMAttachment sMAttachmentImpl = new SMAttachmentImpl();
        String encryptFileId = sMAttachmentImpl.encryptAttachmentId(fileId);
        LOG.debug("#BatchDropSubs4SapTask handleFile encryptFileId:", encryptFileId);
        List<String> fileList = new ArrayList<String>();
        fileList.add(encryptFileId);
        mapDto.setAttachmentIdList(fileList);

    }
    
    private void deleteTempFile(String batchFile)
    {
        String path = getLocalFilePath();
        // 删除文件
        File localFile = new File(path + batchFile);
        boolean rt = localFile.delete();
        boolean deleteFail = false;
        if (rt == deleteFail)
        {
            LOG.error("delete file ", batchFile, "failed");
        }
    }
    
    protected List<String> getBatchFileList()
    {
        return FtpServicePool.getFtpConect(ftpType).getFileNameList(remoteFilePath);
    }

    private void initBatchPara()
    {
        LOG.debug("ExecBatchByFileBase execute ..");
        if (!initBatchFileDir())
        {
            throw new BESException("60107011001", "ExecBatchByFileBase initBatchFileDir error");
        }
        
        if (initBatchFtpTool())
        {
            throw new BESException("60107011001", "ExecBatchByFileBase getBatchFtpTool error");
        }
        
        batchFileList = getBatchFileList();
    }

    private String uploadBatchFile(String batchFile, StringBuffer batchBuffer)
    {
        batchFile = batchFile.replaceFirst(remoteFileNameSuffix, batchFileNameSuffix);
        File file = FileServerHelper.createLocalFile(batchFile, null);
        try
        {
            FileUtils.writeStringToFile(file, batchBuffer.toString(), "UTF-8");
        } catch (IOException e)
        {
            e.printStackTrace();
            LOG.error(e, "uploadBatchFile IOException class error!");
        }
        return FileServerHelper.uplodaBatchFile(file);
    }


    protected long startBatchTask()
    {
        if ((null == defDto) || (null == strDto) || (null == mapDto))
        {
            throw new BESException(83948938493L, "");
        }
        BatchDefineBOService batchDefineBOService = (BatchDefineBOService) ServiceHelper
                .getService(BatchDefineBOService.SERVICE_NAME);
        //调用批量框架创建批量
        long taskId = batchDefineBOService.createAndStartBatchTask(defDto, strDto, mapDto);
        return taskId;
    }
    
    protected String addFileSeparator(String path)
    {
        if (!path.endsWith(File.separator))
        {
            path = path + File.separator;
        }
        return path;
    }
    
    
    //目前的检查项，1，重名。2，名称过长。3，名称含有特殊字符。4，内容格式不正确。
    

}
