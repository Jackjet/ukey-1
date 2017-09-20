package com.itrus.ukey.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.itrus.ukey.db.ProjectKeyInfo;

public class ProjectKeyInfoSort {

	public static ProjectKeyInfo findProjectByKey(
			List<ProjectKeyInfo> projectkeyinfoall, String keySn) {
		if(StringUtils.isBlank(keySn)) return null;
		ProjectKeyInfo ret = null;
		for (ProjectKeyInfo projectkeyinfo : projectkeyinfoall) {
//			if (StringUtils.isBlank(projectkeyinfo.getSn2())){
//			if (projectkeyinfo.getSn2() == null
//					|| projectkeyinfo.getSn2().length() == 0) {
			//若sn1和sn2均为空，直接跳过
			if (StringUtils.isBlank(projectkeyinfo.getSn2())
					&&StringUtils.isBlank(projectkeyinfo.getSn1()))
				continue;
			// 只有sn1时，keySn以 sn1 为匹配
			if (StringUtils.isBlank(projectkeyinfo.getSn2())){
				//前缀匹配模式
				if(keySn.startsWith(projectkeyinfo.getSn1())) {
					ret = projectkeyinfo;
					break;
				}
				//后缀匹配模式
				if(projectkeyinfo.getSn1().endsWith("$")&&keySn.matches("\\S*"+projectkeyinfo.getSn1())){
                    ret = projectkeyinfo;
                    break;
				}
			} else {
				// 存在 sn1, sn2 时， keySn 需要在sn1 和 sn2 之间
				if (keySn.compareTo(projectkeyinfo.getSn1()) >= 0
						&& keySn.compareTo(projectkeyinfo.getSn2()) <= 0) {
					ret = projectkeyinfo;
					break;
				}
			}
		}
		return ret;
	}

	public static void sort(List<ProjectKeyInfo> projectkeyinfoall) {
		Collections.sort(projectkeyinfoall, new Comparator<ProjectKeyInfo>() {

			public int compare(ProjectKeyInfo keyinfo1, ProjectKeyInfo keyinfo2) {
				keySnToVersionFix(keyinfo1);
				keySnToVersionFix(keyinfo2);
				// 1、根据sn1长度，进行逆向排序
				if (keyinfo1.getSn1().length() > keyinfo2.getSn1().length())
					return -1;
				if (keyinfo1.getSn1().length() < keyinfo2.getSn1().length())
					return 1;

				// 2、根据sn1比较，进行逆向排序
				if (keyinfo1.getSn1().compareTo(keyinfo2.getSn1()) > 0)
					return -1;
				if (keyinfo1.getSn1().compareTo(keyinfo2.getSn1()) < 0)
					return 1;

				// 3、如果sn1相等，则进行后续排序

				// 4、sn2 全部为空
				if (StringUtils.isBlank(keyinfo1.getSn2())&&StringUtils.isBlank(keyinfo2.getSn2()))
					return 0;

				// 5、包含sn2的，排序在后面
				if (StringUtils.isNotBlank(keyinfo1.getSn2())&&StringUtils.isBlank(keyinfo2.getSn2()))
					return 1;
				if (StringUtils.isNotBlank(keyinfo2.getSn2())&&StringUtils.isBlank(keyinfo1.getSn2()))
					return -1;

				// 6、根据sn2进行正向排序
				if (keyinfo1.getSn2().compareTo(keyinfo2.getSn2()) > 0)
					return 1;
				if (keyinfo1.getSn2().compareTo(keyinfo2.getSn2()) < 0)
					return -1;

				return 0;
			}

		});
	}

	private static void keySnToVersionFix(ProjectKeyInfo keyinfo) {
		if (StringUtils.isNotBlank(keyinfo.getSn1())
				&& keyinfo.getSn1().matches(ComNames.VERSION_REGEX))
			keyinfo.setSn1(new ComponentVersion(keyinfo.getSn1())
					.GetExtendedVersion());
		if (StringUtils.isNotBlank(keyinfo.getSn2())
				&& keyinfo.getSn2().matches(ComNames.VERSION_REGEX))
			keyinfo.setSn2(new ComponentVersion(keyinfo.getSn2())
					.GetExtendedVersion());
	}
}
