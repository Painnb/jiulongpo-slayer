package org.swu.vehiclecloud.util;

import java.util.regex.Pattern;

/**
 * SQL注入防护工具类
 */
public class SQLInjectionProtector {
    
    // 常见SQL关键词
    private static final String[] SQL_KEYWORDS = {"SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "CREATE", "ALTER", "EXEC", "UNION"};
    
    // 特殊字符正则表达式
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[';\\-]");
    
    /**
     * 验证表名是否安全
     * @param tableName 表名
     * @return 是否安全
     */
    public static boolean validateTableName(String tableName) {
        if (tableName == null || tableName.isEmpty()) {
            return false;
        }
        
        // 检查特殊字符
        if (SPECIAL_CHARS.matcher(tableName).find()) {
            return false;
        }
        
        // 检查SQL关键词
        String upperTableName = tableName.toUpperCase();
        for (String keyword : SQL_KEYWORDS) {
            if (upperTableName.contains(keyword)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 过滤SQL参数
     * @param param 参数值
     * @return 过滤后的安全参数
     */
    public static String filterParameter(String param) {
        if (param == null) {
            return "";
        }
        
        // 移除特殊字符
        return SPECIAL_CHARS.matcher(param).replaceAll("");
    }
}