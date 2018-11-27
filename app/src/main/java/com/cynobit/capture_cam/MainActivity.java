package com.cynobit.capture_cam;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.cynobit.capture_cam.OnActivityResultContract.*;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Uri photoUri;
    private String photoPath;
    private Uri cropUri;
    private Bitmap photo;

    private ImageView photoImageView;

    private int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 27;
    private int CAMERA_PERMISSION_REQUEST_CODE = 45;

    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        photoImageView = findViewById(R.id.photoImageView);
    }

    public Object[] createImageFile() {
        try {
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File image = File.createTempFile(TEMP_PHOTO_FILE_NAME, "", storageDir);
            File crop = File.createTempFile(TEMP_PHOTO_FILE_NAME + "_crop", "", storageDir);
            return new Object[]{image, image.getAbsolutePath(), crop};
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String s = cursor.getString(column_index);
        cursor.close();
        return s;
    }

    private void attemptCrop() {
        final ContentResolver cr = getContentResolver();
        final String[] p1 = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATE_TAKEN
        };
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Cursor c1 = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, p1, null, null, p1[1] + " DESC");
            assert c1 != null;
            if (c1.moveToFirst()) {
                String uriString = "content://media/external/images/media/" + c1.getInt(0);
                photoUri = Uri.parse(uriString);
                c1.close();
                performCrop();
            }
        } else {
            performCrop();
        }
    }

    private void performCrop() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            photoPath = getPath(photoUri);
        }
        UCrop.of(photoUri, cropUri).start(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                attemptCrop();
            }
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                captureButton_Click(null);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.CAMERA_CAPTURE && resultCode == RESULT_OK) {
            boolean permission = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }
            if (permission) {
                attemptCrop();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE);
            }
        } else if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            try {
                photo = MediaStore.Images.Media.getBitmap(this.getContentResolver(), cropUri);
                photoImageView.setImageBitmap(photo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void captureButton_Click(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return;
        }
        try {
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Object[] object = createImageFile();
                File photoFile = (File) object[0];
                if (photoFile != null) {
                    photoUri = FileProvider.getUriForFile(this, "com.cynobit.capture_cam", photoFile);
                    cropUri = FileProvider.getUriForFile(this, "com.cynobit.capture_cam", (File) object[2]);
                    photoPath = (String) object[1];
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                } else {
                    final Snackbar snackBar = Snackbar.make(findViewById(android.R.id.content), "Error Readying Capture Intent.", Snackbar.LENGTH_LONG);
                    snackBar.setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            snackBar.dismiss();
                        }
                    });
                }
            }
            startActivityForResult(captureIntent, RequestCodes.CAMERA_CAPTURE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Your Mobile Device Doesn't Support Image Capture.", Toast.LENGTH_SHORT).show();
        }
    }

}
