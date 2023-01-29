/* relay details */
CREATE SEQUENCE relay_details_id_seq;
ALTER SEQUENCE relay_details_id_seq OWNED BY relay_details.id;
ALTER TABLE relay_details
    ALTER COLUMN id SET DEFAULT nextval('relay_details_id_seq'::regclass);

/* relay location */
CREATE SEQUENCE relay_location_id_seq;
ALTER SEQUENCE relay_location_id_seq OWNED BY relay_location.id;
ALTER TABLE relay_location
    ALTER COLUMN id SET DEFAULT nextval('relay_location_id_seq'::regclass);

/* user trace */
CREATE SEQUENCE user_trace_id_seq;
ALTER SEQUENCE user_trace_id_seq OWNED BY user_trace.id;
ALTER TABLE user_trace
    ALTER COLUMN id SET DEFAULT nextval('user_trace_id_seq'::regclass);
