package com.cunoc.restaurant.report;

import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.report.dto.CashShiftRowView;
import com.cunoc.restaurant.report.dto.DishProfitabilityRowView;
import com.cunoc.restaurant.report.dto.DishRankingRowView;
import com.cunoc.restaurant.report.dto.InventoryRowView;
import com.cunoc.restaurant.report.dto.LoyaltyRowView;
import com.cunoc.restaurant.report.dto.ReportRange;
import com.cunoc.restaurant.report.dto.SalesRowView;
import com.cunoc.restaurant.report.dto.TableOccupancyRowView;
import com.cunoc.restaurant.report.dto.WaiterPerformanceRowView;
import com.cunoc.restaurant.report.dto.WasteRowView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Los nueve reportes del enunciado, en JSON o CSV. Todos son de solo lectura y de rol
 * ADMIN, que es lo que declara SecurityConfig para /api/v1/reports/**.
 *
 * El PDF no lo genera el backend: la aplicacion administrativa dibuja el reporte y lo
 * manda a imprimir con la hoja @media print, que tambien resuelve el "exportar a PDF"
 * del enunciado. Excel abre el CSV directamente.
 *
 * group_by, order y format se reciben como String con @Pattern y no como enum: el enlace
 * de un enum distingue mayusculas, y "day" contra DAY acabaria en un 500 en vez del 400
 * que corresponde a un parametro mal escrito.
 *
 * La clase no lleva @Validated a proposito. Con ella, Spring valida los parametros por un
 * proxy AOP que lanza ConstraintViolationException, que el GlobalExceptionHandler no
 * conoce y acaba en 500; sin ella usa la validacion de metodo integrada, que lanza
 * HandlerMethodValidationException y sale el 400 con su lista de campos.
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Las nueve consultas de negocio, exportables a CSV")
public class ReportController
{
    private final ReportService reportService;

    @GetMapping("/sales")
    @Operation(summary = "Ventas por periodo, categoria y mesero",
               description = "Una fila por franja de tiempo. La venta es el subtotal de las "
                           + "lineas entregadas, sin impuesto ni propina, y excluye las "
                           + "facturas anuladas. category_id y waiter_id filtran el conjunto.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = SalesRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> sales(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(name = "group_by", defaultValue = "day")
            @Pattern(regexp = "day|week|month",
                     message = "Los valores validos son day, week o month")                     String      groupBy,
            @RequestParam(name = "category_id", required = false)   Long        categoryId,
            @RequestParam(name = "waiter_id",   required = false)   Long        waiterId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "ventas", SalesRowView.class,
                       reportService.sales(range, groupBy, categoryId, waiterId));
    }

    @GetMapping("/dish-ranking")
    @Operation(summary = "Platillos mas y menos vendidos",
               description = "order=bottom lista los menos vendidos e incluye los que no se "
                           + "vendieron ninguna vez. Cada componente de un combo puntua en su "
                           + "propio platillo, porque asi se guarda la linea de comanda.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = DishRankingRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> dishRanking(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(defaultValue = "top")
            @Pattern(regexp = "top|bottom",
                     message = "Los valores validos son top o bottom")                         String      order,
            @RequestParam(defaultValue = "10") @Min(value = 1,   message = "El limite minimo es 1")
            @Max(value = 100, message = "El limite maximo es 100")    int         limit,
            @RequestParam(name = "category_id", required = false)   Long        categoryId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "platillos", DishRankingRowView.class,
                       reportService.dishRanking(range, order, limit, categoryId));
    }

    @GetMapping("/dish-profitability")
    @Operation(summary = "Rentabilidad por platillo",
               description = "Compara el precio cobrado con el costo congelado en la linea. "
                           + "menu_price es el precio vigente hoy, para ver la desviacion.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = DishProfitabilityRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> dishProfitability(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(name = "category_id", required = false)   Long        categoryId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "rentabilidad", DishProfitabilityRowView.class,
                       reportService.dishProfitability(range, categoryId));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Existencias, insumos bajo minimo y valor del inventario",
               description = "Solo insumos activos: uno dado de baja no tiene saldo utilizable "
                           + "e inflaria el valor total. No lleva rango de fechas, porque es "
                           + "una foto del momento.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = InventoryRowView.class))))
    public ResponseEntity<?> inventory(
            @RequestParam(name = "low_stock_only", defaultValue = "false") boolean lowStockOnly,
            @RequestParam(name = "category_id",    required = false)       Long    categoryId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                                  String  format)
    {
        return respond(format, "inventario", InventoryRowView.class,
                       reportService.inventory(lowStockOnly, categoryId));
    }

    @GetMapping("/waste")
    @Operation(summary = "Mermas del periodo por insumo, motivo y costo perdido",
               description = "La merma se valora al costo que guarda el movimiento y, cuando "
                           + "no lo trae, al costo actual del insumo.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WasteRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> waste(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(name = "supply_id", required = false)     Long        supplyId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "mermas", WasteRowView.class, reportService.waste(range, supplyId));
    }

    @GetMapping("/table-occupancy")
    @Operation(summary = "Ocupacion de mesas por franja horaria",
               description = "Cuenta solo las cuentas cerradas: una cancelada nunca ocupo la "
                           + "mesa y una fusionada no tiene cierre propio que medir.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = TableOccupancyRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> tableOccupancy(
            @ParameterObject @Valid                     ReportRange range,
            @RequestParam(required = false)             TableZone   zone,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")               String      format)
    {
        return respond(format, "ocupacion", TableOccupancyRowView.class,
                       reportService.tableOccupancy(range, zone));
    }

    @GetMapping("/waiter-performance")
    @Operation(summary = "Desempeno por mesero, con la calificacion promedio",
               description = "Cuentas atendidas, venta, propina, tiempo medio entre que la "
                           + "linea se envia y se entrega, y promedio de las calificaciones.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = WaiterPerformanceRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> waiterPerformance(
            @ParameterObject @Valid                     ReportRange range,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")               String      format)
    {
        return respond(format, "meseros", WaiterPerformanceRowView.class,
                       reportService.waiterPerformance(range));
    }

    @GetMapping("/loyalty")
    @Operation(summary = "Puntos otorgados, redimidos y clientes mas frecuentes",
               description = "Con limit, las sumas son las del top N y no las del periodo "
                           + "completo. Los puntos redimidos se reportan en positivo.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = LoyaltyRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> loyalty(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(defaultValue = "20") @Min(value = 1,   message = "El limite minimo es 1")
            @Max(value = 100, message = "El limite maximo es 100")    int         limit,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "fidelizacion", LoyaltyRowView.class,
                       reportService.loyalty(range, limit));
    }

    @GetMapping("/cash-shifts")
    @Operation(summary = "Cuadres de caja del periodo con sus diferencias",
               description = "El detalle de movimientos de un turno lo sirve "
                           + "GET /cash-shifts/{id}/movements.")
    @ApiResponse(responseCode = "200", description = "Filas del reporte",
                 content = @Content(array = @ArraySchema(schema = @Schema(implementation = CashShiftRowView.class))))
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
    public ResponseEntity<?> cashShifts(
            @ParameterObject @Valid                                 ReportRange range,
            @RequestParam(name = "cashier_id", required = false)    Long        cashierId,
            @RequestParam(defaultValue = "json")
            @Pattern(regexp = "json|csv",
                     message = "Los formatos validos son json o csv")                           String      format)
    {
        return respond(format, "cuadres", CashShiftRowView.class,
                       reportService.cashShifts(range, cashierId));
    }

    /** El unico punto que decide entre JSON y CSV: los nueve endpoints pasan por aqui. */
    private <T extends Record> ResponseEntity<?> respond(String format, String name,
                                                         Class<T> rowType, List<T> rows)
    {
        if (!"csv".equalsIgnoreCase(format))
        {
            return ResponseEntity.ok(rows);
        }

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + ".csv\"")
                .body(ReportCsv.of(rowType, rows));
    }
}
