CREATE FUNCTION %SCHEMA%.reporting_month (range_start TIMESTAMP, attribute_value TIMESTAMP)
   RETURNS TIMESTAMP
   DETERMINISTIC NO SQL
   IF attribute_value <= range_start THEN
     RETURN range_start;
   ELSE
     RETURN TIMESTAMP(DATE_FORMAT(attribute_value, '%Y-%m-01'));
   END IF;
