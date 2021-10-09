# 1.17.1-0.6.0-alpha.5
 - remove serverQueue from Phosphophyllite class, use Queues class instead
   - Queues class also inits queues on class load, allows them to be used w/o forge
 - add new blockstate update function that also triggers light updates
   - also fix old one with worlds that go below 0
 - move config system to object based instead of class based
 - add independent serialization capabilities, allows use of code without minecraft/forge being loaded
 - fix registry bug with item creative tab flag
 - override and finalize other getCapability call in PhosphophylliteTile class

# 1.17.1-0.6.0-alpha.4
  - Fix oregen registration
  - Add TOML support to config system (partial, doesn't support arrays)
  - Ensure multiblock registry is cleared on server stop, 
    - thrown exceptions could have resulted in it not being cleared properly, crashing on world reload
  - Phosphophyllite not built independently, no more monolithic repository

# 1.17.1-0.6.0-alpha.3
 - switch to parchment mappings, shouldn't really effect anything
 - add beginValidation and blockValidated callbacks to rectangular multiblock controller
 - add list neighboring multiblock modules to the multiblock module
 - optimizations
   - mostly magic

# 1.17.1-0.6.0-alpha.2
 - registry rework 2, electric boogaloo, ordering is now more strict and is in the stages that Forge uses, 
   - this is mostly so @RegisterBlock fields aren't classloaded before all @OnModLoad functions are called to register block modules
 - switch ModuleMap to use to Long2ObjectAVLTreeMap, seems marginally faster
 - ensure that an assimilated controllers blocks are reset to disassembled
 - tick ITickableMultiblockTiles properly, missed in modular transision

# 1.17.1-0.6.0-alpha.1
 - tile and block module system properly functional, features still missing
 - multiblock system moved to tile/block module system
 - use mojang's BlockEntitySupplier instead of TileSupplier
   - Registry will now also use a constructor via LambadaMetaFactory if no supplier is provided
 - fix fluids being placed into world 
 - registry now uses statically initialized blocks
 - add capability for multiblocks to not pause on chunk unload

# 1.17.1-0.6.0-alpha
 - 1.17.1
 
# 1.16.5-0.5.1
 - use save delegate, massively reduces save/load time, and save size
 - fix config enum reading, it was still broken
 - add work queues for off thread  work
 - add wrapper for industrial foregoing

# 1.16.5-0.5.0
 - add modular tile system (multiblock system to be ported over), consider alpha, its untested right now
   - add sided multipart module helper
 - move tile blockstate update to tile class from controller, should help with modularity/extensions
 - fix config enum options

# 1.16.5-0.5.0-beta.1
 - fix weird issue with recipe loading (https://github.com/AllTheMods/ATM-6/issues/1384, https://github.com/AllTheMods/ATM-6/issues/1399)

# 1.16.5-0.5.0-beta
 - config properly handles hidden and advanced values
 - new registry backend, some front end changes
 - extent fluid handler to handle NBT
 - add mekanism gas <-> fluid wrappers
 - add fluid handler type wrapper
 - add energy storage extension
 - add energy storage type wrapper
 - add onRemoved multiblock tile call
 - fix some merging issues
 - use DUMMY_UUID for error messages, may fix messages not showing
 - dont reset other blockstate when updating connected texture state

# 1.16.5-0.5.0-alpha.1
 - add on assembly and disassembly listeners
 - add automatic face direction capabilities for rectangular
 - register configuredfeatures for oregen
 - add null controller debeefer message
 - fix null controller with dimension teleporting

# 1.16.5-0.5.0-alpha.0
 - correct do not care bounds range checks
 - patch json loading bug
 - add data loading helper
 - fix class cast exception when a block is broken in the same tick its placed
 - add block type to multiblock API generics
 - allow multiblock controller to be queried for a tile
 - add block type validator, and allow validators to be accessed by sub classes
 - default to block not allowed instead of allowed
 - move tile placement validation from multiblock controller to tiles
 - dont attempt to attach on the client (fixes bag of yurting bug)
 - add ability for tiles to decide what blocks their textures connect to
 - add tick for when a multiblock is disassembled
 - add heat body helper
 - add fluid handling helpers
 - trim out empty config clazz node elements

# 1.16.4-0.4.2
 - add check for performant

# 1.16.4-0.4.1
 - fix bug in TileMap, caused weird multiblock issues after detaching blocks from a multiblock, in some cases

# 1.16.4-0.4.0
 - use A* for block detachment
 - only update min/max when absolutely required
 - only send assembly attempts to blocks that care
 - use linked hashmap where applicable (faster to iterate over)
 - config will correct errors when possible, and yell at you for it
 - config trimming/regeneration, and advanced toggle works
 - debeefer prints last error too
 - paused multiblocks now print error message
 - block assembly state set to false by default
 - dont hold on to NBT client side
 - use specialised map for tiles
 - API uses generics now
 - fix chunk re-loading issue

# 1.16.4-0.3.2
 - fix unresponsive GUI

# 1.16.4-0.3.1
 - config system boolean handling

# 1.16.4-0.3.0
 - blockstate reduction via more fine grained control
 - more descriptive blockstates for rectangular multiblock locations
 - generic multiblock controller decoupled from rectangular completely
 - patch chunkloading bug (better fix does need to be found still)
 - new gui system
 - new config system
 - toggle oregen properly
 - failed registry class loading shouldn't crash anymore (exception is caught now)
 - ticking is done from end of world tick
 - 1.16.4

# 1.16.3-0.2.1
 - Remove paused multiblock errors, don't break it
 - Fix paused multiblock orphaning detaching incorrectly

# 1.16.3-0.2.0
 - Connecting, textures, you are welcome
 - use constant vectors where applicable
 - fix NPE when loading a multiblock that hasn't yet been assembled
 - change multiblock ticks to the world tick events instead of server tick event

# 1.16.3-0.1.1
 - null the tile's caches NBT after it attaches to a controller, should reduce memory usage
 - add models for black holes and white holes
 - fix NPE when saving a multiblock that hasn't yet been assembled

# 1.16.3-0.1.0
 - Rework of multiblock NBT system, breaks mod backwards compatibility, worlds should be ok
 - Other misc breaking changes to multiblock system

# 1.16.3-0.0.5
 - Misc NBT save fixes, reactors were voiding fuel sometimes, turbines were staying at speed when disassembled, should be fixed, but its hard to test when it srandom
 - Fixed renderer stall when updating large multiblock blockstates
 - Fixed NPE with some rectangular multiblock configurations hitting an empty/null chunk section

# 1.16.3-0.0.4
 - Fix saving issues with dedicated servers
 - updated zh_cn translation

# 1.16.3-0.0.3
 - fix java 9+ compatibility

# 1.16.3-0.0.2
 - alphabetically sort creative menu
 - add zh_cn translation (thanks qsefthuopq)
 - remove ROTN
 - remove baked model support from multiblock API

# 1.16.3-0.0.1
 - added deps package as ignored package in the registry for soft dependencies

# 1.16.3-0.0.0
 - first separated release

what changes did you expect, there is nothing to change before here