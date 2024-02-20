import korlibs.korge.*
import korlibs.korge.scene.*
import korlibs.math.geom.*
import org.hexworks.zircon.api.builder.component.*
import org.hexworks.zircon.api.component.*
import org.hexworks.zircon.api.component.builder.base.*
import org.hexworks.zircon.api.graphics.*

suspend fun main() = Korge(
    virtualSize = Size(1280, 720),
    displayMode = KorgeDisplayMode.NO_SCALE,
    scaleMode = ScaleMode.NO_SCALE,
    fullscreen = false,
    title = "Zircon KorGE",
    debug = false,
) {
    sceneContainer().changeTo { ZirconGame() }
}

class ZirconGame() : BaseZirconKorgeScene(1280, 720) {
    override fun gameMain() {
        screen.addComponent(buildVbox {
            lateinit var textArea: TextArea
            hbox {
                textArea = textArea {
                    withPreferredContentSize {
                        width = 24
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
}
