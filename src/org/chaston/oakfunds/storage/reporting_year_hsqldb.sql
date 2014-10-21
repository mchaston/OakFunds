CREATE FUNCTION %SCHEMA%.reporting_year (range_start TIMESTAMP, attribute_value TIMESTAMP)
   RETURNS TIMESTAMP
   DETERMINISTIC
   IF attribute_value <= range_start THEN
     RETURN range_start;
   ELSE
     RETURN TRUNC ( attribute_value, 'YYYY' );
   END IF;
