package org.swu.vehiclecloud.mapper;

import org.swu.vehiclecloud.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.*;

/**
 * 用户数据访问接口
 * 提供用户数据的CRUD操作
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名和密码查询用户
     * @param username 用户名
     * @param password 密码
     * @return 匹配的用户对象
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND password_hash = #{password}")
    User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

    /**
     * 插入新用户
     * @param user 用户对象
     */
    @Insert("INSERT INTO user (username, password, email, create_time, update_time) VALUES (#{username}, #{password}, #{email}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 匹配的用户对象
     */
    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 匹配的用户对象
     */
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(String id);

    /**
     * 更新用户信息
     * @param user 用户对象
     */
    @Update("UPDATE user SET username = #{username}, password = #{password}, email = #{email}, update_time = NOW() WHERE id = #{id}")
    void update(User user);

    /**
     * 根据ID删除用户
     * @param id 用户ID
     */
    @Delete("DELETE FROM user WHERE id = #{id}")
    void deleteById(String id);
}