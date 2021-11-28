Vertex format used

float32 x

float32 y

float32 z

uint8 r

uint8 g

uint8 b

uint8 a

float32 texU

float32 texV

if not a quad rendertype

bits listed are based on bitshifts needed to extract the value

PACKED uint32:

     bits 0-15 normalX

     bits 16-23 lightmapV

     bits 24-31 lightmapU

PACKED uint32:

     bits 0-15 normalZ

     bits 16-32 normalY

if quad rendertype,

vertex indexing is NOT in submission order

order used internally

     vert2 vert3

     vert0 vert1

SUBMISSION TO THIS BUFFER IS STILL CCW order

bits listed are based on bitshifts needed to extract the value

PACKED uint32:

     bits 00-05: lightmapU0

     bits 06-11: lightmapV0

     bits 12-17: lightmapU1

     bits 18-23: lightmapV1

     bits 24-27: normalX

     bits 28-31: normalY

PACKED uint32:

     bits 00-05: lightmapU2

     bits 06-11: lightmapV2

     bits 12-17: lightmapU3

     bits 18-23: lightmapV3

     bits 24-27: normalZ

     bit 28: vertex lightmap X multiplier; 0 for vertices 0 and 2; 1 for vertices 1 and 3

     bit 29: vertex lightmap Y multiplier; 0 for vertices 0 and 1; 1 for vertices 2 and 3

quads will be decomposed into triangles at this step

total size per vertex, 32 bytes
