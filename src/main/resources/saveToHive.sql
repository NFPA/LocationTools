create table %s as
select
  input_data.*
, geocoded_output.geocoder_address_output
from input_data
left join geocoded_output
on input_data.join_key = geocoded_output.geocoder_join_key