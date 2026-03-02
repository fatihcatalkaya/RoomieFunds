ALTER TABLE product ADD COLUMN sort_order INTEGER;
UPDATE product SET sort_order = sub.rn FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY name, id) AS rn FROM product) sub WHERE product.id = sub.id;
ALTER TABLE product ALTER COLUMN sort_order SET NOT NULL;
