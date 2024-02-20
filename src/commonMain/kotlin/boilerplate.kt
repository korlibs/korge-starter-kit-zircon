import korlibs.datastructure.*
import korlibs.korge.annotations.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.time.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.time.*
import org.hexworks.cobalt.databinding.api.extension.*
import org.hexworks.cobalt.databinding.api.value.*
import org.hexworks.cobalt.events.api.*
import org.hexworks.zircon.api.*
import org.hexworks.zircon.api.application.*
import org.hexworks.zircon.api.application.appConfig
import org.hexworks.zircon.api.behavior.*
import org.hexworks.zircon.api.builder.application.*
import org.hexworks.zircon.api.data.*
import org.hexworks.zircon.api.extensions.*
import org.hexworks.zircon.api.graphics.*
import org.hexworks.zircon.api.modifier.*
import org.hexworks.zircon.api.resource.*
import org.hexworks.zircon.api.tileset.*
import org.hexworks.zircon.internal.graphics.*
import org.hexworks.zircon.internal.grid.*
import org.hexworks.zircon.internal.renderer.*
import org.hexworks.zircon.internal.tileset.impl.*
import org.hexworks.zircon.renderer.korge.*
import org.hexworks.zircon.renderer.korge.tileset.*

abstract class BaseRenderer<C : Any, A : Application, V : Any>(
    protected val tileGrid: InternalTileGrid,
    private val tilesetLoader: TilesetLoader<C>
) : Renderer<C, A, V> {

    private val config = tileGrid.config
    private val gridPositions = tileGrid.size.fetchPositions().toList()
    private val isClosed = false.toProperty()

    private val beforeRenderDataProp = RenderData(DateTime.nowUnixMillisLong()).toProperty()
    private val afterRenderDataProp = RenderData(DateTime.nowUnixMillisLong()).toProperty()

    private var blinkOn = true
    private var lastBlink: Long = DateTime.nowUnixMillisLong()
    private var lastRender: Long = lastBlink

    var beforeRenderData: RenderData by beforeRenderDataProp.asDelegate()
    var afterRenderData: RenderData by afterRenderDataProp.asDelegate()

    override val closedValue: ObservableValue<Boolean>
        get() = isClosed

    final override fun close() {
        isClosed.value = true
        doClose()
    }

    final override fun beforeRender(listener: (RenderData) -> Unit) = beforeRenderDataProp.onChange {
        listener(it.newValue)
    }

    final override fun afterRender(listener: (RenderData) -> Unit) = afterRenderDataProp.onChange {
        listener(it.newValue)
    }

    /**
     * Template method that is responsible for processing all input events for the given frame.
     */
    protected abstract fun processInputEvents()

    /**
     * Template method that is called during [render] but before the actual rendering happens.
     * Can be used to configure the [context] or other components for rendering.
     */
    protected open fun prepareRender(context: C) {}

    /**
     * Template method that implments the close mechanism.
     */
    protected open fun doClose() {
    }

    final override fun render(context: C) {
        if (closed.not()) {
            val now = DateTime.nowUnixMillisLong()
            beforeRenderDataProp.value = RenderData(now)
            processInputEvents()
            tileGrid.updateAnimations(now, tileGrid)
            handleBlink(now)
            prepareRender(context)
            renderAllTiles(context)
            lastRender = now
            afterRenderDataProp.value = RenderData(DateTime.nowUnixMillisLong())
        }
    }

    // TODO: use a drawing strategy here
    private fun renderAllTiles(context: C) {
        val layers = fetchLayers()
        val tiles = mutableListOf<Pair<Tile, TilesetResource>>()
        gridPositions.forEach { pos ->
            tiles@ for (i in layers.size - 1 downTo 0) {
                val (layerPos, layer) = layers[i]
                val toRender = layer.getTileAtOrNull(pos - layerPos)?.tiles() ?: listOf()
                for (j in toRender.size - 1 downTo 0) {
                    val tile = toRender[j]
                    val tileset = tile.finalTileset(layer)
                    tiles.add(0, tile to tileset)
                    if (tile.isOpaque) {
                        break@tiles
                    }
                }
            }

            var idx = 1
            for ((tile, tileset) in tiles) {
                var finalTile = tile
                // 📘 we only draw the cursor on top, that's why we have the last check
                if (shouldDrawCursor() && tileGrid.cursorPosition == pos && idx == tiles.size) {
                    finalTile = finalTile.withBackgroundColor(finalTile.foregroundColor)
                        .withForegroundColor(finalTile.backgroundColor)
                }
                val finalTileset = tilesetLoader.loadTilesetFrom(tileset)
                renderTile(
                    context = context,
                    position = pos,
                    tile = finalTile,
                    tileset = finalTileset
                )
                idx++
            }
            tiles.clear()
        }
    }

    private fun shouldDrawCursor(): Boolean {
        return tileGrid.isCursorVisible &&
            (config.isCursorBlinking.not() || config.isCursorBlinking && blinkOn)
    }

    private fun renderTile(
        context: C,
        position: Position,
        tile: Tile,
        tileset: Tileset<C>
    ) {
        if (tile.isBlinking && blinkOn) {
            return
        }
        if (tile.isNotEmpty) {
            var tempTile = tile
            tempTile.modifiers.filterIsInstance<TileModifier<Tile>>().forEach { modifier ->
                if (modifier.canTransform(tempTile)) {
                    tempTile = modifier.transform(tempTile)
                }
            }
            tempTile = if (tile.isBlinking && blinkOn) {
                tile.withBackgroundColor(tile.foregroundColor)
                    .withForegroundColor(tile.backgroundColor)
            } else tile
            val finalTileset = when (val finalTile = tempTile) {
                is TilesetHolder -> tilesetLoader.loadTilesetFrom(finalTile.tileset)
                else -> tileset
            }
            finalTileset.drawTile(
                tile = tempTile,
                context = context,
                position = position
            )
        }
    }

    private fun handleBlink(now: Long) {
        if (now > lastBlink + config.blinkLengthInMilliSeconds) {
            blinkOn = !blinkOn
            lastBlink = now
        }
    }

    /**
     * Returns all the layers that should be rendered.
     */
    private fun fetchLayers(): List<Pair<Position, TileGraphics>> {
        return tileGrid.renderables.map { renderable ->
            val tg = if (renderable.isHidden) FastTileGraphics(
                initialSize = renderable.size,
                initialTileset = renderable.tileset
            ) else renderable.render()
            renderable.position to tg
        }
    }

    private fun Tile.finalTileset(graphics: TileGraphics): TilesetResource = when (this) {
        is TilesetHolder -> tileset
        else -> graphics.tileset
    }

    private fun Tile.tiles(): List<Tile> = when (this) {
        is StackedTile -> tiles.flatMap { it.tiles() }
        else -> listOf(this)
    }
}

abstract class BaseZirconKorgeScene(width: Int, height: Int, tileSize: Int = 20) : PixelatedScene(
    width,
    height,
    sceneScaleMode = ScaleMode.NO_SCALE,
    sceneAnchor = Anchor.TOP_LEFT,
    sceneSmoothing = false
) {
    val config = org.hexworks.zircon.api.builder.application.appConfig {
        withSize {
            this.width = width / tileSize
            this.height = height / tileSize
        }
        defaultTileset = CP437TilesetResources.rexPaint20x20()
        debugMode = true
    }
    val tileGrid = createTileGrid(config).asInternal()
    val screen = tileGrid.toScreen()
    val tilesetLoader = DefaultTilesetLoader<KorgeContext>(
        config.tilesetFactories.filter { it.targetType == KorgeContext::class }.fastCastTo()
    )


    inner class BaseKorgeRenderer : BaseRenderer<KorgeContext, KorgeApplication, Scene>(tileGrid, tilesetLoader) {
        override fun create(): Scene {
            TODO("Not yet implemented")
        }

        override fun processInputEvents() {
            keyboardEventListener.drainEvents().forEach { (event, phase) ->
                tileGrid.process(event, phase)
            }
            mouseEventListener.drainEvents().forEach { (event, phase) ->
                tileGrid.process(event, phase)
            }
        }
    }

    private val keyboardEventListener = KeyboardEventListener(tileGrid)
    private val mouseEventListener = MouseEventListener(tileGrid)
    val renderer = BaseKorgeRenderer()
    val application = KorgeApplication(
        config = config,
        eventBus = EventBus.create(),
        tileGrid = tileGrid.asInternal(),
        renderer = createRenderer(config, tileGrid)
    )

    @OptIn(KorgeExperimental::class)
    override suspend fun SContainer.sceneMain() {
        mouse {
            mouseEventListener.handleMouseEvent(this)
        }
        keys {
            keyboardEventListener.handleEvents(this)
        }

        var blinkOn = false
        interval(config.blinkLengthInMilliSeconds.milliseconds) {
            blinkOn = !blinkOn
        }

        // 📙 Note that this starts continuous rendering
        renderableView {
            val now = DateTime.nowUnixMillisLong()
            val view = this
            renderer.beforeRenderData = RenderData(now)
            this.ctx.useBatcher { batch ->
                renderer.render(
                    KorgeContext(
                        this.ctx,
                        batch,
                        view
                    )
                )
            }
            renderer.afterRenderData = RenderData(DateTime.nowUnixMillisLong())
        }
        gameMain()
    }

    abstract fun gameMain()

    //override fun create() = KorgeScene()

    fun processInputEvents() {
        keyboardEventListener.drainEvents().forEach { (event, phase) ->
            tileGrid.process(event, phase)
        }
        mouseEventListener.drainEvents().forEach { (event, phase) ->
            tileGrid.process(event, phase)
        }
    }

    fun prepareRender(context: KorgeContext) {
        // TODO: do we need this?
    }
}
