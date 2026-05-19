package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.base.RebarInteractEntity
import io.github.pylonmc.rebar.entity.display.BlockDisplayBuilder
import io.github.pylonmc.rebar.entity.display.InteractionBuilder
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.findType
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.swapItem
import io.github.pylonmc.rebar.waila.WailaDisplay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import net.kyori.adventure.text.Component
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.util.Vector
import org.jetbrains.annotations.MustBeInvokedByOverriders
import org.joml.Vector3i
import java.time.Duration
import java.util.*

/**
 * A wrapper over [RebarEntityHolderBlock] which allows for 'ghost blocks' (like those used
 * to show how to build a multiblock) to be managed. Each ghost block has a relative position
 * and can display vanilla blocks, rebar blocks, or both.
 *
 * Due to some annoying technicalities, vanilla blocks need to be displayed as a [BlockDisplay]
 * and Rebar items as an [ItemDisplay]. So we operate a somewhat cursed system where either a
 * BlockDisplay, ItemDisplay, or both are spawned. In the case where both are spawned, one of
 * the displays is hidden while the other is shown as all the possible states are cycled
 * through.
 *
 * You can access the individual displays with [getVanillaGhostBlockDisplay] and
 * [getRebarGhostBlockDisplay]. Keep in mind that any given position may have none, one, or both
 * displays.
 */
interface RebarGhostBlockHolder : RebarEntityHolderBlock {

    class GhostBlockHitbox : RebarEntity<Interaction>, RebarInteractEntity {
        val ghostBlockId: UUID

        var originalSize: Float = 0.5F

        var lastInteract: Long = 0

        constructor(entity: Interaction) : super(entity) {
            this.ghostBlockId = entity.persistentDataContainer.get(GHOST_BLOCK_ID_KEY, RebarSerializers.UUID)!!
        }

        constructor(ghostBlock: GhostBlock<*>) : super(KEY, InteractionBuilder().size(0.5F).build(ghostBlock.entity.location)) {
            this.ghostBlockId = ghostBlock.uuid
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(GHOST_BLOCK_ID_KEY, RebarSerializers.UUID, ghostBlockId)
        }

        @MultiHandler(priorities = [ EventPriority.MONITOR ], ignoreCancelled = true)
        override fun onInteract(event: PlayerInteractEntityEvent, priority: EventPriority) {
            if (System.currentTimeMillis() - lastInteract < 1000) {
                return
            }

            val player = event.player
            val inventory = player.inventory
            val pickStack = EntityStorage.getAs(GhostBlock::class.java, ghostBlockId)?.getPickItem() ?: return
            val pickType = ItemTypeWrapper(pickStack)

            var foundIndex = inventory.findType(pickType)
            if (foundIndex == null) {
                if (player.gameMode == GameMode.CREATIVE) {
                    if (inventory.addItem(pickStack).isNotEmpty()) {
                        return
                    }
                    foundIndex = inventory.findType(pickType) ?: return
                } else {
                    return
                }
            }

            if (foundIndex <= 8) {
                inventory.heldItemSlot = foundIndex
            } else {
                inventory.swapItem(foundIndex, inventory.heldItemSlot)
            }
        }

        override fun getWaila(player: Player): WailaDisplay? {
            return EntityStorage.getAs(GhostBlock::class.java, ghostBlockId)?.getWaila(player)
        }

        fun hide() {
            if (entity.interactionWidth < 0.1F) return
            originalSize = entity.interactionWidth
            entity.interactionWidth = 0.0F
            entity.interactionHeight = 0.0F
        }

        fun show() {
            entity.interactionWidth = originalSize
            entity.interactionHeight = originalSize
        }

        fun setSize(size: Double) {
            entity.interactionWidth = size.toFloat()
            entity.interactionHeight = size.toFloat()
        }

        companion object {
            val KEY = rebarKey("rebar_ghost_block_hitbox")
            val GHOST_BLOCK_ID_KEY = rebarKey("ghost_block_id")
        }
    }

    open class GhostBlock<E: Entity> : RebarEntity<E> {
        val position: Vector3i

        constructor(entity: E) : super(entity) {
            this.position = entity.persistentDataContainer.get(POSITION_KEY, RebarSerializers.VECTOR3I)!!
        }

        constructor(key: NamespacedKey, entity: E, position: Vector3i) : super(key, entity) {
            this.position = position
        }

        @MustBeInvokedByOverriders
        override fun write(pdc: PersistentDataContainer) {
            pdc.set(POSITION_KEY, RebarSerializers.VECTOR3I, position)
        }

        companion object {
            val POSITION_KEY = rebarKey("ghost_block_block_position")
        }
    }

    class VanillaGhostBlock : GhostBlock<BlockDisplay> {
        val vanillaBlocks: MutableList<BlockData>

        constructor(entity: BlockDisplay) : super(entity) {
            vanillaBlocks = entity.persistentDataContainer.get(VANILLA_BLOCKS_KEY, VANILLA_BLOCKS_TYPE)!!
        }

        constructor(ghostBlockHolder: Block, position: Vector3i, blockDatas: MutableList<BlockData>) : super(
            KEY,
            BlockDisplayBuilder()
                .blockData(blockDatas.first())
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.501))
                .build(ghostBlockHolder.location.toCenterLocation().add(Vector.fromJOML(position))),
            position
        ) {
            this.vanillaBlocks = blockDatas
        }

        override fun write(pdc: PersistentDataContainer) {
            super.write(pdc)
            pdc.set(VANILLA_BLOCKS_KEY, VANILLA_BLOCKS_TYPE, vanillaBlocks)
        }

        override fun getWaila(player: Player): WailaDisplay? {
            return WailaDisplay(Component.translatable(entity.block.placementMaterial.let { it.itemTranslationKey ?: it.blockTranslationKey } ?: return null))
        }

        override fun getPickItem(): ItemStack? {
            return entity.block.placementMaterial.let { if (it.isItem) ItemStack(it) else null }
        }

        fun setSize(size: Double) {
            entity.setTransformationMatrix(TransformBuilder().scale(size).buildForBlockDisplay())
        }

        fun setIndex(i: Int) {
            entity.block = vanillaBlocks[i]
        }

        companion object {
            val KEY = rebarKey("vanilla_ghost_block")
            val VANILLA_BLOCKS_KEY = rebarKey("vanilla_ghost_block_vanilla_blocks")
            val VANILLA_BLOCKS_TYPE = RebarSerializers.LIST.listTypeFrom(RebarSerializers.BLOCK_DATA)
        }
    }

    class RebarGhostBlock : GhostBlock<ItemDisplay> {
        val rebarBlocks: MutableList<NamespacedKey>

        constructor(entity: ItemDisplay) : super(entity) {
            rebarBlocks = entity.persistentDataContainer.get(REBAR_BLOCKS_KEY, VANILLA_BLOCKS_TYPE)!!
        }

        constructor(ghostBlockHolder: Block, position: Vector3i, items: MutableList<NamespacedKey>) : super(
            KEY,
            ItemDisplayBuilder()
                .itemStack(RebarRegistry.ITEMS.getOrThrow(items.first()).getItemStack())
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.501))
                .build(ghostBlockHolder.location.toCenterLocation().add(Vector.fromJOML(position))),
            position
        ) {
            this.rebarBlocks = items
        }

        override fun write(pdc: PersistentDataContainer) {
            super.write(pdc)
            pdc.set(REBAR_BLOCKS_KEY, VANILLA_BLOCKS_TYPE, rebarBlocks)
        }

        override fun getWaila(player: Player): WailaDisplay {
            return WailaDisplay(entity.itemStack.effectiveName())
        }

        override fun getPickItem(): ItemStack {
            return entity.itemStack.clone()
        }

        fun setSize(size: Double) {
            entity.setTransformationMatrix(TransformBuilder().scale(size).buildForItemDisplay())
        }

        fun setIndex(i: Int) {
            entity.setItemStack(RebarRegistry.ITEMS.getOrThrow(rebarBlocks[i]).getItemStack())
        }

        companion object {
            val KEY = rebarKey("rebar_ghost_block")
            val REBAR_BLOCKS_KEY = rebarKey("rebar_ghost_block_rebar_blocks")
            val VANILLA_BLOCKS_TYPE = RebarSerializers.LIST.listTypeFrom(RebarSerializers.NAMESPACED_KEY)
        }
    }

    fun addGhostBlock(position: Vector3i, vanillaBlocks: List<BlockData>, rebarBlocks: List<NamespacedKey>) {
        check(!hasGhostBlockAt(position)) { "There is already a ghost block at $position" }

        if (!vanillaBlocks.isEmpty()) {
            val entity = VanillaGhostBlock(block, position, vanillaBlocks.toMutableList())
            val hitbox = GhostBlockHitbox(entity)
            EntityStorage.add(entity)
            EntityStorage.add(hitbox)
            addEntity(getVanillaGhostBlockName(position, false), entity)
            addEntity(getVanillaGhostBlockName(position, true), hitbox)
        }

        if (!rebarBlocks.isEmpty()) {
            val entity = RebarGhostBlock(block, position, rebarBlocks.toMutableList())
            val hitbox = GhostBlockHitbox(entity)
            EntityStorage.add(entity)
            EntityStorage.add(hitbox)
            addEntity(getRebarGhostBlockName(position, false), entity)
            addEntity(getRebarGhostBlockName(position, true), hitbox)
        }

        startCycleTask(position)
    }

    fun removeGhostBlock(position: Vector3i) {
        getHeldEntity(getVanillaGhostBlockName(position, false))?.remove()
        getHeldEntity(getVanillaGhostBlockName(position, true))?.remove()
        getHeldEntity(getRebarGhostBlockName(position, false))?.remove()
        getHeldEntity(getRebarGhostBlockName(position, true))?.remove()
    }

    fun hasGhostBlockAt(position: Vector3i)
            = getVanillaGhostBlockDisplay(position) != null || getRebarGhostBlockDisplay(position) != null

    fun getVanillaGhostBlockDisplay(position: Vector3i)
            = getHeldRebarEntity(VanillaGhostBlock::class.java, getVanillaGhostBlockName(position, false))

    fun getVanillaGhostBlockHitbox(position: Vector3i)
            = getHeldRebarEntity(GhostBlockHitbox::class.java, getVanillaGhostBlockName(position, true))

    fun getRebarGhostBlockDisplay(position: Vector3i)
            = getHeldRebarEntity(RebarGhostBlock::class.java, getRebarGhostBlockName(position, false))

    fun getRebarGhostBlockHitbox(position: Vector3i)
            = getHeldRebarEntity(GhostBlockHitbox::class.java, getRebarGhostBlockName(position, true))

    private fun startCycleTask(position: Vector3i) {
        val vanillaGhostBlock = getVanillaGhostBlockDisplay(position)
        val vanillaHitbox = getVanillaGhostBlockHitbox(position)
        val rebarGhostBlock = getRebarGhostBlockDisplay(position)
        val rebarHitbox = getRebarGhostBlockHitbox(position)
        check(vanillaGhostBlock != null || rebarGhostBlock != null)

        val updateTasks = mutableListOf<Runnable>()
        if (vanillaGhostBlock != null) {
            for (i in 0..<vanillaGhostBlock.vanillaBlocks.size) {
                if (i == 0 && rebarGhostBlock != null) {
                    updateTasks.add {
                        rebarGhostBlock.setSize(0.499)
                        rebarHitbox?.setSize(0.499)
                        vanillaGhostBlock.setSize(0.501)
                        vanillaHitbox?.setSize(0.501)
                        vanillaGhostBlock.setIndex(i)
                    }
                } else {
                    updateTasks.add {
                        vanillaGhostBlock.setIndex(i)
                    }
                }
            }
        }
        if (rebarGhostBlock != null) {
            for (i in 0..<rebarGhostBlock.rebarBlocks.size) {
                if (i == 0 && vanillaGhostBlock != null) {
                    updateTasks.add {
                        vanillaGhostBlock.setSize(0.499)
                        vanillaHitbox?.setSize(0.499)
                        rebarGhostBlock.setSize(0.501)
                        rebarHitbox?.setSize(0.501)
                        rebarGhostBlock.setIndex(i)
                    }
                } else {
                    updateTasks.add {
                        rebarGhostBlock.setIndex(i)
                    }
                }
            }
        }

        if (updateTasks.size <= 1) {
            return
        }

        var i = 0
        Rebar.scope.launch(Rebar.mainThreadDispatcher) {
            while (true) {
                if (vanillaGhostBlock != null && !vanillaGhostBlock.entity.isValid
                    || rebarGhostBlock != null && !rebarGhostBlock.entity.isValid
                ) {
                    break
                }

                updateTasks[i].run()
                i++
                i %= updateTasks.size

                delay(Duration.ofMillis((RebarConfig.GHOST_BLOCK_TICK_INTERVAL * 1000 / 20).toLong()))
            }
        }
    }

    companion object : Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        fun onLoad(event: RebarBlockLoadEvent) {
            val block = event.rebarBlock
            if (block !is RebarGhostBlockHolder) {
                return
            }

            for (name in block.heldEntities.keys) {
                val entity = block.getHeldRebarEntity(name)
                if (entity !is GhostBlock) {
                    continue
                }
                val vanilla = block.getHeldRebarEntity(VanillaGhostBlock::class.java, getVanillaGhostBlockName(entity.position, false))
                val rebar = block.getHeldRebarEntity(RebarGhostBlock::class.java, getRebarGhostBlockName(entity.position, false))
                if (vanilla != null && rebar != null) {
                    if (entity is VanillaGhostBlock) { // avoid starting duplicate tasks
                        block.startCycleTask(entity.position)
                    }
                } else {
                    block.startCycleTask(entity.position)
                }
            }
        }

        private fun getVanillaGhostBlockName(position: Vector3i, hitbox: Boolean) = "vanilla_ghost_block_${position.x}_${position.y}_${position.z}${if (hitbox) "_hitbox" else ""}"

        private fun getRebarGhostBlockName(position: Vector3i, hitbox: Boolean) = "rebar_ghost_block_${position.x}_${position.y}_${position.z}${if (hitbox) "_hitbox" else ""}"
    }
}