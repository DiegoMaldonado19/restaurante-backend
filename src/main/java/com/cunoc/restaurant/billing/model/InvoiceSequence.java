package com.cunoc.restaurant.billing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Fila unica (sequence_id = 1) que respalda la generacion atomica de invoice_number.
 * Se lee con bloqueo (findByIdForUpdate) para que dos facturas simultaneas nunca
 * obtengan el mismo numero.
 */
@Entity
@Table(name = "invoice_sequence")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceSequence
{
    @Id
    private Long sequenceId;

    private Long nextNumber;
}
