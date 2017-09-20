package com.itrus.ukey.task;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.itrus.ukey.db.SysConfig;
import com.itrus.ukey.db.ThreeInOne;
import com.itrus.ukey.service.ThreeInOneService;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.ComNames;

/**
 * 与地税交互的定时任务<br>
 * 1、从地税读取三证合一信息<br>
 * 2、向地税系统发送三证合一中状态为‘已提交’的记录
 * 
 *
 */
public class ThreeInOneTask extends Thread {

	private Logger log = Logger.getLogger(ThreeInOneTask.class);
	@Autowired
	ThreeInOneService threeInOneService;
	@Autowired
	CacheCustomer cacheCustomer;

	@Override
	public void run() {
		boolean isException = false;
		while (true) {
			if (isOpenToTax(cacheCustomer
					.getSysConfigByType(ComNames.OPEN_TAX_SYNC))) {
				log.info("开始地税同步");
				// ...
			} else {
				log.info("没有开启地税同步");
				try {
					sleep(5 * 1000);// 休眠5秒后继续检查是否开启了地税同步
				} catch (InterruptedException e) {
					isException = true;
					log.error(e);
				}
			}
			// 是否开启了回写地税
			if (isOpenToTax(cacheCustomer
					.getSysConfigByType(ComNames.OPEN_WRITE_BACK_TAX))) {
				List<ThreeInOne> threes = threeInOneService.getSubmitStatus();
				if (!threes.isEmpty()) {
					try {
						synchronized (threes) {
							sleep(10 * 1000);
							log.info("开始地税回写");
							// ...
							// 假设全部回写地税成功
							// 修改回写成功的数据状态
							threeInOneService.chanageSyncType(threes);
							// 记录回写失败的数据的统一社会信用代码
							// ...
						}
					} catch (Exception e) {
						isException = true;
						log.error(e);
					}
				}

			} else {
				log.info("没有开启地税回写");
				try {
					sleep(5 * 1000);// 休眠5秒后继续检查是否开启了回写地税
				} catch (InterruptedException e) {
					isException = true;
					log.error(e);
				}
			}

			// 若出现异常则暂停10秒钟
			if (isException) {
				try {
					sleep(10 * 1000);
				} catch (InterruptedException e) {
					log.error(e);
				} finally {
					isException = false;
				}
			}

		}
	}

	/**
	 * 判断系统配置中是否开启了对应功能
	 * 
	 * @param sysConfig
	 * @return
	 */
	public boolean isOpenToTax(SysConfig sysConfig) {
		if (null != sysConfig && "true".equals(sysConfig.getConfig()))
			return true;
		return false;
	}
}
