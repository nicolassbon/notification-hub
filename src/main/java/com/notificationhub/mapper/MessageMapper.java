package com.notificationhub.mapper;

import com.notificationhub.dto.response.MessageResponse;
import com.notificationhub.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MessageDeliveryMapper.class})
public interface MessageMapper {

    @Mapping(target = "username", source = "user.username")
    MessageResponse toResponse(Message message);

    List<MessageResponse> toResponseList(List<Message> messages);
}
