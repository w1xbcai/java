package com.huawei.bes.om.ctz.order1.batch.common.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import com.huawei.bes.common.batch.helper.FileServerHelper;
import com.huawei.bes.common.batchtask.config.app.model.vo.BatchTaskFileStruEntityDTO;
import com.huawei.bes.common.batchtask.configbatch.app.business.bo.BatchDefineBOService;
import com.huawei.bes.common.batchtask.configbatch.app.model.pojo.BusiParamsMap;
import com.huawei.bes.common.batchtask.configbatch.app.model.vo.BatchDefineDTO;
import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.task.intf.Taskable;
import com.huawei.soa.bdf.integration.util.ServiceHelper;

/**
 * lss服务器上传文件方式拉起批开基础类
 * @author wWX377030
 * 
 */
@SuppressWarnings("deprecation")
public abstract class LssBatchBase implements Taskable
{

    private static final BesLog LOG = BesLogFactory.getLog(LssBatchBase.class);

    protected String remoteInputPath;
    protected String remoteInputHisPath;
    protected String localTempPath;
    protected String localErrorPath;

    protected String bathcTemplateId;

    protected String businessCode;

    protected List<String> hisFileNameList = new ArrayList<String>();

    protected BatchDefineDTO defDto = null;

    protected List<BatchTaskFileStruEntityDTO> batchFileStruct = null;

    /**
     * 远程上传的文件后缀
     */
    protected String remoteFileNameSuffix = ".csv";

    /**
     * 上传至批量框架的文件后缀
     */
    protected String batchFileNameSuffix = ".txt";

    protected int minFileSize;

    protected int maxFileSize;

    protected int maxFileLines;

    protected String fileNameRegex = "\\w{0,50}.csv";

    protected boolean allowRepeatFileName = false;

    protected LssSftpFileHelper sftpHelper;

    public boolean execute(TaskItem taskItem)
    {
        try
        {
            LOG.debug("LssBatchBase start ", taskItem.getBeId());
            if (!checkLssFileService() || !checkBatchDefine(taskItem)
                    || !checkBatchFileDir() || !checkOtherCfg())
            {
                return false;
            }
            LOG.debug("LssBatchBase check ok");

            initCommonContext(taskItem);
            LOG.debug("LssBatchBase initCommonContext ok");

            List<String> inputFiles = sftpHelper.listFileNames(remoteInputPath);
            LOG.debug("inputFiles:", inputFiles.size());
            if (!inputFiles.isEmpty())
            {
                hisFileNameList = sftpHelper.listFileNames(remoteInputHisPath);
                LOG.debug("hisFileNameList:", hisFileNameList.size());
            }

            BatchDefineBOService batchDefineBOService = (BatchDefineBOService) ServiceHelper
                    .getService(BatchDefineBOService.SERVICE_NAME);
            for (String batchFile : inputFiles)
            {
                dealOneFile(batchFile, batchDefineBOService);
            }
        } 
        catch (Exception e)
        {
            LOG.error("LssBatchBase ", e.getCause());
            LOG.error(e.getCause());
        } 
        finally
        {
            if (null != sftpHelper)
            {
                sftpHelper.close();
            }
        }
        return true;
    }

    protected abstract boolean checkOtherCfg();

    protected abstract void initCommonContext(TaskItem taskItem);

    /**
     * 处理一个lss服务器的批量文件
     * @param oneFile
     */
    private void dealOneFile(String batchFile,
            BatchDefineBOService batchDefineBOService)
    {
        dealSubClassBeforeBatch(batchFile);
        LOG.debug("LssBatchBase start deal ", batchFile);
        // 校验文件名称,大小,是否重复
        if (!checkFileName(batchFile) || checkRepeatFile(batchFile)
                || !checkFileSize(batchFile))
        {
            LOG.debug("LssBatchBase dealRemoteErrorFile ", batchFile);
            dealRemoteErrorFile(batchFile);
            sftpHelper.deleteFile(remoteInputPath, batchFile);
            return;
        }
        LOG.debug("LssBatchBase checkFile ok");

        // 下载文件到本地
        sftpHelper.downLoadFile(remoteInputPath, batchFile, localTempPath,
                batchFile);
        LOG.debug("LssBatchBase downLoadFile ok");
        // 移动文件到历史
        sftpHelper.moveFile(remoteInputPath, batchFile, remoteInputHisPath,
                batchFile);
        LOG.debug("LssBatchBase moveFile ok");

        StringBuffer desc = new StringBuffer();
        StringBuffer succContent = new StringBuffer();
        StringBuffer errorContent = new StringBuffer();
        int count = formatToBatchFile(localTempPath + batchFile, desc,
                succContent, errorContent);
        // LOG.debug("LssBatchBase formatToBatchFile count ", count);
        // LOG.debug("LssBatchBase formatToBatchFile desc ", desc);
        // LOG.debug("LssBatchBase formatToBatchFile succContent ", succContent);
        // LOG.debug("LssBatchBase formatToBatchFile errorContent ", errorContent);
        if (count > maxFileLines)
        {
            dealLocalErrorFile(batchFile);
            return;
        }

        if (errorContent.length() > 0)
        {
            dealFormatErrorContent(batchFile, errorContent);
        }

        // 默认只要有正确的记录就可以拉起批量
        if ((count < 0) && (succContent.length() == 0))
        {
            deleteTempFile(batchFile);
            return;
        }
        LOG.debug("LssBatchBase formatToBatchFile ok");

        // 这里应该会使用到一些批量相关的全局变量
        String fileId = uploadBatchFile(batchFile, succContent);
        LOG.debug("LssBatchBase fileId is ", fileId);

        long batchNo = startBatchTask(batchDefineBOService, fileId);
        LOG.debug("LssBatchBase batchNo is ", batchNo);

        deleteTempFile(batchFile);
        LOG.debug("LssBatchBase deleteTempFile ok");

        dealSubClassAfterBatch(batchFile, batchNo, desc, succContent,
                errorContent);
    }

    /**
     * 批量开始后子类预留的处理方法
     * @param batchFile
     * @param batchNo
     * @param desc
     * @param succContent
     * @param errorContent
     */
    protected abstract void dealSubClassAfterBatch(String batchFile,
            long batchNo, StringBuffer desc, StringBuffer succContent,
            StringBuffer errorContent);

    /**
     * 批量开始前子类预留的处理方法
     * @param batchFile
     */
    protected abstract void dealSubClassBeforeBatch(String batchFile);

    /**
     * 开始批量
     * @param batchDefineBOService
     * @param fileId
     */
    private long startBatchTask(BatchDefineBOService batchDefineBOService,
            String fileId)
    {
        if ((null == defDto) || (null == batchFileStruct))
        {
            return -1;
        }
        defDto.setContext(buildBatchContext());
        BusiParamsMap busiParamMap = new BusiParamsMap();
        busiParamMap.setAttachmentIdList(Arrays.asList(fileId));
        busiParamMap.setSync(0);
        busiParamMap.setBatchNo(0);
        busiParamMap.setNum(1);

        long taskId = batchDefineBOService.createAndStartBatchTask(defDto,
                batchFileStruct, busiParamMap);
        return taskId;
    }

    protected abstract String buildBatchContext();

    /**
     * 处理错误的文件内容
     * @param batchFile
     * @param errorContent
     */
    protected abstract void dealFormatErrorContent(String batchFile,
            StringBuffer errorContent);

    /**
     * 将文件格式转换成批量框架可执行的格式
     * @param batchFile
     * @param desc
     * @param succContent
     * @param errorContent
     * @return
     */
    protected abstract int formatToBatchFile(String batchFile,
            StringBuffer desc, StringBuffer succContent,
            StringBuffer errorContent);

    /**
     * 处理lss服务器上的错误文件
     * @param batchFile
     */
    protected abstract void dealRemoteErrorFile(String batchFile);

    /**
     * 处理本地临时目录中错误文件
     * @param batchFile
     */
    protected abstract void dealLocalErrorFile(String batchFile);

    private boolean checkFileName(String name)
    {
        LOG.debug("LssBatchBase checkFileName ", fileNameRegex);
        return Pattern.matches(fileNameRegex, name);
    }

    private boolean checkRepeatFile(String name)
    {
        LOG.debug("LssBatchBase checkRepeatFile ", allowRepeatFileName);
        return allowRepeatFileName || hisFileNameList.contains(name);
    }

    private boolean checkFileSize(String name)
    {
        long size = sftpHelper.getFileSize(remoteInputPath + name);
        LOG.debug("LssBatchBase checkFileSize ", size);
        return (size > minFileSize) && (size < maxFileSize);
    }

    protected abstract boolean checkBatchFileDir();

    protected abstract boolean checkBatchDefine(TaskItem taskItem);

    protected boolean checkLssFileService()
    {
        sftpHelper = new LssSftpFileHelper();
        String remoteIP = ConfigHelper.getShareConfig().getString(
                "OM.BATCH.TLF.IP", "100.107.182.102");
        int remotePort = ConfigHelper.getShareConfig().getInt(
                "OM.BATCH.TLF.PORT", 22);
        String userName = ConfigHelper.getShareConfig().getString(
                "OM.BATCH.TLF.USERNAME", "besread");
        String password = ConfigHelper.getShareConfig().getString(
                "OM.BATCH.TLF.PASSWORD", "huaweif2");
        sftpHelper.connect(remoteIP, Integer.valueOf(remotePort), userName,
                password);
        return true;
    }

    private String uploadBatchFile(String batchFile, StringBuffer batchBuffer)
    {
        batchFile = batchFile.replaceFirst(remoteFileNameSuffix,
                batchFileNameSuffix);
        File file = FileServerHelper.createLocalFile(batchFile, null);
        try
        {
            FileUtils.writeStringToFile(file, batchBuffer.toString(), "UTF-8");
        } 
        catch (IOException e)
        {
            LOG.debug(e, "uploadBatchFile IOException class error!");
        }
        return FileServerHelper.uplodaBatchFile(file);
    }

    private void deleteTempFile(String batchFile)
    {
        // 删除文件
        File localFile = new File(localTempPath + batchFile);
        if (localFile.exists())
        {
            if (!localFile.delete())
            {
                LOG.debug("deleteTempFile error ", batchFile);
            }
        }
    }

    protected boolean mklocaldirIfNotExist(String... dir)
    {
        for (String oneDir : dir)
        {
            File temp = new File(oneDir);
            if (temp.exists() && temp.isDirectory())
            {
                continue;
            }
            else
            {
                if (!temp.mkdirs() || !temp.setReadable(true)
                        || !temp.setWritable(true) || !temp.setExecutable(true))
                {
                    return false;
                }
            }
        }
        return true;
    }

    protected String addFileSeparator(String path)
    {
        if (!path.endsWith(File.separator))
        {
            path = path + File.separator;
        }
        return path;
    }

}
