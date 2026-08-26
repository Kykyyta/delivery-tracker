package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.exception.NotificationNotFoundException;
import com.example.notificationservice.mapper.NotificationMapper;
import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationMapper notificationMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    @Transactional
    public void createNotification(
            UUID eventId,
            Long deliveryId,
            Long customerId,
            NotificationType type,
            String message
    ) {
        if (notificationRepository.existsByEventId(eventId)) {
            return;
        }

        Notification notification = new Notification();

        notification.setEventId(eventId);
        notification.setDeliveryId(deliveryId);
        notification.setCustomerId(customerId);
        notification.setType(type);
        notification.setMessage(message);

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getAllNotifications(
            Long deliveryId,
            Boolean read,
            Long currentUserId,
            String role
    ) {
        List<Notification> notifications;

        if ("CUSTOMER".equals(role)) {

            if (deliveryId != null && read != null) {
                notifications =
                        notificationRepository
                                .findByCustomerIdAndDeliveryIdAndRead(
                                        currentUserId,
                                        deliveryId,
                                        read
                                );

            } else if (deliveryId != null) {
                notifications =
                        notificationRepository
                                .findByCustomerIdAndDeliveryId(
                                        currentUserId,
                                        deliveryId
                                );

            } else if (read != null) {
                notifications =
                        notificationRepository
                                .findByCustomerIdAndRead(
                                        currentUserId,
                                        read
                                );

            } else {
                notifications =
                        notificationRepository.findByCustomerId(
                                currentUserId
                        );
            }

            return notifications.stream()
                    .map(notificationMapper::toResponse)
                    .toList();
        }

        if ("ADMIN".equals(role)) {

            if (deliveryId != null && read != null) {
                notifications =
                        notificationRepository.findByDeliveryIdAndRead(
                                deliveryId,
                                read
                        );

            } else if (deliveryId != null) {
                notifications =
                        notificationRepository.findByDeliveryId(
                                deliveryId
                        );

            } else if (read != null) {
                notifications =
                        notificationRepository.findByRead(read);

            } else {
                notifications =
                        notificationRepository.findAll();
            }

            return notifications.stream()
                    .map(notificationMapper::toResponse)
                    .toList();
        }

        throw new AccessDeniedException(
                "Нет доступа к уведомлениям"
        );
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(
            Long id,
            Long currentUserId,
            String role
    ) {
        Notification notification =
                findNotificationById(id);

        checkAccess(
                notification,
                currentUserId,
                role
        );

        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public NotificationResponse markAsRead(
            Long id,
            Long currentUserId,
            String role
    ) {
        Notification notification =
                findNotificationById(id);

        checkAccess(
                notification,
                currentUserId,
                role
        );

        notification.setRead(true);

        Notification updatedNotification =
                notificationRepository.save(notification);

        return notificationMapper.toResponse(
                updatedNotification
        );
    }

    private Notification findNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() ->
                        new NotificationNotFoundException(id)
                );
    }

    private void checkAccess(
            Notification notification,
            Long currentUserId,
            String role
    ) {
        if ("ADMIN".equals(role)) {
            return;
        }

        if ("CUSTOMER".equals(role)
                && Objects.equals(
                notification.getCustomerId(),
                currentUserId
        )) {
            return;
        }

        throw new AccessDeniedException(
                "Нет доступа к этому уведомлению"
        );
    }
}