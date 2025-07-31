package com.acme.air.dto;

import java.util.List;

public record FlightSearchResponse(
        List<FlightDTO> flights
) { }
