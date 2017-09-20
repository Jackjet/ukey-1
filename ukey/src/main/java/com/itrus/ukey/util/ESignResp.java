package com.itrus.ukey.util;

/**
 * Created by jackie on 2014/11/26.
 */
public class ESignResp {
    private String signId;
    private boolean verifyResult;
    private int certState;

    public ESignResp(){}
    public ESignResp(String signId,boolean verifyResult,int certState){
        this.signId = signId;
        this.verifyResult = verifyResult;
        this.certState = certState;
    }

    public String getSignId() {
        return signId;
    }

    public void setSignId(String signId) {
        this.signId = signId;
    }

    public boolean isVerifyResult() {
        return verifyResult;
    }

    public void setVerifyResult(boolean verifyResult) {
        this.verifyResult = verifyResult;
    }

    public int getCertState() {
        return certState;
    }

    public void setCertState(int certState) {
        this.certState = certState;
    }
}
