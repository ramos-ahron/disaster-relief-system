-- Base schema: inquiries, locations, victims and what is recorded about them.
-- Every statement is IF NOT EXISTS, so the file can be applied again.
-- V3 removes the two foreign keys on Supply.reference_id.

CREATE TABLE IF NOT EXISTS INQUIRER (
    id          SERIAL,
    firstName   VARCHAR(50) NOT NULL,
    lastName    VARCHAR(50),
    phoneNumber VARCHAR(20) NOT NULL,
    CONSTRAINT inquirer_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS INQUIRY_LOG (
    id       SERIAL,
    inquirer INT NOT NULL,
    callDate DATE NOT NULL,
    details  VARCHAR(500) NOT NULL,
    CONSTRAINT inquiry_log_pkey PRIMARY KEY (id),
    CONSTRAINT inquiry_log_inquirer_fkey FOREIGN KEY (inquirer)
        REFERENCES INQUIRER (id) ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS Location (
    location_id SERIAL,
    name        VARCHAR(255) NOT NULL,
    address     VARCHAR(255) NOT NULL,
    shelter     BOOLEAN NOT NULL,
    CONSTRAINT location_pkey PRIMARY KEY (location_id)
);

CREATE TABLE IF NOT EXISTS DisasterVictim (
    victim_id     SERIAL,
    first_name    VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255),
    entry_date    DATE,
    gender        VARCHAR(20),
    comments      TEXT,
    date_of_birth DATE,
    location_id   INT NOT NULL,
    CONSTRAINT disastervictim_pkey PRIMARY KEY (victim_id),
    CONSTRAINT disastervictim_location_id_fkey FOREIGN KEY (location_id)
        REFERENCES Location (location_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Supply (
    supply_id      SERIAL,
    type           VARCHAR(255) NOT NULL,
    quantity       INT NOT NULL,
    reference_type VARCHAR(20) NOT NULL,   -- 'Location' or 'Victim'
    reference_id   INT NOT NULL,
    CONSTRAINT supply_pkey PRIMARY KEY (supply_id),
    CONSTRAINT supply_reference_id_fkey FOREIGN KEY (reference_id)
        REFERENCES Location (location_id) ON DELETE CASCADE,
    CONSTRAINT supply_reference_id_fkey1 FOREIGN KEY (reference_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MedicalRecord (
    record_id         SERIAL,
    treatment_details TEXT,
    victim_id         INT,
    location_id       INT,
    date_of_treatment DATE,
    CONSTRAINT medicalrecord_pkey PRIMARY KEY (record_id),
    CONSTRAINT medicalrecord_victim_id_fkey FOREIGN KEY (victim_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT medicalrecord_location_id_fkey FOREIGN KEY (location_id)
        REFERENCES Location (location_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Relationship (
    relationship_id   SERIAL,
    relationship_type VARCHAR(255) NOT NULL,
    victim1_id        INT NOT NULL,
    victim2_id        INT NOT NULL,
    CONSTRAINT relationship_pkey PRIMARY KEY (relationship_id),
    CONSTRAINT relationship_victim1_id_fkey FOREIGN KEY (victim1_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT relationship_victim2_id_fkey FOREIGN KEY (victim2_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS DietaryRestriction (
    restriction_id   SERIAL,
    restriction_name VARCHAR(50) NOT NULL,
    victim_id        INT,
    CONSTRAINT dietaryrestriction_pkey PRIMARY KEY (restriction_id),
    CONSTRAINT dietaryrestriction_victim_id_fkey FOREIGN KEY (victim_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE
);
