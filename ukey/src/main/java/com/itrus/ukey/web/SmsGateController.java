package com.itrus.ukey.web;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.itrus.ukey.db.SmsGateExample;
import com.itrus.ukey.exception.ServiceNullException;
import com.itrus.ukey.service.SmsGateService;
import com.itrus.ukey.service.SmsSendService;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.SmsGate;

@RequestMapping("/smsgate")
@Controller
public class SmsGateController extends AbstractController{
    @Autowired
	SmsGateService smsGateService;
	@Autowired
	private SmsSendService smsSendService;
    
    // 返回新建页面
    @RequestMapping(params = "form", produces = "text/html")
    public String create(@RequestParam(value = "id", required = false) Long id,
    		Model uiModel) {
    	if(id == null) {
    		return "smsgate/create";
    	}
        try {
            SmsGate smsGate = smsGateService.getSmsGateById(id);
            uiModel.addAttribute("smsgate", smsGate);
        } catch (ServiceNullException e){
            uiModel.addAttribute("errMsg",e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","解密密码失败");
        }
		return "smsgate/create";
    }
    
    // 修改处理
    @RequestMapping(method = RequestMethod.POST, produces = "text/html")
    public String update(SmsGate smsGate, Model uiModel) {
        try {
            if (smsGate.getId()==null)
                smsGateService.addSmsGate(smsGate);
            else
                smsGateService.updateSmsGate(smsGate);
        } catch (ServiceNullException e){
            uiModel.addAttribute("errMsg",e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","解密密码失败");
        }
        return "redirect:/smsgate";
    }
    
    // 显示详情
    @RequestMapping(produces = "text/html")
    public String show(Model uiModel) {
        try {
            SmsGate smsGate = smsGateService.getSmsGateByExample(new SmsGateExample());
            if(smsGate == null){
                return "redirect:/smsgate?form";
            }
            uiModel.addAttribute("smsgate", smsGate);
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","解密密码失败");
        }
        return "smsgate/show";
    }
    
    @RequestMapping(value = "testsms")
    public @ResponseBody Map<String,Object> testsms(SmsGate smsGate) {
		return smsSendService.smsGateTest(smsGate);
    }
}
