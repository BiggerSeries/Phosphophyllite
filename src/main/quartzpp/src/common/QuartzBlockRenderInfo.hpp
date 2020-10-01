#pragma once

#include <RogueLib/ROBN/ROBNTranslation.hpp>

namespace Phosphophyllite::Quartz {
    struct QuartzBlockRenderInfo : public RogueLib::ROBN::Serializable {
        RogueLib::ROBN::ROBN toROBN() override;

        void
        fromROBN(RogueLib::ROBN::Byte*& ptr, const RogueLib::ROBN::Byte* endPtr, RogueLib::ROBN::Type type) override;

        std::int32_t x;
        std::int32_t y;
        std::int32_t z;

        // -1 signals to not render that face
        // will not be ignored in buffer (all things are equal size)
        std::uint32_t textureIDWest = -1;
        std::uint32_t textureIDEast = -1;
        std::uint32_t textureIDBottom = -1;
        std::uint32_t textureIDTop = -1;
        std::uint32_t textureIDSouth = -1;
        std::uint32_t textureIDNorth = -1;

        std::uint8_t textureRotationWest = 0;
        std::uint8_t textureRotationEast = 0;
        std::uint8_t textureRotationBottom = 0;
        std::uint8_t textureRotationTop = 0;
        std::uint8_t textureRotationSouth = 0;
        std::uint8_t textureRotationNorth = 0;

        std::uint8_t lightmapBlocklightWestLYLZ = 0x3F;
        std::uint8_t lightmapSkylightWestLYLZ = 0x3F;
        std::uint8_t AOWestLYLZ = 0;
        std::uint8_t lightmapBlocklightWestHYLZ = 0x3F;
        std::uint8_t lightmapSkylightWestHYLZ = 0x3F;
        std::uint8_t AOWestHYLZ = 0;
        std::uint8_t lightmapBlocklightWestLYHZ = 0x3F;
        std::uint8_t lightmapSkylightWestLYHZ = 0x3F;
        std::uint8_t AOWestLYHZ = 0;
        std::uint8_t lightmapBlocklightWestHYHZ = 0x3F;
        std::uint8_t lightmapSkylightWestHYHZ = 0x3F;
        std::uint8_t AOWestHYHZ = 0;

        std::uint8_t lightmapBlocklightEastLYLZ = 0x3F;
        std::uint8_t lightmapSkylightEastLYLZ = 0x3F;
        std::uint8_t AOEastLYLZ = 0;
        std::uint8_t lightmapBlocklightEastHYLZ = 0x3F;
        std::uint8_t lightmapSkylightEastHYLZ = 0x3F;
        std::uint8_t AOEastHYLZ = 0;
        std::uint8_t lightmapBlocklightEastLYHZ = 0x3F;
        std::uint8_t lightmapSkylightEastLYHZ = 0x3F;
        std::uint8_t AOEastLYHZ = 0;
        std::uint8_t lightmapBlocklightEastHYHZ = 0x3F;
        std::uint8_t lightmapSkylightEastHYHZ = 0x3F;
        std::uint8_t AOEastHYHZ = 0;

        std::uint8_t lightmapBlocklightTopLXLZ = 0x3F;
        std::uint8_t lightmapSkylightTopLXLZ = 0x3F;
        std::uint8_t AOTopLXLZ = 0;
        std::uint8_t lightmapBlocklightTopHXLZ = 0x3F;
        std::uint8_t lightmapSkylightTopHXLZ = 0x3F;
        std::uint8_t AOTopHXLZ = 0;
        std::uint8_t lightmapBlocklightTopLXHZ = 0x3F;
        std::uint8_t lightmapSkylightTopLXHZ = 0x3F;
        std::uint8_t AOTopLXHZ = 0;
        std::uint8_t lightmapBlocklightTopHXHZ = 0x3F;
        std::uint8_t lightmapSkylightTopHXHZ = 0x3F;
        std::uint8_t AOTopHXHZ = 0;

        std::uint8_t lightmapBlocklightBottomLXLZ = 0x3F;
        std::uint8_t lightmapSkylightBottomLXLZ = 0x3F;
        std::uint8_t AOBottomLXLZ = 0;
        std::uint8_t lightmapBlocklightBottomHXLZ = 0x3F;
        std::uint8_t lightmapSkylightBottomHXLZ = 0x3F;
        std::uint8_t AOBottomHXLZ = 0;
        std::uint8_t lightmapBlocklightBottomLXHZ = 0x3F;
        std::uint8_t lightmapSkylightBottomLXHZ = 0x3F;
        std::uint8_t AOBottomLXHZ = 0;
        std::uint8_t lightmapBlocklightBottomHXHZ = 0x3F;
        std::uint8_t lightmapSkylightBottomHXHZ = 0x3F;
        std::uint8_t AOBottomHXHZ = 0;

        std::uint8_t lightmapBlocklightSouthLXLY = 0x3F;
        std::uint8_t lightmapSkylightSouthLXLY = 0x3F;
        std::uint8_t AOSouthLXLY = 0x3F;
        std::uint8_t lightmapBlocklightSouthHXLY = 0x3F;
        std::uint8_t lightmapSkylightSouthHXLY = 0x3F;
        std::uint8_t AOSouthHXLY = 0x3F;
        std::uint8_t lightmapBlocklightSouthLXHY = 0x3F;
        std::uint8_t lightmapSkylightSouthLXHY = 0x3F;
        std::uint8_t AOSouthLXHY = 0x3F;
        std::uint8_t lightmapBlocklightSouthHXHY = 0x3F;
        std::uint8_t lightmapSkylightSouthHXHY = 0x3F;
        std::uint8_t AOSouthHXHY = 0x3F;

        std::uint8_t lightmapBlocklightNorthLXLY = 0x3F;
        std::uint8_t lightmapSkylightNorthLXLY = 0x3F;
        std::uint8_t AONorthLXLY = 0;
        std::uint8_t lightmapBlocklightNorthHXLY = 0x3F;
        std::uint8_t lightmapSkylightNorthHXLY = 0x3F;
        std::uint8_t AONorthHXLY = 0;
        std::uint8_t lightmapBlocklightNorthLXHY = 0x3F;
        std::uint8_t lightmapSkylightNorthLXHY = 0x3F;
        std::uint8_t AONorthLXHY = 0;
        std::uint8_t lightmapBlocklightNorthHXHY = 0x3F;
        std::uint8_t lightmapSkylightNorthHXHY = 0x3F;
        std::uint8_t AONorthHXHY = 0;

        // this is an in memory representation of the data that is passed to OpenGL
        // well, some of it is
        // it doesnt match the above layout precisely
        struct Packed {
            std::uint32_t blockPosition;
            std::uint32_t textureIDRotation[6];
            std::uint32_t lightmap[3][4];
        };

        [[nodiscard]] Packed pack() const;
    };
}