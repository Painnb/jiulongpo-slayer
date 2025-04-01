package org.swu.vehiclecloud.util;

/**
 * 密码验证工具类
 */
public class PasswordValidator {
    
    /**
     * 验证密码强度
     * 密码必须满足以下条件：
     * 1. 长度至少8位
     * 2. 包含至少一个大写字母
     * 3. 包含至少一个小写字母
     * 4. 包含至少一个数字
     * 5. 包含至少一个特殊字符
     *
     * @param password 待验证的密码
     * @return 验证结果
     */
    public static boolean validatePassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecialChar = true;
            }
        }
        
        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }
    
    /**
     * 获取密码验证错误信息
     *
     * @param password 待验证的密码
     * @return 错误信息，如果密码符合要求则返回null
     */
    public static String getPasswordValidationMessage(String password) {
        if (password == null || password.length() < 8) {
            return "密码长度至少为8位";
        }
        
        boolean hasUpperCase = false;
        boolean hasLowerCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowerCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecialChar = true;
            }
        }
        
        StringBuilder message = new StringBuilder();
        if (!hasUpperCase) {
            message.append("密码必须包含至少一个大写字母");
        }
        if (!hasLowerCase) {
            if (message.length() > 0) {
                message.append("、");
            }
            message.append("密码必须包含至少一个小写字母");
        }
        if (!hasDigit) {
            if (message.length() > 0) {
                message.append("、");
            }
            message.append("密码必须包含至少一个数字");
        }
        if (!hasSpecialChar) {
            if (message.length() > 0) {
                message.append("、");
            }
            message.append("密码必须包含至少一个特殊字符");
        }
        
        return message.length() > 0 ? message.toString() : null;
    }
} 