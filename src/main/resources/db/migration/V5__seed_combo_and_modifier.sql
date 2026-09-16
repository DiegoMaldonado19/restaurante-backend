-- Demo de defensa: un modificador con receta (explodeRecipe la exige) y un combo
-- activo. No se toca V4. Idempotente por nombre unico.

-- Extra queso sobre Hamburguesa Clasica (dish_id = 1).
INSERT INTO dish_modifier (dish_id, name, extra_price, active)
SELECT 1, 'Extra queso', 5.00, TRUE
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM dish_modifier WHERE dish_id = 1 AND name = 'Extra queso'
);

INSERT INTO recipe (dish_modifier_id, version, effective_from, current_flag, created_by)
SELECT m.dish_modifier_id, 1, '2026-09-16 00:00:00', TRUE, 1
FROM dish_modifier m
WHERE m.dish_id = 1 AND m.name = 'Extra queso'
  AND NOT EXISTS (
      SELECT 1 FROM recipe r
       WHERE r.dish_modifier_id = m.dish_modifier_id AND r.current_flag = TRUE
  );

-- 0.020 kg de Queso mozzarella (supply_id = 7).
INSERT INTO recipe_item (recipe_id, supply_id, quantity)
SELECT r.recipe_id, 7, 0.020
FROM recipe r
JOIN dish_modifier m ON m.dish_modifier_id = r.dish_modifier_id
WHERE m.dish_id = 1 AND m.name = 'Extra queso'
  AND NOT EXISTS (
      SELECT 1 FROM recipe_item i
       WHERE i.recipe_id = r.recipe_id AND i.supply_id = 7
  );

INSERT INTO combo (name, description, combo_price, active)
SELECT 'Combo Clasico', 'Hamburguesa Clasica y gaseosa', 60.00, TRUE
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM combo WHERE name = 'Combo Clasico'
);

INSERT INTO combo_item (combo_id, dish_id, quantity)
SELECT c.combo_id, 1, 1
FROM combo c
WHERE c.name = 'Combo Clasico'
  AND NOT EXISTS (
      SELECT 1 FROM combo_item i WHERE i.combo_id = c.combo_id AND i.dish_id = 1
  );

INSERT INTO combo_item (combo_id, dish_id, quantity)
SELECT c.combo_id, 7, 1
FROM combo c
WHERE c.name = 'Combo Clasico'
  AND NOT EXISTS (
      SELECT 1 FROM combo_item i WHERE i.combo_id = c.combo_id AND i.dish_id = 7
  );
