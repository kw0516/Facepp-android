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
    HttpRequests request = null;// ����api
    private static final int SCALE = 5;//��Ƭ��С����
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
        detecter.init(this, "YOUR-API");
        
        request = new HttpRequests("YOUR-API",
                "YOUR-SELECT");
        
        
		Button btndect=(Button)findViewById(R.id.detect);
		Button btnpick=(Button)findViewById(R.id.pick);
		btndect.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						detectHandler.post(new Runnable() {

		                    @Override
		                    public void run() {

		                        Face[] faceinfo = detecter.findFaces(curBitmap);// ������������
		                        if (faceinfo == null)
		                        {
		                            runOnUiThread(new Runnable() {

		                                @Override
		                                public void run() {
		                                    Toast.makeText(MainActivity.this, "δ����������Ϣ", Toast.LENGTH_LONG)
		                                            .show();
		                                }
		                            });
		                            return;
		                        }
		                        
		                        //����api����
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
        detecter.release(this);// �ͷ�����
    }

    public void showPicturePicker(Context context,boolean isCrop){
		//final boolean crop = isCrop;
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("ͼƬ��Դ");
		builder.setNegativeButton("ȡ��", null);
		builder.setItems(new String[]{"����","����"}, new DialogInterface.OnClickListener() {
			//������
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
					//ָ����Ƭ����·����SD������image.jpgΪһ����ʱ�ļ���ÿ�����պ�����ͼƬ���ᱻ�滻
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
				//�������ڱ��ص�ͼƬȡ������С����ʾ�ڽ�����
				Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory()+"/image.jpg");
				curBitmap = ImageTools.zoomBitmap(bitmap, bitmap.getWidth() / SCALE, bitmap.getHeight() / SCALE);
				//����Bitmap�ڴ�ռ�ýϴ���������Ҫ�����ڴ棬�����ᱨout of memory�쳣
				bitmap.recycle();
				//����������ͼƬ��ʾ�ڽ����ϣ������浽����
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
