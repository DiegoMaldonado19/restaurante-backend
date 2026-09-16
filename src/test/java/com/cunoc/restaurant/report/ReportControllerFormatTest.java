package com.cunoc.restaurant.report;

import com.cunoc.restaurant.config.CorsConfig;
import com.cunoc.restaurant.config.RestaurantProperties;
import com.cunoc.restaurant.config.SecurityConfig;
import com.cunoc.restaurant.report.dto.ReportRange;
import com.cunoc.restaurant.report.dto.SalesRowView;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lo que no se ve desde el servicio: que el rango y los filtros lleguen bien desde la
 * URL, que format=csv cambie la respuesta entera y que solo el administrador entre.
 */
@WebMvcTest(ReportController.class)
@Import({ SecurityConfig.class, CorsConfig.class })
@EnableConfigurationProperties(RestaurantProperties.class)
@TestPropertySource(properties = {
        "restaurant.security.jwt.secret=un-secreto-de-prueba-de-mas-de-32-bytes",
        "restaurant.security.jwt.access-minutes=720",
        "restaurant.cors.allowed-origins=http://localhost:4200"
})
class ReportControllerFormatTest
{
    private static final List<SalesRowView> ONE_ROW = List.of(
            new SalesRowView("2026-09-01", 2L, 5L,
                             new BigDecimal("300.00"), new BigDecimal("100.00"),
                             new BigDecimal("200.00")));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void porDefectoDevuelveJson() throws Exception
    {
        when(reportService.sales(any(), any(), any(), any())).thenReturn(ONE_ROW);

        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from", "2026-09-01")
                        .param("to",   "2026-09-13")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].period").value("2026-09-01"))
                .andExpect(jsonPath("$[0].sales").value(300.00));
    }

    @Test
    void conFormatCsvDevuelveElArchivoConSuNombre() throws Exception
    {
        when(reportService.sales(any(), any(), any(), any())).thenReturn(ONE_ROW);

        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from",   "2026-09-01")
                        .param("to",     "2026-09-13")
                        .param("format", "csv")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                                           "attachment; filename=\"ventas.csv\""))
                .andExpect(content().string(Matchers.containsString("period,invoices")));
    }

    /** El .xlsx tiene que ser un libro de verdad, no un CSV renombrado: Excel lo rechazaria. */
    @Test
    void conFormatXlsxDevuelveUnLibroDeExcelLegible() throws Exception
    {
        when(reportService.sales(any(), any(), any(), any())).thenReturn(ONE_ROW);

        var response = mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from",   "2026-09-01")
                        .param("to",     "2026-09-13")
                        .param("format", "xlsx")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                                           "attachment; filename=\"ventas.xlsx\""))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(response)))
        {
            var sheet = workbook.getSheetAt(0);

            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("period");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("2026-09-01");
            // Las cifras viajan como numero: quien reciba el archivo puede sumar la columna.
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(300.00);
        }
    }

    @Test
    void unFormatoDesconocidoSeRechaza() throws Exception
    {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from",   "2026-09-01")
                        .param("to",     "2026-09-13")
                        .param("format", "pdf")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isBadRequest());
    }

    /**
     * El snake_case de Jackson no alcanza a los query params: sin @RequestParam(name = ...)
     * el filtro llega nulo y el reporte sale completo sin fallar, que es el error que
     * ninguna otra prueba veria.
     */
    @Test
    void losFiltrosDeDosPalabrasYElRangoLleganAlServicio() throws Exception
    {
        when(reportService.sales(any(), any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from",        "2026-09-01")
                        .param("to",          "2026-09-13")
                        .param("group_by",    "month")
                        .param("category_id", "3")
                        .param("waiter_id",   "7")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isOk());

        var range = ArgumentCaptor.forClass(ReportRange.class);

        verify(reportService).sales(range.capture(), eq("month"), eq(3L), eq(7L));
        assertThat(range.getValue().from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(range.getValue().to()).isEqualTo(LocalDate.of(2026, 9, 13));
    }

    /** El dia "to" tiene que entrar completo, o se pierde la ultima jornada del reporte. */
    @Test
    void elRangoLlegaHastaLaMedianocheDelDiaSiguiente()
    {
        var range = new ReportRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 13));

        assertThat(range.toAt()).isEqualTo(LocalDate.of(2026, 9, 14).atStartOfDay());
    }

    @Test
    void elRangoInvertidoSeRechazaSenalandoElCampoTo() throws Exception
    {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from", "2026-09-13")
                        .param("to",   "2026-09-01")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields[0].field").value("to"));
    }

    /**
     * Sin @Validated en el controller, la validacion de metodo integrada de Spring lanza
     * HandlerMethodValidationException y sale 400; con ella saldria un 500.
     */
    @Test
    void unGroupByInvalidoEsCuatrocientosYNoQuinientos() throws Exception
    {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from",     "2026-09-01")
                        .param("to",       "2026-09-13")
                        .param("group_by", "DAY")
                        .with(jwt().authorities(() -> "ROLE_ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"));
    }

    @Test
    void losReportesSonSoloDelAdministrador() throws Exception
    {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("from", "2026-09-01")
                        .param("to",   "2026-09-13")
                        .with(jwt().authorities(() -> "ROLE_CASHIER")))
                .andExpect(status().isForbidden());
    }
}
