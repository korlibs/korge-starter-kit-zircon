import org.hexworks.zircon.api.*
import org.hexworks.zircon.api.application.*
import org.hexworks.zircon.api.builder.application.*
import org.hexworks.zircon.api.builder.application.appConfig
import org.hexworks.zircon.api.builder.component.*
import org.hexworks.zircon.api.component.*
import org.hexworks.zircon.api.component.builder.base.*
import org.hexworks.zircon.api.extensions.*
import org.hexworks.zircon.api.graphics.*

suspend fun main() {
    val tileSize = 20
    val gridCols = 1920 / tileSize / 2
    val gridRows = 1080 / tileSize / 2

    createApplication(appConfig {
        withSize {
            width = gridCols
            height = gridRows
        }
        defaultTileset = CP437TilesetResources.rexPaint20x20()
        debugMode = true
    }).test().start()
}

fun Application.test() = also {
    val screen = tileGrid.toScreen()

    screen.addComponent(buildVbox {
        lateinit var textArea: TextArea
        hbox {
            textArea = textArea {
                withPreferredContentSize {
                    width = 20
                    height = 10
                }
            }
            verticalScrollBar(textArea){
            }
        }
        hbox {
            button {
                +"${Symbols.ARROW_LEFT}"
                onActivated {
                    textArea.scrollOneLeft()
                    textArea.requestFocus()
                }
            }
            button {
                +"${Symbols.ARROW_RIGHT}"
                onActivated {
                    textArea.scrollOneRight()
                    textArea.requestFocus()
                }
            }
            button {
                +"${Symbols.ARROW_UP}"
                onActivated {
                    textArea.scrollOneUp()
                    textArea.requestFocus()
                }
            }
            button {
                +"${Symbols.ARROW_DOWN}"
                onActivated {
                    textArea.scrollOneDown()
                    textArea.requestFocus()
                }
            }
        }
    })

    screen.display()
}
