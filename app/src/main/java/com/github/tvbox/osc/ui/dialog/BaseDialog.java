package com.github.tvbox.osc.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import xyz.doikki.videoplayer.util.CutoutUtil;

public class BaseDialog extends Dialog {
    public BaseDialog(@NonNull Context context) {
        super(context, R.style.CustomDialogStyle);
    }

    public BaseDialog(Context context, int customDialogStyle) {
        super(context, customDialogStyle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CutoutUtil.adaptCutoutAboveAndroidP(this, true);//设置刘海
        super.onCreate(savedInstanceState);
    }

    @Override
    public void show() {
        // 先初始化弹窗
        super.show();
        // 延迟设置临时无焦点Flag，之后再清空，既保留防抢焦点的能力，又不会干扰窗口尺寸计算
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        // 先处理系统栏隐藏，再设置窗口焦点标志
        hideSysBar();
        // 弹窗显示完成后立刻恢复焦点权限
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        // 限制Dialog最大宽高，适配不同设备的显示区域
        WindowManager.LayoutParams params = getWindow().getAttributes();
        // 最大宽度不超过屏幕的0.8倍，避免左右边缘裁剪
        params.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.8f);
        // 最大高度不超过屏幕的0.7倍，避免上下边缘裁剪
        params.height = (int) (getContext().getResources().getDisplayMetrics().heightPixels * 0.7f);
        getWindow().setAttributes(params);
    }

    private void hideSysBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            getWindow().getDecorView().setSystemUiVisibility(uiOptions);
        }
    }
}
