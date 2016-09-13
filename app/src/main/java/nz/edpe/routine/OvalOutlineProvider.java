package nz.edpe.routine;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Outline;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewOutlineProvider;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class OvalOutlineProvider extends ViewOutlineProvider {
    private final Context mContext;

    private final int mPadding;

    public OvalOutlineProvider(Context context, float paddingDp) {
        mContext = context;

        mPadding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, paddingDp, mContext.getResources().getDisplayMetrics());
    }

    @Override
    public void getOutline(View view, Outline outline) {
        outline.setOval(mPadding, mPadding, view.getWidth() - mPadding, view.getHeight() - mPadding);
    }
}
