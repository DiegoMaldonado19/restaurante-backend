package com.cunoc.restaurant.ordering;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de gestión de sub-cuentas (AccountSplit).
 * Se integra con TableAccountService para operaciones de división.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountSplitService
{
    private final AccountSplitRepository splitRepository;

    // Pendiente de implementar lógica específica si se requiere en el futuro.
}
