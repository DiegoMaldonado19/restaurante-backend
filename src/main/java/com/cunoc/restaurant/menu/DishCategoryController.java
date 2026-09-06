package com.cunoc.restaurant.menu;

import com.cunoc.restaurant.menu.dto.DishCategoryDTO;
import com.cunoc.restaurant.menu.dto.DishCategoryView;
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
@RequestMapping("/api/v1/dish-categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "Categorias de platillo",
     description = "Entrada, plato fuerte, bebida, postre... con su orden de despliegue en el menu")
public class DishCategoryController
{
    private final DishCategoryService dishCategoryService;

    @GetMapping
    @Operation(summary = "Categorias de platillo, ordenadas por orden de despliegue")
    @ApiResponse(responseCode = "200", description = "Listado de categorias")
    public List<DishCategoryView> findAll()
    {
        return dishCategoryService.findAll();
    }

    @PostMapping
    @Operation(summary = "Crea una categoria de platillo")
    @ApiResponse(responseCode = "201", description = "Categoria creada")
    @ApiResponse(responseCode = "409", description = "DISH_CATEGORY_NAME_TAKEN")
    public ResponseEntity<DishCategoryView> create(@Valid @RequestBody DishCategoryDTO request)
    {
        var category = dishCategoryService.create(request);

        return ResponseEntity
                .created(URI.create("/api/v1/dish-categories/" + category.dishCategoryId()))
                .body(category);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renombra la categoria y ajusta su orden de despliegue")
    @ApiResponse(responseCode = "404", description = "DISH_CATEGORY_NOT_FOUND")
    @ApiResponse(responseCode = "409", description = "DISH_CATEGORY_NAME_TAKEN")
    public DishCategoryView update(@PathVariable Long id, @Valid @RequestBody DishCategoryDTO request)
    {
        return dishCategoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Da de baja la categoria",
               description = "Baja logica: los platillos que ya la usan la conservan, solo deja de ofrecerse.")
    @ApiResponse(responseCode = "204", description = "Categoria dada de baja")
    @ApiResponse(responseCode = "404", description = "DISH_CATEGORY_NOT_FOUND")
    public ResponseEntity<Void> deactivate(@PathVariable Long id)
    {
        dishCategoryService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}
