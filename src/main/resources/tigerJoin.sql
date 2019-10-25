select
  edges.FULLNAME
, edges.LFROMADD
, edges.RFROMADD
, edges.LTOADD
, edges.RTOADD
, edges.STATEFP
, edges.COUNTYFP
, edges.ZIPL
, edges.ZIPR
, place.NAME as PLACE
, county.NAME as COUNTY
, state.STUSPS
, state.NAME
, faces.BLKGRPCE10
, faces.BLKGRPCE
, faces.BLOCKCE10
, faces.TRACTCE
, faces.SUFFIX1CE
, faces.UACE10
, faces.ZCTA5CE10
, max(edges.GEOMETRY) as GEOMETRY
, avg(cast(faces.INTPTLAT as float)) as INTPTLAT
, avg(cast(faces.INTPTLON as float)) as INTPTLON
from edges
inner join county
on (edges.COUNTYFP = county.COUNTYFP) and (edges.STATEFP = county.STATEFP)
inner join faces
on edges.TFIDR = faces.TFID
inner join state
on (faces.STATEFP = state.STATEFP)
inner join place
on (faces.STATEFP = place.STATEFP and faces.PLACEFP = place.PLACEFP)

group by 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20