package com.shezik.drawanywhere.view.toolbar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PenNib24Px: ImageVector by lazy {
    ImageVector.Builder(
        name = "PenNib24Px",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 16.8f,
        viewportHeight = 16.8f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(14.830574f, 1.6120378f)
            curveTo(15.905474f, 2.6870377f, 15.905474f, 4.4299479f, 14.830574f, 5.5049376f)
            lineTo(14.334974f, 6.0004878f)
            curveTo(14.190674f, 5.9692478f, 14.008974f, 5.9216876f, 13.801874f, 5.8498378f)
            curveTo(13.247574f, 5.6575179f, 12.519074f, 5.2940679f, 11.833774f, 4.6087976f)
            curveTo(11.148474f, 3.9235377f, 10.785074f, 3.1950278f, 10.592774f, 2.6406977f)
            curveTo(10.520874f, 2.4336078f, 10.473374f, 2.2518878f, 10.442074f, 2.1075878f)
            lineTo(10.937674f, 1.6120378f)
            curveTo(12.012674f, 0.53704786f, 13.755574f, 0.53704786f, 14.830574f, 1.6120378f)
            close()
            moveTo(8.216873f, 12.118598f)
            curveTo(7.8128734f, 12.522598f, 7.610873f, 12.724598f, 7.3880734f, 12.898397f)
            curveTo(7.1253734f, 13.103298f, 6.8410735f, 13.278997f, 6.540273f, 13.422398f)
            curveTo(6.2852736f, 13.543897f, 6.0142736f, 13.634198f, 5.4721737f, 13.814898f)
            lineTo(2.6138637f, 14.767699f)
            curveTo(2.3471236f, 14.856599f, 2.0530338f, 14.787199f, 1.8542137f, 14.588399f)
            curveTo(1.6553937f, 14.3895f, 1.5859737f, 14.0955f, 1.6748837f, 13.8287f)
            lineTo(2.6276636f, 10.9704f)
            curveTo(2.8083436f, 10.4283f, 2.8986838f, 10.1573f, 3.0202136f, 9.9023f)
            curveTo(3.1635637f, 9.6015f, 3.3392637f, 9.3172f, 3.5442038f, 9.0545f)
            curveTo(3.7179737f, 8.8317f, 3.9199736f, 8.62974f, 4.3239737f, 8.22573f)
            lineTo(9.240073f, 3.30965f)
            curveTo(9.503773f, 4.00384f, 9.966073f, 4.86243f, 10.773074f, 5.66946f)
            curveTo(11.580174f, 6.47649f, 12.438774f, 6.93876f, 13.132974f, 7.20254f)
            close()
        }
    }.build()
}
