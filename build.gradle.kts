import korlibs.korge.gradle.*

plugins {
	alias(libs.plugins.korge)
}

korge {
	id = "com.sample.demo"
	targetJvm()
	targetJs()
}

dependencies {
    add("commonMainApi", "org.hexworks.zircon:zircon.core:2024.1.0-ALPHA")
    add("commonMainApi", project(":deps"))
    //add("commonMainApi", project(":korge-dragonbones"))
}

