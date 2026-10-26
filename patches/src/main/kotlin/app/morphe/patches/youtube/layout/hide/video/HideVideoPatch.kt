/*
 * Hide video patch, built on top of Morphe patches (GPLv3).
 * See the included LICENSE and NOTICE files for the terms that apply to this code.
 */

package app.morphe.patches.youtube.layout.hide.video

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.layout.player.fullscreen.YouTubePlayerViewOnLayoutFingerprint
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.playertype.playerTypeHookPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.util.addInstructionsAtControlFlowLabel

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/HideVideoPatch;"

@Suppress("unused")
val hideVideoPatch = bytecodePatch(
    name = "Hide video",
    description = "Adds an option to hide the video picture in the player and show a black screen. " +
            "Audio keeps playing.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        playerTypeHookPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        PreferenceScreen.VIDEO.addPreferences(
            SwitchPreference("morphe_hide_video", summary = true)
        )

        // Reuses the existing player view onLayout fingerprint.
        YouTubePlayerViewOnLayoutFingerprint.let {
            it.method.addInstructionsAtControlFlowLabel(
                it.instructionMatches.first().index,
                "invoke-static { p0 }, $EXTENSION_CLASS->" +
                        "onPlayerViewLayout(Landroid/view/View;)V"
            )
        }
    }
}
