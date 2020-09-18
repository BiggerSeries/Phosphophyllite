#Quartz renderer

The quartz render system is a supplementary world render engine  designed to handle dynamic updates better than the main engine

nothing this renderer does will change any state in MC, including what MC renders, so, you will need to ensure that MC isn't rendering anything where Quartz is, to avoid Z-fighting artifacts

## Normal blocks

Normal (full cube) blocks are all rendered in a "cutout" layer, before MC draw's anything (except the sky)

These are renderer in culled chunks identical to MC's render chunks, and are very fast to draw

## Using Quartz

For examples, look at the Reactor and Turbine in BiggerReactors