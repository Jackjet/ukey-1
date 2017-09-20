package com.itrus.ukey.service;

import com.itrus.ukey.db.*;
import com.itrus.ukey.exception.IAuthorizationException;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.util.LogUtil;
import com.itrus.ukey.web.AdminController;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Date;

/**
 * 鉴证审核服务
 * Created by thinker on 2015/3/13.
 */
@Service
public class TrustVerifyService {
    //审批通过
    public static final int TRUST_VERIFY_AGREE = 0;
    //未找到审批记录
    public static final int TRUST_VERIFY_UNFIND = 1;
    //未授权
    public static final int TRUST_VERIFY_UNAUTH = 2;
    //已经审批
    public static final int TRUST_VERIFY_APPROVED = 3;
    //没有认证实体信息
    public static final int TRUST_VERIFY_NOENTITY = 4;
    //没有认证项
    public static final int TRUST_VERIFY_NOITEM = 5;
    //审批异常
    public static final int TRUST_VERIFY_EXCEPTION = 6;
    @Autowired
    SqlSession sqlSession;
    @Autowired
    private DataSourceTransactionManager transactionManager;

    /**
     * 鉴证审核同意
     * @param logId
     * @param admin
     * @return 0:批准；1:未找到鉴证记录；
     *         2:没有权限 3:未找到认证实体信息
     *         4:未找到认证项信息
     * @throws IAuthorizationException
     * @throws ServiceNullException
     */
    public int agree(Long logId,Admin admin) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        TransactionStatus status = transactionManager.getTransaction(def);
        try {
            //1.是否存在
            EntityTrustLog log = sqlSession.selectOne("com.itrus.ukey.db.EntityTrustLogMapper.selectByPrimaryKey", logId);
            if (log == null)
//                throw new ServiceNullException("未找到指定鉴证记录");
                return TRUST_VERIFY_UNFIND;//
            //2.是否有权限
            //管理员不是超级管理员并且所属项目不一致
            if(!AdminController.ACCESS_TYPE_SUPPER.equals(admin.getType())
                    &&!log.getProject().equals(admin.getProject())){
//                throw new IAuthorizationException("权限不足");
                return TRUST_VERIFY_UNAUTH;//
            }
            //3.检查是否已审批
            if(log.getApproveStatus()!=0)
                return TRUST_VERIFY_APPROVED;
            EntityTrueInfo info = sqlSession.selectOne("com.itrus.ukey.db.EntityTrueInfoMapper.selectByPrimaryKey", log.getEntityTrue());
            if (info == null)
                return TRUST_VERIFY_NOENTITY;//未找到认证实体信息
            String name = null;
            if(log.getItemType() == EntityTrueService.ITEM_BUSINESS_LICENSE){
                name = "营业执照";
                BusinessLicenseExample blExample = new BusinessLicenseExample();
                BusinessLicenseExample.Criteria blCriteria = blExample.or();
                blCriteria.andTrustLogEqualTo(log.getId());
                BusinessLicense data = sqlSession.selectOne("com.itrus.ukey.db.BusinessLicenseMapper.selectByExample", blExample);
                if (data == null)
                    return TRUST_VERIFY_NOITEM;//未找到认证信息项
                data.setItemStatus(1);
                sqlSession.update("com.itrus.ukey.db.BusinessLicenseMapper.updateByPrimaryKey", data);

                info.setHasBl(true);
                info.setName(data.getEntityName());
            }else if(log.getItemType() == EntityTrueService.ITEM_ORG_CODE){
                name = "组织机构代码";
                OrgCodeExample ocExampl = new OrgCodeExample();
                OrgCodeExample.Criteria ocCriteria = ocExampl.or();
                ocCriteria.andTrustLogEqualTo(log.getId());
                OrgCode data = sqlSession.selectOne("com.itrus.ukey.db.OrgCodeMapper.selectByExample", ocExampl);
                if (data == null)
                    return TRUST_VERIFY_NOITEM;//未找到认证信息项
                data.setItemStatus(1);
                sqlSession.update("com.itrus.ukey.db.OrgCodeMapper.updateByPrimaryKey", data);

                info.setHasOrgCode(true);
            }else if(log.getItemType() == EntityTrueService.ITEM_TAX_CERT){
                name = "税务登记证";
                TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
                TaxRegisterCertExample.Criteria trcCriteria = trcExample.or();
                trcCriteria.andTrustLogEqualTo(log.getId());
                TaxRegisterCert data = sqlSession.selectOne("com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample", trcExample);
                if (data == null)
                    return TRUST_VERIFY_NOITEM;//未找到认证信息项
                data.setItemStatus(1);
                sqlSession.update("com.itrus.ukey.db.TaxRegisterCertMapper.updateByPrimaryKey", data);

                info.setHasTaxCert(true);
            }else if(log.getItemType() == EntityTrueService.ITEM_ID_CARD){
                name = "身份证";
                IdentityCardExample idCardExample = new IdentityCardExample();
                IdentityCardExample.Criteria idcCritera = idCardExample.or();
                idcCritera.andTrustLogEqualTo(log.getId());
                IdentityCard data = sqlSession.selectOne("com.itrus.ukey.db.IdentityCardMapper.selectByExample", idCardExample);
                if (data == null)
                    return TRUST_VERIFY_NOITEM;//未找到认证信息项
                data.setItemStatus(1);
                sqlSession.update("com.itrus.ukey.db.IdentityCardMapper.updateByPrimaryKey", data);

                info.setHasIdCard(true);
            }
            //更新认证实体信息
            sqlSession.update("com.itrus.ukey.db.EntityTrueInfoMapper.updateByPrimaryKey", info);
            log.setApproveStatus(1);
            log.setApproveAdmin(admin.getId());
            log.setApproveTime(new Date());
            //更新审批日志
            sqlSession.update("com.itrus.ukey.db.EntityTrustLogMapper.updateByPrimaryKey", log);
            //添加管理员操作日志
            LogUtil.adminlog(sqlSession, "审批操作-同意", log.getEntityName() + "-" + name);
            transactionManager.commit(status);
        } catch (Exception e) {
            e.printStackTrace();
            transactionManager.rollback(status);
            return TRUST_VERIFY_EXCEPTION;
        }
        return TRUST_VERIFY_AGREE;
    }
}
