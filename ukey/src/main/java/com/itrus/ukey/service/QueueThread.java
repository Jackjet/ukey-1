package com.itrus.ukey.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.itrus.ukey.db.UserDevice;
import org.apache.ibatis.session.SqlSession;

import com.itrus.ukey.db.ActivityMsg;
import com.itrus.ukey.db.VersionChange;
import org.apache.log4j.Logger;
import org.springframework.security.core.userdetails.User;

public class QueueThread extends Thread {
    private Logger log = Logger.getLogger(this.getName());
	private SqlSession sqlSession;
	
	private static final int TIME_OUT = 30;
	private static final ArrayBlockingQueue<Object> OBJECT_QUEUE = new ArrayBlockingQueue<Object>(20000);
	@Override
	public void run() {
        boolean isException = false;
        while (true) {
            log.debug("execute run,the queue size is ["+OBJECT_QUEUE.size()+"]");
            if (OBJECT_QUEUE.isEmpty()) {
                try {
                    sleep(10 * 1000);//若队列中没有信息则等待十秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            log.debug("execute run,the queue size is ["+OBJECT_QUEUE.size()+"]");
            Map<String, ActivityMsg> amMap = new HashMap<String, ActivityMsg>();
            Map<String, VersionChange> vcMap = new HashMap<String, VersionChange>();
            Map<String, UserDevice> udMap = new HashMap<String, UserDevice>();
            for (int i = 0; !OBJECT_QUEUE.isEmpty() && i < 500; i++) {
                Object object = OBJECT_QUEUE.poll();
                if (object instanceof ActivityMsg) {
                    ActivityMsg activityMsg = (ActivityMsg) object;
                    amMap.put(activityMsg.getThreadId(), activityMsg);
                } else if (object instanceof VersionChange) {
                    VersionChange vc = (VersionChange) object;
                    vcMap.put(vc.getKeySn(), vc);
                } else if (object instanceof  UserDevice){
                    UserDevice ud = (UserDevice) object;
                    udMap.put(ud.getDeviceSn(),ud);
                }
            }
            log.debug("the amMap size is"+amMap.size()+",the vcMap size is "+vcMap.size()+"the udMap size is "+udMap.size());
            //独自捕获异常，使两者不互相影响
            if (!amMap.isEmpty())
                try {
                    sqlSession.insert("com.itrus.ukey.db.ActivityMsgTempMapper.insertOrUpdate", amMap.values().toArray());
                } catch (Exception e) {
                    isException = true;
                    //将错误信息显示，不进行操作
                    log.error(e);
                }
            if (!vcMap.isEmpty())
                try {
                    sqlSession.insert("com.itrus.ukey.db.VersionChangeMapper.insertOrUpdate", vcMap.values().toArray());
                } catch (Exception e) {
                    isException = true;
                    //将错误信息显示，不进行操作
                    log.error(e);
                }
            if (!udMap.isEmpty())
                try {
                    sqlSession.insert("com.itrus.ukey.db.UserDeviceMapper.insertOrUpdate", udMap.values().toArray());
                } catch (Exception e) {
                    isException = true;
                    //将错误信息显示，不进行操作
                    log.error(e);
                }
            log.debug("the exception is ["+isException+"]");
            //若出现异常则暂停10秒钟
            if (isException) {
                try {
                    sleep(10 * 1000);//若队列中没有信息则等待十秒
                } catch (InterruptedException e) {
                    log.error(e);
                } finally {
                    isException = false;
                }
            }
        }
    }

	/**
	 * 添加软件升级检查信息
	 * @param object
	 */
	public void putObjectQueue(Object object){
		try {
			OBJECT_QUEUE.offer(object, TIME_OUT, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setSqlSession(SqlSession sqlSession) {
		this.sqlSession = sqlSession;
	}
	
}
