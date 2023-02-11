package co.coinfinity.infineonandroidapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
import android.provider.Settings;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import org.bouncycastle.util.encoders.Hex;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
//import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;

import co.coinfinity.infineonandroidapp.ethereum.CoinfinityClient;
import co.coinfinity.infineonandroidapp.ethereum.bean.EthBalanceBean;
import co.coinfinity.infineonandroidapp.ethereum.bean.TransactionPriceBean;
import co.coinfinity.infineonandroidapp.ethereum.utils.EthereumUtils;
import co.coinfinity.infineonandroidapp.infineon.NfcUtils;
import co.coinfinity.infineonandroidapp.infineon.exceptions.NfcCardException;
import co.coinfinity.infineonandroidapp.qrcode.QrCodeGenerator;
import co.coinfinity.infineonandroidapp.utils.IsoTagWrapper;
import co.coinfinity.infineonandroidapp.utils.UiUtils;

import static co.coinfinity.AppConstants.*;
import static co.coinfinity.infineonandroidapp.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Main activity. Entry point of the application.
 *
 * @author Coinfinity.co, 2018
 */
public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @BindView(R.id.ethAddress)
    TextView ethAddressView;
    @BindView(R.id.balance)
    TextView balance;
    @BindView(R.id.qrCode)
    ImageView qrCodeView;
    @BindView(R.id.holdCard)
    TextView holdCard;
    @BindView(R.id.send)
    Button sendEthBtn;
    @BindView(R.id.sendErc20)
    Button sendErc20Btn;
    @BindView(R.id.voting)
    Button votingBtn;
    @BindView(R.id.brandProtection)
    Button brandProtectionBtn;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.image_nfc_icon)
    ImageView nfcIcon;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.keyIndexSpinner)
    Spinner keyIndexSpinner;

    public static String pubKeyString;
    public static String privateKeyString;
    public static String ethAddress;
    private EthBalanceBean ethBalance;

    private CoinfinityClient coinfinityClient = new CoinfinityClient();
    private volatile boolean activityPaused = false;

    /**
     * Will be called after card was hold to back of device.
     *
     * @param intent includes nfc extras
     */

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void resolveIntent(Intent intent) {
        // Only handle NFC intents
        if (intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) == null) {
            return;
        }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        UiUtils.logTagInfo(tag);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            //showToast(getString(R.string.wrong_card), this);
            //return;
        }
        // now we have an IsoTag:

        // update UI
        displayOnUI(GuiState.PROGRESS_BAR);

        //showToast("SASA111", this);

        try {
            isoDep.connect();

            try {
                //showToast("sasa1a", this);
                String password = "secr3t";
                //showToast("sasa1b", this);
                //showToast("sasa1d", this);

                String sasakey="";

                String sasakey2="";

                boolean sasaboolean=false;

                for (int i=11; i<=26;i++) {
                    String sasapage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=sasapage.length();j++) {
                        if (!sasaboolean==true) {
                            if (!sasapage.substring(j - 1, j).equals("0")) {
                                sasaboolean = true;
                            }
                        }
                    }
                    sasakey=sasakey+sasapage;
                }

                for (int i=29; i<=36;i++) {
                    String sasapage=bytesToHex(isoDep.readPages(i)).substring(0,8);
                    for (int j=1; j<=sasapage.length();j++) {
                        if (!sasaboolean==true) {
                            if (!sasapage.substring(j - 1, j).equals("0")) {
                                sasaboolean = true;
                            }
                        }
                    }
                    sasakey2=sasakey2+sasapage;
                }

                ECKeyPair keyPair;

                if (sasaboolean==false) {
                    //keyPair = Keys.createEcKeyPair();
                    //WalletFile wallet = Wallet.createLight(password, keyPair);
                    //pubKeyString = keyPair.getPublicKey().toString(16);
                    //privateKeyString = keyPair.getPrivateKey().toString(16);
                } else
                {
                    pubKeyString = sasakey;
                    privateKeyString = sasakey2;
                }
                //showToast("sasa1c", this);

                if (sasaboolean==false) {
                    /*
                    pubKeyString = "7abc660182a9330fc6b25f7b60dfb291710cf1ba03f27b1792e10abba17be2dbebed9583c4fe9b0f688992595407a244c1cef74263536a4cefb51e34dc0d7857";
                    privateKeyString = "b90ee42cc3b3645c74e6ab0a41d3e8506029d2f6dd759600ac0dac6826ac4d3f";

                    Credentials credentials = Credentials.create(privateKeyString);

                    pubKeyString = credentials.getEcKeyPair().getPublicKey().toString(16);

                    sasakey = pubKeyString;

                    Integer sasaindex = ((int) pubKeyString.length() / 8) + 1;

                    for (int i = 0; i <= sasaindex * 8; i++) {
                        if (sasakey.length() < i) {
                            sasakey = sasakey + "0";
                        }
                    }

                    //showToast(" " + sasaindex + " " + sasakey.length(), this);

                    for (int i = 1; i <= sasakey.length(); i++) {
                        if ((i % 8) == 0) {
                            if (i == sasakey.length()) {
                                isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(sasakey.substring(i - 8, sasakey.length())));
                                break;
                            } else {
                                isoDep.writePage(((int) i / 8) + 10, hexStringToByteArray(sasakey.substring(i - 8, i)));
                            }
                        }
                    }

                    sasakey = privateKeyString;

                    sasaindex = ((int) privateKeyString.length() / 8) + 1;

                    for (int i = 0; i <= sasaindex * 8; i++) {
                        if (sasakey.length() < i) {
                            sasakey = sasakey + "0";
                        }
                    }

                    //showToast(" " + sasaindex + " " + sasakey.length(), this);

                    for (int i = 1; i <= sasakey.length(); i++) {
                        if ((i % 8) == 0) {
                            if (i == sasakey.length()) {
                                isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(sasakey.substring(i - 8, sasakey.length())));
                                break;
                            } else {
                                isoDep.writePage(((int) i / 8) + 28, hexStringToByteArray(sasakey.substring(i - 8, i)));
                            }
                        }
                    }

                    //showToast("A : "+sasakey.length(), this);

                     */
                }



                //System.out.println("Priate key: " + keyPair.getPrivateKey().toString(16));
                //System.out.println("Account: " + wallet.getAddress());

                //ethAddress = "0x"+wallet.getAddress();

            } catch(Exception e) {
                showToast("Error: " + e.getMessage(), this);
                //System.err.println("Error: " + e.getMessage());
            }

            //boolean auth = isoDep.authenticateSectorWithKeyA(0,MifareClassic.KEY_DEFAULT);
            //if (auth==true) {
            //    showToast("sasa TRUE", this);
                //byte[] response = isoDep.transceive(new byte[] {(byte)0x00, (byte) 0xA4,(byte) 0x04,(byte) 0x00, (byte) 0xA0, (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x15, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00});
            //} else
            //{
            //    showToast("sasa FALSE", this);
            //}
            //byte[] sasa=new byte[] {(byte)0x00, (byte) 0xA4,(byte) 0x04,(byte) 0x00, (byte) 0xA0, (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x15, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};
            //isoDep.transceive(new byte[] {(byte)0x00, (byte) 0xA4,(byte) 0x04,(byte) 0x00, (byte) 0xA0, (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x15, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00});
            //SharedPreferences pref = this.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
            //pubKeyString = NfcUtils.readPublicKeyOrCreateIfNotExists(IsoTagWrapper.of(isoDep),
            //        pref.getInt(KEY_INDEX_OF_CARD, 1)).getPublicKeyInHexWithoutPrefix();
            isoDep.close();
        //} catch (IOException | NfcCardException e) {
        } catch (IOException e) {
            showToast(e.getMessage(), this);
            Log.e(TAG, "Exception while reading public key from card: ", e);
            resetGuiState();
            return;
        }

        //showToast("sasa2", this);
        //if (pubKeyString!=null) {

        Credentials sasa = Credentials.create(privateKeyString);

        pubKeyString = sasa.getEcKeyPair().getPublicKey().toString(16);

            Log.d(TAG, String.format("pubkey read from card: '%s'", pubKeyString));
            // use web3j to format this public key as ETH address
            ethAddress = Keys.toChecksumAddress(Keys.getAddress(pubKeyString));
            //showToast("sasa3", this);
            ethAddressView.setText(ethAddress);
            //showToast("sasa4", this);
            Log.d(TAG, String.format("ETH address: %s", ethAddress));
            //showToast("sasa5", this);
            qrCodeView.setImageBitmap(QrCodeGenerator.generateQrCode(ethAddress));
            //showToast("sasa6", this);
            holdCard.setText(R.string.card_found);
            //showToast("sasa7", this);
        //}
    }

    /**
     * this method is updating the balance and euro price on UI.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void updateBalance() throws Exception {
        //showToast("sasa1", this);
        Log.d(TAG, "reading ETH balance..");
        ethBalance = EthereumUtils.getBalance(ethAddress, UiUtils.getFullNodeUrl(this));
        //ethBalance = EthereumUtils.getBalance(ethAddress, "ropsten.infura.io/083836b2784f48e19e03487eb3209923");
        /*
        Web3j web3j = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/083836b2784f48e19e03487eb3209923"));

        BigInteger balance = null;
        try {
            EthGetBalance ethGetBalance;
            ethGetBalance = web3j.ethGetBalance(ethAddress, DefaultBlockParameterName.PENDING).send();
            balance = ethGetBalance.getBalance();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("address " + ethAddress + " balance " + balance + " wei");



         */

        Log.d(TAG, String.format("reading ETH balance finished: %s", balance.toString()));
        //showToast(String.format("reading ETH balance finished: %s", balance.toString()), this);
    }

    /**
     * this method is updating the balance and euro price on UI.
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void updateEuroPrice() throws Exception {
        //showToast("sasa2", this);
        if (ethBalance == null)
            return;

        //showToast("YESS", this);

        Log.d(TAG, "reading EUR/ETH price..");
        //TransactionPriceBean transactionPriceBean = coinfinityClient.readEuroPriceFromApiSync("0", "0",
        //        ethBalance.getEther().toString());
        //Log.d(TAG, String.format("reading EUR/ETH price finished: %s", transactionPriceBean));
        //if (transactionPriceBean != null && pubKeyString != null) {
        if (pubKeyString != null) {
            this.runOnUiThread(() -> {
                balance.setText(ethBalance.toString());
                if (!sendEthBtn.isEnabled()) {
                    sendEthBtn.setEnabled(true);
                    sendErc20Btn.setEnabled(true);
                    votingBtn.setEnabled(true);
                    //brand protection
                    brandProtectionBtn.setEnabled(true);
                }
                displayOnUI(GuiState.BALANCE_TEXT);
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.main_activity_title);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        displayOnUI(GuiState.NFC_ICON);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            showToast(getString(R.string.no_nfc), this);
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

        //pendingIntent = PendingIntent.getActivity(this, 0,
        //        new Intent(this, this.getClass()), 0);



        keyIndexSpinner.setSelection(1);
        keyIndexSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                SharedPreferences prefs = parentView.getContext().getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor mEditor = prefs.edit();
                mEditor.putInt(KEY_INDEX_OF_CARD, position)
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        //showToast("sasa1", this);

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                openNfcSettings();
            }
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        activityPaused = false;

        //showToast("sasa2", this);

        new Thread(() -> {
            Log.d(TAG, "Main activity, start reading eth balance thread...");
            try {
                while (!activityPaused && ethAddress != null) {
                    updateBalance();
                    TimeUnit.SECONDS.sleep(FIVE_SECONDS);
                }
            } catch (Exception e) {
                Log.e(TAG, "exception while reading eth balance from api.\n" + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast(e.getMessage(), this);
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        resetGuiState();
                    }
                });
            }
            Log.d(TAG, "Main activity, reading eth balance thread exited.");
        }).start();

        //showToast("sasa3", this);

        new Thread(() -> {
            Log.d(TAG, "Main activity, start reading price thread...");
            try {
                if (ethAddress != null) {
                    updateBalance();
                    while (!activityPaused) {
                        updateEuroPrice();
                        TimeUnit.SECONDS.sleep(TEN_SECONDS);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "exception while reading euro price from api.\n" + e.getMessage(), e);
                runOnUiThread(() -> {
                    showToast(e.getMessage(), this);
                    if (progressBar.getVisibility() == View.VISIBLE) {
                        resetGuiState();
                    }
                });
            }
            Log.d(TAG, "Main activity, reading price thread exited.");
        }).start();

        //showToast("sasa4", this);
    }

    @Override
    protected void onPause() {
        activityPaused = true;
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    /**
     * Opens system settings, wireless settings.
     */
    private void openNfcSettings() {
        showToast(getString(R.string.enable_nfc), this);
        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        startActivity(intent);
    }

    /**
     * Called by Android systems whenever a new Intent is received. NFC tags are also
     * delivered via an Intent.
     *
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        activityPaused = false; // onPause() gets called when a Intent gets dispatched by Android
        setIntent(intent);
        resolveIntent(intent);
    }

    /**
     * If we have already a Public key, allow the user to reset by pressing back.
     */
    @Override
    public void onBackPressed() {
        if (pubKeyString != null) {
            resetGuiState();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * reset everything, like we never had seen a card.
     */
    private void resetGuiState() {
        displayOnUI(GuiState.NFC_ICON);
        pubKeyString = null;
        ethAddress = null;
        ethAddressView.setText("");
        //qrCodeView.setImageBitmap(null);
        qrCodeView.setImageResource(R.drawable.ic_eth_3);
        holdCard.setText(R.string.hold_card);
        sendEthBtn.setEnabled(false);
        sendErc20Btn.setEnabled(false);
        votingBtn.setEnabled(false);
        brandProtectionBtn.setEnabled(false);
    }


    private enum GuiState {NFC_ICON, PROGRESS_BAR, BALANCE_TEXT}

    /**
     *  on buttton click Brand Protection
     */
    public void onBrandProtection(View view){
        Intent intent=new Intent(this,BrandProtection.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }
    /**
     * On button click SEND ETH.
     */
    public void onSend(View view) {
        Intent intent = new Intent(this, SendTransactionActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * On button click SEND ERC-20.
     */
    public void onSendErc20(View view) {
        Intent intent = new Intent(this, SendErc20TokensActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    /**
     * On button click VOTING.
     */
    public void onVoting(View view) {
//        Switch to new voting if needed
//        Intent intent = new Intent(this, VotingActivity.class);
        Intent intent = new Intent(this, VotingActivityOld.class);
        Bundle bundle = new Bundle();
        bundle.putString("pubKey", pubKeyString);
        bundle.putString("ethAddress", ethAddress);
        intent.putExtras(bundle);
        startActivity(intent);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UiUtils.handleOptionItemSelected(this, item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Display only GUI elements of 1 of 3 states.
     * NFC Icon (when waiting for NFC), spinner (when waiting for network background tasks,
     * Text (when displaying balance results)
     *
     * @param state NFC_ICON, PROGRESS_BAR, BALANCE_TEXT
     */
    public void displayOnUI(GuiState state) {
        // only display NFC Icon
        if (GuiState.NFC_ICON.equals(state)) {
            progressBar.setVisibility(View.GONE);
            balance.setVisibility(View.GONE);
            nfcIcon.setVisibility(View.VISIBLE);
        }
        // only display progress bar
        else if (GuiState.PROGRESS_BAR.equals(state)) {
            nfcIcon.setVisibility(View.GONE);
            balance.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
        // only display balance text
        else if (GuiState.BALANCE_TEXT.equals(state)) {
            nfcIcon.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            balance.setVisibility(View.VISIBLE);
        }
    }
}
