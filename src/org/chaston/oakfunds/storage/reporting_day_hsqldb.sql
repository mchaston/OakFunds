CREATE FUNCTION reporting_day (range_start TIMESTAMP, attribute_value TIMESTAMP)
   RETURNS TIMESTAMP
   DETERMINISTIC
   IF attribute_value <= range_start THEN
     RETURN range_start;
   ELSE
     RETURN attribute_value;
   END IF;
