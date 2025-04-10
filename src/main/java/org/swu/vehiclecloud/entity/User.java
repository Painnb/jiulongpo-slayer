package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

/**
 * 用户实体类
 * 对应数据库中的user表
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("user")
public class User {

    /**
     * 用户ID
     */
    @TableId
    private Integer id;// 用户 ID
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码哈希值
     */
    @TableField("password_hash")
    private String password;
    
    /**
     * 用户角色
     */
    private String role;
    
    /**
     * 用户邮箱
     */
    @TableField("email")
    private String email;
    
    /**
     * 用户创建时间
     */
    @TableField("created_at")
    private Date created_time;

    /**
     * 带参数的构造函数
     * @param id 用户ID
     * @param username 用户名
     * @param password 密码
     * @param role 角色
     * @param email 邮箱
     * @param created_time 创建时间
     */
    public User(Integer id, String username, String password, String role, String email, Date created_time) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.created_time = created_time;
    }
}

