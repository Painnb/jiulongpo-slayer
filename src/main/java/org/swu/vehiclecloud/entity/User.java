package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor //自动生成一个无参构造函数
public class User {

    private String id;         // 用户 ID
    private String username; // 用户名

    @TableField("password_hash") // 映射数据库中的字段名 password_hash
    private String password; // 密码

    private String role;     // 角色
    private String email;   // 邮箱

    @TableField("created_at") // 映射数据库中的字段名 created_at
    private Date created_time; // 创建时间

    // 带参数的构造函数
    public User(String id, String username, String password, String role, String email, Date created_time) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.created_time = created_time;
    }

    public void setPhone(String phone) {
    }

    public void setStatus(int i) {
    }

    public void setCreateTime(LocalDateTime now) {
    }

    public void setUpdateTime(LocalDateTime now) {

    }
}
