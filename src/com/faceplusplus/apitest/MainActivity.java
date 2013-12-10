
package com.faceplusplus.apitest;

import java.io.File;

import org.json.JSONObject;

import com.faceplusplus.api.FaceDetecter;
import com.faceplusplus.api.FaceDetecter.Face;
import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;
import com.megvii.apitest.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final int TAKE_PICTURE = 0;
	private static final int CHOOSE_PICTURE = 1;
    private Bitmap curBitmap;
    private final static int REQUEST_GET_PHOTO = 1;
    ImageView imageView = null;
    HandlerThread detectThread = null;
    Handler detectHandler = null;
    Button button = null;
    FaceDetecter detecter = null;
    HttpRequests request = null;// 在线api
    private static final int SCALE = 5;//照片缩小比例
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        detectThread = new HandlerThread("detect");
        detectThread.start();
        detectHandler = new Handler(detectThread.getLooper());
        imageView = (ImageView) findViewById(R.id.imageview);
        curBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test5);
        imageView.setImageBitmap(curBitmap);
        
        detecter = new FaceDetecter();
        detecter.init(this, "075789e387651c6193ad87dd9ca49925");
        
        request = new HttpRequests("075789e387651c6193ad87dd9ca49925",
                "om9asCb_Kxd4P5id5A5cyp8I0x6CYSg3");
        
        
		Button btndect=(Button)findViewById(R.id.detect);
		Button btnpick=(Button)findViewById(R.id.pick);
		btndect.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						detectHandler.post(new Runnable() {

		                    @Override
		                    public void run() {

		                        Face[] faceinfo = detecter.findFaces(curBitmap);// 进行人脸检测
		                        if (faceinfo == null)
		                        {
		                            runOnUiThread(new Runnable() {

		                                @Override
		                                public void run() {
		                                    Toast.makeText(MainActivity.this, "未发现人脸信息", Toast.LENGTH_LONG)
		                                            .show();
		                                }
		                            });
		                            return;
		                        }
		                        
		                        //在线api交互
		                        try {
		                          
		                        	 request.offlineDetect(detecter.getImageByteArray(),detecter.getResultJsonString(), new PostParameters());
		                        
		                        } catch (FaceppParseException e) {
		                            e.printStackTrace();
		                        }
		                        final Bitmap bit = getFaceInfoBitmap(faceinfo, curBitmap);
		                        runOnUiThread(new Runnable() {

		                            @Override
		                            public void run() {
		                                imageView.setImageBitmap(bit);
		                                System.gc();
		                            }
		                        });
		                    }
		                });
					}
				});
		btnpick.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				  showPicturePicker(MainActivity.this,false);
			}
		});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detecter.release(this);// 释放引擎
    }

    public void showPicturePicker(Context context,boolean isCrop){
		//final boolean crop = isCrop;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("图片来源");
		builder.setNegativeButton("取消", null);
		builder.setItems(new String[]{"拍照","相册"}, new DialogInterface.OnClickListener() {
			//类型码
			int REQUEST_CODE;
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case TAKE_PICTURE:
					Uri imageUri = null;
					String fileName = null;
					Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						REQUEST_CODE = TAKE_PICTURE;
						fileName = "image.jpg";
					imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),fileName));
					//指定照片保存路径（SD卡），image.jpg为一个临时文件，每次拍照后这个图片都会被替换
					openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
					startActivityForResult(openCameraIntent, REQUEST_CODE);
					break;
					
				case CHOOSE_PICTURE:
					Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);
						REQUEST_CODE = CHOOSE_PICTURE;
					openAlbumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
					startActivityForResult(openAlbumIntent, REQUEST_CODE);
					break;

				default:
					break;
				}
			}
		});
		builder.create().show();
	}
    
    public static Bitmap getFaceInfoBitmap(Face[] faceinfos,
            Bitmap oribitmap) {
        Bitmap tmp;
        tmp = oribitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas localCanvas = new Canvas(tmp);
        Paint localPaint = new Paint();
        localPaint.setColor(0xffff0000);
        localPaint.setStyle(Paint.Style.STROKE);
        for (Face localFaceInfo : faceinfos) {
            RectF rect = new RectF(oribitmap.getWidth() * localFaceInfo.left, oribitmap.getHeight()
                    * localFaceInfo.top, oribitmap.getWidth() * localFaceInfo.right,
                    oribitmap.getHeight()
                            * localFaceInfo.bottom);
            localCanvas.drawRect(rect, localPaint);
        }
        return tmp;
    }

    public static Bitmap getScaledBitmap(String fileName, int dstWidth)
    {
        BitmapFactory.Options localOptions = new BitmapFactory.Options();
        localOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(fileName, localOptions);
        int originWidth = localOptions.outWidth;
        int originHeight = localOptions.outHeight;

        localOptions.inSampleSize = originWidth > originHeight ? originWidth / dstWidth
                : originHeight / dstWidth;
        localOptions.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(fileName, localOptions);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case TAKE_PICTURE:
				//将保存在本地的图片取出并缩小后显示在界面上
				Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/image.jpg");
				curBitmap = ImageTools.zoomBitmap(bitmap, bitmap.getWidth() / SCALE, bitmap.getHeight() / SCALE);
				//由于Bitmap内存占用较大，这里需要回收内存，否则会报out of memory异常
				bitmap.recycle();
				//将处理过的图片显示在界面上，并保存到本地
				imageView.setImageBitmap(curBitmap);
				//ImageTools.savePhotoToSDCard(newBitmap, Environment.getExternalStorageDirectory().getAbsolutePath(), String.valueOf(System.currentTimeMillis()));
				break;
                case REQUEST_GET_PHOTO: {
                    if (data != null) {
                        final String str;
                        Uri localUri = data.getData();
                        String[] arrayOfString = new String[1];
                        arrayOfString[0] = "_data";
                        Cursor localCursor = getContentResolver().query(localUri,
                                arrayOfString, null, null, null);
                        if (localCursor == null)
                            return;
                        localCursor.moveToFirst();
                        str = localCursor.getString(localCursor
                                .getColumnIndex(arrayOfString[0]));
                        localCursor.close();
                        if ((curBitmap != null) && (!curBitmap.isRecycled()))
                            curBitmap.recycle();
                        curBitmap = getScaledBitmap(str, 600);
                        imageView.setImageBitmap(curBitmap);
                    }
                    break;
                }
            }

        }
    }
}
