package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.CreateModifierDTO;
import com.cunoc.restaurant.menu.dto.ModifierView;
import com.cunoc.restaurant.menu.dto.UpdateModifierDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Modificadores del platillo. Se listan y se crean colgados del platillo (/dishes/{id}/modifiers)
 * y se editan por su propio id (/modifiers/{id}): dos frentes de la misma capacidad, un controller.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Modificadores", description = "Opciones del platillo (sin cebolla, extra queso) con su costo adicional")
public class ModifierController
{
    private final ModifierService modifierService;

    @GetMapping("/dishes/{dishId}/modifiers")
    @Operation(summary = "Modificadores del platillo con su costo adicional")
    @ApiResponse(responseCode = "200", description = "Listado de modificadores")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    public List<ModifierView> findByDish(@PathVariable Long dishId)
    {
        return modifierService.findByDish(dishId);
    }

    @PostMapping("/dishes/{dishId}/modifiers")
    @Operation(summary = "Crea un modificador del platillo")
    @ApiResponse(responseCode = "201", description = "Modificador creado")
    @ApiResponse(responseCode = "404", description = "DISH_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "MODIFIER_NAME_TAKEN")
    public ResponseEntity<ModifierView> create(@PathVariable Long dishId,
                                               @Valid @RequestBody CreateModifierDTO request)
    {
        var modifier = modifierService.create(dishId, request);

        return ResponseEntity
                .created(URI.create("/api/v1/modifiers/" + modifier.dishModifierId()))
                .body(modifier);
    }

    @PutMapping("/modifiers/{id}")
    @Operation(summary = "Modifica el nombre y el costo adicional del modificador")
    @ApiResponse(responseCode = "404", description = "MODIFIER_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "MODIFIER_NAME_TAKEN")
    public ModifierView update(@PathVariable Long id, @Valid @RequestBody UpdateModifierDTO request)
    {
        return modifierService.update(id, request);
    }

    @DeleteMapping("/modifiers/{id}")
    @Operation(summary = "Da de baja el modificador",
               description = "Baja logica: las comandas historicas que lo aplicaron lo conservan.")
    @ApiResponse(responseCode = "204", description = "Modificador dado de baja")
    @ApiResponse(responseCode = "404", description = "MODIFIER_NOT_FOUND")
    public ResponseEntity<Void> delete(@PathVariable Long id)
    {
        modifierService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
