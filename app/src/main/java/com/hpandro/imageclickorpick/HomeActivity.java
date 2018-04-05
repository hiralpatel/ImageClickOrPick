package com.hpandro.imageclickorpick;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private int REQUEST_PERMISSION = 1;
    private int LOAD_IMAGE_RESULTS = 2;
    private String imagePath = "";
    private int LOAD_CAPTURE_IMAGE_RESULTS = 3;
    private ImageView ivImage;
    private Button btnClick;

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ivImage = (ImageView) findViewById(R.id.ivImage);
        btnClick = (Button) findViewById(R.id.btnClick);

        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPermissions(HomeActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(HomeActivity.this, PERMISSIONS, PERMISSION_ALL);
                } else {
                    chooseDialog();
                }
//                if (ContextCompat.checkSelfPermission(HomeActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
//                        PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(HomeActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            REQUEST_PERMISSION);
//                } else if (ContextCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.CAMERA) !=
//                        PackageManager.PERMISSION_GRANTED) {
//                    ActivityCompat.requestPermissions(HomeActivity.this, new String[]{android.Manifest.permission.CAMERA},
//                            REQUEST_PERMISSION);
//                } else {
//                    chooseDialog();
//                }
            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void chooseDialog() {
        final Dialog dialog = new Dialog(HomeActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        View view = getLayoutInflater().inflate(R.layout.choosepicturedialog, null);

        TextView txt_done = (TextView) view.findViewById(R.id.txt_done);
        txt_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        ImageView ivCamera = (ImageView) view.findViewById(R.id.ivCamera);
        ivCamera.setVisibility(View.VISIBLE);
        ivCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                openCameraIntent();
            }
        });

        ImageView ivGallery = (ImageView) view.findViewById(R.id.ivGallery);
        ivGallery.setVisibility(View.VISIBLE);
        ivGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select File"), LOAD_IMAGE_RESULTS);
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void openCameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                System.out.println("++++++ photoFile:" + photoFile);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Uri photoUri = FileProvider.getUriForFile(HomeActivity.this, /*getPackageName() +*/ "com.hpandro.imageclickorpick.provider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(pictureIntent, LOAD_CAPTURE_IMAGE_RESULTS);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getAbsolutePath();
        return image;
    }

    public String getPath(Uri uri) {
        if (uri == null) {
            return null;
        }
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == LOAD_IMAGE_RESULTS && resultCode == Activity.RESULT_OK) {
            Uri selectedImageUri = data.getData();
            imagePath = getPath(selectedImageUri);
            Bitmap bmImg = (BitmapFactory.decodeFile(imagePath));
            if (bmImg != null) {
                ivImage.setImageBitmap(bmImg);
            }
        } else if (requestCode == LOAD_CAPTURE_IMAGE_RESULTS && resultCode == Activity.RESULT_OK) {
            if (!imagePath.isEmpty()) {
                Bitmap bmImg = (BitmapFactory.decodeFile(imagePath));
                bmImg = rotateImage(bmImg);
                if (bmImg != null) {
                    ivImage.setImageBitmap(bmImg);
                }
            } else {
                Toast.makeText(HomeActivity.this, "Please try again!", Toast.LENGTH_LONG).show();
            }
        }
    }

//    public static Bitmap SaveImage(Bitmap finalBitmap) {
//        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
//        File myDir = new File("/data/data/com.hpandro.imageclickorpick/images/");
//        myDir.mkdirs();
//
//        String fname = timeStamp + ".jpg";
//        File file = new File(myDir, fname);
//        if (file.exists()) file.delete();
//        try {
//            FileOutputStream out = new FileOutputStream(file);
//            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
//            out.flush();
//            out.close();
//            return finalBitmap;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public Bitmap rotateImage(Bitmap mBitmap) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            default:
        }
        Bitmap rotatedImage = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        return rotatedImage;
    }
}