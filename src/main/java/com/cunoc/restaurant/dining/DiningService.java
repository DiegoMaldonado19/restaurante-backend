package com.cunoc.restaurant.dining;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.common.exception.NotFoundException;
import com.cunoc.restaurant.customer.CustomerService;
import com.cunoc.restaurant.customer.dto.CreateCustomerDTO;
import com.cunoc.restaurant.dining.dto.*;
import com.cunoc.restaurant.dining.model.CancellationReason;
import com.cunoc.restaurant.dining.model.Reservation;
import com.cunoc.restaurant.dining.model.ReservationStatus;
import com.cunoc.restaurant.dining.model.WaitlistEntry;
import com.cunoc.restaurant.dining.model.WaitlistStatus;
import com.cunoc.restaurant.ordering.TableAccountService;
import com.cunoc.restaurant.ordering.dto.OpenAccountDTO;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiningService
{
    private final ReservationRepository    reservationRepository;
    private final WaitlistEntryRepository  waitlistEntryRepository;
    private final RestaurantTableService   tableService;
    private final TableAccountService      tableAccountService;
    private final CustomerService          customerService;

    @Value("${restaurant.reservation.grace-minutes}")
    private int graceMinutes;

    // === Reservas ============================================================

    @Transactional(readOnly = true)
    public Page<ReservationView> searchReservations(LocalDate date, ReservationStatus status,
                                                     Long tableId, Pageable pageable)
    {
        LocalDateTime from = date != null ? date.atStartOfDay() : null;
        LocalDateTime to   = date != null ? date.plusDays(1).atStartOfDay() : null;

        return reservationRepository.search(from, to, status, tableId, pageable)
                .map(this::toReservationView);
    }

    public ReservationView createReservation(CreateReservationDTO request)
    {
        var table = tableService.findById(request.tableId());

        if (request.guestCount() > table.capacity())
            throw new BusinessException(ErrorCode.TABLE_CAPACITY_EXCEEDED,
                    "La mesa " + table.restaurantTableId() + " tiene capacidad para " + table.capacity()
                            + " comensales, se solicitaron " + request.guestCount() + ".");

        validateSlotAvailable(request.tableId(), request.reservedAt(), null);

        Long customerId = findOrCreateCustomer(request.customerName(), request.customerPhone());

        // Bloquea la mesa de inmediato: el sistema no modela un calendario de horarios
        // por mesa, solo un estado en tiempo real (ver nota de diseno acordada).
        tableService.transitionTo(request.tableId(), TableStatus.RESERVED);

        var reservation = new Reservation();
        reservation.setCustomerId(customerId);
        reservation.setRestaurantTableId(request.tableId());
        reservation.setReservedAt(request.reservedAt());
        reservation.setGuestCount(request.guestCount());
        reservation.setStatus(ReservationStatus.BOOKED);
        reservation.setNote(request.note());
        reservation.setCreatedAt(LocalDateTime.now());

        reservationRepository.save(reservation);

        return toReservationView(reservation);
    }

    @Transactional(readOnly = true)
    public ReservationView findReservationById(Long id)
    {
        return toReservationView(findReservationOrFail(id));
    }

    public ReservationView updateReservation(Long id, UpdateReservationDTO request)
    {
        var reservation = findReservationForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.BOOKED)
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CLOSED,
                    "La reserva " + id + " ya fue sentada, cancelada o marcada como no-show.");

        var table = tableService.findById(request.tableId());

        if (request.guestCount() > table.capacity())
            throw new BusinessException(ErrorCode.TABLE_CAPACITY_EXCEEDED,
                    "La mesa " + table.restaurantTableId() + " tiene capacidad para " + table.capacity()
                            + " comensales, se solicitaron " + request.guestCount() + ".");

        validateSlotAvailable(request.tableId(), request.reservedAt(), id);

        // Si cambia de mesa, libera la anterior y bloquea la nueva.
        if (!reservation.getRestaurantTableId().equals(request.tableId()))
        {
            tableService.transitionTo(reservation.getRestaurantTableId(), TableStatus.FREE);
            tableService.transitionTo(request.tableId(), TableStatus.RESERVED);
        }

        reservation.setRestaurantTableId(request.tableId());
        reservation.setReservedAt(request.reservedAt());
        reservation.setGuestCount(request.guestCount());

        reservationRepository.save(reservation);

        return toReservationView(reservation);
    }

    public SeatingResultView seatReservation(Long id)
    {
        var reservation = findReservationForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.BOOKED)
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CLOSED,
                    "La reserva " + id + " ya fue sentada, cancelada o marcada como no-show.");

        var windowStart = reservation.getReservedAt().minusMinutes(graceMinutes);
        var windowEnd   = reservation.getReservedAt().plusMinutes(graceMinutes);
        var now         = LocalDateTime.now();

        if (now.isBefore(windowStart) || now.isAfter(windowEnd))
            throw new BusinessException(ErrorCode.RESERVATION_NOT_DUE,
                    "La reserva " + id + " solo puede sentarse entre " + windowStart + " y " + windowEnd + ".");

        tableService.transitionTo(reservation.getRestaurantTableId(), TableStatus.FREE);

        var account = tableAccountService.open(
                new OpenAccountDTO(reservation.getRestaurantTableId(), reservation.getGuestCount()));

        reservation.setStatus(ReservationStatus.SEATED);
        reservation.setTableAccountId(account.tableAccountId());
        reservationRepository.save(reservation);

        return new SeatingResultView(account.tableAccountId());
    }

    public ReservationView cancelReservation(Long id, CancelReservationDTO request)
    {
        var reservation = findReservationForUpdate(id);

        if (reservation.getStatus() != ReservationStatus.BOOKED)
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CLOSED,
                    "La reserva " + id + " ya fue sentada, cancelada o marcada como no-show.");

        reservation.setStatus(request.reason() == CancellationReason.NO_SHOW
                ? ReservationStatus.NO_SHOW
                : ReservationStatus.CANCELLED);
        reservation.setCancellationReason(request.reason());
        reservation.setNote(request.note());

        reservationRepository.save(reservation);

        tableService.transitionTo(reservation.getRestaurantTableId(), TableStatus.FREE);

        return toReservationView(reservation);
    }

    // === Lista de espera ======================================================

    @Transactional(readOnly = true)
    public List<WaitlistEntryView> searchWaitlist(WaitlistStatus status, Long fitsTableId)
    {
        Integer fitsCapacity = null;
        if (fitsTableId != null)
            fitsCapacity = tableService.findById(fitsTableId).capacity();

        return waitlistEntryRepository.search(status, fitsCapacity).stream()
                .map(this::toWaitlistEntryView)
                .toList();
    }

    public WaitlistEntryView createWaitlistEntry(CreateWaitlistEntryDTO request)
    {
        Long customerId = findOrCreateCustomer(request.customerName(), request.customerPhone());

        var entry = new WaitlistEntry();
        entry.setCustomerId(customerId);
        entry.setGuestCount(request.guestCount());
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setArrivedAt(LocalDateTime.now());

        waitlistEntryRepository.save(entry);

        return toWaitlistEntryView(entry);
    }

    public SeatingResultView seatWaitlistEntry(Long id, SeatWaitlistDTO request)
    {
        var entry = waitlistEntryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WAITLIST_ENTRY_NOT_FOUND,
                        "No existe la entrada " + id + " en la lista de espera."));

        if (entry.getStatus() != WaitlistStatus.WAITING)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La entrada " + id + " ya fue atendida o el cliente se retiro.");

        var table = tableService.findById(request.tableId());

        if (entry.getGuestCount() > table.capacity())
            throw new BusinessException(ErrorCode.WAITLIST_TABLE_TOO_SMALL,
                    "La mesa " + table.restaurantTableId() + " tiene capacidad para " + table.capacity()
                            + " y el grupo es de " + entry.getGuestCount() + " personas.");

        var account = tableAccountService.open(new OpenAccountDTO(request.tableId(), entry.getGuestCount()));

        entry.setStatus(WaitlistStatus.SEATED);
        entry.setRestaurantTableId(request.tableId());
        entry.setTableAccountId(account.tableAccountId());
        entry.setSeatedAt(LocalDateTime.now());

        waitlistEntryRepository.save(entry);

        return new SeatingResultView(account.tableAccountId());
    }

    public void removeWaitlistEntry(Long id)
    {
        var entry = waitlistEntryRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.WAITLIST_ENTRY_NOT_FOUND,
                        "No existe la entrada " + id + " en la lista de espera."));

        if (entry.getStatus() != WaitlistStatus.WAITING)
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La entrada " + id + " ya fue atendida o el cliente ya se habia retirado.");

        entry.setStatus(WaitlistStatus.LEFT);
        waitlistEntryRepository.save(entry);
    }

    // === Panel de ocupacion ===================================================

    @Transactional(readOnly = true)
    public List<FloorPlanView> floorPlan()
    {
        var tables = tableService.search(null, null, null, Pageable.unpaged()).getContent();
        var endOfDay = LocalDate.now().plusDays(1).atStartOfDay();

        return tables.stream().map(table -> buildFloorPlanRow(table, endOfDay)).toList();
    }

    private FloorPlanView buildFloorPlanRow(RestaurantTableView table, LocalDateTime endOfDay)
    {
        FloorPlanView.OpenAccountSummary openAccount = tableAccountService
                .findOpenByTable(table.restaurantTableId())
                .map(a -> new FloorPlanView.OpenAccountSummary(
                        a.tableAccountId(), a.waiterName(), a.openedAt(), a.runningTotal()))
                .orElse(null);

        FloorPlanView.NextReservationSummary nextReservation = null;
        var upcoming = reservationRepository.search(LocalDateTime.now(), endOfDay, ReservationStatus.BOOKED,
                table.restaurantTableId(), PageRequest.of(0, 1)).getContent();

        if (!upcoming.isEmpty())
        {
            var r = upcoming.get(0);
            nextReservation = new FloorPlanView.NextReservationSummary(
                    r.getReservationId(), customerName(r.getCustomerId()), r.getReservedAt(), r.getGuestCount());
        }

        return new FloorPlanView(table.restaurantTableId(), table.tableNumber(), table.capacity(),
                table.zone(), table.status(), openAccount, nextReservation);
    }

    // === Auxiliares ============================================================

    private void validateSlotAvailable(Long tableId, LocalDateTime reservedAt, Long excludingReservationId)
    {
        var windowStart = reservedAt.minusMinutes(graceMinutes);
        var windowEnd   = reservedAt.plusMinutes(graceMinutes);

        var overlapping = reservationRepository.findActiveOverlapping(tableId, windowStart, windowEnd).stream()
                .filter(r -> excludingReservationId == null || !r.getReservationId().equals(excludingReservationId))
                .toList();

        if (!overlapping.isEmpty())
            throw new BusinessException(ErrorCode.RESERVATION_SLOT_UNAVAILABLE,
                    "No hay mesa libre en la mesa " + tableId + " para ese horario y numero de personas.");
    }

    private Long findOrCreateCustomer(String name, String phone)
    {
        var existing = customerService.search(phone, null, PageRequest.of(0, 1));

        if (!existing.isEmpty())
            return existing.getContent().get(0).customerId();

        return customerService.create(new CreateCustomerDTO(name, phone)).customerId();
    }

    private String customerName(Long customerId)
    {
        return customerService.findById(customerId).fullName();
    }

    private String customerPhone(Long customerId)
    {
        return customerService.findById(customerId).phone();
    }

    private ReservationView toReservationView(Reservation entity)
    {
        return ReservationView.from(entity, customerName(entity.getCustomerId()), customerPhone(entity.getCustomerId()));
    }

    private WaitlistEntryView toWaitlistEntryView(WaitlistEntry entity)
    {
        return WaitlistEntryView.from(entity, customerName(entity.getCustomerId()), customerPhone(entity.getCustomerId()));
    }

    private Reservation findReservationOrFail(Long id)
    {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "No existe la reserva " + id + "."));
    }

    private Reservation findReservationForUpdate(Long id)
    {
        return reservationRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "No existe la reserva " + id + "."));
    }
}