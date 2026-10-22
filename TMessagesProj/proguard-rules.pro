-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
-keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.telegram.utils.code.highlight.PrismaHighlighter {
    private static native <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# ml-kit
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
}

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

-keep class org.telegram.tgnet.** { *; }

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

-keepclassmembernames class org.telegram.ui.Cells.**,
    org.telegram.ui.Business.ChatAttachAlertQuickRepliesLayout$UserCell,
    org.telegram.ui.CallLogActivity$CallCell,
    org.telegram.ui.CallLogActivity$EmptyTextProgressView,
    org.telegram.ui.Charts.view_data.ChartHeaderView,
    org.telegram.ui.ChatLinkActivity$HintInnerCell,
    org.telegram.ui.Components.ChatActivityEnterView,
    org.telegram.ui.Components.ChatAttachAlertContactsLayout$UserCell,
    org.telegram.ui.Components.FilterTabsView,
    org.telegram.ui.Components.FragmentContextView,
    org.telegram.ui.Components.HintView,
    org.telegram.ui.Components.PollVotesAlert$SectionCell,
    org.telegram.ui.Components.PollVotesAlert$UserCell,
    org.telegram.ui.Components.ScrollSlidingTextTabStrip,
    org.telegram.ui.Components.SeekBarView,
    org.telegram.ui.Components.SharedMediaLayout$EmptyStubView,
    org.telegram.ui.Components.StickerEmptyView,
    org.telegram.ui.Components.StorageUsageView,
    org.telegram.ui.Components.UndoView,
    org.telegram.ui.FiltersSetupActivity$FilterCell,
    org.telegram.ui.FiltersSetupActivity$TextCell,
    org.telegram.ui.ManageLinksActivity$HintInnerCell,
    org.telegram.ui.ManageLinksActivity$LinkCell,
    org.telegram.ui.PassportActivity$SecureDocumentCell,
    org.telegram.ui.PassportActivity$TextDetailSecureCell,
    org.telegram.ui.ProxyListActivity$TextDetailProxyCell,
    org.telegram.ui.ThemeActivity$BubbleRadiusCell,
    org.telegram.ui.ThemeActivity$TextSizeCell,
    org.telegram.ui.bots.BotCommandsMenuView$BotCommandView { <fields>; }

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate