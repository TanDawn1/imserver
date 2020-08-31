package com.hutquanim.imserver.service;


import com.hutquanim.imserver.mapper.IMessageMapper;
import com.hutquanim.imserver.mapper.IUserMapper;
import com.hutquanim.imserver.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserService {

    @Autowired
    private IUserMapper iUserMapper;

    public User getUserById(Long id) {
        Optional<User> userData = iUserMapper.findById(id);
        return userData.orElse(null);
    }
    public User saveUser(User user) {
        return iUserMapper.save(user);
    }

}
