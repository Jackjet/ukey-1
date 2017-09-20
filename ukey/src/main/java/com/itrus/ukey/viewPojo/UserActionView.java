package com.itrus.ukey.viewPojo;

import com.itrus.ukey.db.UserAction;

/**
 * Created by jackie on 2015/4/23.
 */
public class UserActionView extends UserAction {
    private String fromViewLabel;
    private String nowViewLabel;

    public UserActionView (UserAction userAction){
        this.setClientType(userAction.getClientType());
        this.setVersionNum(userAction.getVersionNum());
        this.setVersionStr(userAction.getVersionStr());
        this.setKeySn(userAction.getKeySn());
        this.setCertCn(userAction.getCertCn());
        this.setUserUid(userAction.getUserUid());
        this.setModelCode(userAction.getModelCode());
        this.setNowPage(userAction.getNowPage());
        this.setActionName(userAction.getActionName());
        this.setFromPage(userAction.getFromPage());
        this.setCreateTime(userAction.getCreateTime());
        this.setActionTime(userAction.getActionTime());
        this.setProject(userAction.getProject());
    }

    public String getFromViewLabel() {
        return fromViewLabel;
    }

    public void setFromViewLabel(String fromViewLabel) {
        this.fromViewLabel = fromViewLabel;
    }

    public String getNowViewLabel() {
        return nowViewLabel;
    }

    public void setNowViewLabel(String nowViewLabel) {
        this.nowViewLabel = nowViewLabel;
    }
}
