package com.mogsev.application.translator;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.translate.*;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.translate.model.TranslationsResource;
import com.mogsev.application.translator.model.Language;
import com.mogsev.application.translator.model.TranslateItem;
import com.mogsev.application.translator.util.DataBaseHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final String API_KEY = "AIzaSyBhL9iNdRDF3RFIPVD_Tt_Vt6HaKM2QRyg";
    private static final String TEST_REQUEST = "https://www.googleapis.com/language/translate/v2?key=AIzaSyBhL9iNdRDF3RFIPVD_Tt_Vt6HaKM2QRyg&source=en&target=ru&q=main";

    private AppCompatEditText etInput;
    private TextView tvOutput;
    private TextView tvMode;
    private String strInput;
    private Spinner spinner;
    private Button btnDelete;
    private Button btnAdd;
    private AppCompatEditText etOutput;

    private ArrayAdapter<String> adapter;
    private Language language;

    private DataBaseHelper dataBaseHelper;
    private ArrayList<TranslateItem> offlineList;
    private TranslateItem translateItem;

    private boolean isOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataBaseHelper = DataBaseHelper.getInstance(this);

        if (android.os.Build.VERSION.SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll()
                    .build();
            StrictMode.setThreadPolicy(policy);
        }

        //initialize view elements
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        //initialize mode and language adapter
        initMode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        dataBaseHelper.close();
    }

    /**
     * Initialize view elements
     */
    private void initView() {
        etInput = (AppCompatEditText) this.findViewById(R.id.etInput);
        tvOutput = (TextView) this.findViewById(R.id.tvOutput);
        tvMode = (TextView) this.findViewById(R.id.tvMode);
        spinner = (Spinner) this.findViewById(R.id.spinner);
        btnDelete = (Button) this.findViewById(R.id.btnDelete);
        btnAdd = (Button) this.findViewById(R.id.btnAdd);
        etOutput = (AppCompatEditText) this.findViewById(R.id.etOutput);
    }

    /**
     * Return value of the internet available or not
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
        //return false;
    }

    /**
     * Translate
     */
    public void onTranslate(View view) {
        Log.d(TAG, "onTranslate");
        tvOutput.setText("");
        etOutput.setText("");
        strInput = etInput.getText().toString();
        if (!isNetworkAvailable() && isOnline ) {
            Log.d(TAG, "initMode");
            isOnline = false;
            initMode();
        }
        if (isNetworkAvailable() && !isOnline) {
            isOnline = true;
            initMode();
        }
        if (isNetworkAvailable()) {
            onGoogleTranslate();
        } else {
            onOfflineTranslate();
        }
    }

    /**
     * Initialize mode and language adapter
     */
    private void initMode() {
        Log.d(TAG, "initMode");
        List<String> listName;
        if (isNetworkAvailable()) {
            List<String> listCode = Arrays.asList(this.getResources().getStringArray(R.array.language_code));
            listName = Arrays.asList(this.getResources().getStringArray(R.array.language_name));
            language = new Language(listCode, listName);
            tvMode.setText(this.getResources().getString(R.string.online_mode));
            btnDelete.setVisibility(View.GONE);
            isOnline = true;
        } else {
            listName = Arrays.asList(this.getResources().getStringArray(R.array.language_to_language));
            tvMode.setText(this.getResources().getString(R.string.offline_mode));
            btnDelete.setVisibility(View.VISIBLE);
            getOfflineList(getOfflineType());
            isOnline = false;
        }

        //Initialize adapter
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listName);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "spinner selected: " + i);
                switch (i) {
                    case 0:
                        getOfflineList(Language.TRANSLATE.EN_RU);
                        break;
                    case 1:
                        getOfflineList(Language.TRANSLATE.RU_EN);
                    break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    /**
     * Return type of translate
     */
    private Language.TRANSLATE getOfflineType() {
        switch (spinner.getSelectedItemPosition()) {
            case 0:
                return Language.TRANSLATE.EN_RU;
            case 1:
                return Language.TRANSLATE.RU_EN;
            default:
                return Language.TRANSLATE.EN_RU;
        }
    }

    /**
     * Put on offline list
     */
    private void getOfflineList(final Language.TRANSLATE type) {
        Log.d(TAG, "getOfflineList");
        tvOutput.setText("");
        etOutput.setText("");
        etOutput.setVisibility(View.GONE);
        btnAdd.setVisibility(View.GONE);
        new AsyncTask<Void, Void, Void>() {
            ArrayList<TranslateItem> list = new ArrayList<TranslateItem>();

            @Override
            protected Void doInBackground(Void... voids) {
                if (dataBaseHelper != null) {
                    list = dataBaseHelper.getTranslateList(type);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                offlineList = list;
            }
        }.execute();
    }

    /**
     * Call Google Translate API v2
     * https://cloud.google.com/translate/docs/
     * https://developers.google.com/resources/api-libraries/documentation/translate/v2/java/latest/com/google/api/services/translate/package-summary.html
     */
    private void onGoogleTranslate() {
        Log.d(TAG, "onGoogleTranslate");
        new AsyncTask<String, Void, Void>() {
            String result = "";

            @Override
            protected Void doInBackground(String... str) {
                Log.d(TAG, "doInBackground");
                Translate translate = new Translate.Builder(
                        AndroidHttp.newCompatibleTransport(), AndroidJsonFactory.getDefaultInstance(),
                        new HttpRequestInitializer() {
                            @Override
                            public void initialize(HttpRequest httpRequest) throws IOException {
                                Log.d(TAG, "HTTP request: " + httpRequest);
                            }
                        }).setTranslateRequestInitializer(new TranslateRequestInitializer(API_KEY))
                        .build();
                try {
                    Translate.Translations.List request = translate.translations().list(Arrays.asList(strInput), str[0]);
                    TranslationsListResponse tlr = request.execute();
                    List<TranslationsResource> list = tlr.getTranslations();
                    Log.d(TAG, "List: " + list.toString());
                    result = list.get(0).getTranslatedText();
                    Log.d(TAG, "Result: " + result);
                } catch (IOException ex) {
                    Log.e(TAG, "IOException " + ex.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Log.d(TAG, "onPostExecute");
                super.onPostExecute(aVoid);
                tvOutput.setText(result);
            }
        }.execute(choiceLanguage());
    }

    /**
     * Return choice code of language
     */
    private String choiceLanguage() {
        if (spinner.getSelectedItemPosition() == 0) {
            return Locale.getDefault().getLanguage();
        } else {
            return language.getCode(spinner.getSelectedItemPosition());
        }
    }

    /**
     * Execute offline translate
     */
    private void onOfflineTranslate() {
        Log.d(TAG, "onOfflineTranslate");
        translateItem = null;
        btnAdd.setVisibility(View.GONE);
        etOutput.setVisibility(View.GONE);
        String input = etInput.getText().toString();
        String output = getResources().getString(R.string.no_result);
        Log.d(TAG, "offline input: " + input + " / offline output: " + output);
        Log.d(TAG, "offlineList: " + offlineList.toString());
        if (input.isEmpty()) {
            tvOutput.setText(getResources().getString(R.string.enter_text));
            return;
        } else {
            if (offlineList.isEmpty()) {
                btnAdd.setVisibility(View.VISIBLE);
                etOutput.setVisibility(View.VISIBLE);
                tvOutput.setText(getResources().getString(R.string.input_translate_result));
                return;
            }
            for (TranslateItem item : offlineList) {
                Log.d(TAG, "item: " + item.getIn() + " / " + item.getOut());
                if (item.getIn().equals(input)) {
                    translateItem = item;
                    tvOutput.setText(item.getOut());
                    etOutput.setText(item.getOut());
                    break;
                } else {
                    tvOutput.setText(getResources().getString(R.string.no_result));
                    btnAdd.setVisibility(View.VISIBLE);
                    etOutput.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Add or update TranslateItem
     */
    public void addTranslateItem(View view) {
        Log.d(TAG, "addTranslateItem");
        String input = etInput.getText().toString();
        String output = etOutput.getText().toString();
        dataBaseHelper.addTranslateItem(new TranslateItem(input, output), getOfflineType());
        getOfflineList(getOfflineType());
    }

    /**
     * Delete TranslateItem
     */
    public void deleteTranslateItem(View view) {
        Log.d(TAG, "deleteTranslateItem");
        String input = etInput.getText().toString();
        String output = etOutput.getText().toString();
        //dataBaseHelper.removeTranslateItem(new TranslateItem(input, output), getOfflineType());
        if (translateItem != null) {
            dataBaseHelper.removeTranslateItem(translateItem, getOfflineType());
        }
        getOfflineList(getOfflineType());
    }

}
