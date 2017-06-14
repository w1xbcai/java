package com.huawei.bes.om.ctz.order1.batch.dropsubs.business;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huawei.bes.common.adapter.bdf.sequence.SequenceUtil;
import com.huawei.bes.common.attachment.intf.ISMAttachment;
import com.huawei.bes.common.attachment.intf.impl.SMAttachmentImpl;
import com.huawei.bes.common.batch.helper.FileServerHelper;
import com.huawei.bes.common.batchtask.config.app.model.vo.BatchTaskFileStruEntityDTO;
import com.huawei.bes.common.batchtask.configbatch.app.business.bo.BatchDefineBOService;
import com.huawei.bes.common.batchtask.configbatch.app.model.pojo.BusiParamsMap;
import com.huawei.bes.common.batchtask.configbatch.app.model.vo.BatchDefineDTO;
import com.huawei.bes.common.config.ConfigHelper;
import com.huawei.bes.common.context.ContextHelper;
import com.huawei.bes.common.log.BesLog;
import com.huawei.bes.common.log.BesLogFactory;
import com.huawei.bes.common.parse.FileParser;
import com.huawei.bes.common.task.intf.TaskItem;
import com.huawei.bes.common.task.intf.Taskable;
import com.huawei.bes.common.utils.time.DateTimeHelper;
import com.huawei.bes.common.utils.validate.ValidateUtils;
import com.huawei.bes.oh.base.batch.schema.app.model.xmlvo.batchservice.BatchServiceInfoXMLVO;
import com.huawei.bes.oh.base.batch.schema.app.model.xmlvo.batchtype.BatchExtParamInfoXMLVO;
import com.huawei.bes.oh.biz.orderex4telecom.constant.OrderConstant.OrderCreateConsts;
import com.huawei.bes.oh.biz.orderex4telecom.utils.SMEmployeeHelper;
import com.huawei.bes.om.ctz.order.cz4colombiamobile1.vo.OmBatchOtherSysExEntity;
import com.huawei.bes.om.ctz.order.cz4colombiamobile1.vo.OmBatchOtherSysExEntityDTO;
import com.huawei.bes.om.ctz.order1.batch.common.utils.FileManger;
import com.huawei.bes.om.ctz.order1.batch.createsubs.business.bo.OmBatchOtherSysBOImpService;
import com.huawei.bes.om.ctz.order1.batch.dropsubs.constant.Batch4DropSubsConsts;
import com.huawei.bes.sm.orgstaff.domain.employee.view.SMEmployeeDTO;
import com.huawei.bes.sm.orgstaff.intf.SMOrgIntfBOService;
import com.huawei.soa.bdf.integration.util.ServiceHelper;
import com.huawei.soa.daf.util.DafJsonUtils;

/**
 * SAP批量销户，定时任务，文件解析、上传，上下文构造
 * @author jWX384138
 * @date 2017-05-08
 */
@SuppressWarnings("deprecation")
public class BatchDropSubs4SapTask extends BatchFileHandle implements Taskable
{
    /**
     * 日志
     */
    private static final BesLog LOG = BesLogFactory.getLog(BatchDropSubs4SapTask.class);

    @Override
    public boolean dealExecuteException(TaskItem arg0, Throwable arg1)
    {
        return false;
    }

    @Override
    public boolean dealExpired(TaskItem arg0)
    {
        return false;
    }

    @Override
    public boolean dealTimeout(TaskItem arg0)
    {
        return false;
    }

    @Override
    public boolean execute(TaskItem taskItem)
    {
        if (LOG.isDebugEnabled())
        {
            LOG.debug("#BatchDropSubs4SapTask execute taskItem is:", JSONObject.toJSONString(taskItem));
        }
        this.initCommonContext(taskItem);
        this.parseFile();
        return true;
    }

    /**
     * 初始化上下文信息
     * @param taskItem
     */
    private void initCommonContext(TaskItem taskItem)
    {
        //取定时任务配置的BeId和BizBeId
        ContextHelper.getContextAccessor().setBeId(taskItem.getBeId());
        ContextHelper.getContextAccessor().setBizBeId(taskItem.getBeId());
        //其他上下文信息取默认操作员的，基线为101
        String operator = ConfigHelper.getShareConfig().getString(OrderCreateConsts.DEFAULT_OPER_KEY);
        ContextHelper.getContextAccessor().setOperator(operator);
        SMEmployeeDTO employeeVO = SMEmployeeHelper.getEmployee(new BigDecimal(operator));
        String channelType = this.getChannelType(employeeVO.getOrgId());
        LOG.debug("#BatchDropSubs4SapTask channelType is", channelType);
        ContextHelper.getContextAccessor().setChannelId(channelType);
        ContextHelper.getContextAccessor().setChannelType(channelType);
        ContextHelper.getContextAccessor().setDeptId(String.valueOf(employeeVO.getOrgId()));
        ContextHelper.getContextAccessor().setBizDeptId(String.valueOf(employeeVO.getOrgId()));
        ContextHelper.getContextAccessor().setLoginId(employeeVO.getEmployeeCode());
        ContextHelper.getContextAccessor().setBeCode(taskItem.getBeId());
        if (null != employeeVO.getbRegionId())
        {
            ContextHelper.getContextAccessor().setRegionId(employeeVO.getbRegionId().toString());
        }
        if (null != Locale.getDefault())
        {
            ContextHelper.getContextAccessor().setLocale(Locale.getDefault().toString());
        }
    }

    /**
     * 根据orgId查询渠道类型
     * @param orgId
     * @return
     */
    private String getChannelType(BigDecimal orgId)
    {
        LOG.debug("#getChannelType orgId ", orgId);
        SMOrgIntfBOService service = (SMOrgIntfBOService) ServiceHelper.getService(SMOrgIntfBOService.SERVICE_NAME);
        String salesChannel = service.querySalesChannelByOperOrOrgId(null, orgId);
        LOG.debug("#getChannelType return ", salesChannel);
        return salesChannel;
    }

    @Override
    protected void handleFile(String fileName, String localFilePath)
    {
        LOG.debug("#BatchDropSubs4SapTask handleFile fileName:", fileName, ";localFilePath:", localFilePath);
        //1.创建文件
        String[] fileNames = fileName.split("\\.");
        String uploadLssFileName = fileNames[0] + ".txt";
        List<String> fileContents = new ArrayList<String>();
        String fileContent = this.buildFileContent(localFilePath);
        if (ValidateUtils.equals("false", fileContent))
        {
            String errorFilePath = ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.LOCAL_ERR_PATH");
            if (!errorFilePath.endsWith("/"))
            {
                errorFilePath = errorFilePath + "/";
            }
            String errorFilePathStr = errorFilePath + fileNames[0] + ".csv";
            FileManger.moveFile(localFilePath, errorFilePathStr);
            return;
        }
        String[] fileParseResult = fileContent.split("&");
        //构造上传批量文件内容
        fileContents.add(fileParseResult[1]);
        File file = FileServerHelper.createLocalFile(uploadLssFileName, null);
        try
        {
            FileUtils.writeLines(file, "UTF-8", fileContents, "\r\n");
        }
        catch (IOException e)
        {
            LOG.error(e, "IOException class error!");
            return;
        }
        //2.上传文件
        String fileId = FileServerHelper.uplodaBatchFile(file);
        ISMAttachment sMAttachmentImpl = new SMAttachmentImpl();
        String encryptFileId = sMAttachmentImpl.encryptAttachmentId(fileId);
        LOG.debug("#BatchDropSubs4SapTask handleFile encryptFileId:", encryptFileId);

        //构造上下文
        BatchDefineBOService batchDefineBOService = (BatchDefineBOService) ServiceHelper
                .getService(BatchDefineBOService.SERVICE_NAME);
        BatchDefineDTO defDto = batchDefineBOService.queryDefineVOById(Batch4DropSubsConsts.BATCH_TEMPLATE_IDLE_ID);
        List<BatchTaskFileStruEntityDTO> strDto = batchDefineBOService
                .queryBatchDefStru(Batch4DropSubsConsts.BATCH_TEMPLATE_IDLE_ID);
        defDto.setContext(this.buildDefDtoContext(defDto));
        BusiParamsMap mapDto = new BusiParamsMap();
        List<String> fileList = new ArrayList<String>();
        fileList.add(encryptFileId);
        mapDto.setAttachmentIdList(fileList);
        mapDto.setBatchNo(0);
        mapDto.setSync(0);
        mapDto.setNum(1);
        if (LOG.isDebugEnabled())
        {
            LOG.debug("#BatchDropSubs4SapTask handleFile defDto:", JSON.toJSONString(defDto));
            LOG.debug("#BatchDropSubs4SapTask handleFile strDto:", JSON.toJSONString(strDto));
            LOG.debug("#BatchDropSubs4SapTask handleFile mapDto:", JSON.toJSONString(mapDto));
        }

        //调用批量框架创建批量
        long taskId = batchDefineBOService.createAndStartBatchTask(defDto, strDto, mapDto);
        LOG.debug("#BatchDropSubs4SapTask handleFile taskId:", taskId);

        //记录批量相关信息，入om_batch_4othersys_cz表
        this.recordBatchInfo(uploadLssFileName, fileParseResult, taskId);
        // 删除文件
        File localFile = new File(localFilePath);
        boolean rt = localFile.delete();
        boolean deleteFail = false;
        if (rt == deleteFail)
        {
            LOG.error("delete file ", localFilePath, "failed");
        }

        LOG.debug("#BatchDropSubs4SapTask handleFile end");
    }

    private void recordBatchInfo(String uploadLssFileName, String[] fileParseResult, long taskId)
    {
        OmBatchOtherSysBOImpService omBatchOtherSys = (OmBatchOtherSysBOImpService) ServiceHelper
                .getService(OmBatchOtherSysBOImpService.SERVICE_NAME);
        List<OmBatchOtherSysExEntityDTO> ombatch = new ArrayList<OmBatchOtherSysExEntityDTO>();
        OmBatchOtherSysExEntityDTO omBatchOtherSysExEntity = new OmBatchOtherSysExEntityDTO();
        omBatchOtherSysExEntity.setBatch4othersysid(new BigDecimal(SequenceUtil
                .next(OmBatchOtherSysExEntity.VO_TYPE_NAME)));
        omBatchOtherSysExEntity.setBatchno(new BigDecimal(taskId));
        omBatchOtherSysExEntity.setFilename(uploadLssFileName.replaceFirst(".txt", ".csv"));
        omBatchOtherSysExEntity.setBusinesscode(Batch4DropSubsConsts.BATCH_BUSINESS_CODE);
        omBatchOtherSysExEntity.setStatus("0");
        omBatchOtherSysExEntity.setStatusdate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setCreatedate(DateTimeHelper.getNowDate());
        omBatchOtherSysExEntity.setExfield1(fileParseResult[0]);
        ombatch.add(omBatchOtherSysExEntity);
        omBatchOtherSys.insertOmBatchOtherSys(ombatch);
    }

    private String buildDefDtoContext(BatchDefineDTO defDto)
    {
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext begin");
        BatchServiceInfoXMLVO batchServiceInfo = new BatchServiceInfoXMLVO();
        batchServiceInfo.setBeId(new BigDecimal(ContextHelper.getContextAccessor().getBeId()));
        batchServiceInfo.setCreateOperId(new BigDecimal(ContextHelper.getContextAccessor().getOperator()));
        batchServiceInfo.setBusinessCode(Batch4DropSubsConsts.BATCH_BUSINESS_CODE);
        batchServiceInfo.setRegionId(new BigDecimal(ContextHelper.getContextAccessor().getBeId()));
        batchServiceInfo.setCreateDeptId(new BigDecimal(ContextHelper.getContextAccessor().getDeptId()));
        batchServiceInfo.setSalesDepartId(new BigDecimal(ContextHelper.getContextAccessor().getDeptId()));

        this.buildBatchExtParamInfo(batchServiceInfo);

        if (LOG.isDebugEnabled())
        {
            LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext batchServiceInfo", JSON.toJSONString(batchServiceInfo));
        }
        String batchServiceJsonString = DafJsonUtils.marshal(batchServiceInfo);
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext batchServiceJsonString:", batchServiceJsonString);
        String temp = "{\"BusinessCode\":\"" + defDto.getBusiCode() + "\",\"ReqHeader\":";
        String temp2 = "}";
        StringBuffer retString = new StringBuffer();
        retString.append(temp).append(batchServiceJsonString).append(temp2);
        LOG.debug("#BatchDropSubs4SapTask buildDefDtoContext retString:", retString.toString());
        return retString.toString();
    }

    private void buildBatchExtParamInfo(BatchServiceInfoXMLVO batchServiceInfo)
    {
        LOG.debug("#BatchDropSubs4SapTask buildBatchExtParamInfo begin");
        List<BatchExtParamInfoXMLVO> batchExtParamInfo = new ArrayList<BatchExtParamInfoXMLVO>();
        batchServiceInfo.setBatchExtParamInfo(batchExtParamInfo);

        BatchExtParamInfoXMLVO sendSms = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(sendSms);
        sendSms.setParamName("SendSms");
        sendSms.setParamValue(ConfigHelper.getShareConfig().getString("OM.BATCH.TLF.DROPSUBS_SMS_FLAG"));

        BatchExtParamInfoXMLVO idle = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(idle);
        idle.setParamName("idle");
        idle.setParamValue("Y");

        BatchExtParamInfoXMLVO accessChanelType = new BatchExtParamInfoXMLVO();
        batchExtParamInfo.add(accessChanelType);
        accessChanelType.setParamName("AccessChanelType");
        accessChanelType.setParamValue(ContextHelper.getContextAccessor().getChannelType());
        LOG.debug("#BatchDropSubs4SapTask buildBatchExtParamInfo end");
    }

    private String buildFileContent(String filePath)
    {
        LOG.debug("#BatchDropSubs4SapTask buildFileContent begin");
        List<String> data = FileParser.parseFile("|", filePath);
        if (LOG.isDebugEnabled())
        {
            LOG.debug("data: ", JSONObject.toJSONString(data));
        }
        StringBuffer batchBuffer = new StringBuffer();
        String col[] = null;
        String firstLine = "&";
//        int rowNum = 0;
//        int num = 0;
        for (String aData : data)
        {
//            rowNum++;
            LOG.debug("#BatchDropSubs4SapTask buildFileContent aData:", aData);
            col = aData.split(";");
            LOG.debug("#BatchDropSubs4SapTask buildFileContent col length:", col.length);
            //这个代表文件的第一行
            if (col.length == 5)
            {
                firstLine = aData + firstLine;
//                num = Integer.parseInt(col[4]);
            }
            //这个代表第二行、第三行...
            if (col.length == 8)
            {
                //SAP 发起的批销 Transaction type 取SAP文件每一行的第四个字段 
                String splitStr = "|";
                batchBuffer.append(col[3]).append(splitStr).append(col[7]);
                batchBuffer.append("\r\n");
            }
        }
//        if (rowNum != num + 1)
//        {
//            LOG.debug("#BatchDropSubs4SapTask buildFileContent rowNum != firstLine col[4]+1");
//            return "false";
//        }
        String batchBufferStr = batchBuffer.toString();
        return firstLine + batchBufferStr;
    }
}
