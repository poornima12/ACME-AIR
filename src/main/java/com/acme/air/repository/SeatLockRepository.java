package com.acme.air.repository;

import com.acme.air.model.SeatLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeatLockRepository extends JpaRepository<SeatLock, Long> {
}
