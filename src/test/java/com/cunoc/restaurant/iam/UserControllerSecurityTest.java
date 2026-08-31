package com.cunoc.restaurant.iam;

import com.cunoc.restaurant.config.CorsConfig;
import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La regla de seguridad no obvia del modulo: el personal solo lo administra el ADMIN.
 * La interfaz oculta, el backend prohibe.
 */
@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@EnableConfigurationProperties(RestaurantProperties.class)   // @WebMvcTest no corre @ConfigurationPropertiesScan
@TestPropertySource(properties = {
        "restaurant.security.jwt.secret=un-secreto-de-prueba-de-mas-de-32-bytes",
        "restaurant.security.jwt.access-minutes=720",
        "restaurant.cors.allowed-origins=http://localhost:4200"
})
class UserControllerSecurityTest
{
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppUserService appUserService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void unMeseroNoPuedeListarElPersonal() throws Exception
    {
        mockMvc.perform(get("/api/v1/users").with(jwt().authorities(() -> "ROLE_WAITER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error_code").value("FORBIDDEN_RESOURCE"));
    }

    @Test
    void sinTokenLaRespuestaEsUnauthenticated() throws Exception
    {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error_code").value("UNAUTHENTICATED"));
    }

    @Test
    void elAdministradorSiPuedeListarElPersonal() throws Exception
    {
        when(appUserService.search(any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/users").with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());
    }
}
