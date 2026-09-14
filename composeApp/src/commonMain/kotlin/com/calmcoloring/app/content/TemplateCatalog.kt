package com.calmcoloring.app.content

import com.calmcoloring.app.content.generated.AppleTreePaths
import com.calmcoloring.app.content.generated.BalloonRidePaths
import com.calmcoloring.app.content.generated.CatPaths
import com.calmcoloring.app.content.generated.ElephantPaths
import com.calmcoloring.app.content.generated.FunnyCatPaths
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
 * The coloring templates, each wired from the hand-authored (or
 * potrace-generated) [com.calmcoloring.app.content.generated] Path objects
 * plus a hit polygon sampled from each region's outline. Consumed by
 * `RegionCanvas` (Task 3) and `GalleryScreen` (Task 5).
 *
 * Ordered by when each was added — the first 8 are the launch set, and
 * anything added since is appended after, in add order. New templates go
 * at the end of [all], never spliced into the middle, so gallery order
 * always matches "when it was added."
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
        // The 8 templates above are the launch set. Everything below was added
        // later, in the order it was added (oldest first) — see feedback
        // memory on gallery ordering: new templates are appended here, never
        // spliced into the middle of the list.
        Template(
            id = "cat",
            name = "Cat",
            accent = CalmPalette.SkyLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", CatPaths.background, CatPaths.background.toHitPolygon()),
                RegionSpec("tail", CatPaths.tail, CatPaths.tail.toHitPolygon()),
                RegionSpec("body", CatPaths.body, CatPaths.body.toHitPolygon()),
                RegionSpec("earL", CatPaths.earL, CatPaths.earL.toHitPolygon()),
                RegionSpec("earR", CatPaths.earR, CatPaths.earR.toHitPolygon()),
                RegionSpec("head", CatPaths.head, CatPaths.head.toHitPolygon()),
                RegionSpec("eyeL", CatPaths.eyeL, CatPaths.eyeL.toHitPolygon()),
                RegionSpec("eyeR", CatPaths.eyeR, CatPaths.eyeR.toHitPolygon()),
                RegionSpec("nose", CatPaths.nose, CatPaths.nose.toHitPolygon()),
            ),
        ),
        Template(
            id = "funny-cat",
            name = "Funny Cat",
            accent = CalmPalette.LilacLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", FunnyCatPaths.background, FunnyCatPaths.background.toHitPolygon()),
                RegionSpec("body", FunnyCatPaths.body, FunnyCatPaths.body.toHitPolygon()),
                RegionSpec("bellyPatch", FunnyCatPaths.bellyPatch, FunnyCatPaths.bellyPatch.toHitPolygon()),
                RegionSpec("eyeR", FunnyCatPaths.eyeR, FunnyCatPaths.eyeR.toHitPolygon()),
                RegionSpec("eyeL", FunnyCatPaths.eyeL, FunnyCatPaths.eyeL.toHitPolygon()),
                // outline/detail1..8 are potrace-traced ink shapes — the path's own fill IS
                // the drawn line at its own width, so strokeWidth=0 (no extra boundary stroke
                // doubling each edge) and fillsWithOutlineByDefault=true (so they read as ink
                // immediately, the same as the source SVG's fill="#000", rather than only
                // becoming visible once tapped).
                //
                // outline, detail1/2/3 (right eye ring+pupil), detail4/5/6 (left eye
                // ring+pupil) and detail8 (the belly patch's own thin ink outline) are all
                // drawn on top of the solid body/eyeR/eyeL/bellyPatch fills above as ink
                // decoration covering the *same* visual area — isDecorative=true so a tap
                // that also lands inside the solid region beneath resolves to that solid fill
                // instead of just recoloring the thin ink on top of it (see RegionCanvas's tap
                // handler). Their hit polygons (FunnyCatPaths.detailNHitPolygon, not
                // .toHitPolygon()) are deliberately the smallest of their several disjoint
                // subpaths — see the doc comment on FunnyCatPaths for why .toHitPolygon()
                // itself isn't safe to use here. detail7 (nose/mouth) has no solid fill
                // beneath it, so it's left tappable normally.
                RegionSpec("outline", FunnyCatPaths.outline, FunnyCatPaths.outlineHitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail1", FunnyCatPaths.detail1, FunnyCatPaths.detail1HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail2", FunnyCatPaths.detail2, FunnyCatPaths.detail2HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail3", FunnyCatPaths.detail3, FunnyCatPaths.detail3HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail4", FunnyCatPaths.detail4, FunnyCatPaths.detail4HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail5", FunnyCatPaths.detail5, FunnyCatPaths.detail5HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail6", FunnyCatPaths.detail6, FunnyCatPaths.detail6HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail7", FunnyCatPaths.detail7, FunnyCatPaths.detail7HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail8", FunnyCatPaths.detail8, FunnyCatPaths.detail8HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
            ),
        ),
        Template(
            id = "elephant",
            name = "Elephant",
            accent = CalmPalette.SkyLight,
            viewBoxWidth = 320f,
            viewBoxHeight = 320f,
            regions = listOf(
                RegionSpec("background", ElephantPaths.background, ElephantPaths.background.toHitPolygon()),
                RegionSpec("body", ElephantPaths.body, ElephantPaths.body.toHitPolygon()),
                RegionSpec("eye", ElephantPaths.eye, ElephantPaths.eye.toHitPolygon()),
                // outline is potrace's connected ink-stroke network (ears/head/trunk/body/legs/
                // tail boundary + the ear-fold crease) — drawn on top of the solid `body` fill
                // covering the same area, so isDecorative=true (see FunnyCat precedent: without
                // it, a tap on the ink would recolor a chunk of outline instead of filling body).
                RegionSpec("outline", ElephantPaths.outline, ElephantPaths.outlineHitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                // detail1 (ear-fold crease line), detail2 (eyebrow), detail4..9 (mouth crease +
                // leg/toe wrinkle marks) have no dedicated solid counterpart, so they stay
                // tappable normally — same treatment as FunnyCat's detail7 (nose/mouth).
                RegionSpec("detail1", ElephantPaths.detail1, ElephantPaths.detail1HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail2", ElephantPaths.detail2, ElephantPaths.detail2HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                // detail3 is the eye's own ink (already visually a solid dot + highlight hole),
                // redundant with the dedicated `eye` solid above — isDecorative=true so tapping
                // the eye always resolves to the `eye` region, not this duplicate ink.
                RegionSpec("detail3", ElephantPaths.detail3, ElephantPaths.detail3HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true, isDecorative = true),
                RegionSpec("detail4", ElephantPaths.detail4, ElephantPaths.detail4HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail5", ElephantPaths.detail5, ElephantPaths.detail5HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail6", ElephantPaths.detail6, ElephantPaths.detail6HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail7", ElephantPaths.detail7, ElephantPaths.detail7HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail8", ElephantPaths.detail8, ElephantPaths.detail8HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
                RegionSpec("detail9", ElephantPaths.detail9, ElephantPaths.detail9HitPolygon, strokeWidth = 0f, fillsWithOutlineByDefault = true),
            ),
        ),
    )

    fun byId(id: String): Template = all.first { it.id == id }
}
