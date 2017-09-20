INSERT INTO sys_region (code,name_cn,parent_id,parent_ids,sort,status,type,create_time,last_modify) VALUES
(86,'中国省市区',0,'0,',0,1,0,now(),now())
ON DUPLICATE KEY UPDATE code=VALUES(code),name_cn=VALUES(name_cn),parent_id=VALUES(parent_id),
parent_ids=VALUES(parent_ids),sort=VALUES(sort),status=VALUES(status),type=VALUES(type);