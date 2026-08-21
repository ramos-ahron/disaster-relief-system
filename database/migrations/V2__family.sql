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

-- Cycle check and biological slot assignment.
-- A global advisory lock serializes writers, so a transaction always checks against
-- every row committed before it. Under READ COMMITTED each statement in this function
-- takes a fresh snapshot after the lock is granted. The lock is released at commit or rollback.
-- Known limit: all ParentChild writes are serialized. If write volume ever matters,
-- lock per connected component or switch to SERIALIZABLE with retry.
-- Error codes: FC001 cycle, FC002 no free biological slot.
CREATE OR REPLACE FUNCTION parentchild_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.parent_id = NEW.child_id THEN
        RETURN NEW;   -- rejected by parentchild_no_self
    END IF;

    PERFORM pg_advisory_xact_lock(380001);

    -- The new edge closes a cycle if the child is already an ancestor of the parent.
    -- UNION drops repeated rows; the depth cap stops the walk if bad data already holds a cycle.
    IF EXISTS (
        WITH RECURSIVE up(id, depth) AS (
            SELECT NEW.parent_id, 0
            UNION
            SELECT pc.parent_id, up.depth + 1
            FROM ParentChild pc JOIN up ON pc.child_id = up.id
            WHERE up.depth < 1000
              AND NOT (TG_OP = 'UPDATE' AND pc.parent_id = OLD.parent_id AND pc.child_id = OLD.child_id)
        )
        SELECT 1 FROM up WHERE id = NEW.child_id
    ) THEN
        RAISE EXCEPTION 'parent-child cycle' USING ERRCODE = 'FC001';
    END IF;

    IF NEW.kind <> 'BIOLOGICAL' THEN
        NEW.bio_slot := NULL;
    ELSIF TG_OP = 'UPDATE' AND OLD.kind = 'BIOLOGICAL' AND OLD.child_id = NEW.child_id THEN
        NEW.bio_slot := OLD.bio_slot;
    ELSE
        SELECT s INTO NEW.bio_slot FROM generate_series(1, 2) AS s
        WHERE NOT EXISTS (SELECT 1 FROM ParentChild
                          WHERE child_id = NEW.child_id AND kind = 'BIOLOGICAL' AND bio_slot = s)
        ORDER BY s LIMIT 1;
        IF NEW.bio_slot IS NULL THEN
            RAISE EXCEPTION 'child already has two biological parents' USING ERRCODE = 'FC002';
        END IF;
    END IF;
    RETURN NEW;
END
$$;

DROP TRIGGER IF EXISTS parentchild_guard ON ParentChild;
CREATE TRIGGER parentchild_guard
    BEFORE INSERT OR UPDATE OF parent_id, child_id, kind ON ParentChild
    FOR EACH ROW EXECUTE FUNCTION parentchild_guard();
