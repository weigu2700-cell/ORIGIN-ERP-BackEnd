package org.smart.erp.eip.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.utils.PageConvertUtils;
import org.smart.erp.eip.converter.NotificationConverter;
import org.smart.erp.eip.dto.NotificationPageDto;
import org.smart.erp.eip.entity.Notification;
import org.smart.erp.eip.mapper.NotificationMapper;
import org.smart.erp.eip.service.NotificationInboxService;
import org.smart.erp.eip.vo.NotificationVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationInboxServiceImpl implements NotificationInboxService {
    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;
    private final NotificationConverter converter;

    @Override
    public Page<NotificationVo> page(NotificationPageDto dto) {
        NotificationPageDto query = dto == null ? new NotificationPageDto() : dto;
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, currentUser.getUserId())
                .eq(Objects.nonNull(query.getIsRead()), Notification::getIsRead, query.getIsRead())
                .orderByDesc(Notification::getCreateTime);
        Page<Notification> page = new Page<>(
                defaultValue(query.getPageNum(), 1), defaultValue(query.getPageSize(), 10));
        return PageConvertUtils.convert(notificationMapper.selectPage(page, wrapper), converter::toVo);
    }

    @Override
    public NotificationVo get(Long notificationId) {
        Notification notification = notificationMapper.selectOne(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, currentUser.getUserId()));
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return converter.toVo(notification);
    }

    @Override
    public Long unreadCount() {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, currentUser.getUserId())
                .eq(Notification::getIsRead, false));
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, currentUser.getUserId())
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, LocalDateTime.now()));
    }

    @Override
    public void markAllAsRead() {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, currentUser.getUserId())
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, LocalDateTime.now()));
    }

    private static int defaultValue(Integer value, int fallback) {
        return value == null || value < 1 ? fallback : value;
    }
}
