package com.project.stock.investory.alarm.repository;

import com.project.stock.investory.alarm.entity.Alarm;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByUserUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT a FROM Alarm a WHERE a.user.userId = :userId ORDER BY a.createdAt DESC")
    List<Alarm> findAlarmsByUserId(@Param("userId") Long userId);

    List<Alarm> findByUserUserIdAndIsReadFalse(Long userId);

    Optional<Alarm> findByAlarmIdAndIsReadFalse(Long alarmId);
}
