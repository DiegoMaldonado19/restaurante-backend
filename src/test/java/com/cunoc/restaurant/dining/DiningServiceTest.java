package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.customer.CustomerService;
import com.cunoc.restaurant.customer.dto.CustomerDetailView;
import com.cunoc.restaurant.customer.dto.CustomerView;
import com.cunoc.restaurant.dining.dto.*;
import com.cunoc.restaurant.dining.model.CancellationReason;
import com.cunoc.restaurant.dining.model.Reservation;
import com.cunoc.restaurant.dining.model.ReservationStatus;
import com.cunoc.restaurant.dining.model.WaitlistEntry;
import com.cunoc.restaurant.dining.model.WaitlistStatus;
import com.cunoc.restaurant.ordering.TableAccountService;
import com.cunoc.restaurant.ordering.dto.OpenAccountDTO;
import com.cunoc.restaurant.ordering.dto.TableAccountView;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pruebas de DiningService: capacidad, solapamiento de horarios, ventana de tiempo
 * para sentar, y las reglas de la lista de espera.
 */
class DiningServiceTest
{
    private static final Long TABLE_ID    = 1L;
    private static final Long CUSTOMER_ID = 7L;
    private static final int  GRACE_MIN   = 30;

    private final ReservationRepository   reservationRepository   = mock(ReservationRepository.class);
    private final WaitlistEntryRepository waitlistEntryRepository = mock(WaitlistEntryRepository.class);
    private final RestaurantTableService  tableService            = mock(RestaurantTableService.class);
    private final TableAccountService     tableAccountService     = mock(TableAccountService.class);
    private final CustomerService         customerService         = mock(CustomerService.class);

    private final DiningService diningService = new DiningService(
            reservationRepository, waitlistEntryRepository, tableService, tableAccountService, customerService);

    private RestaurantTableView table;
    private CustomerView existingCustomer;

    @BeforeEach
    void setUp()
    {
        setField("graceMinutes", GRACE_MIN);

        table = new RestaurantTableView(TABLE_ID, 1, 4, TableZone.SALON, TableStatus.FREE);
        existingCustomer = new CustomerView(CUSTOMER_ID, "Juan Perez", "50412345678", LocalDateTime.now());

        when(tableService.findById(TABLE_ID)).thenReturn(table);
        when(tableService.transitionTo(any(), any())).thenAnswer(inv -> table);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(waitlistEntryRepository.save(any(WaitlistEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        // Cliente ya existe por telefono: no se crea uno nuevo.
        when(customerService.search(existingCustomer.phone(), null, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(existingCustomer)));
        when(customerService.findById(CUSTOMER_ID))
                .thenReturn(new CustomerDetailView(CUSTOMER_ID, existingCustomer.fullName(), existingCustomer.phone(),
                        LocalDateTime.now(), 0, 0L));
    }

    private void setField(String name, Object value)
    {
        try
        {
            var field = DiningService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(diningService, value);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private TableAccountView accountView(Long accountId)
    {
        return new TableAccountView(accountId, TABLE_ID, 2, AccountStatus.OPEN, LocalDateTime.now(), null,
                new TableAccountView.SplitsInfo(0, BigDecimal.ZERO, List.of()), List.of(), BigDecimal.ZERO, 3L, "Waiter#3");
    }

    // --- Crear reserva ---------------------------------------------------

    @Test
    void crearReservaValidaBloqueaLaMesa()
    {
        when(reservationRepository.findActiveOverlapping(any(), any(), any())).thenReturn(List.of());

        var request = new CreateReservationDTO(existingCustomer.fullName(), existingCustomer.phone(),
                TABLE_ID, LocalDateTime.now().plusHours(2), 2, null);

        var result = diningService.createReservation(request);

        assertThat(result.status()).isEqualTo(ReservationStatus.BOOKED);
        assertThat(result.customerId()).isEqualTo(CUSTOMER_ID);
    }

    @Test
    void crearReservaConMasPersonasQueLaCapacidadEsInvalido()
    {
        var request = new CreateReservationDTO(existingCustomer.fullName(), existingCustomer.phone(),
                TABLE_ID, LocalDateTime.now().plusHours(2), 10, null); // mesa tiene capacidad 4

        assertThatThrownBy(() -> diningService.createReservation(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TABLE_CAPACITY_EXCEEDED);
    }

    @Test
    void crearReservaConHorarioYaOcupadoEsInvalido()
    {
        var reservaExistente = new Reservation();
        reservaExistente.setReservationId(99L);
        when(reservationRepository.findActiveOverlapping(any(), any(), any())).thenReturn(List.of(reservaExistente));

        var request = new CreateReservationDTO(existingCustomer.fullName(), existingCustomer.phone(),
                TABLE_ID, LocalDateTime.now().plusHours(2), 2, null);

        assertThatThrownBy(() -> diningService.createReservation(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESERVATION_SLOT_UNAVAILABLE);
    }

    // --- Sentar desde reserva ------------------------------------------------

    @Test
    void sentarDentroDeLaVentanaEsValido()
    {
        var reservation = new Reservation();
        reservation.setReservationId(1L);
        reservation.setRestaurantTableId(TABLE_ID);
        reservation.setGuestCount(2);
        reservation.setReservedAt(LocalDateTime.now()); // justo a tiempo
        reservation.setStatus(ReservationStatus.BOOKED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(tableAccountService.open(new OpenAccountDTO(TABLE_ID, 2))).thenReturn(accountView(50L));

        var result = diningService.seatReservation(1L);

        assertThat(result.tableAccountId()).isEqualTo(50L);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.SEATED);
        assertThat(reservation.getTableAccountId()).isEqualTo(50L);
    }

    @Test
    void sentarFueraDeLaVentanaEsInvalido()
    {
        var reservation = new Reservation();
        reservation.setReservationId(1L);
        reservation.setRestaurantTableId(TABLE_ID);
        reservation.setGuestCount(2);
        reservation.setReservedAt(LocalDateTime.now().plusHours(3)); // muy lejos todavia
        reservation.setStatus(ReservationStatus.BOOKED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> diningService.seatReservation(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESERVATION_NOT_DUE);
    }

    @Test
    void sentarUnaReservaYaCerradaEsInvalido()
    {
        var reservation = new Reservation();
        reservation.setReservationId(1L);
        reservation.setStatus(ReservationStatus.CANCELLED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> diningService.seatReservation(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESERVATION_ALREADY_CLOSED);
    }

    @Test
    void sentarUnaReservaInexistenteEsInvalido()
    {
        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> diningService.seatReservation(1L))
                .isInstanceOf(NotFoundException.class)
                .extracting(ex -> ((NotFoundException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESERVATION_NOT_FOUND);
    }

    // --- Cancelar reserva --------------------------------------------------

    @Test
      void cancelarReservaLiberaLaMesa()
    {
        var reservation = new Reservation();
        reservation.setReservationId(1L);
        reservation.setRestaurantTableId(TABLE_ID);
        reservation.setCustomerId(CUSTOMER_ID);
        reservation.setStatus(ReservationStatus.BOOKED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        var request = new CancelReservationDTO(CancellationReason.CUSTOMER_CANCELLED, "Cambio de planes");
        diningService.cancelReservation(1L, request);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        org.mockito.Mockito.verify(tableService).transitionTo(TABLE_ID, TableStatus.FREE);
    }

    @Test
      void cancelarPorNoShowMarcaEseEstadoExacto()
    {
        var reservation = new Reservation();
        reservation.setReservationId(1L);
        reservation.setRestaurantTableId(TABLE_ID);
        reservation.setCustomerId(CUSTOMER_ID);
        reservation.setStatus(ReservationStatus.BOOKED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        var request = new CancelReservationDTO(CancellationReason.NO_SHOW, null);
        diningService.cancelReservation(1L, request);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.NO_SHOW);
    }

    // --- Lista de espera ------------------------------------------------------

    @Test
    void sentarDeLaColaEnMesaMuyChicaEsInvalido()
    {
        var entry = new WaitlistEntry();
        entry.setWaitlistEntryId(1L);
        entry.setGuestCount(6); // la mesa tiene capacidad 4
        entry.setStatus(WaitlistStatus.WAITING);

        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entry));

        var request = new SeatWaitlistDTO(TABLE_ID);

        assertThatThrownBy(() -> diningService.seatWaitlistEntry(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.WAITLIST_TABLE_TOO_SMALL);
    }

    @Test
    void sentarDeLaColaEnMesaCompatibleEsValido()
    {
        var entry = new WaitlistEntry();
        entry.setWaitlistEntryId(1L);
        entry.setGuestCount(2);
        entry.setStatus(WaitlistStatus.WAITING);

        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entry));
        when(tableAccountService.open(new OpenAccountDTO(TABLE_ID, 2))).thenReturn(accountView(60L));

        var result = diningService.seatWaitlistEntry(1L, new SeatWaitlistDTO(TABLE_ID));

        assertThat(result.tableAccountId()).isEqualTo(60L);
        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.SEATED);
    }

    @Test
    void retirarDeLaColaMarcaComoLeft()
    {
        var entry = new WaitlistEntry();
        entry.setWaitlistEntryId(1L);
        entry.setStatus(WaitlistStatus.WAITING);

        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entry));

        diningService.removeWaitlistEntry(1L);

        assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.LEFT);
    }

    @Test
    void retirarUnaEntradaYaAtendidaEsInvalido()
    {
        var entry = new WaitlistEntry();
        entry.setWaitlistEntryId(1L);
        entry.setStatus(WaitlistStatus.SEATED);

        when(waitlistEntryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> diningService.removeWaitlistEntry(1L))
                .isInstanceOf(BusinessException.class);
    }
}