package org.smart.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.smart.erp.common.exception.BusinessException;
import org.smart.erp.common.security.CurrentUser;
import org.smart.erp.common.websocket.WebSocketSessionManager;
import org.smart.erp.system.dto.NotificationAddDTO;
import org.smart.erp.system.dto.NotificationPageDto;
import org.smart.erp.system.entity.Notification;
import org.smart.erp.system.mapper.NotificationMapper;
import org.smart.erp.system.service.NotificationService;
import org.smart.erp.system.vo.NotificationVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class NotificationServiceImpl
    extends ServiceImpl<NotificationMapper, Notification>
    implements NotificationService
{

    private final NotificationMapper notificationMapper;
    private final CurrentUser currentUser;
    private final WebSocketSessionManager webSocketSessionManager;
    private final ObjectMapper objectMapper;

    public NotificationServiceImpl(
            NotificationMapper notificationMapper,
            CurrentUser currentUser,
            WebSocketSessionManager webSocketSessionManager,
            ObjectMapper objectMapper
    ) {
        this.notificationMapper = notificationMapper;
        this.currentUser = currentUser;
        this.webSocketSessionManager = webSocketSessionManager;
        this.objectMapper = objectMapper;
    }

    @Override
    public void addNotification(NotificationAddDTO dto) {
        Notification notification = new Notification();
        BeanUtils.copyProperties(dto, notification);
        notification.setIsRead(false);
        save(notification);
        try {
            webSocketSessionManager.sendMessage(dto.getUserId(), objectMapper.writeValueAsString(notification));
        } catch (IOException e) {
            log.warn("通知已保存，WebSocket 推送失败，notificationId={}", notification.getId(), e);
        }
    }

    @Override
    public Page<NotificationVo> pageNotification(NotificationPageDto dto) {
        LambdaQueryWrapper<Notification> qw =
                new LambdaQueryWrapper<Notification>()
                        .eq( Notification::getUserId, currentUser.getUserId())
                        .eq(Objects.nonNull(dto.getIsRead()), Notification::getIsRead,dto.getIsRead())
                        .orderByDesc(Notification::getCreateTime);

        Page<Notification> page = this.page(new Page<>(dto.getPageNum(),dto.getPageSize()),qw);
        Page<NotificationVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());

        voPage.setRecords(
                page.getRecords().stream().map(this::toVo).toList());

        return voPage;
    }

    @Override
    public NotificationVo getNotification(Long notificationId) {
        Notification notification = notificationMapper.selectOne(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getId, notificationId)
                        .eq(Notification::getUserId, currentUser.getUserId())
        );
        if (notification == null) {
            throw new BusinessException(404, "通知不存在");
        }
        return toVo(notification);
    }

    @Override
    public Long getUnReadCount() {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, currentUser.getUserId())
                        .eq(Notification::getIsRead, false)
        );
    }

    @Override
    public void markAsRead( Long notificationId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();

        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getUserId, currentUser.getUserId())
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
    }

    @Override
    public void markAllAsRead() {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();

        wrapper.eq(Notification::getUserId, currentUser.getUserId())
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true)
                .set(Notification::getReadTime, LocalDateTime.now());

        notificationMapper.update(null, wrapper);
    }

    private NotificationVo toVo(Notification notification) {
        NotificationVo notificationVo = new NotificationVo();
        BeanUtils.copyProperties(notification, notificationVo);
        return notificationVo;
    }
}
