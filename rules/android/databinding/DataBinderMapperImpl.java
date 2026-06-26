package androidx.databinding;

import android.view.View;

public class DataBinderMapperImpl extends DataBinderMapper {
    @Override
    public ViewDataBinding getDataBinder(
            DataBindingComponent component,
            View view,
            int layoutId) {
        return null;
    }

    @Override
    public ViewDataBinding getDataBinder(
            DataBindingComponent component,
            View[] views,
            int layoutId) {
        return null;
    }

    @Override
    public int getLayoutId(String tag) {
        return 0;
    }

    @Override
    public String convertBrIdToString(int localId) {
        return null;
    }
}
