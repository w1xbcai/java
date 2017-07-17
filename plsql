PL/SQL
1.添加快捷方式
tools->editor->auto replace->edit


2.设置语言
select userenv('language') from dual;

3,数据库链接显示问题
tools->preferences->logon history->fixed users

>江苏需求主干bbit
--公共库/交易库1/区域库1--------
read_user/huaweif2@10.171.89.125:1521/BESDB
---------交易库2/区域库2--------
read_user/huaweif2@10.171.34.126:1521/BESDB
---------资源库--------
im/im@10.137.82.199:1521/res
---------江苏库(crm2)--------
tbcs/tbcs@10.137.82.172:1521/crm2

