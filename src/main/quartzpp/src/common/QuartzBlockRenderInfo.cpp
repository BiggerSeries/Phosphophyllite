#include "QuartzBlockRenderInfo.hpp"

using namespace RogueLib;

namespace Phosphophyllite::Quartz {

    // autoserializable is great and all, but when im throwing a bunch of these around, id rather not be creating and destroying a ton of maps

    ROBN::ROBN QuartzBlockRenderInfo::toROBN() {
        ROGUELIB_STACKTRACE

        std::map<std::string, ROBN::ROBN> objects;

        objects["x"] = ROBN::toROBN(x);
        objects["y"] = ROBN::toROBN(y);
        objects["z"] = ROBN::toROBN(z);

        objects["textureIDWest"] = ROBN::toROBN(textureIDWest);
        objects["textureIDEast"] = ROBN::toROBN(textureIDEast);
        objects["textureIDBottom"] = ROBN::toROBN(textureIDBottom);
        objects["textureIDTop"] = ROBN::toROBN(textureIDTop);
        objects["textureIDSouth"] = ROBN::toROBN(textureIDSouth);
        objects["textureIDNorth"] = ROBN::toROBN(textureIDNorth);

        objects["textureRotationWest"] = ROBN::toROBN(textureRotationWest);
        objects["textureRotationEast"] = ROBN::toROBN(textureRotationEast);
        objects["textureRotationBottom"] = ROBN::toROBN(textureRotationBottom);
        objects["textureRotationTop"] = ROBN::toROBN(textureRotationTop);
        objects["textureRotationSouth"] = ROBN::toROBN(textureRotationSouth);
        objects["textureRotationNorth"] = ROBN::toROBN(textureRotationNorth);

        objects["lightmapBlocklightWestLYLZ"] = ROBN::toROBN(lightmapBlocklightWestLYLZ);
        objects["lightmapSkylightWestLYLZ"] = ROBN::toROBN(lightmapSkylightWestLYLZ);
        objects["lightmapBlocklightWestHYLZ"] = ROBN::toROBN(lightmapBlocklightWestHYLZ);
        objects["lightmapSkylightWestHYLZ"] = ROBN::toROBN(lightmapSkylightWestHYLZ);
        objects["lightmapBlocklightWestLYHZ"] = ROBN::toROBN(lightmapBlocklightWestLYHZ);
        objects["lightmapSkylightWestLYHZ"] = ROBN::toROBN(lightmapSkylightWestLYHZ);
        objects["lightmapBlocklightWestHYHZ"] = ROBN::toROBN(lightmapBlocklightWestHYHZ);
        objects["lightmapSkylightWestHYHZ"] = ROBN::toROBN(lightmapSkylightWestHYHZ);

        objects["lightmapBlocklightEastLYLZ"] = ROBN::toROBN(lightmapBlocklightEastLYLZ);
        objects["lightmapSkylightEastLYLZ"] = ROBN::toROBN(lightmapSkylightEastLYLZ);
        objects["lightmapBlocklightEastHYLZ"] = ROBN::toROBN(lightmapBlocklightEastHYLZ);
        objects["lightmapSkylightEastHYLZ"] = ROBN::toROBN(lightmapSkylightEastHYLZ);
        objects["lightmapBlocklightEastLYHZ"] = ROBN::toROBN(lightmapBlocklightEastLYHZ);
        objects["lightmapSkylightEastLYHZ"] = ROBN::toROBN(lightmapSkylightEastLYHZ);
        objects["lightmapBlocklightEastHYHZ"] = ROBN::toROBN(lightmapBlocklightEastHYHZ);
        objects["lightmapSkylightEastHYHZ"] = ROBN::toROBN(lightmapSkylightEastHYHZ);

        objects["lightmapBlocklightTopLXLZ"] = ROBN::toROBN(lightmapBlocklightTopLXLZ);
        objects["lightmapSkylightTopLXLZ"] = ROBN::toROBN(lightmapSkylightTopLXLZ);
        objects["lightmapBlocklightTopHXLZ"] = ROBN::toROBN(lightmapBlocklightTopHXLZ);
        objects["lightmapSkylightTopHXLZ"] = ROBN::toROBN(lightmapSkylightTopHXLZ);
        objects["lightmapBlocklightTopLXHZ"] = ROBN::toROBN(lightmapBlocklightTopLXHZ);
        objects["lightmapSkylightTopLXHZ"] = ROBN::toROBN(lightmapSkylightTopLXHZ);
        objects["lightmapBlocklightTopHXHZ"] = ROBN::toROBN(lightmapBlocklightTopHXHZ);
        objects["lightmapSkylightTopHXHZ"] = ROBN::toROBN(lightmapSkylightTopHXHZ);

        objects["lightmapBlocklightBottomLXLZ"] = ROBN::toROBN(lightmapBlocklightBottomLXLZ);
        objects["lightmapSkylightBottomLXLZ"] = ROBN::toROBN(lightmapSkylightBottomLXLZ);
        objects["lightmapBlocklightBottomHXLZ"] = ROBN::toROBN(lightmapBlocklightBottomHXLZ);
        objects["lightmapSkylightBottomHXLZ"] = ROBN::toROBN(lightmapSkylightBottomHXLZ);
        objects["lightmapBlocklightBottomLXHZ"] = ROBN::toROBN(lightmapBlocklightBottomLXHZ);
        objects["lightmapSkylightBottomLXHZ"] = ROBN::toROBN(lightmapSkylightBottomLXHZ);
        objects["lightmapBlocklightBottomHXHZ"] = ROBN::toROBN(lightmapBlocklightBottomHXHZ);
        objects["lightmapSkylightBottomHXHZ"] = ROBN::toROBN(lightmapSkylightBottomHXHZ);

        objects["lightmapBlocklightSouthLXLY"] = ROBN::toROBN(lightmapBlocklightSouthLXLY);
        objects["lightmapSkylightSouthLXLY"] = ROBN::toROBN(lightmapSkylightSouthLXLY);
        objects["lightmapBlocklightSouthHXLY"] = ROBN::toROBN(lightmapBlocklightSouthHXLY);
        objects["lightmapSkylightSouthHXLY"] = ROBN::toROBN(lightmapSkylightSouthHXLY);
        objects["lightmapBlocklightSouthLXHY"] = ROBN::toROBN(lightmapBlocklightSouthLXHY);
        objects["lightmapSkylightSouthLXHY"] = ROBN::toROBN(lightmapSkylightSouthLXHY);
        objects["lightmapBlocklightSouthHXHY"] = ROBN::toROBN(lightmapBlocklightSouthHXHY);
        objects["lightmapSkylightSouthHXHY"] = ROBN::toROBN(lightmapSkylightSouthHXHY);

        objects["lightmapBlocklightNorthLXLY"] = ROBN::toROBN(lightmapBlocklightNorthLXLY);
        objects["lightmapSkylightNorthLXLY"] = ROBN::toROBN(lightmapSkylightNorthLXLY);
        objects["lightmapBlocklightNorthHXLY"] = ROBN::toROBN(lightmapBlocklightNorthHXLY);
        objects["lightmapSkylightNorthHXLY"] = ROBN::toROBN(lightmapSkylightNorthHXLY);
        objects["lightmapBlocklightNorthLXHY"] = ROBN::toROBN(lightmapBlocklightNorthLXHY);
        objects["lightmapSkylightNorthLXHY"] = ROBN::toROBN(lightmapSkylightNorthLXHY);
        objects["lightmapBlocklightNorthHXHY"] = ROBN::toROBN(lightmapBlocklightNorthHXHY);
        objects["lightmapSkylightNorthHXHY"] = ROBN::toROBN(lightmapSkylightNorthHXHY);


        return ROBN::toROBN(objects);
    }

    void QuartzBlockRenderInfo::fromROBN(ROBN::Byte*& ptr, const ROBN::Byte* endPtr, ROBN::Type type) {
        ROGUELIB_STACKTRACE

        auto objects = RogueLib::ROBN::fromROBN<std::map<std::string, ROBN::ROBN>>(ptr, endPtr, type);

        x = ROBN::fromROBN<std::int32_t>(objects["x"]);
        y = ROBN::fromROBN<std::int32_t>(objects["y"]);
        z = ROBN::fromROBN<std::int32_t>(objects["z"]);

        textureIDWest = ROBN::fromROBN<std::uint32_t>(objects["textureIDWest"]);
        textureIDEast = ROBN::fromROBN<std::uint32_t>(objects["textureIDEast"]);
        textureIDBottom = ROBN::fromROBN<std::uint32_t>(objects["textureIDBottom"]);
        textureIDTop = ROBN::fromROBN<std::uint32_t>(objects["textureIDTop"]);
        textureIDSouth = ROBN::fromROBN<std::uint32_t>(objects["textureIDSouth"]);
        textureIDNorth = ROBN::fromROBN<std::uint32_t>(objects["textureIDNorth"]);

        textureRotationWest = ROBN::fromROBN<std::uint8_t>(objects["textureRotationWest"]);
        textureRotationEast = ROBN::fromROBN<std::uint8_t>(objects["textureRotationEast"]);
        textureRotationBottom = ROBN::fromROBN<std::uint8_t>(objects["textureRotationBottom"]);
        textureRotationTop = ROBN::fromROBN<std::uint8_t>(objects["textureRotationTop"]);
        textureRotationSouth = ROBN::fromROBN<std::uint8_t>(objects["textureRotationSouth"]);
        textureRotationNorth = ROBN::fromROBN<std::uint8_t>(objects["textureRotationNorth"]);

        lightmapBlocklightWestLYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightWestLYLZ"]);
        lightmapSkylightWestLYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightWestLYLZ"]);
        lightmapBlocklightWestHYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightWestHYLZ"]);
        lightmapSkylightWestHYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightWestHYLZ"]);
        lightmapBlocklightWestLYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightWestLYHZ"]);
        lightmapSkylightWestLYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightWestLYHZ"]);
        lightmapBlocklightWestHYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightWestHYHZ"]);
        lightmapSkylightWestHYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightWestHYHZ"]);

        lightmapBlocklightEastLYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightEastLYLZ"]);
        lightmapSkylightEastLYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightEastLYLZ"]);
        lightmapBlocklightEastHYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightEastHYLZ"]);
        lightmapSkylightEastHYLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightEastHYLZ"]);
        lightmapBlocklightEastLYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightEastLYHZ"]);
        lightmapSkylightEastLYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightEastLYHZ"]);
        lightmapBlocklightEastHYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightEastHYHZ"]);
        lightmapSkylightEastHYHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightEastHYHZ"]);

        lightmapBlocklightTopLXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightTopLXLZ"]);
        lightmapSkylightTopLXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightTopLXLZ"]);
        lightmapBlocklightTopHXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightTopHXLZ"]);
        lightmapSkylightTopHXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightTopHXLZ"]);
        lightmapBlocklightTopLXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightTopLXHZ"]);
        lightmapSkylightTopLXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightTopLXHZ"]);
        lightmapBlocklightTopHXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightTopHXHZ"]);
        lightmapSkylightTopHXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightTopHXHZ"]);

        lightmapBlocklightBottomLXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightBottomLXLZ"]);
        lightmapSkylightBottomLXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightBottomLXLZ"]);
        lightmapBlocklightBottomHXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightBottomHXLZ"]);
        lightmapSkylightBottomHXLZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightBottomHXLZ"]);
        lightmapBlocklightBottomLXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightBottomLXHZ"]);
        lightmapSkylightBottomLXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightBottomLXHZ"]);
        lightmapBlocklightBottomHXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightBottomHXHZ"]);
        lightmapSkylightBottomHXHZ = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightBottomHXHZ"]);

        lightmapBlocklightSouthLXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightSouthLXLY"]);
        lightmapSkylightSouthLXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightSouthLXLY"]);
        lightmapBlocklightSouthHXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightSouthHXLY"]);
        lightmapSkylightSouthHXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightSouthHXLY"]);
        lightmapBlocklightSouthLXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightSouthLXHY"]);
        lightmapSkylightSouthLXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightSouthLXHY"]);
        lightmapBlocklightSouthHXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightSouthHXHY"]);
        lightmapSkylightSouthHXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightSouthHXHY"]);

        lightmapBlocklightNorthLXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightNorthLXLY"]);
        lightmapSkylightNorthLXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightNorthLXLY"]);
        lightmapBlocklightNorthHXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightNorthHXLY"]);
        lightmapSkylightNorthHXLY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightNorthHXLY"]);
        lightmapBlocklightNorthLXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightNorthLXHY"]);
        lightmapSkylightNorthLXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightNorthLXHY"]);
        lightmapBlocklightNorthHXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapBlocklightNorthHXHY"]);
        lightmapSkylightNorthHXHY = ROBN::fromROBN<std::uint8_t>(objects["lightmapSkylightNorthHXHY"]);

    }

    QuartzBlockRenderInfo::Packed QuartzBlockRenderInfo::pack() const {
        QuartzBlockRenderInfo::Packed packedData = {};

        packedData.blockPosition = 0;
        {
            std::uint8_t hiddenFaces = 0;

            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);

            packedData.blockPosition |= (std::uint32_t(hiddenFaces) & 0xFFu) << 24u;
        }

        packedData.blockPosition |= (std::uint32_t(x) & 0xFFu) << 16u;
        packedData.blockPosition |= (std::uint32_t(y) & 0xFFu) << 8u;
        packedData.blockPosition |= (std::uint32_t(z) & 0xFFu) << 0u;
        
        packedData.textureIDRotation[0] = textureIDWest;
        packedData.textureIDRotation[0] <<= 2u;
        packedData.textureIDRotation[0] |= (textureRotationWest & 0x3u);
        
        packedData.textureIDRotation[1] = textureIDEast;
        packedData.textureIDRotation[1] <<= 2u;
        packedData.textureIDRotation[1] |= (textureRotationEast & 0x3u);
        
        packedData.textureIDRotation[2] = textureIDBottom;
        packedData.textureIDRotation[2] <<= 2u;
        packedData.textureIDRotation[2] |= (textureRotationBottom & 0x3u);
        
        packedData.textureIDRotation[3] = textureIDTop;
        packedData.textureIDRotation[3] <<= 2u;
        packedData.textureIDRotation[3] |= (textureRotationTop & 0x3u);
        
        packedData.textureIDRotation[4] = textureIDNorth;
        packedData.textureIDRotation[4] <<= 2u;
        packedData.textureIDRotation[4] |= (textureRotationNorth & 0x3u);
        
        packedData.textureIDRotation[5] = textureIDSouth;
        packedData.textureIDRotation[5] <<= 2u;
        packedData.textureIDRotation[5] |= (textureRotationSouth & 0x3u);
        
        // the ordering here doesnt match exactly,
        // thats on purpose
        // this aligns them with the face/vertex IDs in the VBO
        // see WorldManager#64 for said VBO data

        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightEastLYLZ << 8u) | (lightmapSkylightEastLYLZ);
        packedData.lightmap[0][0] <<= 16u;
        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightWestLYLZ << 8u) | (lightmapSkylightWestLYLZ);
        
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightEastHYLZ << 8u) | (lightmapSkylightEastHYLZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightWestHYLZ << 8u) | (lightmapSkylightWestHYLZ);
        
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightEastLYHZ << 8u) | (lightmapSkylightEastLYHZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightWestLYHZ << 8u) | (lightmapSkylightWestLYHZ);
        
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightEastHYHZ << 8u) | (lightmapSkylightEastHYHZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightWestHYHZ << 8u) | (lightmapSkylightWestHYHZ);


        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightTopLXLZ << 8u) | (lightmapSkylightTopLXLZ);
        packedData.lightmap[0][0] <<= 16u;
        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightBottomLXLZ << 8u) | (lightmapSkylightBottomLXLZ);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightTopHXLZ << 8u) | (lightmapSkylightTopHXLZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightBottomHXLZ << 8u) | (lightmapSkylightBottomHXLZ);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightTopLXHZ << 8u) | (lightmapSkylightTopLXHZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightBottomLXHZ << 8u) | (lightmapSkylightBottomLXHZ);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightTopHXHZ << 8u) | (lightmapSkylightTopHXHZ);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightBottomHXHZ << 8u) | (lightmapSkylightBottomHXHZ);


        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightNorthLXLY << 8u) | (lightmapSkylightNorthLXLY);
        packedData.lightmap[0][0] <<= 16u;
        packedData.lightmap[0][0] = std::uint8_t(lightmapBlocklightSouthLXLY << 8u) | (lightmapSkylightSouthLXLY);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightNorthHXLY << 8u) | (lightmapSkylightNorthHXLY);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightSouthHXLY << 8u) | (lightmapSkylightSouthHXLY);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightNorthLXHY << 8u) | (lightmapSkylightNorthLXHY);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightSouthLXHY << 8u) | (lightmapSkylightSouthLXHY);

        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightNorthHXHY << 8u) | (lightmapSkylightNorthHXHY);
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][1] = std::uint8_t(lightmapBlocklightSouthHXHY << 8u) | (lightmapSkylightSouthHXHY);
        
        return packedData;
    }
}