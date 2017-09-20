INSERT INTO activity_msg_temp
(id,cert_cn,create_time,host_id,key_sn,life_time,off_line_time,on_line_time,process_id,thread_id,ukey_version,version,project,os_type) SELECT id,cert_cn,create_time,host_id,key_sn,life_time,off_line_time,on_line_time,process_id,thread_id,ukey_version,version,project,os_type 
FROM activity_msg 
WHERE 
	(SELECT count(*) FROM activity_msg_temp) = 0 
AND 
	id = IFNULL((select max(id) from activity_msg),0);