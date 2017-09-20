INSERT INTO app_category (serial_num,name,order_num) VALUES
(1000,'默认应用',1),
(1001,'金融应用',2),
(1002,'政务应用',3),
(1003,'办公应用',4)
ON DUPLICATE KEY UPDATE name=VALUES(name),order_num=VALUES(order_num);