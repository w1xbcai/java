package com.huawei.bes.om.ctz.order1.batch.common.utils;

import com.huawei.bes.common.exception.BESException;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.task.intf.Taskable;
import com.huawei.bes.common.utils.validate.ValidateUtils;

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
    
    /**
     * 批量拉起模式
     * true: 表示需转换再拉起批量
     * false: 表示不需要转换
     */
    protected boolean execBatchMode = true;
    
    /**
     * 远程文件FTP服务工具
     */
    private BatchFtpService  ftpTool = null;
    
    private static final BesLog LOG = BesLogFactory.getLog(ExecBatchByFileBase.class);

    private String ftpServerName = null;
    private String ftpServerPwd = null;
    private String ftpServerIp = null;
    private String ftpServerPort = null;
    
    @Override
    public boolean execute(TaskItem taskItem)
    {
        initBatchPara();
        
        if (!checkRemoteFile())
        {
            
        }
        
        return false;
    }
    

    private void initBatchPara()
    {
        LOG.debug("ExecBatchByFileBase execute ..");
        if (!initBatchFileDir())
        {
            throw new BESException("60107011001", "ExecBatchByFileBase initBatchFileDir error");
        }
        
        ftpTool = getBatchFtpTool();
        if (null == ftpTool)
        {
            throw new BESException("60107011001", "ExecBatchByFileBase getBatchFtpTool error");
        }
    }

    private BatchFtpService getBatchFtpTool()
    {
        if ((null == ftpServerName)
                || (null == ftpServerPwd)
                || (null == ftpServerIp)
                || (null == ftpServerPort))
        {
            return null;
        }
        
        
        return null;
    }

    protected abstract boolean initBatchFileDir();
    
    protected abstract boolean checkRemoteFile();
    
    
    /**
     * 异常结束批量任务时,处理未释放的资源.
     */
    private void stopTaskWithError()
    {
        
    }
    

}
