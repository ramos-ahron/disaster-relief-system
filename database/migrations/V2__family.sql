-- Family graph over DisasterVictim. Additive: two new tables, one function, one trigger.
-- Old and new application versions can run against this schema at the same time,
-- because the old version never touches these objects.

CREATE TABLE IF NOT EXISTS ParentChild (
    parent_id INT NOT NULL,
    child_id  INT NOT NULL,
    kind      VARCHAR(10) NOT NULL,
    bio_slot  SMALLINT,   -- 1 or 2 for BIOLOGICAL rows, set by the trigger, NULL otherwise
    CONSTRAINT parentchild_pkey PRIMARY KEY (parent_id, child_id),
    CONSTRAINT parentchild_parent_fkey FOREIGN KEY (parent_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT parentchild_child_fkey FOREIGN KEY (child_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT parentchild_no_self CHECK (parent_id <> child_id),
    CONSTRAINT parentchild_kind_check CHECK (kind IN ('BIOLOGICAL', 'ADOPTIVE', 'STEP')),
    CONSTRAINT parentchild_slot_check CHECK (
        (kind = 'BIOLOGICAL') = (bio_slot IS NOT NULL)
        AND (bio_slot IS NULL OR bio_slot IN (1, 2)))
);

-- At most two biological parents per child. Hard guarantee, independent of the trigger lock.
CREATE UNIQUE INDEX IF NOT EXISTS parentchild_bio_slot_uq
    ON ParentChild (child_id, bio_slot) WHERE kind = 'BIOLOGICAL';

-- Ancestor lookups walk child_id -> parent_id. The primary key already serves parent_id -> child_id.
CREATE INDEX IF NOT EXISTS parentchild_child_idx ON ParentChild (child_id);

CREATE TABLE IF NOT EXISTS Partnership (
    partnership_id SERIAL,
    person1_id     INT NOT NULL,
    person2_id     INT NOT NULL,   -- always the larger id, so (a,b) and (b,a) cannot both exist
    start_date     DATE NOT NULL,
    end_date       DATE,
    CONSTRAINT partnership_pkey PRIMARY KEY (partnership_id),
    CONSTRAINT partnership_person1_fkey FOREIGN KEY (person1_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT partnership_person2_fkey FOREIGN KEY (person2_id)
        REFERENCES DisasterVictim (victim_id) ON DELETE CASCADE,
    CONSTRAINT partnership_order_check CHECK (person1_id < person2_id),
    CONSTRAINT partnership_dates_check CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT partnership_unique UNIQUE (person1_id, person2_id, start_date)
);
