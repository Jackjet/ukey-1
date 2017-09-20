package com.itrus.ukey.service;

import org.springframework.stereotype.Service;

/**
 * Created by jackie on 2014/11/18.
 * 鉴证审核记录
 */
@Service
public class EntityTrustLogService {
    /*审批状态
        0：未审批（提交）
        1：审核通过
        2：拒绝
        3：变更或失效*/
    //未审批（提交）
    public static final int ITEM_UNAUDITED_STATUS = 0;
    //审核通过
    public static final int ITEM_APPROVE_STATUS = 1;
    //拒绝
    public static final int ITEM_REJECT_STATUS = 2;
    //变更
    public static final int ITEM_CHANGE_STATUS = 3;
    //失效
    public static final int ITEM_FAILURE_STATUS = 4;
}
