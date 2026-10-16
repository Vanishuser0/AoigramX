package org.telegram.ui;

import org.telegram.ui.ActionBar.BaseFragment;

public interface MainTabsActivityController {
    void setTabsVisible(boolean visible);

    void setTabsHiddenByScroll(BaseFragment fragment, boolean hidden);
}
