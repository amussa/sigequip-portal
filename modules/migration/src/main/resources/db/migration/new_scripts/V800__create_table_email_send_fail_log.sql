
CREATE TABLE email_send_fail_log (
  id SERIAL PRIMARY KEY,
  requisitionid integer NOT NULL,
  emailid integer,
  errormsg text,
  type character varying(20),
  createddate timestamp without time zone DEFAULT now(),
  modifieddate timestamp without time zone DEFAULT now(),
  ismanualsent boolean NOT NULL DEFAULT false
);

