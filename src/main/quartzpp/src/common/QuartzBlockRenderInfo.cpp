#include "QuartzBlockRenderInfo.hpp"

using namespace RogueLib;

namespace Phosphophyllite::Quartz {

    // autoserializable is great and all, but when im throwing a bunch of these around, id rather not be creating and destroying a ton of maps

    ROBN::ROBN QuartzBlockRenderInfo::toROBN() {
        ROGUELIB_STACKTRACE

        std::map<std::string, std::uint32_t> objects;

        objects["x"] = (x);
        objects["y"] = (y);
        objects["z"] = (z);

        objects["textureIDWest"] = (textureIDWest);
        objects["textureIDEast"] = (textureIDEast);
        objects["textureIDBottom"] = (textureIDBottom);
        objects["textureIDTop"] = (textureIDTop);
        objects["textureIDSouth"] = (textureIDSouth);
        objects["textureIDNorth"] = (textureIDNorth);

        objects["textureRotationWest"] = (textureRotationWest);
        objects["textureRotationEast"] = (textureRotationEast);
        objects["textureRotationBottom"] = (textureRotationBottom);
        objects["textureRotationTop"] = (textureRotationTop);
        objects["textureRotationSouth"] = (textureRotationSouth);
        objects["textureRotationNorth"] = (textureRotationNorth);

        objects["lightmapBlocklightWestLYLZ"] = (lightmapBlocklightWestLYLZ);
        objects["lightmapSkylightWestLYLZ"] = (lightmapSkylightWestLYLZ);
        objects["AOWestLYLZ"] = (AOWestLYLZ);
        objects["lightmapBlocklightWestHYLZ"] = (lightmapBlocklightWestHYLZ);
        objects["lightmapSkylightWestHYLZ"] = (lightmapSkylightWestHYLZ);
        objects["AOWestHYLZ"] = (AOWestHYLZ);
        objects["lightmapBlocklightWestLYHZ"] = (lightmapBlocklightWestLYHZ);
        objects["lightmapSkylightWestLYHZ"] = (lightmapSkylightWestLYHZ);
        objects["AOWestLYHZ"] = (AOWestLYHZ);
        objects["lightmapBlocklightWestHYHZ"] = (lightmapBlocklightWestHYHZ);
        objects["lightmapSkylightWestHYHZ"] = (lightmapSkylightWestHYHZ);
        objects["AOWestHYHZ"] = (AOWestHYHZ);

        objects["lightmapBlocklightEastLYLZ"] = (lightmapBlocklightEastLYLZ);
        objects["lightmapSkylightEastLYLZ"] = (lightmapSkylightEastLYLZ);
        objects["AOEastLYLZ"] = (AOEastLYLZ);
        objects["lightmapBlocklightEastHYLZ"] = (lightmapBlocklightEastHYLZ);
        objects["lightmapSkylightEastHYLZ"] = (lightmapSkylightEastHYLZ);
        objects["AOEastHYLZ"] = (AOEastHYLZ);
        objects["lightmapBlocklightEastLYHZ"] = (lightmapBlocklightEastLYHZ);
        objects["lightmapSkylightEastLYHZ"] = (lightmapSkylightEastLYHZ);
        objects["AOEastLYHZ"] = (AOEastLYHZ);
        objects["lightmapBlocklightEastHYHZ"] = (lightmapBlocklightEastHYHZ);
        objects["lightmapSkylightEastHYHZ"] = (lightmapSkylightEastHYHZ);
        objects["AOEastHYHZ"] = (AOEastHYHZ);

        objects["lightmapBlocklightTopLXLZ"] = (lightmapBlocklightTopLXLZ);
        objects["lightmapSkylightTopLXLZ"] = (lightmapSkylightTopLXLZ);
        objects["AOTopLXLZ"] = (AOTopLXLZ);
        objects["lightmapBlocklightTopHXLZ"] = (lightmapBlocklightTopHXLZ);
        objects["lightmapSkylightTopHXLZ"] = (lightmapSkylightTopHXLZ);
        objects["AOTopHXLZ"] = (AOTopHXLZ);
        objects["lightmapBlocklightTopLXHZ"] = (lightmapBlocklightTopLXHZ);
        objects["lightmapSkylightTopLXHZ"] = (lightmapSkylightTopLXHZ);
        objects["AOTopLXHZ"] = (AOTopLXHZ);
        objects["lightmapBlocklightTopHXHZ"] = (lightmapBlocklightTopHXHZ);
        objects["lightmapSkylightTopHXHZ"] = (lightmapSkylightTopHXHZ);
        objects["AOTopHXHZ"] = (AOTopHXHZ);

        objects["lightmapBlocklightBottomLXLZ"] = (lightmapBlocklightBottomLXLZ);
        objects["lightmapSkylightBottomLXLZ"] = (lightmapSkylightBottomLXLZ);
        objects["AOBottomLXLZ"] = (AOBottomLXLZ);
        objects["lightmapBlocklightBottomHXLZ"] = (lightmapBlocklightBottomHXLZ);
        objects["lightmapSkylightBottomHXLZ"] = (lightmapSkylightBottomHXLZ);
        objects["AOBottomHXLZ"] = (AOBottomHXLZ);
        objects["lightmapBlocklightBottomLXHZ"] = (lightmapBlocklightBottomLXHZ);
        objects["lightmapSkylightBottomLXHZ"] = (lightmapSkylightBottomLXHZ);
        objects["AOBottomLXHZ"] = (AOBottomLXHZ);
        objects["lightmapBlocklightBottomHXHZ"] = (lightmapBlocklightBottomHXHZ);
        objects["lightmapSkylightBottomHXHZ"] = (lightmapSkylightBottomHXHZ);
        objects["AOBottomHXHZ"] = (AOBottomHXHZ);

        objects["lightmapBlocklightSouthLXLY"] = (lightmapBlocklightSouthLXLY);
        objects["lightmapSkylightSouthLXLY"] = (lightmapSkylightSouthLXLY);
        objects["AOSouthLXLY"] = (AOSouthLXLY);
        objects["lightmapBlocklightSouthHXLY"] = (lightmapBlocklightSouthHXLY);
        objects["lightmapSkylightSouthHXLY"] = (lightmapSkylightSouthHXLY);
        objects["AOSouthHXLY"] = (AOSouthHXLY);
        objects["lightmapBlocklightSouthLXHY"] = (lightmapBlocklightSouthLXHY);
        objects["lightmapSkylightSouthLXHY"] = (lightmapSkylightSouthLXHY);
        objects["AOSouthLXHY"] = (AOSouthLXHY);
        objects["lightmapBlocklightSouthHXHY"] = (lightmapBlocklightSouthHXHY);
        objects["lightmapSkylightSouthHXHY"] = (lightmapSkylightSouthHXHY);
        objects["AOSouthHXHY"] = (AOSouthHXHY);

        objects["lightmapBlocklightNorthLXLY"] = (lightmapBlocklightNorthLXLY);
        objects["lightmapSkylightNorthLXLY"] = (lightmapSkylightNorthLXLY);
        objects["AONorthLXLY"] = (AONorthLXLY);
        objects["lightmapBlocklightNorthHXLY"] = (lightmapBlocklightNorthHXLY);
        objects["lightmapSkylightNorthHXLY"] = (lightmapSkylightNorthHXLY);
        objects["AONorthHXLY"] = (AONorthHXLY);
        objects["lightmapBlocklightNorthLXHY"] = (lightmapBlocklightNorthLXHY);
        objects["lightmapSkylightNorthLXHY"] = (lightmapSkylightNorthLXHY);
        objects["AONorthLXHY"] = (AONorthLXHY);
        objects["lightmapBlocklightNorthHXHY"] = (lightmapBlocklightNorthHXHY);
        objects["lightmapSkylightNorthHXHY"] = (lightmapSkylightNorthHXHY);
        objects["AONorthHXHY"] = (AONorthHXHY);


        return ROBN::toROBN(objects);
    }

    void QuartzBlockRenderInfo::fromROBN(ROBN::Byte*& ptr, const ROBN::Byte* endPtr, ROBN::Type type) {
        ROGUELIB_STACKTRACE

        auto objects = RogueLib::ROBN::fromROBN<std::map<std::string, std::uint32_t>>(ptr, endPtr, type);

        x = (objects["x"]);
        y = (objects["y"]);
        z = (objects["z"]);

        textureIDWest = (objects["textureIDWest"]);
        textureIDEast = (objects["textureIDEast"]);
        textureIDBottom = (objects["textureIDBottom"]);
        textureIDTop = (objects["textureIDTop"]);
        textureIDSouth = (objects["textureIDSouth"]);
        textureIDNorth = (objects["textureIDNorth"]);

        textureRotationWest = (objects["textureRotationWest"]);
        textureRotationEast = (objects["textureRotationEast"]);
        textureRotationBottom = (objects["textureRotationBottom"]);
        textureRotationTop = (objects["textureRotationTop"]);
        textureRotationSouth = (objects["textureRotationSouth"]);
        textureRotationNorth = (objects["textureRotationNorth"]);

        lightmapBlocklightWestLYLZ = (objects["lightmapBlocklightWestLYLZ"]);
        lightmapSkylightWestLYLZ = (objects["lightmapSkylightWestLYLZ"]);
        AOWestLYLZ = (objects["AOWestLYLZ"]);
        lightmapBlocklightWestHYLZ = (objects["lightmapBlocklightWestHYLZ"]);
        lightmapSkylightWestHYLZ = (objects["lightmapSkylightWestHYLZ"]);
        AOWestHYLZ = (objects["AOWestHYLZ"]);
        lightmapBlocklightWestLYHZ = (objects["lightmapBlocklightWestLYHZ"]);
        lightmapSkylightWestLYHZ = (objects["lightmapSkylightWestLYHZ"]);
        AOWestLYHZ = (objects["AOWestLYHZ"]);
        lightmapBlocklightWestHYHZ = (objects["lightmapBlocklightWestHYHZ"]);
        lightmapSkylightWestHYHZ = (objects["lightmapSkylightWestHYHZ"]);
        AOWestHYHZ = (objects["AOWestHYHZ"]);

        lightmapBlocklightEastLYLZ = (objects["lightmapBlocklightEastLYLZ"]);
        lightmapSkylightEastLYLZ = (objects["lightmapSkylightEastLYLZ"]);
        AOEastLYLZ = (objects["AOEastLYLZ"]);
        lightmapBlocklightEastHYLZ = (objects["lightmapBlocklightEastHYLZ"]);
        lightmapSkylightEastHYLZ = (objects["lightmapSkylightEastHYLZ"]);
        AOEastHYLZ = (objects["AOEastHYLZ"]);
        lightmapBlocklightEastLYHZ = (objects["lightmapBlocklightEastLYHZ"]);
        lightmapSkylightEastLYHZ = (objects["lightmapSkylightEastLYHZ"]);
        AOEastLYHZ = (objects["AOEastLYHZ"]);
        lightmapBlocklightEastHYHZ = (objects["lightmapBlocklightEastHYHZ"]);
        lightmapSkylightEastHYHZ = (objects["lightmapSkylightEastHYHZ"]);
        AOEastHYHZ = (objects["AOEastHYHZ"]);

        lightmapBlocklightTopLXLZ = (objects["lightmapBlocklightTopLXLZ"]);
        lightmapSkylightTopLXLZ = (objects["lightmapSkylightTopLXLZ"]);
        AOTopLXLZ = (objects["AOTopLXLZ"]);
        lightmapBlocklightTopHXLZ = (objects["lightmapBlocklightTopHXLZ"]);
        lightmapSkylightTopHXLZ = (objects["lightmapSkylightTopHXLZ"]);
        AOTopHXLZ = (objects["AOTopHXLZ"]);
        lightmapBlocklightTopLXHZ = (objects["lightmapBlocklightTopLXHZ"]);
        lightmapSkylightTopLXHZ = (objects["lightmapSkylightTopLXHZ"]);
        AOTopLXHZ = (objects["AOTopLXHZ"]);
        lightmapBlocklightTopHXHZ = (objects["lightmapBlocklightTopHXHZ"]);
        lightmapSkylightTopHXHZ = (objects["lightmapSkylightTopHXHZ"]);
        AOTopHXHZ = (objects["AOTopHXHZ"]);

        lightmapBlocklightBottomLXLZ = (objects["lightmapBlocklightBottomLXLZ"]);
        lightmapSkylightBottomLXLZ = (objects["lightmapSkylightBottomLXLZ"]);
        AOBottomLXLZ = (objects["AOBottomLXLZ"]);
        lightmapBlocklightBottomHXLZ = (objects["lightmapBlocklightBottomHXLZ"]);
        lightmapSkylightBottomHXLZ = (objects["lightmapSkylightBottomHXLZ"]);
        AOBottomHXLZ = (objects["AOBottomHXLZ"]);
        lightmapBlocklightBottomLXHZ = (objects["lightmapBlocklightBottomLXHZ"]);
        lightmapSkylightBottomLXHZ = (objects["lightmapSkylightBottomLXHZ"]);
        AOBottomLXHZ = (objects["AOBottomLXHZ"]);
        lightmapBlocklightBottomHXHZ = (objects["lightmapBlocklightBottomHXHZ"]);
        lightmapSkylightBottomHXHZ = (objects["lightmapSkylightBottomHXHZ"]);
        AOBottomHXHZ = (objects["AOBottomHXHZ"]);

        lightmapBlocklightSouthLXLY = (objects["lightmapBlocklightSouthLXLY"]);
        lightmapSkylightSouthLXLY = (objects["lightmapSkylightSouthLXLY"]);
        AOSouthLXLY = (objects["AOSouthLXLY"]);
        lightmapBlocklightSouthHXLY = (objects["lightmapBlocklightSouthHXLY"]);
        lightmapSkylightSouthHXLY = (objects["lightmapSkylightSouthHXLY"]);
        AOSouthHXLY = (objects["AOSouthHXLY"]);
        lightmapBlocklightSouthLXHY = (objects["lightmapBlocklightSouthLXHY"]);
        lightmapSkylightSouthLXHY = (objects["lightmapSkylightSouthLXHY"]);
        AOSouthLXHY = (objects["AOSouthLXHY"]);
        lightmapBlocklightSouthHXHY = (objects["lightmapBlocklightSouthHXHY"]);
        lightmapSkylightSouthHXHY = (objects["lightmapSkylightSouthHXHY"]);
        AOSouthHXHY = (objects["AOSouthHXHY"]);

        lightmapBlocklightNorthLXLY = (objects["lightmapBlocklightNorthLXLY"]);
        lightmapSkylightNorthLXLY = (objects["lightmapSkylightNorthLXLY"]);
        AONorthLXLY = (objects["AONorthLXLY"]);
        lightmapBlocklightNorthHXLY = (objects["lightmapBlocklightNorthHXLY"]);
        lightmapSkylightNorthHXLY = (objects["lightmapSkylightNorthHXLY"]);
        AONorthHXLY = (objects["AONorthHXLY"]);
        lightmapBlocklightNorthLXHY = (objects["lightmapBlocklightNorthLXHY"]);
        lightmapSkylightNorthLXHY = (objects["lightmapSkylightNorthLXHY"]);
        AONorthLXHY = (objects["AONorthLXHY"]);
        lightmapBlocklightNorthHXHY = (objects["lightmapBlocklightNorthHXHY"]);
        lightmapSkylightNorthHXHY = (objects["lightmapSkylightNorthHXHY"]);
        AONorthHXHY = (objects["AONorthHXHY"]);

    }

    QuartzBlockRenderInfo::Packed QuartzBlockRenderInfo::pack() const {
        QuartzBlockRenderInfo::Packed packedData = {};

        packedData.blockPosition = 0;
        {
            std::uint8_t hiddenFaces = 0;

            hiddenFaces |= std::uint8_t((textureIDWest >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDEast >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDBottom >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDTop >= 0) << 0u);
            hiddenFaces |= std::uint8_t((textureIDSouth >= 0) << 0u);

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
        // this aligns them with the face/vertex IDs and default UV coordinates in the VBO
        // see WorldManager#64 for said VBO data

        // 0, 0: 3 LL
        // 1, 0: 2 HL
        // 0, 1: 1 LH
        // 1, 1: 0 HH

        packedData.lightmap[0][0] = std::uint32_t(lightmapBlocklightEastLYLZ << 8u) | (lightmapSkylightEastLYLZ);
        packedData.lightmap[0][1] = std::uint32_t(lightmapBlocklightEastLYHZ << 8u) | (lightmapSkylightEastLYHZ);
        packedData.lightmap[0][2] = std::uint32_t(lightmapBlocklightEastHYLZ << 8u) | (lightmapSkylightEastHYLZ);
        packedData.lightmap[0][3] = std::uint32_t(lightmapBlocklightEastHYHZ << 8u) | (lightmapSkylightEastHYHZ);

        packedData.lightmap[0][0] <<= 16u;
        packedData.lightmap[0][1] <<= 16u;
        packedData.lightmap[0][2] <<= 16u;
        packedData.lightmap[0][3] <<= 16u;

        packedData.lightmap[0][0] |= std::uint32_t(lightmapBlocklightWestLYHZ << 8u) | (lightmapSkylightWestLYHZ);
        packedData.lightmap[0][1] |= std::uint32_t(lightmapBlocklightWestLYLZ << 8u) | (lightmapSkylightWestLYLZ);
        packedData.lightmap[0][2] |= std::uint32_t(lightmapBlocklightWestHYHZ << 8u) | (lightmapSkylightWestHYHZ);
        packedData.lightmap[0][3] |= std::uint32_t(lightmapBlocklightWestHYLZ << 8u) | (lightmapSkylightWestHYLZ);


        packedData.lightmap[1][0] = std::uint32_t(lightmapBlocklightTopHXHZ << 8u) | (lightmapSkylightTopHXHZ);
        packedData.lightmap[1][1] = std::uint32_t(lightmapBlocklightTopLXHZ << 8u) | (lightmapSkylightTopLXHZ);
        packedData.lightmap[1][2] = std::uint32_t(lightmapBlocklightTopHXLZ << 8u) | (lightmapSkylightTopHXLZ);
        packedData.lightmap[1][3] = std::uint32_t(lightmapBlocklightTopLXLZ << 8u) | (lightmapSkylightTopLXLZ);

        packedData.lightmap[1][0] <<= 16u;
        packedData.lightmap[1][1] <<= 16u;
        packedData.lightmap[1][2] <<= 16u;
        packedData.lightmap[1][3] <<= 16u;

        packedData.lightmap[1][0] |= std::uint32_t(lightmapBlocklightBottomHXLZ << 8u) | (lightmapSkylightBottomHXLZ);
        packedData.lightmap[1][1] |= std::uint32_t(lightmapBlocklightBottomLXLZ << 8u) | (lightmapSkylightBottomLXLZ);
        packedData.lightmap[1][2] |= std::uint32_t(lightmapBlocklightBottomHXHZ << 8u) | (lightmapSkylightBottomHXHZ);
        packedData.lightmap[1][3] |= std::uint32_t(lightmapBlocklightBottomLXHZ << 8u) | (lightmapSkylightBottomLXHZ);


        packedData.lightmap[2][0] = std::uint32_t(lightmapBlocklightSouthHXLY << 8u) | (lightmapSkylightSouthHXLY);
        packedData.lightmap[2][1] = std::uint32_t(lightmapBlocklightSouthLXLY << 8u) | (lightmapSkylightSouthLXLY);
        packedData.lightmap[2][2] = std::uint32_t(lightmapBlocklightSouthHXHY << 8u) | (lightmapSkylightSouthHXHY);
        packedData.lightmap[2][3] = std::uint32_t(lightmapBlocklightSouthLXHY << 8u) | (lightmapSkylightSouthLXHY);

        packedData.lightmap[2][0] <<= 16u;
        packedData.lightmap[2][1] <<= 16u;
        packedData.lightmap[2][2] <<= 16u;
        packedData.lightmap[2][3] <<= 16u;

        packedData.lightmap[2][0] |= std::uint32_t(lightmapBlocklightNorthLXLY << 8u) | (lightmapSkylightNorthLXLY);
        packedData.lightmap[2][1] |= std::uint32_t(lightmapBlocklightNorthHXLY << 8u) | (lightmapSkylightNorthHXLY);
        packedData.lightmap[2][2] |= std::uint32_t(lightmapBlocklightNorthLXHY << 8u) | (lightmapSkylightNorthLXHY);
        packedData.lightmap[2][3] |= std::uint32_t(lightmapBlocklightNorthHXHY << 8u) | (lightmapSkylightNorthHXHY);

        return packedData;
    }
}