package co.coinfinity.infineonandroidapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import co.coinfinity.infineonandroidapp.infineon.exceptions.NfcCardException;
import co.coinfinity.infineonandroidapp.utils.IsoTagWrapper;
import co.coinfinity.infineonandroidapp.utils.UiUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static android.app.PendingIntent.getActivity;
import static co.coinfinity.AppConstants.TAG;
import static co.coinfinity.infineonandroidapp.infineon.NfcUtils.initializePinAndReturnPuk;
import static co.coinfinity.infineonandroidapp.utils.ByteUtils.bytesToHex;
import static co.coinfinity.infineonandroidapp.utils.UiUtils.showToast;

import androidx.appcompat.app.AppCompatActivity;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

/**
 * Activity class used for setting PIN functionality.
 */
public class SetPinActivity extends AppCompatActivity {

    @BindView(R.id.pin)
    TextView pin;

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private String pubKeyString;
    private String privateKeyString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin);
        ButterKnife.bind(this);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Create a generic PendingIntent that will be deliver to this activity. The NFC stack
        // will fill in the intent with the details of the discovered tag before delivering to
        // this activity.
        pendingIntent = getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0 | PendingIntent.FLAG_MUTABLE);
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
        setIntent(intent);
        resolveIntent(intent);
    }

    /* Will be called after card was hold to back of device.
     *
     * @param intent includes nfc extras
     */

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
            showToast(getString(R.string.wrong_card), this);
            return;
        }

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

                //pubKeyString = sasakey;
                //privateKeyString = sasakey2;
                //showToast("sasa1c", this);

                pin.setText(privateKeyString);

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



            //showToast("A : "+sasakey.length(), this);
            /*
            if (NfcUtils.generateKeyFromSeed(IsoTagWrapper.of(isoDep), ByteUtils.fromHexString(seed.getText().toString()), pinTxt.getText().toString().getBytes(StandardCharsets.UTF_8))) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.generate_from_seed)
                        .setMessage(String.format(getString(R.string.generate_from_seed_message), seed.getText()))
                        .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                        .show();
            }

             */
            isoDep.close();
            // PIN in the card simply is a byte[] array, so for this DEMO we just take the bytes
            // of the entered String

            // the returned PUK is also a byte[], so we display it in hexadecimal
            // representation to the user
            /*
            final String puk = bytesToHex(initializePinAndReturnPuk(IsoTagWrapper.of(isoDep),
                    pin.getText().toString().getBytes(StandardCharsets.UTF_8)));

            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                    .setTitle(R.string.set_pin)
                    .setMessage(String.format(getString(R.string.set_pin_message), pin.getText(), puk))
                    .setPositiveButton(R.string.copy_puk_to_clipboard, ((dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(getString(R.string.puk_code), puk);
                        clipboard.setPrimaryClip(clip);
                        UiUtils.showToast(getString(R.string.puk_copied), SetPinActivity.this);
                    }));
                    //.setPositiveButton(R.string.ok, (dialog, which) -> finish());
            alert.show();
             */
        } catch (IOException | IllegalArgumentException e) {
            showToast(e.getMessage(), this);
            Log.e(TAG, "Exception while setting PIN", e);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }
}
