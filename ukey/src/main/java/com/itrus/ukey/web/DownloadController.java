package com.itrus.ukey.web;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;


import com.itrus.ukey.service.SystemConfigService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.Version;
import com.itrus.ukey.util.ComNames;
import com.itrus.ukey.util.LogUtil;

@RequestMapping("/download")
@Controller
public class DownloadController extends AbstractController {
	
	private SqlSession sqlSession;

	@Autowired
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}

   // 下载
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public void download(@PathVariable("id") Long id, HttpServletResponse response) throws Exception {
    	Version version = sqlSession.selectOne("com.itrus.ukey.db.VersionMapper.selectByPrimaryKey", id);
    	
    	if(version==null){
    		response.setStatus(404);
    		return;
    	}
    	
        String outfilename =  version.getFile();
         
        File file = new File(getSoftDir(), outfilename);
        if(!file.exists())
        {
    		response.setStatus(404);
    		return;
        }
        
        String fileName = URLEncoder.encode(version.getName(), "UTF-8");
        
        response.reset();
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Content-Length", "" + file.length());
        response.setContentType("application/octet-stream; charset=UTF-8");
        

        InputStream input = FileUtils.openInputStream(file);
        
        IOUtils.copy(input, response.getOutputStream());
        
        input.close();
        
        response.getOutputStream().flush();
        response.getOutputStream().close();
        
    }
    /**
     * 获取软件存放目录对象
     * @return
     * @throws Exception
     */
    private File getSoftDir() throws Exception{
    	String dirname = "D:\\fileupload_ukey\\";
		//读取系统配置信息 获取软件上传目录
        String type = SystemConfigService.SOFT_DIR_CONFIG_NAME;
        SysConfig sysConfig = sqlSession.selectOne("com.itrus.ukey.db.SysConfigMapper.selectByType", type);
        if(sysConfig!=null) dirname = sysConfig.getConfig();

        File softDir = new File(dirname);
        //判断指定目录是否存在，是否有读写权限
        if(!softDir.exists()||!softDir.canRead()||!softDir.canWrite()){
        	LogUtil.syslog(sqlSession, "版本管理", "【"+dirname+"】目录不存在或权限不足");
        	throw new Exception("【"+dirname+"】目录不存在或权限不足");
        }
        return softDir;
    }
}
