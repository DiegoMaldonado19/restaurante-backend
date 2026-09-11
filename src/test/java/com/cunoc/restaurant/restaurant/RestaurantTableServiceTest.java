package com.cunoc.restaurant.restaurant;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import com.cunoc.restaurant.restaurant.dto.UpdateTableStatusDTO;
import com.cunoc.restaurant.restaurant.model.RestaurantTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de la matriz de transiciones de mesas.
 * La matriz vive en RestaurantTableService.transitionTo() y es la que la defensa pregunta.
 */
class RestaurantTableServiceTest
{
    private static final Long TABLE_ID = 1L;

    private final RestaurantTableRepository tableRepository = mock(RestaurantTableRepository.class);

    private final RestaurantTableService tableService = new RestaurantTableService(tableRepository);

    private RestaurantTable table;

    @BeforeEach
    void setUp()
    {
        table = new RestaurantTable();
        table.setRestaurantTableId(TABLE_ID);
        table.setTableNumber(1);
        table.setCapacity(4);
        table.setZone(TableZone.SALON);
        table.setStatus(TableStatus.FREE);
        table.setActive(true);

        when(tableRepository.findByIdForUpdate(TABLE_ID)).thenReturn(Optional.of(table));
        when(tableRepository.findById(TABLE_ID)).thenReturn(Optional.of(table));
        when(tableRepository.save(any(RestaurantTable.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Transiciones válidas ------------------------------------------------

    @Test
    void libreAPendienteEsValido()
    {
        var result = tableService.transitionTo(TABLE_ID, TableStatus.RESERVED);

        assertThat(result.status()).isEqualTo(TableStatus.RESERVED);
    }

    @Test
    void libreAOcupadoEsValido()
    {
        var result = tableService.transitionTo(TABLE_ID, TableStatus.OCCUPIED);

        assertThat(result.status()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void pendienteALibreEsValido()
    {
        table.setStatus(TableStatus.RESERVED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.FREE);

        assertThat(result.status()).isEqualTo(TableStatus.FREE);
    }

    @Test
    void pendienteAOcupadoEsValido()
    {
        table.setStatus(TableStatus.RESERVED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.OCCUPIED);

        assertThat(result.status()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void ocupadoALibreEsValido()
    {
        table.setStatus(TableStatus.OCCUPIED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.FREE);

        assertThat(result.status()).isEqualTo(TableStatus.FREE);
    }

    @Test
    void ocupadoAOcupadoEsValido()
    {
        table.setStatus(TableStatus.OCCUPIED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.OCCUPIED);

        assertThat(result.status()).isEqualTo(TableStatus.OCCUPIED);
    }

    @Test
    void ocupadoACuentaSolicitadaEsValido()
    {
        table.setStatus(TableStatus.OCCUPIED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.BILL_REQUESTED);

        assertThat(result.status()).isEqualTo(TableStatus.BILL_REQUESTED);
    }

    @Test
    void cuentaSolicitadaALibreEsValido()
    {
        table.setStatus(TableStatus.BILL_REQUESTED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.FREE);

        assertThat(result.status()).isEqualTo(TableStatus.FREE);
    }

    @Test
    void cuentaSolicitadaAOcupadoEsValido()
    {
        table.setStatus(TableStatus.BILL_REQUESTED);

        var result = tableService.transitionTo(TABLE_ID, TableStatus.OCCUPIED);

        assertThat(result.status()).isEqualTo(TableStatus.OCCUPIED);
    }

    // --- Transiciones inválidas ----------------------------------------------

    @Test
    void libreACuentaSolicitadaEsInvalido()
    {
        assertThatThrownBy(() -> tableService.transitionTo(TABLE_ID, TableStatus.BILL_REQUESTED))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TABLE_TRANSITION);
    }

    @Test
    void pendienteAPendienteEsInvalido()
    {
        table.setStatus(TableStatus.RESERVED);

        assertThatThrownBy(() -> tableService.transitionTo(TABLE_ID, TableStatus.RESERVED))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TABLE_TRANSITION);
    }

    @Test
    void pendienteACuentaSolicitadaEsInvalido()
    {
        table.setStatus(TableStatus.RESERVED);

        assertThatThrownBy(() -> tableService.transitionTo(TABLE_ID, TableStatus.BILL_REQUESTED))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TABLE_TRANSITION);
    }

    @Test
    void cuentaSolicitadaAPendienteEsInvalido()
    {
        table.setStatus(TableStatus.BILL_REQUESTED);

        assertThatThrownBy(() -> tableService.transitionTo(TABLE_ID, TableStatus.RESERVED))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TABLE_TRANSITION);
    }

    @Test
    void cuentaSolicitadaACuentaSolicitadaEsInvalido()
    {
        table.setStatus(TableStatus.BILL_REQUESTED);

        assertThatThrownBy(() -> tableService.transitionTo(TABLE_ID, TableStatus.BILL_REQUESTED))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TABLE_TRANSITION);
    }

    // --- changeStatus pasa por la misma matriz --------------------------------

    @Test
    void changeStatusUsaLaMismaMatrizQueTransitionTo()
    {
        table.setStatus(TableStatus.OCCUPIED);

        var result = tableService.changeStatus(TABLE_ID, new UpdateTableStatusDTO(TableStatus.BILL_REQUESTED));

        assertThat(result.status()).isEqualTo(TableStatus.BILL_REQUESTED);
    }
}
