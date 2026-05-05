package com.attendance.system.repository;
import com.attendance.system.model.domain.Notification;
import com.attendance.system.model.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // In NotificationRepository
    List<Notification> findByUser(User user);

}
