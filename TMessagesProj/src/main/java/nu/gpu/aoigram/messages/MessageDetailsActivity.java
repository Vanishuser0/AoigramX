package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.media.AudioFormat;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.text.Layout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.exifinterface.media.ExifInterface;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.GradientClip;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.recyclerview.ChatListItemAnimator;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.profile.PeerIdDc;
import nu.gpu.aoigram.settings.BaseAoiSettingsActivity;

public final class MessageDetailsActivity extends BaseAoiSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private final MessageObject message;
    private final ArrayList<MessageObject> album;
    private final int albumIndex;
    private final TLRPC.Document document;
    private final TLRPC.Photo photo;
    private final TLRPC.PhotoSize photoSize;
    private final boolean protectedContent;
    private final FlagSecureReason flagSecure;
    private final boolean isImage;
    private final boolean canInspectMedia;
    private final String downloadName;
    private final HashSet<String> downloadNames = new HashSet<>();
    private NotificationCenter.ObserversGroup observersGroup;
    private File localFile;
    private long localFileSize;
    private long localDocumentId;
    private MediaDetails mediaDetails;
    private boolean loading;
    private boolean reloadAfterDownload;
    private boolean checkedFile;
    private boolean textExpanded;
    private String language;

    public MessageDetailsActivity(BaseFragment parent, MessageObject message, MessageObject.GroupedMessages group, boolean protectedContent) {
        this(parent, message, group == null ? null : new ArrayList<>(group.messages), protectedContent);
    }

    private MessageDetailsActivity(BaseFragment parent, MessageObject message, ArrayList<MessageObject> album, boolean protectedContent) {
        this.message = message;
        this.album = album;
        int index = -1;
        for (int i = 0; album != null && i < album.size(); i++) {
            if (album.get(i).getId() == message.getId()) {
                index = i;
            }
        }
        albumIndex = index;
        this.protectedContent = protectedContent;
        setCurrentAccount(parent.getCurrentAccount());
        setResourceProvider(parent.getResourceProvider());
        flagSecure = new FlagSecureReason(parent.getParentActivity().getWindow(), () -> protectedContent && !AoiConfig.allowScreenshots.get());
        document = MessageObject.getDocument(message.messageOwner);
        photo = message.getPhoto();
        photoSize = photo == null ? null : FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(true), false, null, true);
        String mime = document == null || document.mime_type == null ? "" : document.mime_type;
        boolean hasImageSize = false;
        boolean hasAudioVideo = mime.startsWith("audio/") || mime.startsWith("video/");
        if (document != null) {
            for (TLRPC.DocumentAttribute attribute : document.attributes) {
                hasImageSize |= attribute instanceof TLRPC.TL_documentAttributeImageSize;
                hasAudioVideo |= attribute instanceof TLRPC.TL_documentAttributeAudio || attribute instanceof TLRPC.TL_documentAttributeVideo;
            }
        }
        isImage = document == null ? photo != null : mime.startsWith("image/") || hasImageSize && !hasAudioVideo;
        canInspectMedia = isImage || hasAudioVideo;
        downloadName = document != null ? FileLoader.getAttachFileName(document) : photoSize != null ? FileLoader.getAttachFileName(photoSize) : null;
        if (downloadName != null) {
            downloadNames.add(downloadName);
        }
        TLRPC.MessageMedia media = MessageObject.getMedia(message.messageOwner);
        if (media != null) {
            for (TLRPC.Document alternative : media.alt_documents) {
                downloadNames.add(FileLoader.getAttachFileName(alternative));
            }
        }
    }

    @Override
    public boolean onFragmentCreate() {
        observersGroup = getNotificationCenter().createObserversGroup(this).add(NotificationCenter.fileLoaded);
        language = message.messageOwner.originalLanguage;
        if (language == null && !TextUtils.isEmpty(message.messageOwner.message)) {
            LanguageDetector.detectLanguage(message.messageOwner.message, result -> {
                language = result;
                if (!isFinished) {
                    listView.adapter.update(true);
                }
            }, null);
        }
        return super.onFragmentCreate();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMedia();
    }

    @Override
    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        flagSecure.attach();
    }

    @Override
    public void onBecomeFullyHidden() {
        flagSecure.detach();
        super.onBecomeFullyHidden();
    }

    @Override
    public void onFragmentDestroy() {
        observersGroup.removeAllObservers();
        flagSecure.detach();
        super.onFragmentDestroy();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoaded && downloadNames.contains((String) args[0])) {
            reloadAfterDownload = true;
            loadMedia();
        }
    }

    private void loadMedia() {
        if (loading || localFile != null && !reloadAfterDownload || document == null && photoSize == null) {
            return;
        }
        loading = true;
        reloadAfterDownload = false;
        Utilities.globalQueue.postRunnable(() -> {
            TLRPC.MessageMedia media = MessageObject.getMedia(message.messageOwner);
            TLObject attachment = document != null ? document : photoSize;
            File file = TextUtils.isEmpty(message.messageOwner.attachPath) ? null : new File(message.messageOwner.attachPath);
            if (file == null || !file.isFile()) {
                boolean forceCache = media != null && media.ttl_seconds != 0;
                file = getFileLoader().getPathToAttach(attachment, forceCache);
                if (!file.isFile() && !forceCache) {
                    file = getFileLoader().getPathToAttach(attachment, true);
                }
            }
            boolean encrypted = false;
            if (!file.isFile()) {
                File encryptedFile = new File(file.getAbsolutePath() + ".enc");
                if (encryptedFile.isFile()) {
                    file = encryptedFile;
                    encrypted = true;
                }
            }
            long documentId = document == null ? 0 : document.id;
            if (!file.isFile() && media != null && !media.alt_documents.isEmpty()) {
                VideoPlayer.VideoUri cached = VideoPlayer.getCachedQuality(VideoPlayer.getQualities(currentAccount, media, true));
                if (cached != null) {
                    file = new File(Objects.requireNonNull(cached.uri.getPath()));
                    documentId = cached.document.id;
                }
            }
            File resultFile = file.isFile() ? file : null;
            long resultSize = resultFile == null ? 0 : resultFile.length();
            long resultDocumentId = documentId;
            MediaDetails result = resultFile != null && canInspectMedia ? MediaDetails.read(resultFile, isImage, encrypted) : null;
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinished) {
                    return;
                }
                loading = false;
                if (reloadAfterDownload) {
                    loadMedia();
                    return;
                }
                localFile = resultFile;
                localFileSize = resultSize;
                localDocumentId = resultDocumentId;
                mediaDetails = result;
                checkedFile = true;
                listView.adapter.update(true);
            });
        });
    }

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.MessageDetails);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(R.string.ViewAsJson, R.drawable.msg_stories_caption, getString(R.string.ViewAsJson)).accent());
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.Message)));
        TLRPC.Message owner = message.messageOwner;
        add(items, R.string.MessageDetailsId, Integer.toString(owner.id));
        addPeer(items, R.string.MessageDetailsChat, message.getDialogId());
        addPeer(items, R.string.MessageDetailsSender, DialogObject.getPeerDialogId(owner.from_id != null ? owner.from_id : owner.post ? owner.peer_id : null));
        add(items, R.string.MessageDetailsAuthor, owner.post_author);
        addDate(items, R.string.MessageDetailsSent, owner.date);
        addDate(items, R.string.MessageDetailsEdited, owner.edit_date);
        addPeer(items, R.string.MessageDetailsViaBot, owner.via_bot_id);
        if (owner.reply_to != null && owner.reply_to.reply_to_msg_id != 0) {
            add(items, R.string.MessageDetailsReplyId, Integer.toString(owner.reply_to.reply_to_msg_id));
        }
        if (ChatObject.isForum(currentAccount, message.getDialogId()) || owner.reply_to != null && owner.reply_to.forum_topic) {
            addPositive(items, R.string.MessageDetailsTopicId, message.getTopicId());
        }
        if (owner.grouped_id != 0) {
            add(items, R.string.MessageDetailsAlbumId, Long.toString(owner.grouped_id));
            if (album != null) {
                add(items, R.string.Album, albumLabel(albumIndex));
            }
        }
        addPositive(items, R.string.MessageDetailsViews, owner.views);
        addPositive(items, R.string.MessageDetailsForwards, owner.forwards);
        if (owner.ttl_period > 0) {
            add(items, R.string.MessageDetailsAutoDelete, LocaleController.formatTTLString(owner.ttl_period));
        }
        items.add(UItem.asShadow(null));

        if (owner.fwd_from != null) {
            TLRPC.MessageFwdHeader forward = owner.fwd_from;
            items.add(UItem.asHeader(getString(R.string.ForwardedFrom)));
            if (forward.from_id != null) {
                addPeer(items, R.string.MessageDetailsSender, DialogObject.getPeerDialogId(forward.from_id));
            } else {
                add(items, R.string.MessageDetailsSender, forward.from_name);
            }
            add(items, R.string.MessageDetailsAuthor, forward.post_author);
            addDate(items, R.string.MessageDetailsSent, forward.date);
            addPositive(items, R.string.MessageDetailsId, forward.channel_post);
            addPeer(items, R.string.MessageDetailsOriginalChat, DialogObject.getPeerDialogId(forward.saved_from_peer));
            addPositive(items, R.string.MessageDetailsOriginalId, forward.saved_from_msg_id);
            items.add(UItem.asShadow(null));
        }

        if (!TextUtils.isEmpty(owner.message)) {
            UItem text = DetailFactory.of(R.string.MessageDetailsText, getString(message.caption != null ? R.string.Caption : R.string.MessageDetailsText), owner.message);
            items.add(text.setChecked(textExpanded));
            String languageName = TranslateAlert2.languageNameCapital(language);
            if (languageName != null) {
                add(items, R.string.Language, languageName + " (" + language + ")");
            }
            items.add(UItem.asShadow(null));
        }

        if (document != null || photo != null) {
            fillFileItems(items);
        }
        if (localFile != null) {
            fillLocalFileItems(items);
        }
    }

    private void fillFileItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.MessageDetailsFile)));
        String fileName = document != null ? FileLoader.getDocumentFileName(document) : downloadName;
        add(items, R.string.MessageDetailsFileName, TextUtils.isEmpty(fileName) ? downloadName : fileName);
        add(items, R.string.MessageDetailsMediaId, Long.toString(document != null ? document.id : photo.id));
        int dc = document != null ? document.dc_id : photo.dc_id;
        if (dc > 0) {
            add(items, R.string.MessageDetailsDc, PeerIdDc.formatDc(dc));
        }
        addDate(items, R.string.MessageDetailsCreated, document != null ? document.date : photo.date);
        long size = document != null ? document.size : photoSize == null ? 0 : photoSize.size;
        if (size > 0) {
            add(items, R.string.MessageDetailsFileSize, formatSize(size));
        }
        if (document != null) {
            add(items, R.string.MessageDetailsMime, document.mime_type);
            TLRPC.InputStickerSet stickerSet = MessageObject.getInputStickerSet(document);
            long stickerSetId = stickerSet == null ? 0 : stickerSet.id;
            if (stickerSet instanceof TLRPC.TL_inputStickerSetShortName) {
                TLRPC.TL_messages_stickerSet cachedSet = getMediaDataController().getStickerSetByName(stickerSet.short_name);
                stickerSetId = cachedSet == null ? 0 : cachedSet.set.id;
            }
            if (stickerSetId != 0) {
                long creatorId = stickerSetId >> 32;
                if (((stickerSetId >> 16) & 0xff) == 0x3f) {
                    creatorId |= 0x80000000L;
                }
                if (((stickerSetId >> 24) & 0xff) != 0) {
                    creatorId += 0x100000000L;
                }
                if (creatorId > 0) {
                    addPeer(items, R.string.MessageDetailsStickerCreator, creatorId);
                }
            }
            for (TLRPC.DocumentAttribute attribute : document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeVideo || attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                    addDimensions(items, attribute.w, attribute.h);
                }
                if (attribute instanceof TLRPC.TL_documentAttributeVideo || attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    addDuration(items, attribute.duration);
                }
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    add(items, R.string.MessageDetailsCodec, attribute.video_codec);
                } else if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    add(items, R.string.MessageDetailsTitle, attribute.title);
                    add(items, R.string.MessageDetailsArtist, attribute.performer);
                }
            }
        } else if (photoSize != null) {
            add(items, R.string.MessageDetailsMime, "image/jpeg");
            addDimensions(items, photoSize.w, photoSize.h);
        }
        items.add(UItem.asShadow(downloadName == null ? null : !checkedFile || loading ? getString(R.string.MessageDetailsReading) : localFile == null ? getString(R.string.MessageDetailsNotDownloaded) : null));
    }

    @SuppressLint("InlinedApi")
    private void fillLocalFileItems(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.MessageDetailsLocalMedia)));
        add(items, R.string.MessageDetailsPath, localFile.getAbsolutePath());
        add(items, R.string.MessageDetailsFileSize, formatSize(localFileSize));
        if (document != null && localDocumentId != document.id) {
            add(items, R.string.MessageDetailsMediaId, Long.toString(localDocumentId));
        }
        if (mediaDetails == null) {
            items.add(UItem.asShadow(null));
            return;
        }
        if (isImage) {
            fillImageItems(items);
            return;
        }
        addMetadata(items, R.string.MessageDetailsMime, MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
        double duration = metadataNumber(MediaMetadataRetriever.METADATA_KEY_DURATION) / 1000.0;
        addDuration(items, duration);
        addBitrate(items, R.string.MessageDetailsTotalBitrate, metadataNumber(MediaMetadataRetriever.METADATA_KEY_BITRATE));
        addDimensions(items, (int) metadataNumber(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH), (int) metadataNumber(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        addMetadata(items, R.string.MessageDetailsRotation, MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION, "°");
        double frameCount = metadataNumber(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
        addPositive(items, R.string.MessageDetailsFrameCount, (long) frameCount);
        if (frameCount > 0 && duration > 0) {
            add(items, R.string.MessageDetailsAverageFrameRate, number(frameCount / duration) + " fps");
        }
        addMetadata(items, R.string.MessageDetailsCaptureFrameRate, MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE, " fps");
        addMetadata(items, R.string.MessageDetailsBitDepth, MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE, " bit");
        addMetadata(items, R.string.MessageDetailsTitle, MediaMetadataRetriever.METADATA_KEY_TITLE);
        addMetadata(items, R.string.MessageDetailsArtist, MediaMetadataRetriever.METADATA_KEY_ARTIST);
        addMetadata(items, R.string.MessageDetailsAlbum, MediaMetadataRetriever.METADATA_KEY_ALBUM);
        addMetadata(items, R.string.MessageDetailsAlbumArtist, MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
        addMetadata(items, R.string.MessageDetailsComposer, MediaMetadataRetriever.METADATA_KEY_COMPOSER);
        addMetadata(items, R.string.MessageDetailsGenre, MediaMetadataRetriever.METADATA_KEY_GENRE);
        addMetadata(items, R.string.MessageDetailsYear, MediaMetadataRetriever.METADATA_KEY_YEAR);
        addMetadata(items, R.string.MessageDetailsTrackNumber, MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
        items.add(UItem.asShadow(mediaDetails.metadata.size() == 0 && mediaDetails.tracks.isEmpty() ? getString(R.string.MessageDetailsUnsupported) : null));

        for (int i = 0; i < mediaDetails.tracks.size(); i++) {
            MediaFormat track = mediaDetails.tracks.get(i);
            String mime = track.getString(MediaFormat.KEY_MIME, "");
            int title = mime.startsWith("video/") ? R.string.MessageDetailsVideoTrack : mime.startsWith("audio/") ? R.string.MessageDetailsAudioTrack : R.string.MessageDetailsTrack;
            items.add(UItem.asHeader(LocaleController.formatString(title, i + 1)));
            add(items, R.string.MessageDetailsCodec, codecName(mime));
            add(items, R.string.MessageDetailsMime, mime);
            add(items, R.string.Language, track.getString(MediaFormat.KEY_LANGUAGE));
            addDuration(items, track.getNumber(MediaFormat.KEY_DURATION, 0).doubleValue() / 1_000_000.0);
            addBitrate(items, R.string.MessageDetailsBitrate, track.getNumber(MediaFormat.KEY_BIT_RATE, 0).doubleValue());
            addBitrate(items, R.string.MessageDetailsMaxBitrate, track.getNumber("max-bitrate", 0).doubleValue());
            addDimensions(items, track.getInteger(MediaFormat.KEY_WIDTH, 0), track.getInteger(MediaFormat.KEY_HEIGHT, 0));
            addTrackNumber(items, R.string.MessageDetailsFrameRate, track, MediaFormat.KEY_FRAME_RATE, " fps");
            addTrackNumber(items, R.string.MessageDetailsRotation, track, MediaFormat.KEY_ROTATION, "°");
            addTrackNumber(items, R.string.MessageDetailsSampleRate, track, MediaFormat.KEY_SAMPLE_RATE, " Hz");
            addTrackNumber(items, R.string.MessageDetailsChannels, track, MediaFormat.KEY_CHANNEL_COUNT, "");
            int pcm = track.getInteger(MediaFormat.KEY_PCM_ENCODING, 0);
            int bits = switch (pcm) {
                case AudioFormat.ENCODING_PCM_8BIT -> 8;
                case AudioFormat.ENCODING_PCM_16BIT -> 16;
                case AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24;
                case AudioFormat.ENCODING_PCM_32BIT, AudioFormat.ENCODING_PCM_FLOAT -> 32;
                default -> 0;
            };
            if (bits > 0) {
                add(items, R.string.MessageDetailsBitDepth, bits + " bit" + (pcm == AudioFormat.ENCODING_PCM_FLOAT ? " (float)" : ""));
            }
            add(items, R.string.MessageDetailsColorStandard, switch (track.getInteger(MediaFormat.KEY_COLOR_STANDARD, 0)) {
                case MediaFormat.COLOR_STANDARD_BT709 -> "BT.709";
                case MediaFormat.COLOR_STANDARD_BT601_PAL -> "BT.601 PAL";
                case MediaFormat.COLOR_STANDARD_BT601_NTSC -> "BT.601 NTSC";
                case MediaFormat.COLOR_STANDARD_BT2020 -> "BT.2020";
                default -> null;
            });
            add(items, R.string.MessageDetailsColorTransfer, switch (track.getInteger(MediaFormat.KEY_COLOR_TRANSFER, 0)) {
                case MediaFormat.COLOR_TRANSFER_LINEAR -> getString(R.string.MessageDetailsLinear);
                case MediaFormat.COLOR_TRANSFER_SDR_VIDEO -> "SDR";
                case MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR (PQ / ST 2084)";
                case MediaFormat.COLOR_TRANSFER_HLG -> "HDR (HLG)";
                default -> null;
            });
            add(items, R.string.MessageDetailsColorRange, switch (track.getInteger(MediaFormat.KEY_COLOR_RANGE, 0)) {
                case MediaFormat.COLOR_RANGE_FULL -> getString(R.string.MessageDetailsFullRange);
                case MediaFormat.COLOR_RANGE_LIMITED -> getString(R.string.MessageDetailsLimitedRange);
                default -> null;
            });
            items.add(UItem.asShadow(null));
        }
    }

    private void fillImageItems(ArrayList<UItem> items) {
        add(items, R.string.MessageDetailsMime, mediaDetails.image.outMimeType);
        addDimensions(items, mediaDetails.image.outWidth, mediaDetails.image.outHeight);
        if (mediaDetails.image.outColorSpace != null) {
            add(items, R.string.MessageDetailsColorSpace, mediaDetails.image.outColorSpace.getName());
        }
        ExifInterface exif = mediaDetails.exif;
        if (exif != null) {
            add(items, R.string.MessageDetailsCamera, exif.getAttribute(ExifInterface.TAG_MAKE));
            add(items, R.string.MessageDetailsCameraModel, exif.getAttribute(ExifInterface.TAG_MODEL));
            add(items, R.string.MessageDetailsLens, exif.getAttribute(ExifInterface.TAG_LENS_MODEL));
            add(items, R.string.MessageDetailsDateTaken, exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
            add(items, R.string.MessageDetailsSoftware, exif.getAttribute(ExifInterface.TAG_SOFTWARE));
            add(items, R.string.MessageDetailsArtist, exif.getAttribute(ExifInterface.TAG_ARTIST));
            add(items, R.string.MessageDetailsCopyright, exif.getAttribute(ExifInterface.TAG_COPYRIGHT));
            add(items, R.string.MessageDetailsIso, exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY));
            double exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0);
            if (exposure > 0) {
                add(items, R.string.MessageDetailsExposure, (exposure < 1 ? "1/" + number(1 / exposure) : number(exposure)) + " s");
            }
            double aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0);
            if (aperture > 0) {
                add(items, R.string.MessageDetailsAperture, "ƒ/" + number(aperture));
            }
            double focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0);
            if (focalLength > 0) {
                add(items, R.string.MessageDetailsFocalLength, number(focalLength) + " mm");
            }
            if (exif.hasAttribute(ExifInterface.TAG_ORIENTATION)) {
                add(items, R.string.MessageDetailsRotation, exif.getRotationDegrees() + "°");
            }
            double[] location = exif.getLatLong();
            if (location != null) {
                add(items, R.string.MessageDetailsCoordinates, location[0] + ", " + location[1]);
            }
        }
        items.add(UItem.asShadow(mediaDetails.image.outWidth <= 0 ? getString(R.string.MessageDetailsUnsupported) : null));
    }

    private String albumLabel(int index) {
        String label = (index + 1) + "/" + album.size();
        return TextUtils.isEmpty(album.get(index).messageOwner.message) ? label : label + " · " + getString(R.string.Caption);
    }

    private void addPeer(ArrayList<UItem> items, int title, long peerId) {
        if (peerId == 0) {
            return;
        }
        String name = null;
        String username = null;
        if (DialogObject.isUserDialog(peerId)) {
            TLRPC.User user = getMessagesController().getUser(peerId);
            if (user != null) {
                name = UserObject.getUserName(user);
                username = UserObject.getPublicUsername(user);
            }
        } else if (DialogObject.isChatDialog(peerId)) {
            TLRPC.Chat chat = getMessagesController().getChat(-peerId);
            if (chat != null) {
                name = chat.title;
                username = ChatObject.getPublicUsername(chat);
            }
        }
        String value = (TextUtils.isEmpty(name) ? "" : name + "\n") + (TextUtils.isEmpty(username) ? "" : "@" + username + "\n") + peerId;
        UItem item = DetailFactory.of(title, getString(title), value);
        item.dialogId = name == null ? 0 : peerId;
        items.add(item);
    }

    private static void add(ArrayList<UItem> items, int title, CharSequence value) {
        if (!TextUtils.isEmpty(value)) {
            items.add(DetailFactory.of(title, getString(title), value));
        }
    }

    private static void addPositive(ArrayList<UItem> items, int title, long value) {
        if (value > 0) {
            add(items, title, Long.toString(value));
        }
    }

    private static void addDate(ArrayList<UItem> items, int title, long date) {
        if (date > 0) {
            LocaleController locale = LocaleController.getInstance();
            add(items, title, locale.getFormatterYearMax().format(date * 1000) + " " + locale.getFormatterDayWithSeconds().format(date * 1000));
        }
    }

    private static void addDimensions(ArrayList<UItem> items, int width, int height) {
        if (width > 0 && height > 0) {
            add(items, R.string.MessageDetailsDimensions, width + " × " + height + " px");
        }
    }

    private static void addDuration(ArrayList<UItem> items, double seconds) {
        if (seconds > 0) {
            add(items, R.string.MessageDetailsDuration, AndroidUtilities.formatShortDuration((int) seconds) + " (" + number(seconds) + " s)");
        }
    }

    private static void addBitrate(ArrayList<UItem> items, int title, double bitrate) {
        if (bitrate > 0) {
            add(items, title, number(bitrate / 1000.0) + " kb/s");
        }
    }

    private void addMetadata(ArrayList<UItem> items, int title, int key) {
        addMetadata(items, title, key, "");
    }

    private void addMetadata(ArrayList<UItem> items, int title, int key, String unit) {
        String value = mediaDetails.metadata.get(key);
        if (!TextUtils.isEmpty(value)) {
            add(items, title, value + unit);
        }
    }

    private double metadataNumber(int key) {
        String value = mediaDetails.metadata.get(key);
        return value == null ? 0 : Double.parseDouble(value);
    }

    private static void addTrackNumber(ArrayList<UItem> items, int title, MediaFormat track, String key, String unit) {
        Number value = track.getNumber(key);
        if (value != null) {
            add(items, title, number(value.doubleValue()) + unit);
        }
    }

    private static String number(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(LocaleController.getInstance().getCurrentLocale());
        format.setMaximumFractionDigits(3);
        return format.format(value);
    }

    private static String formatSize(long size) {
        return AndroidUtilities.formatFileSize(size) + " (" + LocaleController.formatNumber(size, ',') + " B)";
    }

    private static String codecName(String mime) {
        return switch (mime) {
            case "video/avc" -> "H.264 (AVC)";
            case "video/hevc" -> "H.265 (HEVC)";
            case "video/x-vnd.on2.vp8" -> "VP8";
            case "video/x-vnd.on2.vp9" -> "VP9";
            case "video/av01" -> "AV1";
            case "video/mp4v-es" -> "MPEG-4 Visual";
            case "video/3gpp" -> "H.263";
            case "video/dolby-vision" -> "Dolby Vision";
            case "audio/mp4a-latm" -> "AAC";
            case "audio/mpeg" -> "MP3";
            case "audio/opus" -> "Opus";
            case "audio/vorbis" -> "Vorbis";
            case "audio/flac" -> "FLAC";
            case "audio/ac3" -> "Dolby Digital (AC-3)";
            case "audio/eac3" -> "Dolby Digital Plus (E-AC-3)";
            case "audio/raw" -> "PCM";
            case "audio/3gpp" -> "AMR-NB";
            case "audio/amr-wb" -> "AMR-WB";
            default -> null;
        };
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == R.string.ViewAsJson) {
            MessageJsonActivity.open(this, message.messageOwner, protectedContent);
        } else if (item.id == R.string.Album) {
            String[] labels = new String[album.size()];
            for (int i = 0; i < labels.length; i++) {
                labels[i] = album.get(i).getId() + " · " + albumLabel(i);
            }
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.Album), albumIndex, (dialog, which) -> {
                if (which != albumIndex) {
                    presentFragment(new MessageDetailsActivity(this, album.get(which), album, protectedContent), true);
                }
            }));
        } else if (item.id == R.string.MessageDetailsText && ((DetailCell) view).arrowView.getVisibility() == View.VISIBLE) {
            item.checked = textExpanded = !textExpanded;
            ((DetailCell) view).setExpanded(textExpanded, true);
        } else if (item.dialogId != 0) {
            presentFragment(ProfileActivity.of(item.dialogId));
        } else if (item.id == R.string.MessageDetailsPath && !localFile.getName().endsWith(".enc")) {
            shareFile();
        } else {
            copy(item);
        }
    }

    private void shareFile() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(document != null ? document.mime_type : "image/jpeg");
        try {
            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", localFile));
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (IllegalArgumentException e) {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(localFile));
        }
        getParentActivity().startActivityForResult(Intent.createChooser(intent, getString(R.string.ShareFile)), 500);
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        return copy(item);
    }

    private boolean copy(UItem item) {
        if (item.subtext == null) {
            return false;
        }
        AndroidUtilities.addToClipboard(item.subtext);
        BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
        return true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void updateCustomColors() {
        listView.adapter.notifyDataSetChanged();
    }

    private static final class DetailCell extends TextDetailCell {
        private final Theme.ResourcesProvider resourcesProvider;
        private final ImageView arrowView;
        private final GradientClip clip = new GradientClip();
        private ValueAnimator expandAnimator;
        private float expandProgress;
        private boolean text;

        DetailCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context, 21, resourcesProvider, false, true);
            this.resourcesProvider = resourcesProvider;
            arrowView = new ImageView(context);
            arrowView.setImageResource(R.drawable.arrow_more);
            addView(arrowView, LayoutHelper.createFrame(16, 16, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 21, 10));
        }

        void setExpanded(boolean expanded, boolean animated) {
            ViewCompat.setStateDescription(this, getString(expanded ? R.string.AccDescrExpanded : R.string.AccDescrCollapsed));
            if (expandAnimator != null) {
                expandAnimator.cancel();
            }
            if (!animated) {
                setExpandProgress(expanded ? 1 : 0);
                return;
            }
            expandAnimator = ValueAnimator.ofFloat(expandProgress, expanded ? 1 : 0);
            expandAnimator.addUpdateListener(animation -> setExpandProgress((float) animation.getAnimatedValue()));
            expandAnimator.setDuration(ChatListItemAnimator.DEFAULT_DURATION);
            expandAnimator.setInterpolator(ChatListItemAnimator.DEFAULT_INTERPOLATOR);
            expandAnimator.start();
        }

        private void setExpandProgress(float progress) {
            expandProgress = progress;
            arrowView.setRotation(180 * progress);
            requestLayout();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Layout layout = valueTextView.getLayout();
            boolean collapsible = text && layout.getLineCount() > 6;
            arrowView.setVisibility(collapsible ? VISIBLE : INVISIBLE);
            if (collapsible) {
                int hiddenHeight = layout.getHeight() - layout.getLineTop(6);
                setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() - (int) (hiddenHeight * (1 - expandProgress)));
            } else {
                ViewCompat.setStateDescription(this, null);
            }
        }

        @Override
        protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
            if (child != valueTextView || arrowView.getVisibility() != VISIBLE || expandProgress == 1) {
                return super.drawChild(canvas, child, drawingTime);
            }
            int bottom = getHeight() - dp(10);
            canvas.saveLayerAlpha(0, 0, getWidth(), bottom, 0xFF);
            boolean result = super.drawChild(canvas, child, drawingTime);
            AndroidUtilities.rectTmp.set(0, bottom - dp(24), getWidth(), bottom);
            clip.draw(canvas, AndroidUtilities.rectTmp, GradientClip.BOTTOM, 1 - expandProgress);
            canvas.restore();
            return result;
        }
    }

    public static final class DetailFactory extends UItem.UItemFactory<TextDetailCell> {
        static { setup(new DetailFactory()); }

        static UItem of(int id, CharSequence title, CharSequence value) {
            UItem item = UItem.ofFactory(DetailFactory.class);
            item.id = id;
            item.text = title;
            item.subtext = value;
            return item;
        }

        @Override
        public TextDetailCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new DetailCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            DetailCell cell = (DetailCell) view;
            boolean text = cell.text = item.id == R.string.MessageDetailsText;
            ((ViewGroup.MarginLayoutParams) cell.valueTextView.getLayoutParams()).rightMargin = dp(text ? 41 : 21);
            cell.setExpanded(item.checked, false);
            cell.setTextAndValue(item.text, item.subtext, divider);
            cell.updateColors();
            cell.arrowView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, cell.resourcesProvider));
        }

        @Override
        public boolean contentsEquals(UItem a, UItem b) {
            return a.checked == b.checked && TextUtils.equals(a.text, b.text) && TextUtils.equals(a.subtext, b.subtext);
        }
    }
}
