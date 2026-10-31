package nu.gpu.aoigram.utils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

public final class LocaleUtil {

    private static final String JAPANESE_BETA = "ja-beta";

    public static void resolveBetaLanguage(int account, String language, Utilities.Callback<LocaleController.LocaleInfo> callback) {
        if (!"ja".equals(language)) {
            return;
        }
        TLRPC.TL_langpack_getLanguage req = new TLRPC.TL_langpack_getLanguage();
        req.lang_pack = "android";
        req.lang_code = JAPANESE_BETA;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.TL_langPackLanguage pack) {
                AndroidUtilities.runOnUIThread(() -> callback.run(localeInfo(pack)));
            }
        }, ConnectionsManager.RequestFlagWithoutLogin);
    }

    private static LocaleController.LocaleInfo localeInfo(TLRPC.TL_langPackLanguage pack) {
        LocaleController.LocaleInfo info = new LocaleController.LocaleInfo();
        info.name = pack.native_name;
        info.nameEnglish = pack.name;
        info.shortName = pack.lang_code.replace('-', '_').toLowerCase();
        if (pack.base_lang_code != null) {
            info.baseLangCode = pack.base_lang_code.replace('-', '_').toLowerCase();
        }
        info.pluralLangCode = pack.plural_code.replace('-', '_').toLowerCase();
        info.isRtl = pack.rtl;
        info.pathToFile = pack.official ? "remote" : "unofficial";
        LocaleController.LocaleInfo existing = LocaleController.getInstance().getLanguageFromDict(info.getKey());
        return existing != null ? existing : info;
    }
}
