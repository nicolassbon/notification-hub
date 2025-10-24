package com.notificationhub.mapper;

import com.notificationhub.dto.response.MessageDeliveryResponse;
import com.notificationhub.entity.MessageDelivery;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageDeliveryMapper {

    @Mapping(target = "platform", source = "platformType")
    MessageDeliveryResponse toResponse(MessageDelivery messageDelivery);

    List<MessageDeliveryResponse> toResponseList(List<MessageDelivery> deliveries);
}
