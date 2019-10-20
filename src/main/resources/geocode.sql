select
  address_data.*
, FN_GEOCODE(address_data.address) as geocoded_address
from address_data