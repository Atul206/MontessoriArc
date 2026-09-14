package com.calmcoloring.app.content

import com.calmcoloring.app.content.generated.AppleTreePaths
import com.calmcoloring.app.content.generated.BalloonRidePaths
import com.calmcoloring.app.content.generated.GardenFlowerPaths
import com.calmcoloring.app.content.generated.LittleFishPaths
import com.calmcoloring.app.content.generated.LittleHousePaths
import com.calmcoloring.app.content.generated.SailboatPaths
import com.calmcoloring.app.content.generated.SleepyCatPaths
import com.calmcoloring.app.content.generated.SunnyDayPaths
import com.calmcoloring.app.geometry.toHitPolygon
import com.calmcoloring.app.model.RegionSpec
import com.calmcoloring.app.model.Template
import com.calmcoloring.app.theme.CalmPalette

/**
 * The 8 launch coloring templates, each wired from the hand-authored
 * [com.calmcoloring.app.content.generated] Path objects (see the mechanism
 * note in `SunnyDayPaths`) plus a hit polygon sampled from each region's
 * outline. Consumed by `RegionCanvas` (Task 3) and `GalleryScreen`
 * (Task 5).
 */
object TemplateCatalog {
    val all: List<Template> = listOf(
        Template(
            id = "sunny-day",
            name = "Sunny Day",
            accent = CalmPalette.SandLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", SunnyDayPaths.background, SunnyDayPaths.background.toHitPolygon()),
                RegionSpec("hill", SunnyDayPaths.hill, SunnyDayPaths.hill.toHitPolygon()),
                RegionSpec("cloud", SunnyDayPaths.cloud, SunnyDayPaths.cloud.toHitPolygon()),
                RegionSpec("sun", SunnyDayPaths.sun, SunnyDayPaths.sun.toHitPolygon()),
            ),
        ),
        Template(
            id = "little-house",
            name = "Little House",
            accent = CalmPalette.ClayLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", LittleHousePaths.background, LittleHousePaths.background.toHitPolygon()),
                RegionSpec("wall", LittleHousePaths.wall, LittleHousePaths.wall.toHitPolygon()),
                RegionSpec("roof", LittleHousePaths.roof, LittleHousePaths.roof.toHitPolygon()),
                RegionSpec("door", LittleHousePaths.door, LittleHousePaths.door.toHitPolygon()),
                RegionSpec("window", LittleHousePaths.window, LittleHousePaths.window.toHitPolygon()),
            ),
        ),
        Template(
            id = "apple-tree",
            name = "Apple Tree",
            accent = CalmPalette.MossLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", AppleTreePaths.background, AppleTreePaths.background.toHitPolygon()),
                RegionSpec("trunk", AppleTreePaths.trunk, AppleTreePaths.trunk.toHitPolygon()),
                RegionSpec("canopy", AppleTreePaths.canopy, AppleTreePaths.canopy.toHitPolygon()),
                RegionSpec("apple1", AppleTreePaths.apple1, AppleTreePaths.apple1.toHitPolygon()),
                RegionSpec("apple2", AppleTreePaths.apple2, AppleTreePaths.apple2.toHitPolygon()),
            ),
        ),
        Template(
            id = "sleepy-cat",
            name = "Sleepy Cat",
            accent = CalmPalette.LilacLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", SleepyCatPaths.background, SleepyCatPaths.background.toHitPolygon()),
                RegionSpec("tail", SleepyCatPaths.tail, SleepyCatPaths.tail.toHitPolygon()),
                RegionSpec("body", SleepyCatPaths.body, SleepyCatPaths.body.toHitPolygon()),
                RegionSpec("earL", SleepyCatPaths.earL, SleepyCatPaths.earL.toHitPolygon()),
                RegionSpec("earR", SleepyCatPaths.earR, SleepyCatPaths.earR.toHitPolygon()),
                RegionSpec("head", SleepyCatPaths.head, SleepyCatPaths.head.toHitPolygon()),
            ),
        ),
        Template(
            id = "little-fish",
            name = "Little Fish",
            accent = CalmPalette.SkyLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", LittleFishPaths.background, LittleFishPaths.background.toHitPolygon()),
                RegionSpec("tail", LittleFishPaths.tail, LittleFishPaths.tail.toHitPolygon()),
                RegionSpec("fin", LittleFishPaths.fin, LittleFishPaths.fin.toHitPolygon()),
                RegionSpec("body", LittleFishPaths.body, LittleFishPaths.body.toHitPolygon()),
            ),
        ),
        Template(
            id = "garden-flower",
            name = "Garden Flower",
            accent = CalmPalette.SageLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", GardenFlowerPaths.background, GardenFlowerPaths.background.toHitPolygon()),
                RegionSpec("stem", GardenFlowerPaths.stem, GardenFlowerPaths.stem.toHitPolygon()),
                RegionSpec("petal1", GardenFlowerPaths.petal1, GardenFlowerPaths.petal1.toHitPolygon()),
                RegionSpec("petal2", GardenFlowerPaths.petal2, GardenFlowerPaths.petal2.toHitPolygon()),
                RegionSpec("petal3", GardenFlowerPaths.petal3, GardenFlowerPaths.petal3.toHitPolygon()),
                RegionSpec("petal4", GardenFlowerPaths.petal4, GardenFlowerPaths.petal4.toHitPolygon()),
                RegionSpec("petal5", GardenFlowerPaths.petal5, GardenFlowerPaths.petal5.toHitPolygon()),
                RegionSpec("center", GardenFlowerPaths.center, GardenFlowerPaths.center.toHitPolygon()),
            ),
        ),
        Template(
            id = "sailboat",
            name = "Sailboat",
            accent = CalmPalette.ClayLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", SailboatPaths.background, SailboatPaths.background.toHitPolygon()),
                RegionSpec("hull", SailboatPaths.hull, SailboatPaths.hull.toHitPolygon()),
                RegionSpec("sailBig", SailboatPaths.sailBig, SailboatPaths.sailBig.toHitPolygon()),
                RegionSpec("sailSmall", SailboatPaths.sailSmall, SailboatPaths.sailSmall.toHitPolygon()),
                RegionSpec("flag", SailboatPaths.flag, SailboatPaths.flag.toHitPolygon()),
            ),
        ),
        Template(
            id = "balloon-ride",
            name = "Balloon Ride",
            accent = CalmPalette.SandLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", BalloonRidePaths.background, BalloonRidePaths.background.toHitPolygon()),
                RegionSpec("basket", BalloonRidePaths.basket, BalloonRidePaths.basket.toHitPolygon()),
                RegionSpec("knot", BalloonRidePaths.knot, BalloonRidePaths.knot.toHitPolygon()),
                RegionSpec("balloon", BalloonRidePaths.balloon, BalloonRidePaths.balloon.toHitPolygon()),
            ),
        ),
    )

    fun byId(id: String): Template = all.first { it.id == id }
}
