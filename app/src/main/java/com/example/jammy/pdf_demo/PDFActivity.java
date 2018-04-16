package com.example.jammy.pdf_demo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.artifex.mupdf.MuPDFCore;
import com.artifex.mupdf.MuPDFPageAdapter;
import com.artifex.mupdf.ReaderView;
import com.artifex.mupdf.SavePdf;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;


/**
 * Created by b on 2018/4/16.
 */     //bug:标注的区域应该只能在pdf里面，不可在菜单上。
public class PDFActivity extends Activity implements View.OnClickListener{

    //几个按钮的id，后面写定义和监听方法。
    ReaderView readerView;
    RelativeLayout rlSign;
    RelativeLayout rlClear;
    RelativeLayout rlSave;

    String in_path;
    String out_path;
    SignatureView signView;
    float density; // 屏幕分辨率密度
    MuPDFCore muPDFCore;
    Save_Pdf save_pdf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);
        rlSign = (RelativeLayout) findViewById(R.id.rl_sign);
        rlSign.setOnClickListener(this);
        rlClear = (RelativeLayout) findViewById(R.id.rl_clear);
        rlClear.setOnClickListener(this);
        rlSave = (RelativeLayout) findViewById(R.id.rl_save);
        rlSave.setOnClickListener(this);

        signView = (SignatureView)findViewById(R.id.sign);

        // 计算分辨率密度
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        density = metric.density;

//		in_path = "/storage/self/primary/" + "123.pdf";
//		out_path = "/storage/self/primary/" + "ttt.pdf";

//		in_path = "/storage/emulated/0/" + "123.pdf";
//		out_path = "/storage/emulated/0/" + "ttt.pdf";

//GIONEE		in_path = "/storage/emulated/legacy/" + "123.pdf";
//GIONEE		out_path = "/storage/emulated/legacy/" + "ttt.pdf";

        in_path = Environment.getExternalStorageDirectory().toString() + "/UZMap" + "/123.pdf";
        out_path = Environment.getExternalStorageDirectory().toString() + "/UZMap" + "/ttt.pdf";

        try {
            readerView = (ReaderView)findViewById(R.id.reader);
            muPDFCore = new MuPDFCore(in_path);// PDF的文件路径
            readerView.setAdapter(new MuPDFPageAdapter(this, muPDFCore));
            readerView.setDisplayedViewIndex(0);        //展示第一页
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.rl_sign:
                if (rlSave.getVisibility() == View.GONE) {
                    signView.setVisibility(View.VISIBLE);
                    rlSave.setVisibility(View.VISIBLE);
                    rlClear.setVisibility(View.VISIBLE);
                } else {
                    signView.clear();
                    signView.setVisibility(View.GONE);
                    rlSave.setVisibility(View.GONE);
                    rlClear.setVisibility(View.GONE);
                }
                break;
            case R.id.rl_clear:
                signView.clear();
                break;
            case R.id.rl_save:
                float scale = readerView.getmScale();// /得到放大因子
                SavePdf savePdf = new SavePdf(in_path, out_path);
                savePdf.setScale(scale);
                savePdf.setPageNum(readerView.getDisplayedViewIndex() + 1);

                savePdf.setWidthScale(1.0f * readerView.scrollX
                        / readerView.getDisplayedView().getWidth());// 计算宽偏移的百分比
                savePdf.setHeightScale(1.0f * readerView.scrollY
                        / readerView.getDisplayedView().getHeight());// 计算长偏移的百分比

                savePdf.setDensity(density);
                Bitmap bitmap = Bitmap.createBitmap(signView.getWidth(),
                        signView.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                signView.draw(canvas);
                savePdf.setBitmap(bitmap);
                save_pdf = new Save_Pdf(savePdf);
                save_pdf.execute();
                signView.clear();
                signView.setVisibility(View.GONE);
                rlSave.setVisibility(View.GONE);
                rlClear.setVisibility(View.GONE);
                break;

            default:
                break;
        }
    }

    /*
     * 用于存储的异步,并上传更新
     */
    class Save_Pdf extends AsyncTask<String, String, String> {

        private SavePdf savePdf;
        private AlertDialog dialog;

        public Save_Pdf(SavePdf savePdf) {
            this.savePdf = savePdf;
            dialog = new AlertDialog.Builder(PDFActivity.this)
                    .setTitle("正在存储...").create();
        }

        @Override
        protected String doInBackground(String... params) {
            savePdf.addText();
            return null;
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected void onPostExecute(String o) {
            try {
                muPDFCore = new MuPDFCore(out_path);
                readerView.setAdapter(new MuPDFPageAdapter(PDFActivity.this,
                        muPDFCore));

                String temp = in_path;
                in_path = out_path;
                out_path = temp;
                readerView.setmScale(1.0f);
                readerView.setDisplayedViewIndex(readerView
                        .getDisplayedViewIndex());
                dialog.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(save_pdf != null){
            save_pdf.cancel(true);
        }
    }
}
