package org.swu.vehiclecloud.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Getter
@Setter
@ToString
@NoArgsConstructor
@TableName("user")

public class User implements UserDetails {

    @TableId
    private Integer id;         // 用户 ID
    
    private String username; // 用户名
    
    @TableField("password_hash")
    private String password; // 密码哈希
    
    private String role;     // 角色
    
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

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 创建一个 SimpleGrantedAuthority 对象，表示角色
        Collection<? extends GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));

        // 打印出返回的权限集合
        authorities.forEach(authority -> System.out.println("Granted Authority: " + authority.getAuthority()));

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}

