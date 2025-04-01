package org.swu.vehiclecloud.dto;

import lombok.Getter;
import java.io.Serializable;

@Getter
public class UserDTO implements Serializable {
    // Getters and Setters
    private Long id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer status;

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}