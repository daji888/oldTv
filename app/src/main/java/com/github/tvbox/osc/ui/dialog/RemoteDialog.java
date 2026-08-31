package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.tv.QRCodeGen;

import org.jetbrains.annotations.NotNull;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class RemoteDialog extends BaseDialog {
    private ImageView ivQRCode;
    private TextView tvAddress;

    public RemoteDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_remote);
        setCanceledOnTouchOutside(false);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAddress = findViewById(R.id.tvAddress);
        refreshQRCode();
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("扫描二维码，或者使用浏览器访问以下网址：\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.dp2px(getContext(), 300), AutoSizeUtils.dp2px(getContext(), 300)));
    }
}
