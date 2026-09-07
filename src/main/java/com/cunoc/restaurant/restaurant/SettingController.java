package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.restaurant.dto.RestaurantSettingView;
import com.cunoc.restaurant.restaurant.dto.UpdateSettingDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Validated
@Tag(name = "Configuracion del restaurante",
     description = "Impuesto, propina sugerida y tasas del programa de puntos. Una sola fila, por eso no lleva {id}")
public class SettingController
{
    private final RestaurantSettingService settingService;

    @GetMapping
    @Operation(summary = "Configuracion actual del restaurante",
               description = "La app de operacion la necesita para la precuenta; la administrativa, para editarla.")
    @ApiResponse(responseCode = "200", description = "Configuracion")
    public RestaurantSettingView get()
    {
        return settingService.get();
    }

    @PutMapping
    @Operation(summary = "Actualiza la configuracion",
               description = "Es una sola fila (setting_id = 1): la actualizacion reemplaza las cuatro columnas.")
    @ApiResponse(responseCode = "200", description = "Configuracion actualizada")
    @ApiResponse(responseCode = "404", description = "SETTING_NOT_FOUND")
    public RestaurantSettingView update(@Valid @RequestBody UpdateSettingDTO request)
    {
        return settingService.update(request);
    }
}