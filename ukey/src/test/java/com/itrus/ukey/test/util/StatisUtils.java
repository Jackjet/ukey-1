package com.itrus.ukey.test.util;

import com.itrus.ukey.db.*;
import com.itrus.ukey.service.SysRegionService;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sound.sampled.AudioFormat;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by jackie on 2015/6/24.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:applicationContext.xml"})
public class StatisUtils {
    @Autowired
    SqlSession sqlSession;
    @Autowired
    private SysRegionService sysRegionService;

    @Test
    public void statisEntityInfo4Csv() throws IOException {
        File etiFile = new File("d:/eti.csv");
        etiFile.deleteOnExit();
//        etiFile.createNewFile();
        //查询所有企业信息
        List<EntityTrueInfo> etiList = sqlSession.selectList("com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample");
        //查询相关用户
        try {
            String bom = String.valueOf(new Byte[]{(byte) 0xEF,(byte) 0xBB,(byte) 0xBF});
            // 追记模式
            BufferedWriter bw = new BufferedWriter(new FileWriter(etiFile, true));
            String title = "企业名称,组织机构代码,营业执照认证," +
                    "组织机构代码认证,税务登记证认证," +
                    "税务登记证号,用户姓名,手机号," +
                    "电子邮箱,固定电话,地址";
            bw.write(bom.toCharArray());
            bw.newLine();
            bw.write(title);
            for (EntityTrueInfo eti : etiList) {
                //查询税务登记证
                String taxCert = "";
                if (Boolean.TRUE != eti.getHasTaxCert()){
                    TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
                    TaxRegisterCertExample.Criteria trcCriteria = trcExample.createCriteria();
                    trcCriteria.andEntityTrueEqualTo(eti.getId());
                    trcExample.setLimit(1);
                    trcExample.setOrderByClause("last_modify desc");
                    TaxRegisterCert trc = sqlSession.selectOne("com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",trcExample);
                }
                SysUser sysUser = null;
                SysUserExample sysUserExample = new SysUserExample();
                SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
                suCriteria.andEntityTrueEqualTo(eti.getId());
                sysUserExample.setLimit(1);
                sysUserExample.setOrderByClause("last_modify desc");
                sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);

                // 新增一行数据
                bw.newLine();
                bw.write(eti.getName() + "," + eti.getIdCode() + ","
                        + (Boolean.TRUE == eti.getHasBl()?"是":"否") +","
                        + (Boolean.TRUE == eti.getHasOrgCode()?"是":"否")+","
                        + (Boolean.TRUE == eti.getHasTaxCert()?"是":"否")+","
                        + (Boolean.TRUE == eti.getHasIdCard()?"是":"否")+","
                        + taxCert+",");
                if (sysUser == null)
                    bw.write(",,,,");
                else {
                    //根据省市区code值获取省市区最新名称
                    String regionCodes = sysUser.getRegionCodes();
                    String userAdds = sysUser.getUserAdds();
                    if(StringUtils.isNotBlank(regionCodes)&&regionCodes.indexOf("@")>=0){
                        String[] codes = regionCodes.split("@");
                        String regionName = sysRegionService.getAllName(codes[1], codes[2], codes[3]);
                        userAdds=regionName+userAdds;
                        sysUser.setUserAdds(userAdds);

                    }
                    bw.write(sysUser.getRealName() + "," + sysUser.getmPhone() + "," + sysUser.getEmail() + ","
                            + sysUser.getTelephone() + "," + sysUser.getUserAdds());
                }
            }
            bw.close();
        } catch (FileNotFoundException e) {
            // 捕获File对象生成时的异常
            e.printStackTrace();
        } catch (IOException e) {
            // 捕获BufferedWriter对象关闭时的异常
            e.printStackTrace();
        }
    }
    @Test
    public void statisEntityInfoPoi() throws IOException {
        //查询所有企业信息
        List<EntityTrueInfo> etiList = sqlSession.selectList("com.itrus.ukey.db.EntityTrueInfoMapper.selectByExample");
        //查询相关用户
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String fileDate = dateFormat.format(new Date());
            File file = new File("D:\\iTrusChina\\产品\\天威盾\\企业数据\\"+fileDate+"企业信息.xls");
//            file.deleteOnExit();
            file.createNewFile();

            FileOutputStream fileOut = new FileOutputStream(file);//创建excel表格//"/tmp/tmpfiles/workbook.xls"
            Workbook wb = new HSSFWorkbook();//获取workbook
            //FileOutputStream fileOut = new FileOutputStream("workbook.xls");
            HSSFSheet sheet = (HSSFSheet) wb.createSheet("report");// 生成一个表格
            sheet.setColumnWidth(1, 4000);
            HSSFRow row = sheet.createRow((short)0);//创建行并插入表头
            row.createCell(0).setCellValue("企业名称");
            row.createCell(1).setCellValue("组织机构代码");
            row.createCell(2).setCellValue("营业执照认证");
            row.createCell(3).setCellValue("组织机构代码认证");
            row.createCell(4).setCellValue("法定代表人认证");
            row.createCell(5).setCellValue("税务登记证认证");
            row.createCell(6).setCellValue("税务登记证号");
            row.createCell(7).setCellValue("用户姓名");
            row.createCell(8).setCellValue("手机号");
            row.createCell(9).setCellValue("电子邮箱");
            row.createCell(10).setCellValue("固定电话");
            row.createCell(11).setCellValue("用户地址");
            for (int i =0 ;i< etiList.size();i++) {
                EntityTrueInfo eti = etiList.get(i);
                //查询税务登记证
                String taxCert = "";
                if (Boolean.TRUE == eti.getHasTaxCert()){
                    TaxRegisterCertExample trcExample = new TaxRegisterCertExample();
                    TaxRegisterCertExample.Criteria trcCriteria = trcExample.createCriteria();
                    trcCriteria.andEntityTrueEqualTo(eti.getId());
                    trcExample.setLimit(1);
                    trcExample.setOrderByClause("last_modify desc");
                    TaxRegisterCert trc = sqlSession.selectOne("com.itrus.ukey.db.TaxRegisterCertMapper.selectByExample",trcExample);
                    taxCert = trc.getCertNo();
                }
                SysUser sysUser = null;
                SysUserExample sysUserExample = new SysUserExample();
                SysUserExample.Criteria suCriteria = sysUserExample.createCriteria();
                suCriteria.andEntityTrueEqualTo(eti.getId());
                sysUserExample.setLimit(1);
                sysUserExample.setOrderByClause("last_modify desc");
                sysUser = sqlSession.selectOne("com.itrus.ukey.db.SysUserMapper.selectByExample",sysUserExample);
                row = sheet.createRow(i+1);
                row.createCell(0).setCellValue(eti.getName());
                row.createCell(1).setCellValue(eti.getIdCode());
                row.createCell(2).setCellValue(Boolean.TRUE == eti.getHasBl()?"是":"否");
                row.createCell(3).setCellValue(Boolean.TRUE == eti.getHasOrgCode()?"是":"否");
                row.createCell(4).setCellValue(Boolean.TRUE == eti.getHasTaxCert()?"是":"否");
                row.createCell(5).setCellValue(Boolean.TRUE == eti.getHasIdCard()?"是":"否");
                row.createCell(6).setCellValue(taxCert);

                if (sysUser != null){
                    //根据省市区code值获取省市区最新名称
                    String regionCodes = sysUser.getRegionCodes();
                    String userAdds = sysUser.getUserAdds();
                    if(StringUtils.isNotBlank(regionCodes)&&regionCodes.indexOf("@")>=0){
                        String[] codes = regionCodes.split("@");
                        String regionName = sysRegionService.getAllName(codes[1], codes[2], codes[3]);
                        userAdds=regionName+userAdds;
                        sysUser.setUserAdds(userAdds);
                    }
                    row.createCell(7).setCellValue(sysUser.getRealName());
                    row.createCell(8).setCellValue(sysUser.getmPhone());
                    row.createCell(9).setCellValue(sysUser.getEmail());
                    row.createCell(10).setCellValue(sysUser.getTelephone());
                    row.createCell(11).setCellValue(sysUser.getUserAdds());
                }
            }
            wb.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
