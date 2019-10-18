select
  address_data.*
, FN_GEOCODE(address) as geocoded_address
from address_data