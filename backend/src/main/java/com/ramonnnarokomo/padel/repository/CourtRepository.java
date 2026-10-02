package com.ramonnnarokomo.padel.repository;

import com.ramonnnarokomo.padel.domain.Court;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {

    /**
     * Loads the court with a row lock (SELECT ... FOR UPDATE) until the transaction ends.
     * Two requests booking the same court therefore run one after the other,
     * so both cannot pass the overlap check at the same time.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Court c where c.id = :id")
    Optional<Court> findByIdForUpdate(@Param("id") Long id);
}
