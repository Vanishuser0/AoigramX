package nu.gpu.aoigram.cloud;

import android.content.Context;
import android.view.animation.AlphaAnimation;

import org.telegram.ui.Components.Easings;
import org.telegram.ui.Components.TextViewSwitcher;

public class AlphaTextViewSwitcher extends TextViewSwitcher {

    public AlphaTextViewSwitcher(Context context) {
        super(context);
        AlphaAnimation alphaIn = new AlphaAnimation(0f, 1f);
        alphaIn.setDuration(250);
        alphaIn.setInterpolator(Easings.easeInOutQuad);
        setInAnimation(alphaIn);
        AlphaAnimation alphaOut = new AlphaAnimation(1f, 0f);
        alphaOut.setDuration(250);
        alphaOut.setInterpolator(Easings.easeInOutQuad);
        setOutAnimation(alphaOut);
    }

    public void setTextColor(int color) {
        if (getCurrentView() != null) {
            getCurrentView().setTextColor(color);
        }
    }
}
