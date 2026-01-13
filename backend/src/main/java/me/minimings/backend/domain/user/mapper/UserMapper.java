package me.minimings.backend.domain.user.mapper;

import org.apache.ibatis.annotations.Mapper;

import me.minimings.backend.domain.user.model.User;


@Mapper
public interface UserMapper {

    // signup - user
    public Integer insertUser(User user);

}
