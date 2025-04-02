package org.swu.vehiclecloud.mapper;

import org.swu.vehiclecloud.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    void insert(User user);

    User findByUsername(String username);

    User findById(Long id);

    void update(User user);

    void deleteById(Long id);
}