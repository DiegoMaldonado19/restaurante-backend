-- Fila unica que respalda la generacion de invoice_number en billing.
-- Sin esto, la primera factura no tiene de donde leer/incrementar el contador.

INSERT INTO invoice_sequence (sequence_id, next_number)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE sequence_id = sequence_id;
