package com.itrus.ukey.test.util;

import com.itrus.ukey.db.SysRegion;
import com.itrus.ukey.db.SysRegionExample;
import org.apache.ibatis.session.SqlSession;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by jackie on 2015/3/26.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/applicationContext.xml")
public class SysRegionImport {
    @Autowired
    SqlSession sqlSession;
    @Test
    public void importRegionInfos(){
        try {
            // XSSFWorkbook, File
            OPCPackage pkg = OPCPackage.open(new File("D:\\regionData.xlsx"));
            XSSFWorkbook wb = new XSSFWorkbook(pkg);
            System.out.println(wb.getNumberOfSheets());
            //省份信息
//            importShengfen(wb.getSheetAt(1));
//            importCity(wb.getSheetAt(2));
            importQuXian(wb.getSheetAt(3));
            pkg.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
    }

    private void importShengfen(XSSFSheet sheet){
        //国家信息
        SysRegion sysRegionRoot = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByPrimaryKey",1);
        //利用foreach循环 遍历sheet中的所有行
        for (Row row : sheet) {
            installRegion(row,sysRegionRoot,1);
        }
    }
    private void importCity(XSSFSheet sheet){
        //利用foreach循环 遍历sheet中的所有行
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria;
        sysRegionExample.setLimit(1);
        for (Row row : sheet) {
            //编码
            String pcode = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue()+"";
            pcode = pcode.substring(0,2)+"0000";
            sysRegionExample.clear();
            srCriteria = sysRegionExample.createCriteria();
            srCriteria.andCodeEqualTo(pcode);
            SysRegion pSysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
            installRegion(row,pSysRegion,2);
        }
    }
    private void importQuXian(XSSFSheet sheet){
        //利用foreach循环 遍历sheet中的所有行
        SysRegionExample sysRegionExample = new SysRegionExample();
        SysRegionExample.Criteria srCriteria;
        sysRegionExample.setLimit(1);
        for (Row row : sheet) {
            //编码
            String pcode = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue()+"";
            pcode = pcode.substring(0,4)+"00";
            sysRegionExample.clear();
            srCriteria = sysRegionExample.createCriteria();
            srCriteria.andCodeEqualTo(pcode);
            SysRegion pSysRegion = sqlSession.selectOne("com.itrus.ukey.db.SysRegionMapper.selectByExample",sysRegionExample);
            installRegion(row,pSysRegion,3);
        }
    }

    private void installRegion(Row row,SysRegion pSysRegion,Integer type){
        //编码
        String code = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue()+"";
        //名称
        String name = row.getCell(1).getStringCellValue();
        SysRegion sysRegion = new SysRegion();
        sysRegion.setType(type);
        sysRegion.setLastModify(new Date());
        sysRegion.setCreateTime(new Date());
        sysRegion.setParentId(pSysRegion.getId());
        sysRegion.setParentIds(pSysRegion.getParentIds()+pSysRegion.getId()+",");
        sysRegion.setCode(code);
        sysRegion.setNameCn(name);
        sysRegion.setSort(0);
        sysRegion.setStatus(1);
        sqlSession.insert("com.itrus.ukey.db.SysRegionMapper.insert",sysRegion);
    }
}
