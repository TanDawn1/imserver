package com.hutquanim.imserver.mapper;

import com.hutquanim.imserver.pojo.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUserMapper {


    Optional<User> findById(Long id);

    User save(User user);
}
