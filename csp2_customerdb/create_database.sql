CREATE TABLE customer_id_mapping
(
    csp2_id INTEGER PRIMARY KEY,
    our_id  VARCHAR UNIQUE
);

INSERT INTO customer_id_mapping VALUES
    (373451, 'Bryan'),
    (670990, 'Jeri'),
    (681601, 'Gabrielle'),
    (813366, 'Amethyst'),
    (697992, 'Peony'),
    (980555, 'Will');

CREATE TABLE chargingstation_id_mapping
(
    csp2_id INTEGER PRIMARY KEY,
    our_id VARCHAR UNIQUE
);

INSERT INTO chargingstation_id_mapping VALUES
    (1, 'CST2:ATL'),
    (2, 'CST2:PEK'),
    (3, 'CST2:PKX'),
    (4, 'CST2:CHR'),
    (5, 'CST2:HND'),
    (6, 'CST2:CDG'),
    (7, 'CST2:DFW'),
    (8, 'CST2:CGK'),
    (9, 'CST2:DXB'),
    (10, 'CST2:FRA');
