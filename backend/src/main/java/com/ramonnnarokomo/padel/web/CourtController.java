package com.ramonnnarokomo.padel.web;

import com.ramonnnarokomo.padel.dto.CourtResponse;
import com.ramonnnarokomo.padel.repository.CourtRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only list of courts. There is no business logic here, so it talks to the repository directly. */
@RestController
@RequestMapping("/api/courts")
public class CourtController {

    private final CourtRepository courtRepository;

    public CourtController(CourtRepository courtRepository) {
        this.courtRepository = courtRepository;
    }

    @GetMapping
    public List<CourtResponse> findAll() {
        return courtRepository.findAll(Sort.by("id")).stream()
                .map(CourtResponse::from)
                .toList();
    }
}
