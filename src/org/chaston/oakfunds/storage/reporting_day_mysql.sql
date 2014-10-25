CREATE FUNCTION %SCHEMA%.reporting_day (range_start TIMESTAMP, attribute_value TIMESTAMP)
   RETURNS TIMESTAMP
   DETERMINISTIC NO SQL
   IF attribute_value <= range_start THEN
     RETURN range_start;
   ELSE
     RETURN TIMESTAMP(DATE_FORMAT(attribute_value, '%Y-%m-%d'));
   END IF;
