package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.inventory.dto.SupplyCategoryDTO;
import com.cunoc.restaurant.inventory.dto.SupplyCategoryView;
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

@RestController
@RequestMapping("/api/v1/supply-categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Categorias de insumo",
     description = "Proteina, verdura, lacteo, abarrote... El enunciado deja la lista abierta")
public class SupplyCategoryController
{
    private final SupplyCategoryService supplyCategoryService;

    @GetMapping
    @Operation(summary = "Categorias de insumo, ordenadas por nombre")
    @ApiResponse(responseCode = "200", description = "Listado de categorias")
    public List<SupplyCategoryView> findAll()
    {
        return supplyCategoryService.findAll();
    }

    @PostMapping
    @Operation(summary = "Crea una categoria de insumo")
    @ApiResponse(responseCode = "201", description = "Categoria creada")
    @ApiResponse(responseCode = "409", description = "SUPPLY_CATEGORY_NAME_TAKEN")
    public ResponseEntity<SupplyCategoryView> create(@Valid @RequestBody SupplyCategoryDTO request)
    {
        var category = supplyCategoryService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/supply-categories/" + category.supplyCategoryId()))
                .body(category);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renombra la categoria")
    @ApiResponse(responseCode = "404", description = "SUPPLY_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "SUPPLY_CATEGORY_NAME_TAKEN")
    public SupplyCategoryView rename(@PathVariable Long id, @Valid @RequestBody SupplyCategoryDTO request)
    {
        return supplyCategoryService.rename(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Da de baja la categoria",
               description = "Baja logica: los insumos que ya la usan la conservan, solo deja de ofrecerse.")
    @ApiResponse(responseCode = "204", description = "Categoria dada de baja")
    @ApiResponse(responseCode = "404", description = "SUPPLY_CATEGORY_NOT_FOUND")
    public ResponseEntity<Void> deactivate(@PathVariable Long id)
    {
        supplyCategoryService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
