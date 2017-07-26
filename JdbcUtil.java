import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.jdbc.driver.OracleConnection;

/**
 * @Description: JDBC连接类(示例连接Oralce)
 * @CreateTime: 2014-1-19 下午9:46:44
 * @author: chenzw 
 * @version V1.0
 */
public class JdbcUtil {
    //驱动名  
    private static String DRIVER = "oracle.jdbc.driver.OracleDriver";  
    //获得url  
    private static String URL = "admin";  
    //获得连接数据库的用户名  
    private static String USER = "jdbc:oracle:thin:@localhost:7001:test";  
    //获得连接数据库的密码  
    private static String PASS = "";  

    static {  
        try {   
            //1.初始化JDBC驱动并让驱动加载到jvm中,加载JDBC驱动后,会将加载的驱动类注册给DriverManager类。
            Class.forName(DRIVER);  
        } catch (ClassNotFoundException e) {  
            e.printStackTrace();  
        }  
    }  
	
     public static Connection getConnection(){  
        Connection conn = null;  
        try {   
            //2.取得连接数据库  
            conn = DriverManager.getConnection(URL,USER,PASS);  
            //3.开启自动提交
            conn.setAutoCommit(true);
        } catch (SQLException e) {  
            e.printStackTrace();  
        }  
        return conn;  
     }  

    //开启事务
    public static void beginTransaction(Connection conn) {  
        if (conn != null) {  
            try {  
                if (conn.getAutoCommit()) {  
                	conn.setAutoCommit(false);  
                }  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    //提交事务
    public static void commitTransaction(Connection conn) {  
        if (conn != null) {  
            try {  
                if (!conn.getAutoCommit()) {  
                	conn.commit();  
                }  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    //回滚事务
    public static void rollBackTransaction(Connection conn) {  
        if (conn != null) {  
            try {  
                if (!conn.getAutoCommit()) {  
                	conn.rollback();  
                }  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
	
     //关闭连接
     public static void close(Object o){  
        if (o == null){  
            return;  
        }  
        if (o instanceof ResultSet){  
            try {  
                ((ResultSet)o).close();  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        } else if(o instanceof Statement){  
            try {  
                ((Statement)o).close();  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        } else if (o instanceof Connection){  
            Connection c = (Connection)o;  
            try {  
                if (!c.isClosed()){  
                    c.close();  
                }  
            } catch (SQLException e) {  
                e.printStackTrace();  
            }  
        }    
    }  

    //重载关闭连接
    public static void close(ResultSet rs, Statement stmt,   
            Connection conn){  
    	close(rs);  
    	close(stmt);  
    	close(conn);  
    }  
    //重载关闭连接
    public static void close(ResultSet rs,   
            Connection conn){  
    	close(rs);   
    	close(conn);  
    } 
    //重载关闭连接
    public static void close(ResultSet rs, PreparedStatement ps,   
            Connection conn){  
    	close(rs);  
    	close(ps);  
    	close(conn);  
    }  
    
}

1. Statement ：执行静态SQL语句的对象。
             Statement接口常用的2个方法：

             (1) executeUpdate(String sql) ：执行insert / update / delete 等SQL语句，成功返回影响数据库记录行数的int整数型。

             (2) executeQuery(String sql) ： 执行查询语句，成功返回一个ResultSet类型的结果集对象。

[java] view plain copy 在CODE上查看代码片派生到我的代码片
ResultSet rs = null;  
Statement stmt = null;  
try {  
    stmt = conn.createStatement();  
    int num = stmt.executeUpdate("insert into company values('No.1','CSDN')");  
    rs = stmt.executeQuery("select * from company");  
} catch (SQLException e) {  
    // TODO Auto-generated catch block  
    e.printStackTrace();  
        conn.rollback();  
}finally{  
    JdbcUtil.close(rs, stmt, conn);  
}  

1.1 Statement批量处理函数：
               (1) addBatch(String sql) ：添加批量处理的数据；

               (2) executeBatch()：提交批量数据；

               (3) clearBatch()：清空已添加的批量数据；

[java] view plain copy 在CODE上查看代码片派生到我的代码片
/** 
  * 实验一：Statement.executeBatch(); 
  */  
ResultSet rs = null;  
Statement stmt = null;  
try {  
    conn.setAutoCommit(false); //切记:必须设置成手动提交模式,否则每次addBatch都会提交一次,而不是批量提交  
    stmt = conn.createStatement();  
    Long startMemory  = Runtime.getRuntime().freeMemory();  
    Long startTime = System.currentTimeMillis();   
    for(int i = 0; i < 10000; i++){  
        stmt.addBatch("insert into t_dept values('No.1','CSDN')");  
        //stmt.executeUpdate("insert into t_dept values('No.1','CSDN')");  
    }  
    stmt.executeBatch();  
    conn.commit();  
    Long endMemory = Runtime.getRuntime().freeMemory();  
    Long endTime = System.currentTimeMillis();   
    System.out.println("使用内存大小:"+ (startMemory-endMemory)/1024 + "KB");  
    System.out.println("用时:"+ (endTime-startTime)/1000 + "s");  
} catch (SQLException e) {  
    e.printStackTrace();  
    conn.rollback();  
}finally{  
    JdbcUtil.close(rs, stmt, conn);  
}  
  
//执行结果：  
使用内存大小:488KB  
用时:116s   --汗，原谅我的电脑龟速。  
[java] view plain copy 在CODE上查看代码片派生到我的代码片
/** 
  * 实验二：Statement.executeUpdate(); 
  */  
//而如果直接使用executeUpdate更新（其实一样是批量提交），竟然比executeBatch速度更快，效率更高，这个真的百思不得其解，留待以后解决。。  
ResultSet rs = null;  
Statement stmt = null;  
try {  
    conn.setAutoCommit(false); //切记:必须设置成手动提交模式,否则每次addBatch都会提交一次,而不是批量提交  
    stmt = conn.createStatement();  
    Long startMemory  = Runtime.getRuntime().freeMemory();  
    Long startTime = System.currentTimeMillis();   
    for(int i = 0; i < 10000; i++){  
            //stmt.addBatch("insert into t_dept values('No.1','CSDN')");  
        stmt.executeUpdate("insert into t_dept values('No.1','CSDN')");  
    }  
    //stmt.executeBatch();  
    conn.commit();  
    Long endMemory = Runtime.getRuntime().freeMemory();  
    Long endTime = System.currentTimeMillis();   
    System.out.println("使用内存大小:"+ (startMemory-endMemory)/1024 + "KB");  
    System.out.println("用时:"+ (endTime-startTime)/1000 + "s");  
} catch (SQLException e) {  
    e.printStackTrace();  
    conn.rollback();  
}finally{  
    JdbcUtil.close(rs, stmt, conn);  
}  
//执行结果：  
<span style="font-family: Arial, Helvetica, sans-serif;">使用内存大小:329KB</span>  
用时:98s  
Note：本次实验得出的结论是使用executeUpdate的批量提交法比executeBatch效率高，速度更快，占用内存更小，如果有朋友有不同结论的话，欢迎留言交流。
