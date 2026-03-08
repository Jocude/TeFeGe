package com.example.vectorscan;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.*;
import com.google.android.material.button.MaterialButton;
import java.io.*;
import java.text.*;
import java.util.*;
import okhttp3.*;

public class HomeActivity extends AppCompatActivity {

    private Uri photoUri;
    private File photoFile;

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && photoFile != null) {

                    enviarImagenAServidor(photoFile);

                } else {
                    Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {

                    String path = FileUtils.getPath(this, uri);

                    if (path != null) {
                        File f = new File(path);
                        enviarImagenAServidor(f);
                    } else {
                        Toast.makeText(this, "No se pudo leer la imagen", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String username = getIntent().getStringExtra("username");
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        if (username != null) {
            tvWelcome.setText("Bienvenido, " + username);
        }

        MaterialButton btnCamera = findViewById(R.id.btnCamera);
        MaterialButton btnGallery = findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnGallery.setOnClickListener(v -> {
            galleryLauncher.launch("image/*");
        });
    }

    private void openCamera() {
        try {
            photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, "com.example.vectorscan.fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException ex) {
            Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "SCAN_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void enviarImagenAServidor(File f) {

        OkHttpClient client = new OkHttpClient();

        RequestBody fileBody = RequestBody.create(f, MediaType.parse("image/jpeg"));

        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", f.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url("http://10.0.2.2:8000/predict")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {

                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Error conectando con IA", Toast.LENGTH_LONG).show()
                );

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String respuesta = response.body().string();

                runOnUiThread(() ->
                        Toast.makeText(HomeActivity.this, "Predicción completada", Toast.LENGTH_LONG).show()
                );

                System.out.println(respuesta);
            }
        });
    }
}