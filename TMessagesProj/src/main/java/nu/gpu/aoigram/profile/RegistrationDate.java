package nu.gpu.aoigram.profile;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.UserInfoCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stories.recorder.HintView2;

import java.util.Arrays;
import java.util.Calendar;

public final class RegistrationDate {

    private static final long COARSE_ID = 5_000_000_000L;
    private static final int COARSE_MONTHS = 4;

    private static final long[] IDS = {
            1000000L, 2768409L, 7679610L, 11538514L, 15835244L, 23646077L,
            38015510L, 44634663L, 46145305L, 54845238L, 63263518L, 101260938L,
            101323197L, 103151531L, 103258382L, 109393468L, 111220210L, 112594714L,
            122600695L, 124872445L, 125828524L, 130029930L, 133909606L, 143445125L,
            148670295L, 157242073L, 171295414L, 181783990L, 222021233L, 225034354L,
            278941742L, 285253072L, 294851037L, 297621225L, 328594461L, 337808429L,
            341546272L, 352940995L, 369669043L, 400169472L, 616816630L, 681896077L,
            727572658L, 796147074L, 805158066L, 925078064L, 928636984L, 1054883348L,
            1057704545L, 1145856008L, 1227964864L, 1382531194L, 1658586909L, 1660971491L,
            1692464211L, 1719536397L, 1787945512L, 1807942741L, 1893429550L, 1972424006L,
            1974255900L, 4317845111L, 5162494923L, 5186883095L, 5304951856L, 5362593868L,
            5387234031L, 5396587273L, 5409444610L, 5416026704L, 5591759222L, 5608562550L,
            5772670706L, 5778063231L, 5795034000L, 5802242180L, 5853442730L, 5885964106L,
            5982648124L, 6020888206L, 6032606998L, 6057123350L, 6058560984L, 6108011341L,
            6132325730L, 6227468000L, 6279839148L, 6306077724L, 6321562426L, 6364973680L,
            6527226055L, 6813121418L, 6865576492L, 6925870357L, 7000000000L, 7100000000L,
            7103700000L, 7107400000L, 7229898489L, 7398600000L, 7423300000L, 7444300000L,
            7469400000L, 7469700000L, 7533900000L, 7583599300L, 7857659678L, 7884373548L,
            7939700000L, 7947063900L, 8102800000L, 8174700000L, 8178600000L, 8235679900L,
            8454563873L, 8461412540L
    };

    private static final int[] DATES = {
            1380326400, 1383264000, 1388448000, 1391212000, 1392940000, 1393459000, 1393632000, 1399334000,
            1400198000, 1411257000, 1414454000, 1425600000, 1426204000, 1433073500, 1433073500, 1434326000,
            1434326000, 1438387000, 1438387000, 1439856000, 1442663500, 1442663500, 1444176000, 1448928000,
            1450799666, 1450799666, 1457481000, 1460246000, 1465344000, 1466208000, 1473465000, 1476835000,
            1479600000, 1481846000, 1482969000, 1487707000, 1487782000, 1487894000, 1490918000, 1501459000,
            1529625600, 1532821500, 1542540300, 1542540300, 1563208000, 1563290000, 1581513420, 1583034030,
            1583034030, 1586342040, 1596127860, 1600188120, 1613148540, 1613329440, 1615402500, 1618462626,
            1618462626, 1623780150, 1623780150, 1631669400, 1631952294, 1631952294, 1650607080, 1650607080,
            1652057620, 1652057620, 1655076250, 1655076250, 1659025020, 1659414306, 1659414306, 1662451545,
            1662451545, 1664777220, 1664777220, 1671821040, 1673021900, 1673021900, 1681238250, 1681238250,
            1681598495, 1681598495, 1684923600, 1684923600, 1686391660, 1686391660, 1688399160, 1690464840,
            1690464840, 1692604515, 1692604515, 1698489600, 1699052400, 1701192327, 1711889200, 1717045200,
            1717045200, 1718272800, 1723041600, 1723041600, 1723716000, 1723975200, 1725098400, 1726221600,
            1726480800, 1731690900, 1731690900, 1732233600, 1732791600, 1740072600, 1740072600, 1740567600,
            1740999600, 1758758400, 1760304000, 1760304000
    };

    public static HintView2 showHint(ViewGroup container, View anchor, int currentAccount, long userId, long chatId, Theme.ResourcesProvider resourcesProvider) {
        HintView2 hint = new HintView2(container.getContext(), HintView2.DIRECTION_TOP)
                .setMultilineText(true)
                .setDuration(5000)
                .setRounding(12)
                .setBgColor(Theme.getColor(Theme.key_undo_background, resourcesProvider))
                .setTextColor(Theme.getColor(Theme.key_undo_infoColor, resourcesProvider));
        hint.setText(userId != 0 ? getText(currentAccount, userId) : getChatText(currentAccount, chatId));
        hint.setMaxWidthPx(HintView2.cutInFancyHalf(hint.getText(), hint.getTextPaint()));
        hint.setOnHiddenListener(() -> container.removeView(hint));
        container.addView(hint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 120, Gravity.TOP, 16, 0, 16, 0));
        int[] anchorLocation = new int[2];
        int[] containerLocation = new int[2];
        anchor.getLocationInWindow(anchorLocation);
        container.getLocationInWindow(containerLocation);
        hint.setTranslationY(anchorLocation[1] - containerLocation[1] + anchor.getHeight());
        hint.setJointPx(0, anchorLocation[0] - containerLocation[0] - dp(16) + anchor.getWidth() / 2f);
        hint.show();
        return hint;
    }

    private static CharSequence getText(int currentAccount, long userId) {
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        TLRPC.User user = messagesController.getUser(userId);
        boolean self = UserObject.isUserSelf(user);
        String name = UserObject.getUserName(user);
        TLRPC.PeerSettings settings = messagesController.getPeerSettings(userId);
        String text;
        if (settings != null && settings.registration_month != null) {
            text = formatString(self ? R.string.RegistrationDateExactSelf : R.string.RegistrationDateExact, name, UserInfoCell.displayDate(settings.registration_month));
        } else if (userId > IDS[IDS.length - 1]) {
            text = formatString(self ? R.string.RegistrationDateAfterSelf : R.string.RegistrationDateAfter, name, formatMonth(DATES[DATES.length - 1], 0));
        } else if (userId < COARSE_ID) {
            text = formatString(self ? R.string.RegistrationDateApproxSelf : R.string.RegistrationDateApprox, name, formatMonth(estimate(userId), 0));
        } else {
            long date = estimate(userId);
            text = formatString(self ? R.string.RegistrationDateRangeSelf : R.string.RegistrationDateRange, name, formatMonth(date, -COARSE_MONTHS), formatMonth(date, COARSE_MONTHS));
        }
        return AndroidUtilities.replaceTags(text);
    }

    private static CharSequence getChatText(int currentAccount, long chatId) {
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        int res = ChatObject.isChannel(chat) && ChatObject.isInChat(chat) && !chat.creator ? R.string.ChatJoinDate : R.string.ChatCreationDate;
        return AndroidUtilities.replaceTags(formatString(res, chat.title, LocaleController.formatDateChat(chat.date)));
    }

    private static long estimate(long userId) {
        int index = Arrays.binarySearch(IDS, userId);
        if (index >= 0) {
            return DATES[index];
        }
        index = -index - 1;
        if (index == 0) {
            return DATES[0];
        }
        double progress = (double) (userId - IDS[index - 1]) / (IDS[index] - IDS[index - 1]);
        return DATES[index - 1] + Math.round(progress * (DATES[index] - DATES[index - 1]));
    }

    private static String formatMonth(long date, int monthOffset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date * 1000);
        calendar.add(Calendar.MONTH, monthOffset);
        return LocaleController.formatYearMont(calendar.getTimeInMillis() / 1000, true);
    }
}
