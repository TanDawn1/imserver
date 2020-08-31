package com.hutquanim.imserver.mapper;

import com.hutquanim.imserver.socket.Message;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IMessageMapper {

    Message save(Message message);

    Iterable<Message> saveAll(List<Message> messages);

    List<Message> findByDesUserIdAndAlreadySent(Integer userId, boolean b);
}
