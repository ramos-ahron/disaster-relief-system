-- Demo data for local use. Load into an empty database after the migrations:
--   psql -f database/sample/demo_data.sql
-- The people, places and calls are invented.

INSERT INTO INQUIRER (id, firstName, lastName, phoneNumber) VALUES
    (1, 'Dominik', 'Pflug', '123-456-9831'),
    (2, 'Yaa', 'Odei', '123-456-8913'),
    (3, 'Cecilia', 'Cobos', '123-456-7891'),
    (4, 'Hongjoo', 'Park', '123-456-8912');
INSERT INTO INQUIRER (id, firstName, phoneNumber) VALUES
    (5, 'Saartje', '123-456-7234'),
    (6, 'Urjoshi', '456-123-4281');

INSERT INTO INQUIRY_LOG (id, inquirer, callDate, details) VALUES
    (1, 1, '2024-02-28', 'Theresa Pflug'),
    (2, 2, '2024-02-28', 'Offer to assist as volunteer'),
    (3, 3, '2024-03-01', 'Valesk Souza'),
    (4, 1, '2024-03-01', 'Theresa Pflug'),
    (5, 1, '2024-03-02', 'Theresa Pflug'),
    (6, 4, '2024-03-02', 'Yoyo Jefferson and Roisin Fitzgerald'),
    (7, 5, '2024-03-02', 'Henk Wouters'),
    (8, 3, '2024-03-03', 'Melinda'),
    (9, 6, '2024-03-04', 'Julius');

SELECT setval('inquirer_id_seq', (SELECT max(id) FROM INQUIRER));
SELECT setval('inquiry_log_id_seq', (SELECT max(id) FROM INQUIRY_LOG));

INSERT INTO Location (name, address, shelter) VALUES
    ('Maple Grove Hospital', '123 Main St', true),
    ('UrgentCare Emergency Center', '456 Elm Street', true),
    ('Meadowview Retirement Village', '789 Oak Street', false);

INSERT INTO DisasterVictim (first_name, last_name, entry_date, gender, comments, date_of_birth, location_id) VALUES
    ('John', 'Doe', '2024-04-01', 'man', 'Injured leg', '1994-01-01', 1),
    ('Jane', 'Doe', '2024-04-03', 'woman', 'Needs medication', '1999-05-15', 2),
    ('Alice', 'Smith', '2024-04-05', NULL, 'Fractured arm', '1984-03-10', 2),
    ('Bob', 'Johnson', '2024-04-07', 'man', 'Severe cough', '1989-08-20', 3);

INSERT INTO Supply (type, quantity, reference_type, reference_id) VALUES
    ('Splints', 50, 'Location', 1),
    ('Bottled Water', 30, 'Location', 1),
    ('Canned Food', 20, 'Location', 1),
    ('Blankets', 20, 'Location', 2),
    ('Bottled Water', 30, 'Location', 2),
    ('Canned Food', 20, 'Location', 2),
    ('Splints', 3, 'Victim', 2),
    ('Disinfectants', 1, 'Victim', 3),
    ('Burn Cream', 2, 'Victim', 1);

INSERT INTO MedicalRecord (treatment_details, victim_id, location_id, date_of_treatment) VALUES
    ('Applied bandage to leg', 1, 1, '2024-04-02'),
    ('Prescribed medication', 2, 2, '2024-04-04'),
    ('Applied a cast to the fractured arm', 3, 2, '2024-04-05'),
    ('Administered cough syrup for symptom relief', 4, 1, '2024-04-07');

INSERT INTO Relationship (relationship_type, victim1_id, victim2_id) VALUES
    ('Couple', 1, 2),
    ('Friend', 1, 3),
    ('Friend', 3, 4);

INSERT INTO DietaryRestriction (restriction_name, victim_id) VALUES
    ('Asian vegetarian meal', 1),
    ('Diabetic meal', 2),
    ('Gluten intolerant meal', 3),
    ('Kosher meal', 4);

-- Family graph: Alice and Bob are John's biological parents, Alice is Jane's step parent.
INSERT INTO ParentChild (parent_id, child_id, kind) VALUES
    (3, 1, 'BIOLOGICAL'),
    (4, 1, 'BIOLOGICAL'),
    (3, 2, 'STEP');
INSERT INTO Partnership (person1_id, person2_id, start_date) VALUES (3, 4, '2015-06-01');
