package com.hutquanim.imserver.socket;

import com.hutquanim.imserver.common.Constants;
import lombok.Data;


@Data

public class Message {

    private Integer id;

    private Integer srcUserId;//发送人ID

    private Integer desUserId;//接收人ID

    private String content;//消息内容

    private long time;

    private boolean alreadySent;//是否已发送

    public String buildTopic() {
        return Constants.WEBSOCKET_MESSAGE + srcUserId + ":" + desUserId;
    }
}
