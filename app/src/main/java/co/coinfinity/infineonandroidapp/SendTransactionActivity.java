package co.coinfinity.infineonandroidapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import co.coinfinity.infineonandroidapp.adapter.UnitSpinnerAdapter;
import co.coinfinity.infineonandroidapp.ethereum.CoinfinityClient;
import co.coinfinity.infineonandroidapp.ethereum.bean.EthBalanceBean;
import co.coinfinity.infineonandroidapp.ethereum.bean.TransactionPriceBean;
import co.coinfinity.infineonandroidapp.ethereum.exceptions.InvalidEthereumAddressException;
import co.coinfinity.infineonandroidapp.ethereum.utils.EthereumUtils;
import co.coinfinity.infineonandroidapp.ethereum.utils.UriUtils;
import co.coinfinity.infineonandroidapp.infineon.NfcUtils;
import co.coinfinity.infineonandroidapp.infineon.apdu.response.GenerateSignatureResponseApdu;
import co.coinfinity.infineonandroidapp.infineon.exceptions.NfcCardException;
import co.coinfinity.infineonandroidapp.qrcode.QrCodeScanner;
import co.coinfinity.infineonandroidapp.utils.InputErrorUtils;
import co.coinfinity.infineonandroidapp.utils.IsoTagWrapper;
import co.coinfinity.infineonandroidapp.utils.UiUtils;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
//import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.websocket.WebSocketService;
import org.web3j.tx.ChainId;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
//import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import static org.web3j.tx.Contract.GAS_LIMIT;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.getActivity;
import static co.coinfinity.AppConstants.*;
import static co.coinfinity.infineonandroidapp.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Activity class used for Ethereum functionality.
 */
public class SendTransactionActivity extends AppCompatActivity {

    @BindView(R.id.recipientAddress)
    TextView recipientAddressTxt;
    @BindView(R.id.amount)
    TextView amountTxt;
    @BindView(R.id.gasPrice)
    TextView gasPriceTxt;
    @BindView(R.id.gasLimit)
    TextView gasLimitTxt;
    @BindView(R.id.priceInEuro)
    TextView priceInEuroTxt;
    @BindView(R.id.pin)
    TextView pinTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    //@BindView(R.id.spinner)
    //Spinner spinner;
    @BindView(R.id.toggleButton)
    ToggleButton toggleButton;

    private InputErrorUtils inputErrorUtils;

    private String pubKeyString;
    private String recipientPrivateKeyString;

    private String ethAddress;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private CoinfinityClient coinfinityClient = new CoinfinityClient();
    private volatile boolean activityPaused = false;

    private UnitSpinnerAdapter spinnerAdapter = new UnitSpinnerAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_transaction);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        //spinnerAdapter.addSpinnerAdapter(this, spinner);
        inputErrorUtils = new InputErrorUtils(this, recipientAddressTxt, amountTxt, gasPriceTxt, gasLimitTxt);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

        SharedPreferences mPrefs = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        String savedRecipientAddressTxt = mPrefs.getString(PREF_KEY_RECIPIENT_ADDRESS, "");
        recipientAddressTxt.setText(savedRecipientAddressTxt);
        String savedGasPriceWei = mPrefs.getString(PREF_KEY_GASPRICE_WEI, DEFAULT_GASPRICE_IN_GIGAWEI);
        gasPriceTxt.setText(savedGasPriceWei);
        String savedGasLimit = mPrefs.getString(PREF_KEY_GASLIMIT_SEND_ETH, DEFAULT_GASLIMIT);
        gasLimitTxt.setText(savedGasLimit);
        String savedPin = mPrefs.getString(PREF_KEY_PIN, "");
        pinTxt.setText(savedPin);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            pubKeyString = bundle.getString("pubKey");
            ethAddress = bundle.getString("ethAddress");
        }

        new Thread(() -> {
            try {
                while (!activityPaused) {
                    updateReadingEuroPrice();
                    TimeUnit.SECONDS.sleep(TEN_SECONDS);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception while reading price info from API in thread", e);
            }
        }).start();
    }

    /**
     * This method reads the euro price and updates UI accordingly.
     *
     * @throws Exception
     */
    public void updateReadingEuroPrice() throws Exception {
        Log.d(TAG, "reading EUR/ETH price..");
        TransactionPriceBean transactionPriceBean = coinfinityClient.readEuroPriceFromApiSync(
                gasPriceTxt.getText().toString(), gasLimitTxt.getText().toString(), amountTxt.getText().toString());
        Log.d(TAG, "reading EUR/ETH price finished: " + transactionPriceBean);
        this.runOnUiThread(() -> {
            if (transactionPriceBean != null) {
                priceInEuroTxt.setText(transactionPriceBean.toString());
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }

        SharedPreferences mPrefs = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString(PREF_KEY_RECIPIENT_ADDRESS, recipientAddressTxt.getText().toString())
                .putString(PREF_KEY_GASPRICE_WEI, gasPriceTxt.getText().toString())
                .putString(PREF_KEY_GASLIMIT_SEND_ETH, gasLimitTxt.getText().toString())
                .putString(PREF_KEY_PIN, pinTxt.getText().toString())
                .apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
        activityPaused = false;
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        resolveIntent(intent);
    }

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
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        UiUtils.logTagInfo(tag);
        MifareUltralight isoDep = MifareUltralight.get(tag);
        if (isoDep == null) {
            showToast(getString(R.string.wrong_card), this);
            return;
        }

        if (toggleButton.isChecked()) {
            try {

                isoDep.connect();
                /*
                SharedPreferences pref = this.getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                String readRecipientAddress = NfcUtils.readPublicKeyOrCreateIfNotExists(IsoTagWrapper.of(isoDep),
                        pref.getInt(KEY_INDEX_OF_CARD, 1)).getPublicKeyInHexWithoutPrefix();

                 */
                String readRecipientAddress="";

                    //showToast("sasa1a", this);
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
                    //readRecipientAddress = keyPair.getPublicKey().toString(16);
                    //recipientPrivateKeyString = keyPair.getPrivateKey().toString(16);
                } else
                {
                    Credentials sasa = Credentials.create(sasakey2);
                    readRecipientAddress = sasa.getEcKeyPair().getPublicKey().toString(16);
                    recipientPrivateKeyString = sasa.getEcKeyPair().getPrivateKey().toString(16);
                }
                //showToast("sasa1c", this);
                //showToast("SASA222", this);
                if (sasaboolean==false) {
                    /*
                    sasakey = readRecipientAddress;

                    Integer sasaindex = ((int) readRecipientAddress.length() / 8) + 1;

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

                    sasakey = recipientPrivateKeyString;

                    sasaindex = ((int) recipientPrivateKeyString.length() / 8) + 1;

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

                     */

                    //showToast("A : "+sasakey.length(), this);
                }
                //readRecipientAddress=Keys.toChecksumAddress(Keys.getAddress(readRecipientAddress));

                Log.d(TAG, String.format("pubkey read from card: '%s'", readRecipientAddress));
                final String newAddress = Keys.toChecksumAddress(Keys.getAddress(readRecipientAddress));
                // use web3j to format this public key as ETH address
                showToast(String.format(getString(R.string.change_recipient_address), newAddress), this);
                recipientAddressTxt.setText(newAddress);
                toggleButton.toggle();

                isoDep.close();
            //} catch (IOException | NfcCardException e) {
            } catch (IOException e) {
                showToast(e.getMessage(), this);
                Log.e(TAG, "Exception while reading public key from card: ", e);
            //} catch (InvalidAlgorithmParameterException e) {
            //    e.printStackTrace();
            //} catch (CipherException e) {
            //    e.printStackTrace();
            //} catch (NoSuchAlgorithmException e) {
            //    e.printStackTrace();
            //} catch (NoSuchProviderException e) {
            //    e.printStackTrace();
            }
        } else {
            if (inputErrorUtils.isNoInputError()) {
                showToast(getString(R.string.hold_card_for_while), this);

                //sendTransactionAndShowFeedback(isoDep);
                new Thread(() -> sendTransactionAndShowFeedback(isoDep)).start();
                finish();
            }
        }
    }

    /**
     * Reads data needed for transaction, sends an Ethereum transaction and shows feedback on UI.
     *
     * @param isoDep
     */
    private Credentials getCredentialsFromPrivateKey() {
        return Credentials.create(MainActivity.privateKeyString);
    }

    private void sendTransactionAndShowFeedback(MifareUltralight isoDep) {
        final String valueStr = amountTxt.getText().toString();
        //showToast("sasa1a", this);
        //final BigDecimal value = Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER);
        //BigDecimal value = new BigDecimal(new BigInteger("5"));
        //value=value.divide(new BigDecimal(new BigInteger("100")));
        BigDecimal value = new BigDecimal(valueStr);
        //showToast("sasa1b", this);
        //BigInteger gasPrice = new BigInteger(gasPriceTxt.getText().toString());
        //BigInteger gasPrice = new BigInteger(gasPriceTxt.getText().toString().replace(".",""));
        BigInteger gasPrice = BigInteger.valueOf(20000000000L);
        //showToast("sasa1c", this);
        //gasPrice = gasPrice.multiply(new BigInteger(spinnerAdapter.getMultiplier().toString()));
        //final String gasLimitStr = gasLimitTxt.getText().toString();
        //final BigInteger gasLimit = new BigInteger(gasLimitStr.equals("") ? "0" : gasLimitStr);
        BigInteger gasLimit = BigInteger.valueOf(6721975L);
        //showToast("sasa1d", this);

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        byte chainId = ChainId.MAINNET;
        if (!pref.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
            chainId = 5;
        }

        //Pair<EthSendTransaction, GenerateSignatureResponseApdu> response = null;

        //EthSendTransaction response = null;
        try {
            Log.d(TAG, "sending ETH transaction..");

            //showToast("sasa1", this);

            //WebSocketService web3jService = new WebSocketService("wss://ropsten.infura.io/ws/v3/0a9b46c76be54c5a94de2c577b407401", true);
            //web3jService.connect();

            Web3j web3 = Web3j.build(new HttpService(UiUtils.getFullNodeUrl(this)));
            //Web3j web3 = Web3j.build(web3jService);

            //showToast(UiUtils.getFullNodeUrl(this), this);


            /*

            BigInteger value2 = Convert.toWei(valueStr, Convert.Unit.ETHER).toBigInteger();

            // Gas Parameters
            BigInteger gasLimit2 = new BigInteger(gasLimitTxt.getText().toString());
            BigInteger gasPrice2 = Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();

            Transaction transaction = new Transaction(
                    MainActivity.ethAddress, null, gasPrice2, gasLimit2, recipientAddressTxt.getText().toString(), value2, null);

            web3.ethSendTransaction(transaction)
                    .send();

            //showToast("sasa2", this);

            //TransactionManager transactionManager = new RawTransactionManager(
            //        web3,
            //        getCredentialsFromPrivateKey()
            //);

             */

            //showToast("sasa1", this);

            RemoteCall<TransactionReceipt> rc = Transfer.sendFundsEIP1559(
                    web3, getCredentialsFromPrivateKey(),
                    recipientAddressTxt.getText().toString(),
                    value,
                    //BigDecimal.ONE.valueOf(1),
                    Convert.Unit.ETHER,
                    //Transfer.GAS_LIMIT,
                    GAS_LIMIT.divide(new BigInteger("100")),
                    Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger(), //gasLimit
                    //DefaultGasProvider.GAS_LIMIT,//maxPriorityFeePerGas
                    Convert.toWei(gasLimitTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger()//maxFeePerGas//,
                    //Transfer.GAS_LIMIT,
                    //Convert.toWei("3000000000", Convert.Unit.WEI).toBigInteger(),
                    //Convert.toWei("100000000000", Convert.Unit.WEI).toBigInteger()  // 1 wei = 10^-18 Ether
            );

            //showToast("sasa2", this);

            TransactionReceipt receipt = rc.send();

            showToast(getString(R.string.send_success), this);

            //showToast("receipt="+ receipt.toString(), this);

            //try {
            /*
                String pk = MainActivity.privateKeyString; // Add a private key here

                // Decrypt and open the wallet into a Credential object
                Credentials credentials = Credentials.create(pk);
                //System.out.println("Account address: " + credentials.getAddress());
                //System.out.println("Balance: " + Convert.fromWei(web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance().toString(), Unit.ETHER));

                // Get the latest nonce
                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce =  ethGetTransactionCount.getTransactionCount();

                // Recipient address
                String recipientAddress = recipientAddressTxt.getText().toString();

                // Value to transfer (in wei)
                BigInteger value2 = Convert.toWei(valueStr, Convert.Unit.ETHER).toBigInteger();

                // Gas Parameters
                BigInteger gasLimit2 = new BigInteger(gasLimitTxt.getText().toString());
                BigInteger gasPrice2 = Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();

                // Prepare the rawTransaction
                RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
                        nonce,
                        gasPrice2,
                        gasLimit2,
                        recipientAddress,
                        value2);

                // Sign the transaction
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                // Send transaction
                EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).send();
                String transactionHash = ethSendTransaction.getTransactionHash();
                System.out.println("transactionHash: " + transactionHash);

                // Wait for transaction to be mined

                Optional<TransactionReceipt> transactionReceipt = null;
                do {
                    System.out.println("checking if transaction " + transactionHash + " is mined....");
                    EthGetTransactionReceipt ethGetTransactionReceiptResp = web3.ethGetTransactionReceipt(transactionHash).send();
                    transactionReceipt = ethGetTransactionReceiptResp.getTransactionReceipt();
                    Thread.sleep(3000); // Wait 3 sec
                } while(!transactionReceipt.isPresent());

             */



                //System.out.println("Transaction " + transactionHash + " was mined in block # " + transactionReceipt.get().getBlockNumber());
                //System.out.println("Balance: " + Convert.fromWei(web3.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance().toString(), Unit.ETHER));


            //} catch (IOException | InterruptedException ex) {
            //    throw new RuntimeException(ex);
            //}

            //showToast("sasa3", this);

            //Transfer transfer = new Transfer(web3,transactionManager);

            //showToast("sasa4", this);

            //TransactionReceipt transactionReceipt= transfer.sendFunds(recipientAddressTxt.getText().toString(), value, Convert.Unit.ETHER, gasPrice, gasLimit).send();

            //  showToast("sasa5", this);
            //Transaction transaction = new Transaction(
            //        ethAddress, null, gasPrice.toBigInteger(), gasLimit.toBigInteger(), recipientAddressTxt.getText().toString(), value.toBigInteger(), "");

            //response = web3.ethSendTransaction(transaction)
            //       .send();
/*
            response = EthereumUtils.sendTransaction(gasPrice.toBigInteger(),
                    gasLimit.toBigInteger(), ethAddress, recipientAddressTxt.getText().toString(),
                    value.toBigInteger(), isoDep, pubKeyString, "", UiUtils.getFullNodeUrl(this), chainId,
                    1, null);

 */
            //Log.d(TAG, String.format("sending ETH transaction finished with Hash: %s", response.getTransactionHash()));
/*
            if (transfer.getError() != null) {
                showToast(response.getError().getMessage(), this);
            } else {
                showToast(getString(R.string.send_success), this);
            }
*/

        } catch (IOException e) {
            showToast(e.getMessage(), this);
            Log.e(TAG, "IOException while sending ether transaction", e);
        } catch (Exception e) {
            showToast(e.getMessage(), this);
            Log.e(TAG, "Exception while sending ether transaction", e);
        } finally {
            //if (response != null && (response.second.getGlobalSigCounterAsInteger() < WARNING_SIG_COUNTER ||
            //        response.second.getSigCounterAsInteger() < WARNING_SIG_COUNTER)) {
            //    showToast(String.format(getString(R.string.signature_counter_below), WARNING_SIG_COUNTER), this);
            //}
        }
    }

    public void scanQrCode(View view) {
        QrCodeScanner.scanQrCode(this, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == 0) {
                    recipientAddressTxt.setText(UriUtils.extractEtherAddressFromUri(data.getStringExtra("SCAN_RESULT")));
                }
            } catch (InvalidEthereumAddressException e) {
                Log.e(TAG, "Exception on reading ethereum address", e);
                showToast(getString(R.string.invalid_ethereum_address), this);
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "QR Code scanning canceled.");
        }
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

    public void onSendAll(View view) {
        new Thread(() -> {
            try {
                EthBalanceBean balance = EthereumUtils.getBalance(ethAddress, UiUtils.getFullNodeUrl(this));
                final BigDecimal ethBalanceInWei = Convert.toWei(balance.getEther(), Convert.Unit.ETHER);
                //final BigDecimal gasPrice = new BigDecimal(
                //        gasPriceTxt.getText().toString().equals("") ? "0" : gasPriceTxt.getText().toString())
                //        .multiply(spinnerAdapter.getMultiplier());
                final BigDecimal gasLimit = new BigDecimal(
                        gasLimitTxt.getText().toString().equals("") ? "0" : gasLimitTxt.getText().toString());

                //this.runOnUiThread(() -> amountTxt.setText(
                //        Convert.fromWei(ethBalanceInWei.subtract(gasPrice.multiply(gasLimit)), Convert.Unit.ETHER).toPlainString()));
            } catch (Exception e) {
                Log.e(TAG, "exception while reading eth balance from api: ", e);
            }
        }).start();
    }
}
