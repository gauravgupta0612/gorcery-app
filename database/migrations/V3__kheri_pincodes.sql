-- Serviceable pincodes in Lakhimpur Kheri district (Uttar Pradesh)
INSERT INTO serviceable_pincodes(pincode, city, area, delivery_fee, eta_minutes)
VALUES ('262802', 'Gola Gokarannath', 'Gola Gokarannath, Kheri', NULL, 30),
       ('262701', 'Lakhimpur', 'Lakhimpur Kheri', NULL, 30)
ON CONFLICT (pincode) DO UPDATE SET active = TRUE;
