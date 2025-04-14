package org.swu.vehiclecloud.util;

import java.util.regex.Pattern;
import java.util.Arrays;

/**
 * SQL注入防护工具类
 * <p>
 * 该工具类提供了一系列方法用于防止SQL注入攻击，包括但不限于：
 * <ul>
 *   <li>表名验证 - 确保表名不包含SQL关键词和特殊字符</li>
 *   <li>参数过滤 - 移除或转义可能导致SQL注入的特殊字符</li>
 *   <li>SQL语句验证 - 检测SQL语句中是否存在潜在的注入风险</li>
 * </ul>
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>动态构建SQL语句时</li>
 *   <li>接收用户输入作为SQL查询条件时</li>
 *   <li>处理外部数据源提供的数据时</li>
 * </ul>
 * </p>
 */
public class SQLInjectionProtector {
    
    /**
     * 常见SQL关键词列表
     * 这些关键词通常被用于SQL注入攻击中，应当在用户输入中进行检测和过滤
     */
    private static final String[] SQL_KEYWORDS = {
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "TRUNCATE", "CREATE", "ALTER", 
        "EXEC", "UNION", "WHERE", "OR", "AND", "FROM", "INTO", "TABLE", "DATABASE", 
        "SCHEMA", "GRANT", "REVOKE", "COMMIT", "ROLLBACK", "PROCEDURE", "FUNCTION"
    };
    
    /**
     * 特殊字符正则表达式
     * 匹配可能在SQL注入中使用的特殊字符，包括：
     * <ul>
     *   <li>单引号(') - 用于字符串闭合</li>
     *   <li>分号(;) - 用于语句分隔</li>
     *   <li>连字符(-) - 用于注释</li>
     * </ul>
     */
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[';\\-]");
    
    /**
     * 扩展的特殊字符正则表达式
     * 包含更多可能用于SQL注入的特殊字符和模式
     */
    private static final Pattern EXTENDED_SPECIAL_CHARS = Pattern.compile("[';\\-\\/*=\\\\\\)\\(]");
    
    /**
     * 验证表名是否安全，防止SQL注入攻击
     * <p>
     * 该方法检查表名是否包含SQL关键词或特殊字符，以防止通过表名进行SQL注入攻击。
     * 表名应当只包含字母、数字和下划线。
     * </p>
     * 
     * @param tableName 需要验证的表名
     * @return 如果表名安全返回true，否则返回false
     * @throws SQLInjectionException 当表名为null或空时抛出异常
     */
    public static boolean validateTableName(String tableName) throws SQLInjectionException {
        if (tableName == null || tableName.isEmpty()) {
            throw new SQLInjectionException("表名不能为空");
        }
        
        // 检查表名格式 - 只允许字母、数字和下划线
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
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
     * 过滤SQL参数，移除可能导致SQL注入的特殊字符
     * <p>
     * 该方法移除参数中的特殊字符，如单引号、分号和连字符等，以防止SQL注入攻击。
     * 对于需要保留这些字符的场景，建议使用预编译语句和参数绑定而非此方法。
     * </p>
     * 
     * @param param 需要过滤的参数值
     * @return 过滤后的安全参数
     * @throws SQLInjectionException 当参数处理过程中发生错误时抛出异常
     */
    public static String filterParameter(String param) throws SQLInjectionException {
        if (param == null) {
            return "";
        }
        
        try {
            // 移除特殊字符
            return SPECIAL_CHARS.matcher(param).replaceAll("");
        } catch (Exception e) {
            throw new SQLInjectionException("参数过滤失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 增强版参数过滤，使用更严格的规则过滤SQL参数
     * <p>
     * 该方法使用扩展的特殊字符集进行过滤，提供比基本过滤更严格的保护。
     * 适用于安全要求较高的场景。
     * </p>
     * 
     * @param param 需要过滤的参数值
     * @return 过滤后的安全参数
     * @throws SQLInjectionException 当参数处理过程中发生错误时抛出异常
     */
    public static String filterParameterStrict(String param) throws SQLInjectionException {
        if (param == null) {
            return "";
        }
        
        try {
            // 使用扩展的特殊字符集进行过滤
            return EXTENDED_SPECIAL_CHARS.matcher(param).replaceAll("");
        } catch (Exception e) {
            throw new SQLInjectionException("严格参数过滤失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 检查SQL语句是否存在注入风险
     * <p>
     * 该方法检查SQL语句中是否包含多个语句（通过分号分隔）或注释标记，
     * 这些通常是SQL注入攻击的标志。
     * </p>
     * 
     * @param sql 需要检查的SQL语句
     * @return 如果SQL语句安全返回true，否则返回false
     * @throws SQLInjectionException 当SQL语句为null或空时抛出异常
     */
    public static boolean isSafeSqlStatement(String sql) throws SQLInjectionException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SQLInjectionException("SQL语句不能为空");
        }
        
        // 检查是否包含多个语句（通过分号分隔）
        if (sql.contains(";")) {
            return false;
        }
        
        // 检查是否包含注释标记
        if (sql.contains("--") || sql.contains("/*") || sql.contains("*/")) {
            return false;
        }
        
        // 检查是否包含SQL关键词组合，可能表示注入攻击
        String upperSql = sql.toUpperCase();
        if (upperSql.contains("UNION") && upperSql.contains("SELECT")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * SQL注入异常类
     * 用于表示在SQL注入防护过程中发生的异常
     */
    public static class SQLInjectionException extends RuntimeException {
        
        /**
         * 构造一个新的SQL注入异常
         * 
         * @param message 异常信息
         */
        public SQLInjectionException(String message) {
            super(message);
        }
        
        /**
         * 构造一个新的SQL注入异常
         * 
         * @param message 异常信息
         * @param cause 原始异常
         */
        public SQLInjectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}