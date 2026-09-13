package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.dining.dto.FloorPlanView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/floor-plan")
@RequiredArgsConstructor
@Tag(name = "Panel de ocupacion", description = "Estado en tiempo real de todas las mesas")
public class FloorPlanController
{
    private final DiningService diningService;

    @GetMapping
    @Operation(summary = "Por mesa: estado, cuenta abierta si la hay, y la proxima reserva del dia")
    public List<FloorPlanView> get()
    {
        return diningService.floorPlan();
    }
}
