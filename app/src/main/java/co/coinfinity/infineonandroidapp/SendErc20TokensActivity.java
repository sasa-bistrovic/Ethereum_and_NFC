package co.coinfinity.infineonandroidapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import co.coinfinity.infineonandroidapp.adapter.UnitSpinnerAdapter;
import co.coinfinity.infineonandroidapp.ethereum.contract.SimpleStorage;
import co.coinfinity.infineonandroidapp.ethereum.exceptions.InvalidEthereumAddressException;
import co.coinfinity.infineonandroidapp.ethereum.utils.Erc20Utils;
import co.coinfinity.infineonandroidapp.ethereum.utils.UriUtils;
import co.coinfinity.infineonandroidapp.qrcode.QrCodeScanner;
import co.coinfinity.infineonandroidapp.utils.InputErrorUtils;
import co.coinfinity.infineonandroidapp.utils.UiUtils;

//import org.web3j.codegen.TruffleJsonFunctionWrapperGenerator;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.datatypes.generated.Uint256;
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
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
//import org.web3j.tx.Contract;
//import org.web3j.tx.Contract;
import org.web3j.tx.ChainId;
import org.web3j.tx.Contract;
import org.web3j.tx.FastRawTransactionManager;
import org.web3j.tx.ManagedTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.tx.gas.ContractEIP1559GasProvider;
import org.web3j.tx.response.NoOpProcessor;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
//import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import static org.web3j.tx.Contract.GAS_LIMIT;
import static org.web3j.tx.ManagedTransaction.GAS_PRICE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.getActivity;
import static co.coinfinity.AppConstants.*;
import static co.coinfinity.infineonandroidapp.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import co.coinfinity.infineonandroidapp.ethereum.contract.SimpleStorage;

/**
 * Activity class used for ER20 Token functionality.
 */
public class SendErc20TokensActivity extends AppCompatActivity {

    private String pubKeyString;
    private String privateKeyString;
    private String ethAddress;
    public static BigDecimal ethvalue;

    @BindView(R.id.recipientAddress)
    TextView recipientAddressTxt;
    @BindView(R.id.amount)
    TextView amountTxt;
    @BindView(R.id.gasPrice)
    TextView gasPriceTxt;
    @BindView(R.id.gasLimit)
    TextView gasLimitTxt;
    @BindView(R.id.contractAddress)
    TextView contractAddress;
    @BindView(R.id.currentBalance)
    TextView currentBalance;
    @BindView(R.id.textViewInfo)
    TextView infoTxt;
    @BindView(R.id.pin)
    TextView pinTxt;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.spinner)
    Spinner spinner;

    private InputErrorUtils inputErrorUtils;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    private volatile boolean activityPaused = false;

    private UnitSpinnerAdapter spinnerAdapter = new UnitSpinnerAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_erc20_tokens);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        spinnerAdapter.addSpinnerAdapter(this, spinner);
        inputErrorUtils = new InputErrorUtils(this, recipientAddressTxt, amountTxt, gasPriceTxt,
                gasLimitTxt, contractAddress);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            pubKeyString = b.getString("pubKey");
            ethAddress = b.getString("ethAddress");
        }

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        final String savedContractAddress = pref.getString(PREF_KEY_ERC20_CONTRACT_ADDRESS, DEFAULT_ERC20_CONTRACT_ADDRESS);
        contractAddress.setText(savedContractAddress.trim().isEmpty() ? DEFAULT_ERC20_CONTRACT_ADDRESS : savedContractAddress);
        recipientAddressTxt.setText(pref.getString(PREF_KEY_ERC20_RECIPIENT_ADDRESS,
                ""));
        //gasPriceTxt.setText(pref.getString(PREF_KEY_GASPRICE_WEI, DEFAULT_GASPRICE_IN_GIGAWEI));
        //gasLimitTxt.setText(pref.getString(PREF_KEY_ERC20_GASLIMIT, DEFAULT_GASLIMIT));
        amountTxt.setText(pref.getString(PREF_KEY_ERC20_AMOUNT, "1"));
        //pinTxt.setText(pref.getString(PREF_KEY_PIN, ""));

        new Thread(() -> {
            try {
                while (!activityPaused) {
                    readAndDisplayErc20Balance();
                    TimeUnit.SECONDS.sleep(FIVE_SECONDS);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted exception while reading ERC20 Balance", e);
            }
        }).start();
    }

    /**
     * this method read ERC20 balance via Api request and displays it.
     */
    public void readAndDisplayErc20Balance() {
        BigDecimal erc20BalanceIn10e18Units = BigDecimal.ZERO;
        try {
            Log.d(TAG, "reading ERC20 Balance..");
            BigInteger erc20Balance = Erc20Utils.getErc20Balance(contractAddress.getText().toString(), ethAddress,
                    UiUtils.getFullNodeUrl(this));
            erc20BalanceIn10e18Units = Convert.fromWei(erc20Balance.toString(10), Convert.Unit.ETHER);
            Log.d(TAG, String.format("got ERC20 Balance: %s", erc20BalanceIn10e18Units.toPlainString()));
        } catch (Exception e) {
            Log.e(TAG, "exception while reading ERC20 Balance", e);
        }
        BigDecimal finalErc20Balance = erc20BalanceIn10e18Units;
        this.runOnUiThread(() -> {
            infoTxt.setText(R.string.hold_card_payment);
            currentBalance.setText(String.format(getString(R.string.current_token_balance), finalErc20Balance.toPlainString()));
            progressBar.setVisibility(View.INVISIBLE);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        activityPaused = true;
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }

        SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor
                .putString(PREF_KEY_ERC20_CONTRACT_ADDRESS, contractAddress.getText().toString())
                .putString(PREF_KEY_ERC20_RECIPIENT_ADDRESS, recipientAddressTxt.getText().toString())
                //.putString(PREF_KEY_ERC20_GASLIMIT, gasLimitTxt.getText().toString())
                .putString(PREF_KEY_ERC20_AMOUNT, amountTxt.getText().toString())
                //.putString(PREF_KEY_GASPRICE_WEI, gasPriceTxt.getText().toString())
                //.putString(PREF_KEY_PIN, pinTxt.getText().toString())
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
        if (inputErrorUtils.isNoInputError()) {
            showToast(getString(R.string.hold_card_for_while), this);
            resolveIntent(intent);
        }
    }

    /**
     * will be called after card was hold to back of device.
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

        final String valueStr = amountTxt.getText().toString();
        ethvalue = new BigDecimal(valueStr);
        //BigDecimal amountInTokenBaseUnit = Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER);
        //BigDecimal amountInTokenBaseUnit = new BigDecimal("10000000").add((new BigDecimal("21000")).multiply((new BigDecimal("10")).add(new BigDecimal("1"))));
        BigDecimal amountInTokenBaseUnit =  new BigDecimal("10000000000000000");
        //BigDecimal gasPrice = new BigDecimal(gasPriceTxt.getText().toString());
        //gasPrice = gasPrice.multiply(spinnerAdapter.getMultiplier());
        //final String gasLimitStr = gasLimitTxt.getText().toString();
        //final String gasPriceStr = gasPriceTxt.getText().toString();
        //final BigDecimal gasLimit = Convert.toWei(gasLimitStr.equals("") ? "0" : gasLimitStr, Convert.Unit.WEI);
        //final BigDecimal gasLimit = new BigDecimal("99606");

        //BigDecimal finalGasPrice = gasPrice;

        ///BigDecimal finalGasPrice = new BigDecimal("1500000010");;

        new Thread(() -> {
            try {
                isoDep.connect();

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
                    /*
                    keyPair = Keys.createEcKeyPair();
                    WalletFile wallet = Wallet.createLight(password, keyPair);
                    pubKeyString = keyPair.getPublicKey().toString(16);
                    privateKeyString = keyPair.getPrivateKey().toString(16);

                     */
                } else
                {
                    Credentials sasa = Credentials.create(sasakey2);
                    pubKeyString = sasa.getEcKeyPair().getPublicKey().toString(16);
                    privateKeyString = sasa.getEcKeyPair().getPrivateKey().toString(16);
                    //pubKeyString = sasakey;
                    //privateKeyString = sasakey2;
                }
                //showToast("sasa1c", this);

                if (sasaboolean==false) {
                    /*
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
                     */

                    //showToast("A : "+sasakey.length(), this);
                }
                isoDep.close();

                SharedPreferences pref = getSharedPreferences(PREFERENCE_FILENAME, Context.MODE_PRIVATE);
                byte chainId = ChainId.MAINNET;
                if (!pref.getBoolean(PREF_KEY_MAIN_NETWORK, true)) {
                    chainId = 5;
                }

                //showToast("SASA : "+chainId, this);

                Web3j web3 =  Web3j.build(new HttpService(UiUtils.getFullNodeUrl(this))); // for ropsten test network


                // Connect to the node
                System.out.println("Connecting to Ethereum ...");
                //Web3j web3j = Web3j.build(new HttpService("http://localhost:8545"));
                System.out.println("Successfuly connected to Ethereum");

// Load an account
                String pk = MainActivity.privateKeyString;
                Credentials credentials = Credentials.create(pk);

// Contract and functions
                String contractAddress2 = contractAddress.getText().toString();

                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();

                TransactionManager txManager = new RawTransactionManager(web3, credentials);

                //showToast("SASA1", this);

                //showToast("SASA1", this);

                byte finalChainId = chainId;
                ContractGasProvider contractGasProvider = new ContractEIP1559GasProvider() {
                    @Override
                    public boolean isEIP1559Enabled() {
                        return true;
                    }

                    @Override
                    public long getChainId() {
                        return finalChainId;
                    }

                    @Override
                    public BigInteger getMaxFeePerGas(String contractFunc) {
                        return Convert.toWei(gasLimitTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();
                    }

                    @Override
                    public BigInteger getMaxPriorityFeePerGas(String contractFunc) {
                        return Convert.toWei(gasPriceTxt.getText().toString(), Convert.Unit.GWEI).toBigInteger();
                    }

                    @Override
                    public BigInteger getGasPrice(String contractFunc) {
                        return GAS_PRICE.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasPrice() {
                        return GAS_PRICE.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasLimit(String contractFunc) {
                        return GAS_LIMIT.divide(new BigInteger("100"));
                    }

                    @Override
                    public BigInteger getGasLimit() {
                        return GAS_LIMIT.divide(new BigInteger("100"));
                    }
                };

                //showToast("SASA2", this);

                Function function = new Function("set", // Function name
                        Arrays.asList(new Uint(BigInteger.valueOf(20))), // Function input parameters
                        Collections.emptyList()); // Function returned parameters

                //showToast("SASA2", this);

//Encode function values in transaction data format
                String txData = FunctionEncoder.encode(function);

                BigInteger initVal = BigInteger.valueOf(42);

                //showToast("SASA3", this);

                SimpleStorage contract = SimpleStorage.load(
                        contractAddress2, web3, txManager, contractGasProvider);

                //showToast("SASA4", this);

                contract.transfer(recipientAddressTxt.getText().toString(), Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER).toBigInteger()).send();

                //contract = SimpleStorage.deploy(
                //        web3, txManager, contractGasProvider, initVal).send();

                //showToast("SASA5", this);

                ///contract.set(initVal).send();

                showToast(getString(R.string.send_success), this);

/*
                ContractGasProvider contractGasProvider = new StaticGasProvider(DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT);

                RemoteCall<SimpleStorage> simpleStorage = SimpleStorage.deploy(
                        web3, credentials, new BigInteger("22"), new BigInteger("4300000"));  // ether value of contract

                RemoteCall<TransactionReceipt> transactionReceipt = simpleStorage.send().set(new BigInteger("20"));

                RemoteCall<BigInteger> result = simpleStorage.send().get();
*/
/*
                BigInteger gasLimit = BigInteger.valueOf(71000); // you should get this from api
                BigInteger gasPrice = new BigInteger("d693a400", 16); // decimal 3600000000


                RawTransaction rawTransaction =
                        RawTransaction.createTransaction(
                                nonce, DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT, contractAddress2, Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER).toBigIntegerExact(), txData);

 */
                // create the transaction
                /*
                RawTransaction rawTransaction =
                        RawTransaction.createTransaction(
                                chainId,
                nonce, Transfer.GAS_LIMIT, contractAddress2, Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER).toBigIntegerExact(), txData,
                                BigInteger.valueOf(2500000000L),  // 1 wei = 10^-18 Ether
                                BigInteger.valueOf(25000000000L));

                 */

                // sign the transaction
                /*
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);

                // Send transaction
                EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).send();
                String transactionHash = ethSendTransaction.getTransactionHash();

                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(
                        web3,
                        TransactionManager.DEFAULT_POLLING_FREQUENCY,
                        TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);

                TransactionReceipt txReceipt = receiptProcessor.waitForTransactionReceipt(transactionHash);
*/
                //PollingPrivateTransactionReceiptProcessor processor = new PollingPrivateTransactionReceiptProcessor(this, 1000, 15);
                //PrivateTransactionReceipt receipt = processor.waitForTransactionReceipt(transactionHash);

                //showToast(getString(R.string.send_success), this);
                /*

                //showToast("SASA3", this);

// RawTransactionManager use a wallet (credential) to create and sign transaction

                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(
                        web3,
                        TransactionManager.DEFAULT_POLLING_FREQUENCY,
                        TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);

                TransactionManager txManager = new RawTransactionManager(web3, credentials, ChainId.ROPSTEN, receiptProcessor);

                //showToast("SASA4", this);

// Send transaction
                String txHash = txManager.sendTransaction(
                        //chainId,
                        DefaultGasProvider.GAS_PRICE,
                        DefaultGasProvider.GAS_LIMIT,
                        //BigInteger.valueOf(2500000000L),  // 1 wei = 10^-18 Ether
                        //BigInteger.valueOf(25000000000L),
                        //Transfer.GAS_LIMIT,
                        contractAddress2,
                        txData,
                        Convert.toWei(valueStr.equals("") ? "0" : valueStr, Convert.Unit.ETHER).toBigIntegerExact()
                ).getTransactionHash();


                //Optional<TransactionReceipt> transactionReceipt =
                //        web3.ethGetTransactionReceipt(txHash).send().getTransactionReceipt();

                //if(transactionReceipt.isPresent())
                //    showToast(getString(R.string.send_success), this);
                //else
                //    showToast("SASA ERROR", this);
                //showToast("SASA5", this);

// Wait for transaction to be mined
                //EthGetTransactionReceipt transReceipt = web3.ethGetTransactionReceipt(txHash).send();

                TransactionReceiptProcessor receiptProcessor = new PollingTransactionReceiptProcessor(
                        web3,
                        TransactionManager.DEFAULT_POLLING_FREQUENCY,
                        TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);

                TransactionReceipt txReceipt = receiptProcessor.waitForTransactionReceipt(txHash);

                showToast(getString(R.string.send_success), this);

                 */


                /*
                // load private key into eckey to sign
                String privatekey = "***********************************";
                BigInteger privkey = new BigInteger(MainActivity.privateKeyString, 16);
                ECKeyPair ecKeyPair = ECKeyPair.create(privkey);
                Credentials credentials = Credentials.create(ecKeyPair);
                NoOpProcessor processor = new NoOpProcessor(web3);

                //deploy new contract
                TransactionManager txManager = new RawTransactionManager(web3, credentials, processor);

                RemoteCall<SimpleStorage> request = SimpleStorage.deploy(web3, txManager, DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT);
                SimpleStorage token = request.send();
                String contractAddress2 = token.getDeployedAddress("3"); // 3 is ropsten testnet

                // load existing contract by address
                // ERC20 token = ERC20.load(contractAddress, web3j, txManager, DefaultGasProvider.GAS_PRICE, DefaultGasProvider.GAS_LIMIT);


                // create transaction transfer token to receiver
                //String receiver = contractAddress.getText().toString();
                BigInteger value = Convert.toWei(valueStr, Convert.Unit.ETHER).toBigIntegerExact();
                TransactionReceipt receipt = token.transfer(recipientAddressTxt.getText().toString(), value).send();
                // get transaction result
                System.out.println(receipt.getTransactionHash());

                 */
/*
//                Credentials credentials = Credentials.create(MainActivity.privateKeyString);
                EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                final BigInteger transferAmountWei = Convert.toWei(valueStr, Convert.Unit.ETHER).toBigIntegerExact();// you can provide yourself how much you want to send
                ethAddress = MainActivity.ethAddress;
                Function function = new Function(
                        "transfer",
                        Arrays.asList(new Address(ethAddress),
                                new Uint256(transferAmountWei)),
                        Collections.singletonList(new TypeReference<Bool>() {
                        }));;
                String encodedFunction = FunctionEncoder.encode(function);

                //EthGasPrice ethGasPrice = web3.ethGasPrice().send();

                //EthGasLimit ethGasLimit = web3.ethGasPrice().send();

                BigInteger gasLimit = new BigInteger(gasLimitStr); // you can customize these
                BigInteger gasPrice = new BigInteger(gasPriceStr); //
                RawTransaction rawTransaction =
                        RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress.getText().toString(), encodedFunction);
                byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                String hexValue = Numeric.toHexString(signedMessage);
                EthSendTransaction ethSendTransaction = web3.ethSendRawTransaction(hexValue).sendAsync().get();
                String transactionHash = ethSendTransaction.getTransactionHash();
*/

                /*
                if(transactionReceipt.isEmpty())
                    receipt = null;
                else
                    receipt = transactionReceipt.get();

                 */

                /*
                Log.d(TAG, "Sending ERC20 tokens: FROM: " + ethAddress + ", TO: " + recipientAddressTxt.getText().toString() +
                        ", erc-20 contract: " + contractAddress.toString() + ", gasPriceInWei: " + finalGasPrice.toPlainString() +
                        ", gasLimit: " + gasLimit.toBigInteger() + " amountInBaseUnit: " +
                        amountInTokenBaseUnit.toBigInteger().toString());
                showToast("Sending ERC20 tokens: FROM: " + ethAddress + ", TO: " + recipientAddressTxt.getText().toString() +
                        ", erc-20 contract: " + contractAddress.toString() + ", gasPriceInWei: " + finalGasPrice.toPlainString() +
                        ", gasLimit: " + gasLimit.toBigInteger() + " amountInBaseUnit: " +
                        amountInTokenBaseUnit.toBigInteger().toString(),this);

                 */


                //final TransactionReceipt receipt = Erc20Utils.sendErc20Tokens(contractAddress.getText().toString(),
                //        isoDep, pubKeyString, MainActivity.ethAddress, recipientAddressTxt.getText().toString(),
                //        amountInTokenBaseUnit.toBigInteger(), finalGasPrice.toBigInteger(),
                //        gasLimit.toBigInteger(), this, UiUtils.getFullNodeUrl(this));
                //final TransactionReceipt receipt = Erc20Utils.sendErc20Tokens(contractAddress.getText().toString(),
                //        isoDep, pubKeyString, MainActivity.ethAddress, "0xE8A56a4390ff7A01Bd9e5d5164A8746E54d0F9d2",
                //        amountInTokenBaseUnit.toBigInteger(), finalGasPrice.toBigInteger(),
                //        gasLimit.toBigInteger(), this, UiUtils.getFullNodeUrl(this));

                //Web3j web3j = Web3j.build(new HttpService());

//                Credentials credentials = Credentials.create(MainActivity.privateKeyString);

                //log.info("Connected to Ethereum client version: "
                //        + web3j.web3ClientVersion().send().getWeb3ClientVersion());
                //Credentials credentials =
                //        WalletUtils.loadCredentials(
                //                "password",
                //                "chaindata\\keystore\\UTC--2018-06-21T06-34-32.658490800Z--5ade9a7f8f57ab3995ac4d56c78a22649d3b1686");
                //log.info("Credentials loaded");
                //log.info("Sending Ether ..");
/*
                TransactionReceipt transferReceipt = Transfer.sendFunds(
                        web3, credentials,
                        contractAddress.getText().toString(),  // you can put any address here
                        new BigDecimal(valueStr), Convert.Unit.ETHER)  // 1 wei = 10^-18 Ether
                        .send();

                        SimpleStorage contract = SimpleStorage.deploy(
                        web3, credentials,
                        ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT).send();
                Log.d(TAG, String.format("ERC20 tokens sent with Hash: %s", transferReceipt.getTransactionHash()));
*/

            } catch (Exception e) {
                showToast("ERROR : "+e.getMessage(), this);
                Log.e(TAG, "Exception while sending ERC20 tokens", e);
            }
        }).start();

        finish();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == 0) {
                    contractAddress.setText(UriUtils.extractEtherAddressFromUri(
                            data.getStringExtra("SCAN_RESULT")));
                } else if (requestCode == 1) {
                    recipientAddressTxt.setText(UriUtils.extractEtherAddressFromUri(
                            data.getStringExtra("SCAN_RESULT")));
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

    public void onScanContract(View view) {
        QrCodeScanner.scanQrCode(this, 0);
    }

    public void onScanRecipient(View view) {
        QrCodeScanner.scanQrCode(this, 1);
    }
}
