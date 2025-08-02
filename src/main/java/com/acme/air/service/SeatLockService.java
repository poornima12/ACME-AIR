package com.acme.air.service;

import com.acme.air.model.Seat;
import com.acme.air.model.SeatLock;
import com.acme.air.repository.SeatLockRepository;
import com.acme.air.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatLockService {

    private final SeatLockRepository seatLockRepository;
    private final SeatRepository seatRepository;
    private static final int LOCK_DURATION_MINUTES = 10;

    @Transactional
    public void createOrUpdateSeatLock(Seat seat, String sessionId) {
        releaseExpiredLocksForSeat(seat.getId());
        Optional<SeatLock> existingLock = seatLockRepository.findBySessionIdAndSeatId(sessionId, seat.getId());
        SeatLock seatLock;
        if (existingLock.isPresent() && existingLock.get().getStatus() == SeatLock.LockStatus.ACTIVE) {
            seatLock = existingLock.get();
            seatLock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
        } else {
            seatLock = new SeatLock();
            seatLock.setSeat(seat);
            seatLock.setSessionId(sessionId);
            seatLock.setLockedAt(LocalDateTime.now());
            seatLock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            seatLock.setStatus(SeatLock.LockStatus.ACTIVE);
        }
        seatLockRepository.save(seatLock);
    }

    @Transactional
    public void releaseLocksForSession(String sessionId, List<String> seatNumbers) {
        List<SeatLock> locks = seatLockRepository.findActiveLocksForSession(sessionId);
        for (SeatLock lock : locks) {
            lock.setStatus(SeatLock.LockStatus.RELEASED);
            seatLockRepository.save(lock);
            // Release the seat if it's still locked
            Seat seat = lock.getSeat();
            if (seat.getStatus() == Seat.SeatStatus.LOCKED) {
                seat.setStatus(Seat.SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
        }
        log.debug("Released {} seat locks for session {}", locks.size(), sessionId);
    }

    @Transactional
    public void releaseExpiredLocksForSeat(Long seatId) {
        List<SeatLock> expiredLocks = seatLockRepository.findExpiredLocksBySeatId(seatId, LocalDateTime.now());
        for (SeatLock lock : expiredLocks) {
            lock.setStatus(SeatLock.LockStatus.EXPIRED);
            seatLockRepository.save(lock);
        }
    }
}
