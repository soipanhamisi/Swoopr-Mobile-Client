package com.example.carpoolclient.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    private String timeStamp;
    private String senderName;
    private String message;
}
