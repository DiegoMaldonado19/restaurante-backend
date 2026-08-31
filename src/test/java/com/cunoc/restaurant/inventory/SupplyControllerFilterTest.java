package com.cunoc.restaurant.inventory;

import com.cunoc.restaurant.config.CorsConfig;
import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * spring.jackson.property-naming-strategy=SNAKE_CASE solo aplica al cuerpo JSON, no a
 * los query params: sin @RequestParam(name = "low_stock") el filtro llega nulo y el
 * endpoint devuelve todo el catalogo sin fallar. Un filtro que no filtra no lanza nada,
 * asi que solo una prueba lo ve.
 */
@WebMvcTest(SupplyController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@EnableConfigurationProperties(RestaurantProperties.class)
@TestPropertySource(properties = {
        "restaurant.security.jwt.secret=un-secreto-de-prueba-de-mas-de-32-bytes",
        "restaurant.security.jwt.access-minutes=720",
        "restaurant.cors.allowed-origins=http://localhost:4200"
})
class SupplyControllerFilterTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void losFiltrosDeDosPalabrasLleganEnSnakeCase() throws Exception
    {
        when(inventoryService.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/supplies")
                        .param("category_id", "3")
                        .param("low_stock", "true")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());

        var lowStock = ArgumentCaptor.forClass(Boolean.class);

        verify(inventoryService).search(eq(3L), any(), any(), lowStock.capture(), any(Pageable.class));
        assertThat(lowStock.getValue()).isTrue();
    }
}
