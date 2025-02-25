INSERT INTO account (name, is_active) VALUES
    ('Aktiv:Barkasse', true),
    ('Aktiv:Bankkonto', true),
    ('Aktiv:PayPal', true),
    ('Passiv:Flurkasse', false),
    ('Passiv:Bewohner:Anna R420', false),
    ('Passiv:Bewohner:Peter R469', false);

INSERT INTO person (name, room, pays_floor_fees, account_id) VALUES
    ('Anna', 'R420', true, 5),
    ('Peter', 'R469', true, 6);

INSERT INTO transaction (value_date, source_account_id, target_account_id, amount, description) VALUES
    ('2025-01-01', 5, 4, 800, 'Flurbeitrag 01/25'),
    ('2025-01-01', 6, 4, 800, 'Flurbeitrag 01/25'),
    ('2025-01-12', 4, 5, 1199, 'Einkauf LIDL'),
    ('2025-02-01', 5, 4, 800, 'Flurbeitrag 02/25'),
    ('2025-02-01', 6, 4, 800, 'Flurbeitrag 02/25'),
    ('2025-01-12', 4, 5, 3249, 'Einkauf METRO'),
    ('2025-03-01', 5, 4, 800, 'Flurbeitrag 03/25'),
    ('2025-03-01', 6, 4, 800, 'Flurbeitrag 03/25');

INSERT INTO product (name, price, print) VALUES
    ('Paulaner Spezi', 83, true),
    ('Club Mate', 76, true),
    ('Coca Cola 1,0L', 108, true);

UPDATE settings
    SET value_int = (SELECT id from account WHERE name = 'Passiv:Flurkasse')
    WHERE setting_key = 'flur_account_id';
