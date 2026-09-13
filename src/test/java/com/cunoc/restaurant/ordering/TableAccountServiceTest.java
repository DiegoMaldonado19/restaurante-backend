package com.cunoc.restaurant.ordering;

import com.cunoc.restaurant.common.enums.TableStatus;
import com.cunoc.restaurant.common.enums.TableZone;
import com.cunoc.restaurant.common.exception.BusinessException;
import com.cunoc.restaurant.common.exception.ErrorCode;
import com.cunoc.restaurant.ordering.dto.*;
import com.cunoc.restaurant.ordering.model.AccountStatus;
import com.cunoc.restaurant.ordering.model.AccountSplit;
import com.cunoc.restaurant.ordering.model.OrderItem;
import com.cunoc.restaurant.ordering.model.TableAccount;
import com.cunoc.restaurant.restaurant.RestaurantTableService;
import com.cunoc.restaurant.restaurant.dto.RestaurantTableView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Pruebas de cuentas de mesa: apertura, división, fusión y cancelación.
 */
class TableAccountServiceTest
{
    private static final Long ACCOUNT_ID = 1L;
    private static final Long TABLE_ID = 10L;
    private static final Long WAITER_ID = 5L;

    private final TableAccountRepository accountRepository = mock(TableAccountRepository.class);
    private final AccountSplitRepository splitRepository = mock(AccountSplitRepository.class);
    private final OrderTicketRepository ticketRepository = mock(OrderTicketRepository.class);
    private final OrderItemRepository itemRepository = mock(OrderItemRepository.class);
    private final RestaurantTableService tableService = mock(RestaurantTableService.class);

    private final TableAccountService accountService =
            new TableAccountService(accountRepository, splitRepository, ticketRepository, itemRepository, tableService);

    private TableAccount account;

    @BeforeEach
    void setUp()
    {
        // Mock security context
        var jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(String.valueOf(WAITER_ID));
        var auth = new UsernamePasswordAuthenticationToken(jwt, null,
                List.of(new SimpleGrantedAuthority("ROLE_WAITER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        account = new TableAccount();
        account.setTableAccountId(ACCOUNT_ID);
        account.setRestaurantTableId(TABLE_ID);
        account.setWaiterId(WAITER_ID);
        account.setGuestCount(4);
        account.setStatus(AccountStatus.OPEN);
        account.setOpenedAt(LocalDateTime.now());
        account.setAccountSplits(new ArrayList<>());
        account.setOrderTickets(new ArrayList<>());

        when(accountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(TableAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(splitRepository.save(any(AccountSplit.class))).thenAnswer(inv ->
        {
            var split = inv.getArgument(0, AccountSplit.class);
            if (split.getAccountSplitId() == null)
            {
                split.setAccountSplitId(100L); // ID fijo para testing
            }
            return split;
        });
        when(splitRepository.saveAll(any())).thenAnswer(inv ->
        {
            var splits = inv.getArgument(0, List.class);
            for (Object obj : splits)
            {
                var split = (AccountSplit) obj;
                if (split.getAccountSplitId() == null)
                {
                    split.setAccountSplitId(100L); // ID fijo para testing
                }
            }
            return splits;
        });
        when(tableService.findById(TABLE_ID)).thenReturn(
                new RestaurantTableView(TABLE_ID, 1, 4, TableZone.SALON, TableStatus.FREE));
    }
    @AfterEach
    void limpiarContextoDeSeguridad()
    {
        // El SecurityContextHolder es estatico y surefire reutiliza la JVM: sin esto la
        // autenticacion se filtra a la siguiente clase y UserControllerSecurityTest ve un
        // token donde esperaba una peticion anonima.
        SecurityContextHolder.clearContext();
    }


    // --- División por persona ------------------------------------------------

    @Test
    void splitPorPersonaN_creaSubCuentasConMontoIgual()
    {
        var request = new SplitAccountDTO(
                com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON, 3, null);

        var splits = accountService.split(ACCOUNT_ID, request);

        assertThat(splits).hasSize(3);
        assertThat(splits).allSatisfy(split ->
        {
            assertThat(split.label()).startsWith("Persona");
            assertThat(split.mode()).isEqualTo(com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON);
        });
    }

    @Test
    void splitPorPersonaNMinimoEs2()
    {
        var request = new SplitAccountDTO(
                com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON, 1, null);

        assertThatThrownBy(() -> accountService.split(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void splitPorPersonaNMaximoEs10()
    {
        var request = new SplitAccountDTO(
                com.cunoc.restaurant.ordering.model.SplitMode.BY_PERSON, 11, null);

        assertThatThrownBy(() -> accountService.split(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // --- División por ítem ---------------------------------------------------

    @Test
    void splitPorItemRequiereItems()
    {
        var request = new SplitAccountDTO(
                com.cunoc.restaurant.ordering.model.SplitMode.BY_ITEM, null, List.of());

        assertThatThrownBy(() -> accountService.split(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void splitPorItemConItemYaAsignadoLanzaError()
    {
        // Crear un ítem ya asignado a otro split
        var existingSplit = new AccountSplit();
        existingSplit.setAccountSplitId(99L);

        var item = new OrderItem();
        item.setOrderItemId(1L);
        item.setSplit(existingSplit);

        // Crear un split nuevo que será devuelto por el repositorio
        var newSplit = new AccountSplit();
        newSplit.setAccountSplitId(100L);
        newSplit.setAccount(account);

        var line = new SplitLineDTO(1L, 100L);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(splitRepository.findById(100L)).thenReturn(Optional.of(newSplit));
        when(splitRepository.findByAccountTableAccountId(ACCOUNT_ID)).thenReturn(List.of());

        var request = new SplitAccountDTO(
                com.cunoc.restaurant.ordering.model.SplitMode.BY_ITEM, null, List.of(line));

        assertThatThrownBy(() -> accountService.split(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SPLIT_ITEMS_MISMATCH);
    }

    // --- Eliminar sub-cuenta -------------------------------------------------

    @Test
    void deleteSplitDevuelveItemsAlPool()
    {
        var split = new AccountSplit();
        split.setAccountSplitId(1L);
        split.setAccount(account);

        var item = new OrderItem();
        item.setOrderItemId(1L);
        item.setSplit(split);

        when(splitRepository.findById(1L)).thenReturn(Optional.of(split));
        when(itemRepository.findBySplitAccountSplitId(1L)).thenReturn(List.of(item));

        accountService.deleteSplit(1L);

        assertThat(item.getSplit()).isNull();
    }

    @Test
    void deleteSplitDeCuentaCerradaNoPermitido()
    {
        account.setStatus(AccountStatus.CLOSED);

        var split = new AccountSplit();
        split.setAccountSplitId(1L);
        split.setAccount(account);

        when(splitRepository.findById(1L)).thenReturn(Optional.of(split));

        assertThatThrownBy(() -> accountService.deleteSplit(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_OPEN);
    }

    @Test
    void deleteSplitYaFacturadaLanza409()
    {
        // La FK fk_invoice_split dispara DataIntegrityViolationException al hacer flush
        // del DELETE: el servicio la traduce a 409 SPLIT_ALREADY_INVOICED.
        var split = new AccountSplit();
        split.setAccountSplitId(1L);
        split.setAccount(account);

        when(splitRepository.findById(1L)).thenReturn(Optional.of(split));
        when(itemRepository.findBySplitAccountSplitId(1L)).thenReturn(List.of());
        // La FK no se dispara en delete() (solo encola el DELETE) sino en flush(), cuando
        // el DELETE llega a la BD y viola fk_invoice_split. Es exactamente el escenario
        // que el flush() dentro del try convierte en 409 en vez de 500 en el commit.
        doThrow(new DataIntegrityViolationException("fk_invoice_split"))
                .when(splitRepository).flush();

        assertThatThrownBy(() -> accountService.deleteSplit(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.SPLIT_ALREADY_INVOICED);

        // El flush debe ir despues del delete: es lo que fuerza la FK a dispararse
        // dentro del try/catch en vez de en el commit (donde daria 500, no 409).
        var calls = inOrder(splitRepository);
        calls.verify(splitRepository).delete(split);
        calls.verify(splitRepository).flush();
    }

    // --- Abrir cuenta --------------------------------------------------------

    @Test
    void openCuentaEnMesaOcupadaFalla()
    {
        when(tableService.findById(TABLE_ID)).thenReturn(
                new RestaurantTableView(TABLE_ID, 1, 4, TableZone.SALON, TableStatus.OCCUPIED));
        when(accountRepository.findByRestaurantTableIdAndStatusIn(TABLE_ID,
                java.util.Set.of(AccountStatus.OPEN, AccountStatus.BILL_REQUESTED)))
                .thenReturn(Optional.empty());

        var request = new OpenAccountDTO(TABLE_ID, 2);

        assertThatThrownBy(() -> accountService.open(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TABLE_NOT_FREE);
    }

    @Test
    void openCuentaExcediendoCapacidadFalla()
    {
        when(accountRepository.findByRestaurantTableIdAndStatusIn(TABLE_ID,
                java.util.Set.of(AccountStatus.OPEN, AccountStatus.BILL_REQUESTED)))
                .thenReturn(Optional.empty());

        var request = new OpenAccountDTO(TABLE_ID, 10); // Capacidad es 4

        assertThatThrownBy(() -> accountService.open(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TABLE_CAPACITY_EXCEEDED);
    }

    // --- Transferir cuenta ---------------------------------------------------

    @Test
    void transferCuentaAMesaOcupadaFalla()
    {
        var targetTableId = 20L;
        when(tableService.findById(targetTableId)).thenReturn(
                new RestaurantTableView(targetTableId, 2, 4, TableZone.TERRACE, TableStatus.OCCUPIED));

        var request = new TransferAccountDTO(targetTableId);

        assertThatThrownBy(() -> accountService.transfer(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TABLE_NOT_FREE);
    }

    // --- Fusionar cuentas ----------------------------------------------------

    @Test
    void mergeCuentasIgualesFalla()
    {
        var request = new MergeAccountDTO(ACCOUNT_ID); // Misma cuenta

        assertThatThrownBy(() -> accountService.merge(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_MERGE_INVALID);
    }

    @Test
    void mergeConCuentaCerradaFalla()
    {
        account.setStatus(AccountStatus.CLOSED);

        var sourceAccount = new TableAccount();
        sourceAccount.setTableAccountId(2L);
        sourceAccount.setStatus(AccountStatus.OPEN);

        when(accountRepository.findById(2L)).thenReturn(Optional.of(sourceAccount));

        var request = new MergeAccountDTO(2L);

        assertThatThrownBy(() -> accountService.merge(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_MERGE_INVALID);
    }

    // --- Cancelar cuenta -----------------------------------------------------

    @Test
    void cancelCuentaCerradaNoPermitido()
    {
        account.setStatus(AccountStatus.CLOSED);

        var request = new CancelAccountDTO("Motivo de prueba");

        assertThatThrownBy(() -> accountService.cancel(ACCOUNT_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_NOT_OPEN);
    }

    @Test
    void cancelCuentaAbiertaFunciona()
    {
        var request = new CancelAccountDTO("Cliente se fue");

        var result = accountService.cancel(ACCOUNT_ID, request);

        assertThat(result.status()).isEqualTo(AccountStatus.CANCELLED);
    }
}
