package com.example.lis.identificadordehuelladigital;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    /*
    %--------------------------------------------------------------------------
    %------- Reconocimiento de huellas digitales-------------------------------
    %------- Por: Cristian D. Berrio Pulido    dario.berrio@udea.edu.co -------
    %-------      Tel 3167911832   --------------------------------------------
    %-------      David A. Acevedo Garcia      davida.acevedo@udea.edu.co -----
    %-------      Tel 3016324416   --------------------------------------------
    %------- Curso Básico de Procesamiento de Imágenes y Visión Artificial-----
    %------- V2.5 Junio de 2018--------------------------------------------------
    %--------------------------------------------------------------------------
    * */
    /*
    * %--------------------------------------------------------------------------
    %--1. Inicialización del sistema -----------------------------------------------
    %--------------------------------------------------------------------------
    */
    //Inicialización de partes de la UI
    ImageView imageToUpload;
    Button bUploadImage;
    TextView tLabel;
    String rPath;
    //Se inicializa un resultado en la carga de la imagen, para el momento en que se inicia la actividad de de mostrar la imagen seleccionada
    private static final int RESULT_LOAD_IMAGE=1;
    //Datos de la parte backend en python
    private static final  String SERVER_ADRESS = "http://192.168.0.6:9999/upload";
    //Metodo en el cual se le asignan los listener a los elementos de la UI
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Instanciamiento de imagenes
        imageToUpload = (ImageView) findViewById(R.id.imageToUpload);
        //Instanciamiento de Botones
        bUploadImage = (Button) findViewById(R.id.bUploadImage);
        //Instanciamiento de Textos
        tLabel = (TextView) findViewById(R.id.tLabel);
        //Listeners
        imageToUpload.setOnClickListener(this);
        bUploadImage.setOnClickListener(this);
    }


    /*
    * %--------------------------------------------------------------------------
    %--2. Definicion de acciones -----------------------------------------------
    %--------------------------------------------------------------------------
    */
    @Override
    //Metodo para la asignar acciones cuando se clickeen los elementos de la UI
    public void onClick(View v) {//Se obtiene la vista
        switch (v.getId()){//Casos para los elementos de la UI
            //Para cuando se quiere buscar la imagen a subir
            case R.id.imageToUpload:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);//Abre la galeria, tener en cuenta que
                //Se necesitan permisos en el manifest
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);//Inicia la actividad de mostrar la imagen seleccionada
                break;
            //Cuando se ejecuta la subida
            case R.id.bUploadImage:
                //Se ejecuta el constructor del metodo UploadImage
                new UploadImage(rPath).execute();
                break;
        }
    }

    /*
    %------------------------------------------------------------------------
    %--3. Conversión de la imagen a bitmap y obtención de su ruta ABSOLUTA-----
    %--------------------------------------------------------------------------
    */
    @Override
    //Metodo para el desarrollo de las actividades
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Validaciones de que venga desde la galeria, y que si se haya seleccionado una imagen
        if(requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null){
            Uri selectedImage = data.getData();//Se obtiene la ruta de la imagen en formato internacional
            InputStream imageStream=null;//Se hace la trasformación a texto para poder pasarlo a bitmap
            Cursor cursor = null;//Apuntador para la ruta
            try{
                String[] proj = { MediaStore.Images.Media.DATA };//Obtiene los datos del storage de imagenes
                cursor = getContentResolver().query(selectedImage,  proj, null, null, null);//Se asigna el apuntador a la ruta de la imagen
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA); //Se añade la parte no relativa de la ruta
                cursor.moveToFirst();//Apunta el cursor al primer caracter de la ruta
                String realPath = cursor.getString(column_index); //Se obtiene la ruta absoluta de la imagen
                rPath = realPath;//Se lleva a la variable global
                imageStream = getContentResolver().openInputStream(selectedImage);//Se obtiene la imagen como bytes
            }catch(FileNotFoundException e ){
                e.printStackTrace();
            }

            Bitmap decodedImage = BitmapFactory.decodeStream(imageStream);//Se decodifica el texto y se pasa a bitmap
            Bitmap resizedImage = Bitmap.createScaledBitmap(decodedImage, (int)(decodedImage.getWidth()*0.9), (int)(decodedImage.getHeight()*0.9), true);//Se hace un resize a las imagenes grandes
            imageToUpload.setImageBitmap(resizedImage);//Se coloca la imagen en el sector de la UI
        }
    }

    /*
    %--------------------------------------------------------------------------
    %-- 4. Envio de la imagen al servidor -------------------------------------
    %--------------------------------------------------------------------------
    */

    //Clase para la subida de la imagen al servidor
    private class UploadImage extends AsyncTask<Void, Void, Void>{//Se necesita que sea un proceso asincrono para no llenar el hilo de trabajo de la aplicación
        //Variables a usar en la clase
        String imagePath;
        String response;
        //Constructor de la clase
        public UploadImage(String imagePath){
            this.imagePath = imagePath;
        }
        @Override
        //Accion que ocurre cuando se termine la subida
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Texto pop-up para cuando se suba correctamente la imagen
            tLabel.setText(response);
        }
        @Override
        //Actividad principal de subida en background
        protected Void doInBackground(Void... voids) {
            File image = new File(imagePath);//Se instancia la imagen como archivo

            SyncHttpClient client = new SyncHttpClient();//Creación del cliente http
            RequestParams params = new RequestParams();//Creación de los parametros
            try {
                params.put("image", image);//Se asigna la imagen a los parametros
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            client.setTimeout(600000);//Timeout para los sockets y la conexión
            client.post(SERVER_ADRESS, params, new TextHttpResponseHandler() {//Envio del post
                @Override
                public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                    response = responseString;
                }
                @Override
                public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                    response = responseString;
                }
            });
            return null;
        }
    }
}
