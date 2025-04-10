package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("user")

public class User {

    @TableId
    private Integer id;// 用户 ID
    
    private String username; // 用户名
    
    @TableField("password_hash")
    private String password; // 密码哈希
    
    private String role;     // 角色
    
    @TableField("email")
    private String email;   // 邮箱
    
    @TableField("created_at")
    private Date created_time; // 创建时间

    // 带参数的构造函数
    public User(Integer id, String username, String password, String role, String email, Date created_time) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.created_time = created_time;
    }
}

