package com.itrus.ukey.web;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import com.itrus.ukey.db.EmailServerExample;
import com.itrus.ukey.service.EmailServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.itrus.ukey.db.EmailServer;
import com.itrus.ukey.service.SendEmailImpl;

@RequestMapping("/mailconfig")
@Controller
public class EmailServerController extends AbstractController {
	@Resource
	private SendEmailImpl sendEmailImpl;
    @Autowired
    EmailServerService emailServerService;
    //显示邮箱配置详细信息
	@RequestMapping(produces = "text/html")
	public String details(Model uiModel) {
        try {
            EmailServer emailServer = emailServerService.getEmailServerByExample(new EmailServerExample());
            if (emailServer==null) {
                return "redirect:/mailconfig?form";
            }
            uiModel.addAttribute("emailServer", emailServer);
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","解密密码失败");
        }
        return "mailconfig/details";
	}

	/**
	 * 添加/更新邮箱页面
	 * 
	 * @return
	 */
	@RequestMapping(params = "form", produces = "text/html")
	public String create(
			@RequestParam(value = "id", required = false) Long id,
			Model uiModel) {
		if (id == null) {
			return "mailconfig/create";
		}
        try {
            EmailServer emailServer = emailServerService.getEmailServerByID(id);
            uiModel.addAttribute("emailServer", emailServer);
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","解密密码失败");
        }
        return "mailconfig/create";
	}

	/**
	 * 添加/修改邮箱配置
	 * 
	 * @param emailServer
	 * @param bindingResult
	 * @param uiModel
	 * @param httpServletRequest
	 * @return
	 */
	@RequestMapping(method = RequestMethod.POST, produces = "text/html")
	public String updateEmail(@Valid EmailServer emailServer,
			BindingResult bindingResult, Model uiModel,
			HttpServletRequest httpServletRequest) {
		if (bindingResult.hasErrors()) {
			uiModel.addAttribute("admin", emailServer);
			return "mailconfig/details";
		}
        try {
            if (emailServer.getId() == null) {
                emailServerService.addEamilServer(emailServer);
            }else {
                emailServerService.updateEmailServer(emailServer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            uiModel.addAttribute("errMsg","对密码加解密失败");
            return "mailconfig/details";
        }

        return "redirect:/mailconfig";
	}

	/**
	 * 发送测试邮件
	 * 
	 * @param emailServer
	 * @return
	 */
	@RequestMapping(params = "testEmailServer", method = RequestMethod.POST, produces = "text/html")
	public @ResponseBody Map<String, Object> testEmailServer(
			@Valid EmailServer emailServer) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("retCode", false);
		try {
			sendEmailImpl.sendEmailServerTest(emailServer);
			map.put("retCode", true);
		} catch (Exception e) {
			map.put("retCode", false);
			e.printStackTrace();
		}

		return map;
	}

}
