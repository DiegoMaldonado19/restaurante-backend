package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.ComboDetailView;
import com.cunoc.restaurant.menu.dto.ComboView;
import com.cunoc.restaurant.menu.dto.CreateComboDTO;
import com.cunoc.restaurant.menu.dto.UpdateComboDTO;
import com.cunoc.restaurant.menu.dto.UpdateComboStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/combos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Combos", description = "Promociones: varios platillos a un precio especial")
public class ComboController
{
    private final ComboService comboService;

    @GetMapping
    @Operation(summary = "Combos y promociones. Filtro: active")
    @ApiResponse(responseCode = "200", description = "Listado de combos")
    public List<ComboView> findAll(@RequestParam(required = false) Boolean active)
    {
        return comboService.search(active);
    }

    @PostMapping
    @Operation(summary = "Crea un combo con sus platillos y su precio especial")
    @ApiResponse(responseCode = "201", description = "Combo creado")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "COMBO_NAME_TAKEN")
    public ResponseEntity<ComboDetailView> create(@Valid @RequestBody CreateComboDTO request)
    {
        var combo = comboService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/combos/" + combo.comboId()))
                .body(combo);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del combo con los platillos incluidos y el ahorro frente a comprarlos sueltos")
    @ApiResponse(responseCode = "404", description = "COMBO_NOT_FOUND")
    public ComboDetailView findById(@PathVariable Long id)
    {
        return comboService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifica la composicion y el precio del combo")
    @ApiResponse(responseCode = "404", description = "COMBO_NOT_FOUND | DISH_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "COMBO_NAME_TAKEN")
    public ComboDetailView update(@PathVariable Long id, @Valid @RequestBody UpdateComboDTO request)
    {
        return comboService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva el combo sin borrarlo")
    @ApiResponse(responseCode = "404", description = "COMBO_NOT_FOUND")
    public ComboView changeStatus(@PathVariable Long id, @Valid @RequestBody UpdateComboStatusDTO request)
    {
        return comboService.changeStatus(id, request);
    }
}
