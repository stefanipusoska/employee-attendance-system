package com.attendance.system.service;

import com.attendance.system.model.domain.Notification;
import com.attendance.system.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.attendance.system.model.domain.User;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public List<Notification> getAllNotifications() {return notificationRepository.findAll();}

    public Notification getNotificationById(Long id) {return notificationRepository.findById(id)
            .orElseThrow(()-> new RuntimeException("Notification not found with id: " + id));}

    public List<Notification> getNotificationsByUser(User user) {
        return notificationRepository.findByUser(user);
    }
    public Notification saveNotification(Notification notification) {
        if (notification.getCreated_at() == null) {
            notification.setCreated_at(LocalDateTime.now());
        }
        return notificationRepository.save(notification);
    }
    public void deleteNotification(Long id) {
        Notification notification = getNotificationById(id);
        notificationRepository.delete(notification);
    }

    public Notification markAsRead(Long id) {
        Notification notification = getNotificationById(id);
        notification.set_read(true);
        return notificationRepository.save(notification);
    }
}
