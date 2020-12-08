package id.ac.cobalogin;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {

    private TextInputLayout layoutName, layoutLastName;
    private TextInputEditText etName, etLastName;
    private Button btnSubmit;
    private TextView txtSelectPhoto;
    private CircleImageView circleImageView;
    private static final int GALLERY_AND_PROFILE = 1;
    private Bitmap bitmap = null;
    private SharedPreferences userPref;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        init();
    }

    private void init() {
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        userPref = getApplicationContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        layoutName = findViewById(R.id.txtLayoutNameUserInfo);
        layoutLastName = findViewById(R.id.txtLayoutLastNameUserInfo);
        etName = findViewById(R.id.etNameUserInfo);
        etLastName = findViewById(R.id.etLastNameUserInfo);
        btnSubmit = findViewById(R.id.btnSubmitUserInfo);
        txtSelectPhoto = findViewById(R.id.txtSelectPhoto);
        circleImageView = findViewById(R.id.imgUserInfo);

        //get photo from gallery
        txtSelectPhoto.setOnClickListener(v->{
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, GALLERY_AND_PROFILE);
        });

        btnSubmit.setOnClickListener(v->{
            //validate if clicked
            if(validate()){
                saveUserInfo();
            }
        });
    }

    private boolean validate() {
        if(etName.getText().toString().isEmpty()){
            layoutName.setErrorEnabled(true);
            layoutName.setError("Name is Required");
            return false;
        }
        if(etLastName.getText().toString().isEmpty()){
            layoutLastName.setErrorEnabled(true);
            layoutLastName.setError("Last Name is Required");
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==GALLERY_AND_PROFILE && resultCode==RESULT_OK){
            Uri imgUri = data.getData();
            circleImageView.setImageURI(imgUri);

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imgUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveUserInfo(){
        dialog.setMessage("Saving");
        dialog.show();
        String name = etName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        StringRequest request = new StringRequest(Request.Method.POST, Constant.SAVE_USER_INFO, response -> {

            try {
                JSONObject object = new JSONObject(response);
                if(object.getBoolean("success")){
//                    JSONObject user = object.getJSONObject("user");
                    SharedPreferences.Editor editor = userPref.edit();
                    editor.putString("photo", object.getString("photo"));
                    editor.putString("name", object.getString("name"));
                    editor.apply();
//                    startActivity(new Intent(getBaseContext(), DashboardActivity.class));
//                    startActivity(new Intent(UserInfoActivity.this, DashboardActivity.class));
                    startActivity(new Intent(UserInfoActivity.this, DashboardActivity.class));
                    finish();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
            dialog.dismiss();
        },error ->{
            error.printStackTrace();
            dialog.dismiss();
        }){
            //add token to header
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                String token = userPref.getString("token","");
                HashMap<String, String> map = new HashMap<>();
                map.put("Authorization","Bearer"+token);
                return map;
            }
            //add params

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> map = new HashMap<>();
                map.put("name", name);
                map.put("lastname", lastName);
                map.put("photo", bitmapToString(bitmap));
                return map;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(UserInfoActivity.this);
        queue.add(request);

    }

    private String bitmapToString(Bitmap bitmap){

        if(bitmap!=null){
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte [] array = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(array,Base64.DEFAULT);
        }
        return "";
    }
}